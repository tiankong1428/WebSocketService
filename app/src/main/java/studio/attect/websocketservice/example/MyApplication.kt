package studio.attect.websocketservice.example

import androidx.multidex.MultiDexApplication

import android.app.Application
import studio.attect.staticviewmodelstore.StaticViewModelStore

class MyApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        StaticViewModelStore.application = this
    }
}