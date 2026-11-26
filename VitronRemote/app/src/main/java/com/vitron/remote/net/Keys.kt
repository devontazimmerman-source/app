package com.vitron.remote.net

/** Android KeyEvent codes understood by the TV. */
object Keys {
    const val HOME = 3
    const val BACK = 4
    const val UP = 19
    const val DOWN = 20
    const val LEFT = 21
    const val RIGHT = 22
    const val OK = 23
    const val VOL_UP = 24
    const val VOL_DOWN = 25
    const val POWER = 26
    const val MENU = 82
    const val PLAY_PAUSE = 85
    const val STOP = 86
    const val NEXT = 87
    const val PREV = 88
    const val REWIND = 89
    const val FAST_FORWARD = 90
    const val MUTE = 164
    const val INFO = 165
    const val CH_UP = 166
    const val CH_DOWN = 167
    const val TV = 170
    const val GUIDE = 172
    const val CAPTIONS = 175
    const val SETTINGS = 176
    const val INPUT = 178
    const val RED = 183
    const val GREEN = 184
    const val YELLOW = 185
    const val BLUE = 186
    const val LAST_CHANNEL = 229

    fun digit(n: Int) = 7 + n // KEYCODE_0 = 7
}
