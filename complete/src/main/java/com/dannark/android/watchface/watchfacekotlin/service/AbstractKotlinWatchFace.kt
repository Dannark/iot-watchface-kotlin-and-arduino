/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dannark.android.watchface.watchfacekotlin.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.rendering.ComplicationDrawable
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.util.SparseArray
import android.view.SurfaceHolder
import androidx.core.content.res.ResourcesCompat
import com.dannark.android.watchface.watchfacekotlin.R
import com.dannark.android.watchface.watchfacekotlin.model.DigitalWatchFaceStyle
import com.dannark.android.watchface.watchfacekotlin.model.WatchFaceBackgroundImage
import com.google.firebase.database.FirebaseDatabase
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


/**
 * Updates rate in milliseconds for interactive mode. We update once a second to advance the
 * second hand.
 */
private const val INTERACTIVE_UPDATE_RATE_MS = 1000

/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0

abstract class AbstractKotlinWatchFace : CanvasWatchFaceService() {

    private lateinit var digitalWatchFaceStyle: DigitalWatchFaceStyle

    abstract fun getWatchFaceStyle():DigitalWatchFaceStyle

    override fun onCreateEngine(): Engine {
        Log.i("watchfacekotlin", "onCreateEngine")
        return Engine()
    }

    private class EngineHandler(reference: AbstractKotlinWatchFace.Engine) : Handler() {
        private val weakReference: WeakReference<AbstractKotlinWatchFace.Engine> =
            WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = weakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private lateinit var calendar: Calendar

        private var registeredTimeZoneReceiver = false
        private var muteMode: Boolean = false
        private var centerX: Float = 0F
        private var centerY: Float = 0F

        private var width: Int = 0
        private var height: Int = 0

        private var batteryPct: Float = 0.0f

        private var secondHandLengthRatio: Float = 0F
        private var minuteHandLengthRatio: Float = 0F
        private var hourHandLengthRatio: Float = 0F


        private lateinit var backgroundPaint: Paint
        // Best practice is to always use black for watch face in ambient mode (saves battery
        // and prevents burn-in.
        private val backgroundAmbientPaint:Paint = Paint().apply { color = Color.BLACK }

        private var backgroundImageEnabled:Boolean = false
        private lateinit var backgroundBitmap: Bitmap
        private lateinit var grayBackgroundBitmap: Bitmap

        private var ambient: Boolean = false
        private var lowBitAmbient: Boolean = false
        private var burnInProtection: Boolean = false

        /* Handler to update the time once a second in interactive mode. */
        private val updateTimeHandler = EngineHandler(this)


        private val timeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i("watchfacekotlin", "onReceive")
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            Log.i("watchfacekotlin", "onCreate")
            digitalWatchFaceStyle = getWatchFaceStyle()

            setWatchFaceStyle(
                    WatchFaceStyle.Builder(this@AbstractKotlinWatchFace)
                            .setAcceptsTapEvents(true)
                            .build()
            )

            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                baseContext.registerReceiver(null, ifilter)
            }

            batteryPct = batteryStatus?.let { intent ->
                val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                level * 100 / scale.toFloat()
            }!!

            calendar = Calendar.getInstance()

            initializeBackground()
            getRandomBackgroundColor()
        }

        private fun initializeBackground() {
            backgroundImageEnabled =
                    digitalWatchFaceStyle.watchFaceBackgroundImage.backgroundImageResource !=
                    WatchFaceBackgroundImage.EMPTY_IMAGE_RESOURCE

            if (backgroundImageEnabled) {
                backgroundBitmap = BitmapFactory.decodeResource(
                        resources,
                        digitalWatchFaceStyle.watchFaceBackgroundImage.backgroundImageResource
                )
            }
        }

        private fun getRandomBackgroundColor() {
            Log.i("watchfacekotlin", "getRandomBackgroundColor")

            val rnds = (0..digitalWatchFaceStyle.watchFaceColors.background.size-1).random()
            backgroundPaint = Paint().apply {
                color = digitalWatchFaceStyle.watchFaceColors.background[rnds]
            }
        }

        override fun onDestroy() {
            Log.i("watchfacekotlin", "onDestroy")
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            lowBitAmbient = properties.getBoolean(
                    WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
            )
            burnInProtection = properties.getBoolean(
                    WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
            )
        }

        override fun onTimeTick() {
            Log.i("watchfacekotlin", "onTimeTick")
            super.onTimeTick()
            invalidate()

            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                baseContext.registerReceiver(null, ifilter)
            }

            batteryPct = batteryStatus?.let { intent ->
                val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                level * 100 / scale.toFloat()
            }!!

            getRandomBackgroundColor()

            Log.i("watchfacekotlin", "level ${batteryPct}")
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            ambient = inAmbientMode
            Log.i("watchfacekotlin", "onAmbientModeChanged= " + ambient)

            updateWatchHandStyle()

            // Check and trigger whether or not timer should be running (only
            // in active mode).
            updateTimer()
        }

        private fun updateWatchHandStyle() {
            ambient = false
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            super.onInterruptionFilterChanged(interruptionFilter)
            val inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE
            Log.i("watchfacekotlin", "onInterruptionFilterChanged ${interruptionFilter}")

            /* Dim display in mute mode. */
            if (muteMode != inMuteMode) {
                muteMode = inMuteMode
                invalidate()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.i("watchfacekotlin", "onSurfaceChanged")

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            centerX = width / 2f
            centerY = height / 2f
            this.width = width
            this.height = height

            if (backgroundImageEnabled) {
                // Scale loaded background image (more efficient) if surface dimensions change.
                val scale = width.toFloat() / backgroundBitmap.width.toFloat()

                backgroundBitmap = Bitmap.createScaledBitmap(
                        backgroundBitmap,
                        (backgroundBitmap.width * scale).toInt(),
                        (backgroundBitmap.height * scale).toInt(), true
                )

            }
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            calendar.timeInMillis = now

            drawBackground(canvas)
            drawWatchFace(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            canvas.drawColor(backgroundPaint.color)

            if (ambient && burnInProtection) {
                canvas.drawColor(backgroundAmbientPaint.color)

            } else if (ambient && backgroundImageEnabled) {
                canvas.drawBitmap(grayBackgroundBitmap, 0f, 0f, backgroundAmbientPaint)

            } else if (backgroundImageEnabled) {
                canvas.drawBitmap(backgroundBitmap, 0f, 0f, backgroundPaint)

            } else {

            }

        }

        private fun drawWatchFace(canvas: Canvas) {

            val customTypeface = ResourcesCompat.getFont(baseContext, R.font.msyi)

            val hourPaint = Paint()
            hourPaint.textAlign = Paint.Align.CENTER
            hourPaint.color = Color.WHITE
            hourPaint.textSize = 72f
            hourPaint.typeface = customTypeface

            val secondaryPaint = Paint()
            secondaryPaint.textAlign = Paint.Align.CENTER
            secondaryPaint.color = Color.GRAY
            secondaryPaint.textSize = 22f

            val thirdPaint = Paint()
            thirdPaint.textAlign = Paint.Align.CENTER
            thirdPaint.color = Color.LTGRAY
            thirdPaint.textSize = 15f


            var dayOfWeek = "${calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())}"

            val date = Date()
            val timeFormat: DateFormat = SimpleDateFormat("HH:mm")
            val timeformatted: String = timeFormat.format(date)

            val dateFormat: DateFormat = SimpleDateFormat("dd/MM")
            val dateformatted: String = dateFormat.format(date)

            canvas.drawText(timeformatted, centerX, (height - hourPaint.ascent()) / 2, hourPaint);

            canvas.drawText(dayOfWeek, centerX, centerY + 62f, secondaryPaint)

            canvas.drawText(dateformatted, centerX, centerY - 30f, secondaryPaint)

            //Light
            var isLightOn = if (lightState == 1) "Off" else "On"
            canvas.drawText("${isLightOn}", width * 0.65f, height * 0.9f, thirdPaint)

            //Battery
            canvas.drawText("${batteryPct.toInt()}%", width * 0.2f, height * 0.43f, thirdPaint)

            //
            var isFanOn = if (fanState == 1) "On" else "Off"
            canvas.drawText("${isFanOn}", width * 0.83f, height * 0.71f, thirdPaint)


            canvas.drawText("0", width * 0.36f, height * 0.25f, thirdPaint)
        }

        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            super.onTapCommand(tapType, x, y, eventTime)

            var deltaX = x.toFloat() / width
            var deltaY = y.toFloat() / height
            Log.i("watchfacekotlin", "tapType= ${tapType}, x= ${deltaX}, y= ${deltaY}")

            if(tapType == 2) {
                if (deltaX < 0.23f && y > centerY && deltaY < 0.71) {
                    openApp("com.whatsapp")

                }
                if (deltaX > 0.7f && y > centerY && deltaY < 0.71) {
                    switchFan()
                }
                if (deltaX > 0.23f && x < centerX && y > height * 0.71) {
                    openApp("com.ubercab")
                }
                if (x > centerX && deltaX < 0.7f && y > height * 0.71) {
                    switchLight()
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.i("watchfacekotlin", "onVisibilityChanged; " + visible)
            if (visible) {
                registerReceiver()
                /* Update time zone in case it changed while we weren't visible. */
                calendar.timeZone = TimeZone.getDefault()
                invalidate()


            } else {
                unregisterReceiver()
            }

            Log.i("watchfacekotlin", "updating time...")
            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer()
        }

        private fun registerReceiver() {
            Log.i("watchfacekotlin", "registerReceiver")
            if (registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@AbstractKotlinWatchFace.registerReceiver(timeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            Log.i("watchfacekotlin", "unregisterReceiver")
            if (!registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = false
            this@AbstractKotlinWatchFace.unregisterReceiver(timeZoneReceiver)
        }

        /**
         * Starts/stops the [.updateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.updateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !ambient
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }
    }

    var lightState = 1
    var fanState = 1
    private fun switchLight() {
        Log.d("watchfacekotlin", "connecting Database.... pin0")

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("users/Daniel/ESP01-A/pin0")

        lightState = 1 - lightState // the most beautiful way to switch light state
        myRef.setValue(lightState)
    }

    private fun switchFan() {
        Log.d("watchfacekotlin", "connecting Database.... pin2")
        //Toast.makeText(applicationContext, "Ligar/Desligar Luz", Toast.LENGTH_SHORT)

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("users/Daniel/ESP01-A/pin2")

        fanState = 1 - fanState // the most beautiful way to switch light state
        myRef.setValue(fanState)
    }

    private fun openApp(app: String){
//        val url = "https://api.whatsapp.com/send?phone=$number"
//        val i = Intent(Intent.ACTION_VIEW)
//        i.data = Uri.parse(url)
//        startActivity(i)
        Log.d("watchfacekotlin", "Launch App ${app}")
        val launchIntent = packageManager.getLaunchIntentForPackage(app)
        launchIntent?.let { startActivity(it) }
    }

}
