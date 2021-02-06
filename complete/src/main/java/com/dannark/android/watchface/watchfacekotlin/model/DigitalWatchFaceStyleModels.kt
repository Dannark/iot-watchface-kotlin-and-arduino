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
package com.dannark.android.watchface.watchfacekotlin.model

/**
 * Represents all data required to style an analog watch face.
 */
data class DigitalWatchFaceStyle(
    val watchFaceColors: WatchFaceColors,
    val watchFaceBackgroundImage: WatchFaceBackgroundImage
)

/**
 * Represents all colors associated with watch face:
 * - main - hour hand, minute hand, and mark colors
 * - highlight - second hand color
 * - background - background color
 * - shadow - shadow color beneath all hands and ticks.
 */
data class WatchFaceColors(
    val main:Int,
    val highlight:Int,
    val background:MutableList<Int>,
    val shadow:Int)

/**
 * Represents the background image resource id for a watch face, or 0 if there isn't a
 * background image drawable.
 *
 * Image is scaled to fit the device screen by width but will maintain its aspect ratio, and
 * centered to the top of the screen.
 */
data class WatchFaceBackgroundImage(val backgroundImageResource:Int) {
    companion object {
        const val EMPTY_IMAGE_RESOURCE = 0
    }
}
