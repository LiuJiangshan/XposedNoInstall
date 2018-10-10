package ljs.xposed.noreboot

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    private var mXSharedPreferences = XSharedPreferences(BuildConfig.APPLICATION_ID)

    override fun handleLoadPackage(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        if (mXSharedPreferences.hasFileChanged()) {
        }
    }
}
