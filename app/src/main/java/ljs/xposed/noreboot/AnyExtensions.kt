package ljs.xposed.noreboot

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import ljs.io.IOUtil
import java.io.File
import java.util.zip.ZipFile

val Any.Key_SelectedModules: String
    get() = "selected_modules"
val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
fun Any.toJson(): String = gson.toJson(this)
fun Any.toModules(): ArrayList<Module> = gson.fromJson(this.toString(), object : TypeToken<ArrayList<Module>>() {}.type)
fun Any.getMainHookClass(apkFile: File): String {
    val zipFile = ZipFile(apkFile)
    val initFile = zipFile.getEntry("assets/xposed_init")

    return IOUtil.toString(zipFile.getInputStream(initFile), "UTF-8", true).toString().trim()
}