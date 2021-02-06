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

package com.dannark.android.watchface.watchfacekotlin

import android.graphics.Color
import android.util.Log
import com.dannark.android.watchface.watchfacekotlin.model.DigitalWatchFaceStyle
import com.dannark.android.watchface.watchfacekotlin.service.AbstractKotlinWatchFace
import com.dannark.android.watchface.watchfacekotlin.service.digitalWatchFaceStyle

/**
 * Renders watch face via data object created by DSL.
 */
class DigitalDslWatchFace : AbstractKotlinWatchFace() {

    override fun getWatchFaceStyle(): DigitalWatchFaceStyle {
        Log.i("AnalogDslWatchFace","AnalogWatchFaceStyle")

        return digitalWatchFaceStyle {
            watchFaceColors {
                main = Color.CYAN
                highlight = Color.parseColor("#ffa500")
                background = mutableListOf(
                        Color.rgb(70, 0, 0),
                        Color.rgb(0, 70, 0),
                        Color.rgb(0, 0, 70),
                        Color.rgb(11, 100, 95),
                        Color.rgb(69, 37, 95),
                        Color.rgb(94, 37, 95),
                        Color.rgb(20, 100, 100),
                        Color.rgb(100, 29, 50),
                        Color.rgb(50, 50, 50)
                )
            }

            watchFaceBackgroundImage {
                backgroundImageResource = R.drawable.relogio_translucent
            }
        }
    }
}
