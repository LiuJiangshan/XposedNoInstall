package ljs.xposed.noreboot

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.io.File

val Context.installedXposedModules: List<PackageInfo>
    get() {
        val modules = ArrayList<PackageInfo>()
        for (pkg in packageManager.getInstalledPackages(PackageManager.GET_META_DATA)) {
            val app = pkg.applicationInfo
            if (!app.enabled) continue
            if (app.metaData != null && app.metaData.containsKey("xposedmodule")) modules.add(pkg)
        }
        return modules
    }

fun Context.getApkFile(modulePackageName: String): File {
    val moudleContext = this.createPackageContext(modulePackageName, Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY)
    val apkPath = moudleContext.packageCodePath
    return File(apkPath)
}
