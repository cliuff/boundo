/*
 * Copyright 2025 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cliuff.boundo.os

import android.os.Build.VERSION_CODES as Code

enum class OS(val api: Int, val versionName: String, val codename: String) {

    A         (Code.BASE,                   "1.0",         ""),                   // 1.0 null
    B         (Code.BASE_1_1,               "1.1",         "Petit Four"),         // 1.1 null
    C         (Code.CUPCAKE,                "1.5",         "Cupcake"),            // 1.5 Cupcake
    D         (Code.DONUT,                  "1.6",         "Donut"),              // 1.6 Donut
    E         (Code.ECLAIR,                 "2.0",         "Eclair"),             // 2.0 Eclair
    E_0_1     (Code.ECLAIR_0_1,             "2.0.1",       "Eclair"),             // 2.0.1 Eclair
    E_MR1     (Code.ECLAIR_MR1,             "2.1",         "Eclair"),             // 2.1 Eclair
    F         (Code.FROYO,                  "2.2",         "Froyo"),              // 2.2.x Froyo
    G         (Code.GINGERBREAD,            "2.3-2.3.2",   "Gingerbread"),        // 2.3 - 2.3.2 Gingerbread
    G_MR1     (Code.GINGERBREAD_MR1,        "2.3.3-2.3.7", "Gingerbread"),        // 2.3.3 - 2.3.7 Gingerbread
    H         (Code.HONEYCOMB,              "3.0",         "Honeycomb"),          // 3.0 Honeycomb
    H_MR1     (Code.HONEYCOMB_MR1,          "3.1",         "Honeycomb"),          // 3.1 Honeycomb
    H_MR2     (Code.HONEYCOMB_MR2,          "3.2",         "Honeycomb"),          // 3.2.x Honeycomb
    I         (Code.ICE_CREAM_SANDWICH,     "4.0.1-4.0.2", "Ice Cream Sandwich"), // 4.0.1 - 4.0.2 Ice Cream Sandwich
    I_MR1     (Code.ICE_CREAM_SANDWICH_MR1, "4.0.3-4.0.4", "Ice Cream Sandwich"), // 4.0.3 - 4.0.4 Ice Cream Sandwich
    J         (Code.JELLY_BEAN,             "4.1",         "Jelly Bean"),         // 4.1.x Jelly Bean
    J_MR1     (Code.JELLY_BEAN_MR1,         "4.2",         "Jelly Bean"),         // 4.2.x Jelly Bean
    J_MR2     (Code.JELLY_BEAN_MR2,         "4.3",         "Jelly Bean"),         // 4.3.x Jelly Bean
    K         (Code.KITKAT,                 "4.4",         "KitKat"),             // 4.4 - 4.4.4 KitKat
    K_WATCH   (Code.KITKAT_WATCH,           "4.4W",        "KitKat"),             // 4.4W KitKat
    L         (Code.LOLLIPOP,               "5.0",         "Lollipop"),           // 5.0 Lollipop
    L_MR1     (Code.LOLLIPOP_MR1,           "5.1",         "Lollipop"),           // 5.1 Lollipop
    M         (Code.M,                      "6.0",         "Marshmallow"),        // 6.0 Marshmallow
    N         (Code.N,                      "7.0",         "Nougat"),             // 7.0 Nougat
    N_MR1     (Code.N_MR1,                  "7.1",         "Nougat"),             // 7.1 Nougat
    O         (Code.O,                      "8.0",         "Oreo"),               // 8.0.0 Oreo
    O_MR1     (Code.O_MR1,                  "8.1",         "Oreo"),               // 8.1.0 Oreo
    P         (Code.P,                      "9",           "Pie"),                // 9 Pie
    Q         (Code.Q,                      "10",          "Quince Tart"),
    R         (Code.R,                      "11",          "Red Velvet Cake"),
    S         (Code.S,                      "12",          "Snow Cone"),
    S_V2      (Code.S_V2,                   "12L",         "Snow Cone"),
    T         (Code.TIRAMISU,               "13",          "Tiramisu"),
    U         (Code.UPSIDE_DOWN_CAKE,       "14",          "Upside Down Cake"),
    V         (Code.VANILLA_ICE_CREAM,      "15",          "Vanilla Ice Cream"),
    Baklava   (Code.BAKLAVA,                "16",          "Baklava"),
;

    fun getMajorVersionName(): Int =
        when {
            this == S_V2 -> 12
            // Android 9+: convert from version name
            this >= P -> versionName.toInt()
            // Android 1-8: convert integer part of the version name
            else -> versionName.substringBefore('.').toInt()
        }

    companion object {
        private val Mapping: Map<Int, OS> = entries.associateBy(OS::api)

        fun from(api: Int): OS? = Mapping[api]
    }
}
