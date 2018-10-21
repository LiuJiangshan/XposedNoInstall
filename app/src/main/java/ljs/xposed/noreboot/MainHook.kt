package ljs.xposed.noreboot

import android.app.Application
import android.content.Context
import dalvik.system.PathClassLoader
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        preferences.reload()
        preferences.makeWorldReadable()
        selectedModules.addAll(preferences.getString(Key_SelectedModules, "[]").toModules())
    }

    companion object {
        val preferences = XSharedPreferences(BuildConfig.APPLICATION_ID)
        val selectedModules = ArrayList<Module>()

        init {
            loadConfig()
        }

        fun loadConfig() {
            preferences.reload()
            preferences.makeWorldReadable()
            selectedModules.clear()
            selectedModules.addAll(preferences.getString(Key_SelectedModules, "[]").toModules())
        }
    }

    override fun handleLoadPackage(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("进入:handleLoadPackage")

        if (preferences.hasFileChanged()) loadConfig()
        XposedHelpers.findAndHookMethod(Application::class.java, "attach", Context::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam?) {
                val context = param!!.args[0] as Context

                loadPackageParam.classLoader = context.classLoader

                XposedBridge.log("主动加载模块前")

                try {
                    for (module in selectedModules) {
                        XposedBridge.log("主动加载模块:${module.packageName}")
                        val apkFile: String = module.apkFile
                        val mainHookClass: String = module.mainHookClass
                        if (apkFile != "" && mainHookClass != "") {

                            val pathClassLoader = PathClassLoader(apkFile, ClassLoader.getSystemClassLoader())
                            val moduleHookClass = Class.forName(mainHookClass, true, pathClassLoader)

                            val instance = moduleHookClass.newInstance()

                            val method = moduleHookClass.getDeclaredMethod("handleLoadPackage", XC_LoadPackage.LoadPackageParam::class.java)
                            method.invoke(instance, loadPackageParam)
                        }
                    }
                    XposedBridge.log("主动加载模块后")
                } catch (e: Throwable) {
                    XposedBridge.log("加载模块发生异常:${e.message}")
                }
            }
        })
    }
}
