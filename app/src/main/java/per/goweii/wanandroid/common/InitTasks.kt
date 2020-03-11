package per.goweii.wanandroid.common

import android.app.Application
import android.graphics.Color
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.sohu.cyan.android.sdk.api.Config
import com.sohu.cyan.android.sdk.api.CyanSdk
import com.sohu.cyan.android.sdk.exception.CyanException
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.bugly.crashreport.CrashReport.CrashHandleCallback
import com.tencent.bugly.crashreport.CrashReport.UserStrategy
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.QbSdk.PreInitCallback
import com.tencent.smtt.sdk.WebView
import io.realm.Realm
import per.goweii.basic.core.CoreInit
import per.goweii.basic.utils.AsyncInitTask
import per.goweii.basic.utils.DebugUtils
import per.goweii.basic.utils.LogUtils
import per.goweii.basic.utils.SyncInitTask
import per.goweii.basic.utils.listener.SimpleCallback
import per.goweii.burred.Blurred
import per.goweii.rxhttp.core.RxHttp
import per.goweii.wanandroid.BuildConfig
import per.goweii.wanandroid.http.RxHttpRequestSetting
import per.goweii.wanandroid.http.WanCache
import per.goweii.wanandroid.module.login.activity.LoginActivity
import per.goweii.wanandroid.module.main.activity.CrashActivity
import per.goweii.wanandroid.module.main.activity.MainActivity
import per.goweii.wanandroid.utils.UserUtils
import java.util.*

/**
 * @author CuiZhen
 * @date 2020/2/20
 */

class RxHttpInitTask : SyncInitTask() {
    override fun init(application: Application) {
        RxHttp.init(application)
        RxHttp.initRequest(RxHttpRequestSetting(WanApp.getCookieJar()))
    }

    override fun onlyMainProcess(): Boolean {
        return true
    }

    override fun level(): Int {
        return 0
    }
}

class WanCacheInitTask : SyncInitTask() {
    override fun init(application: Application) {
        WanCache.init()
    }

    override fun onlyMainProcess(): Boolean {
        return true
    }

    override fun level(): Int {
        return 0
    }
}

class CoreInitTask : SyncInitTask() {
    override fun init(application: Application) {
        CoreInit.getInstance().onGoLoginCallback = SimpleCallback { data ->
            UserUtils.getInstance().doIfLogin(data)
        }
    }

    override fun onlyMainProcess(): Boolean {
        return true
    }

    override fun level(): Int {
        return 0
    }
}

class BlurredInitTask : AsyncInitTask() {
    override fun init(application: Application) {
        Blurred.init(application)
    }

    override fun onlyMainProcess(): Boolean {
        return true
    }

    override fun level(): Int {
        return 2
    }
}

class RealmInitTask : AsyncInitTask() {
    override fun init(application: Application) {
        Realm.init(application)
    }

    override fun onlyMainProcess(): Boolean {
        return true
    }

    override fun level(): Int {
        return 2
    }
}

class CrashInitTask : SyncInitTask() {
    override fun init(application: Application) {
        CaocConfig.Builder.create()
                .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT)
                .enabled(true)
                .showErrorDetails(true)
                .showRestartButton(true)
                .logErrorOnRestart(false)
                .trackActivities(false)
                .minTimeBetweenCrashesMs(2000)
                .restartActivity(MainActivity::class.java)
                .errorActivity(CrashActivity::class.java)
                .apply()
    }

    override fun onlyMainProcess(): Boolean {
        return false
    }

    override fun level(): Int {
        return 0
    }
}

class CyanInitTask : AsyncInitTask() {
    override fun init(application: Application) {
        val config = Config()
        config.ui.toolbar_bg = Color.WHITE
        config.ui.style = "indent"
        config.ui.depth = 1
        config.ui.sub_size = 20
        config.comment.showScore = false
        config.comment.uploadFiles = true
        config.comment.useFace = false
        config.login.SSO_Assets_ICon = "ico31.png"
        config.login.SSOLogin = true
        config.login.loginActivityClass = LoginActivity::class.java
        try {
            CyanSdk.register(application,
                    BuildConfig.CHANGYAN_APP_ID,
                    BuildConfig.CHANGYAN_APP_KEY,
                    null,
                    config)
        } catch (e: CyanException) {
            e.printStackTrace()
        }
    }

    override fun onlyMainProcess(): Boolean {
        return false
    }

    override fun level(): Int {
        return 5
    }
}

class X5InitTask : AsyncInitTask() {
    override fun init(application: Application) {
        QbSdk.initX5Environment(application, object : PreInitCallback {
            override fun onCoreInitFinished() {
                LogUtils.d("x5", "initX5Environment->onCoreInitFinished")
            }

            override fun onViewInitFinished(b: Boolean) {
                LogUtils.d("x5", "initX5Environment->onViewInitFinished=$b")
            }
        })
    }

    override fun onlyMainProcess(): Boolean {
        return false
    }

    override fun level(): Int {
        return 0
    }
}

class BuglyInitTask : SyncInitTask() {
    override fun init(application: Application) {
        if (DebugUtils.isDebug()) return
        CrashReport.setIsDevelopmentDevice(application, DebugUtils.isDebug())
        val strategy = UserStrategy(application)
        strategy.setCrashHandleCallback(object : CrashHandleCallback() {
            override fun onCrashHandleStart(crashType: Int, errorType: String, errorMessage: String, errorStack: String): Map<String, String> {
                val map = LinkedHashMap<String, String>()
                val x5CrashInfo = WebView.getCrashExtraMessage(application)
                map["x5crashInfo"] = x5CrashInfo
                return map
            }

            override fun onCrashHandleStart2GetExtraDatas(crashType: Int, errorType: String, errorMessage: String, errorStack: String): ByteArray? {
                return try {
                    "Extra data.".toByteArray(charset("UTF-8"))
                } catch (e: Exception) {
                    null
                }
            }
        })
        strategy.isUploadProcess = WanApp.isMainProcess()
        CrashReport.initCrashReport(application, BuildConfig.APPID_BUGLY, DebugUtils.isDebug(), strategy)
    }

    override fun onlyMainProcess(): Boolean {
        return false
    }

    override fun level(): Int {
        return 3
    }
}