package ljs.xposed.noreboot

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

val Context.installedXposedModules: List<Module>
    get() {
        val modules = ArrayList<Module>()
        for (pkg in packageManager.getInstalledPackages(PackageManager.GET_META_DATA)) {
            val app = pkg.applicationInfo
            if (!app.enabled) continue
            if (app.metaData != null && app.metaData.containsKey("xposedmodule")) {
                val module = Module()
                module.packageName = pkg.packageName
                module.versionName = pkg.versionName
                module.appName = pkg.applicationInfo.loadLabel(packageManager).toString()
                module.info = app?.metaData?.getString("xposeddescription") ?: ""
                modules.add(module)
            }
        }
        return modules
    }

fun Context.getApkFile(modulePackageName: String?): File? {
    val moudleContext = this.createPackageContext(modulePackageName, Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY)
    val apkPath = moudleContext.packageCodePath
    return File(apkPath)
}


fun Context.getMainHookClass(packageName: String): String {
    val apkFile = getApkFile(packageName)
    return if (apkFile != null) getMainHookClass(apkFile.absoluteFile) else ""
}

