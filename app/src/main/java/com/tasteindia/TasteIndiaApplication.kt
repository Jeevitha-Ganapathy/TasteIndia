package com.tasteindia

import android.app.Application

class TasteIndiaApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(this) }
}
