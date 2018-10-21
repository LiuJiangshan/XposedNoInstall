package ljs.xposed.noreboot

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.google.gson.annotations.Expose

open class Module {
    constructor()

    @Expose(serialize = false, deserialize = false)
    var appName: String? = ""
    @Expose(serialize = false, deserialize = false)
    var info: String = ""
    @Expose(serialize = false, deserialize = false)
    var versionName: String = ""
    @Expose(serialize = false, deserialize = false)
    var packageName: String = ""
    @Expose(serialize = false, deserialize = false)
    var isXposed: Boolean = false
    //主要存储字段
    @Expose
    var apkFile: String = ""
    @Expose
    var mainHookClass: String = ""

    fun loadModuleInfo(packageManager: PackageManager): Drawable {
        val apkInfo = packageManager.getPackageArchiveInfo(apkFile, PackageManager.GET_META_DATA)

        info = apkInfo.applicationInfo.metaData.getString("xposeddescription") ?: ""
        isXposed = apkInfo.applicationInfo.metaData.getBoolean("xposedmodule")

        packageName = apkInfo.packageName
        versionName = apkInfo.versionName
        appName = apkInfo.applicationInfo.loadLabel(packageManager).toString()

        return apkInfo.applicationInfo.loadIcon(packageManager)
    }
}