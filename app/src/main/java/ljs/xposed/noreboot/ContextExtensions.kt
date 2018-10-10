package ljs.xposed.noreboot

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

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
