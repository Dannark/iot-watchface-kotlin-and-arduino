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

import android.graphics.Color
import android.util.Log
import com.dannark.android.watchface.watchfacekotlin.model.*

/**
 * Creates watch face style DSL so developers can easily customize watch faces without learning the
 * underlying complex implementation.
 */
@DslMarker
annotation class WatchFaceStyleDSL

@WatchFaceStyleDSL
class WatchFaceColorsBuilder {

    // Initializes defaults for fields. Check [DigitalWatchFaceStyle] for detailed explanation of
    // each field.
    private val attributesMap: MutableMap<String, Any?> = mutableMapOf(
            "main" to Color.WHITE,
            "highlight" to Color.RED,
            "background" to mutableListOf(Color.DKGRAY, Color.BLUE, Color.RED, Color.GREEN),
            "shadow" to Color.BLACK
    )

    var main:Int by attributesMap
    var highlight:Int by attributesMap
    var background:MutableList<Int> by attributesMap
    var shadow:Int by attributesMap

    fun build(): WatchFaceColors {
        Log.i("watchfacekotlin","color build")
        return WatchFaceColors(
                main, highlight, background, shadow
        )
    }
}

@WatchFaceStyleDSL
class WatchFaceBackgroundImageBuilder {

    // A background image isn't required for a watch face, so if it isn't defined in the DSL,
    // it gets an empty image resource value which means it won't be rendered.
    private val attributesMap: MutableMap<String, Any?> = mutableMapOf(
            "backgroundImageResource" to WatchFaceBackgroundImage.EMPTY_IMAGE_RESOURCE
    )

    var backgroundImageResource:Int by attributesMap

    fun build(): WatchFaceBackgroundImage {
        Log.i("watchfacekotlin","background image build")
        return WatchFaceBackgroundImage(backgroundImageResource)
    }
}

@WatchFaceStyleDSL
class DigitalWatchFaceStyleBuilder {

    private var watchFaceColors: WatchFaceColors? = null
    private var watchFaceBackgroundImage: WatchFaceBackgroundImage =
        WatchFaceBackgroundImageBuilder().build()

    fun watchFaceColors(setup: WatchFaceColorsBuilder.() -> Unit) {
        val watchFaceColorsBuilder = WatchFaceColorsBuilder()
        watchFaceColorsBuilder.setup()
        watchFaceColors = watchFaceColorsBuilder.build()
    }

    fun watchFaceBackgroundImage(setup: WatchFaceBackgroundImageBuilder.() -> Unit) {
        val digitalWatchFaceBackgroundImageBuilder = WatchFaceBackgroundImageBuilder()
        digitalWatchFaceBackgroundImageBuilder.setup()
        watchFaceBackgroundImage = digitalWatchFaceBackgroundImageBuilder.build()
    }


    fun build(): DigitalWatchFaceStyle {
        Log.i("watchfacekotlin","build")
        return DigitalWatchFaceStyle(
                watchFaceColors ?:
                    throw InstantiationException("Must define watch face styles in DSL."),
                watchFaceBackgroundImage
        )
    }

    @Suppress("UNUSED_PARAMETER")
    @Deprecated(level = DeprecationLevel.ERROR, message = "WatchFaceStyles can't be nested.")
    fun digitalWatchFaceStyle(param: () -> Unit = {}) {
    }
}

@WatchFaceStyleDSL
fun digitalWatchFaceStyle (setup: DigitalWatchFaceStyleBuilder.() -> Unit): DigitalWatchFaceStyle {
    Log.i("watchfacekotlin","digitalWatchFaceStyle")
    val digitalWatchFaceStyleBuilder = DigitalWatchFaceStyleBuilder()
    digitalWatchFaceStyleBuilder.setup()
    return digitalWatchFaceStyleBuilder.build()
}
