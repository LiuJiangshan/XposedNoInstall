package ljs.xposed.noreboot

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dalvik.system.PathClassLoader
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.io.FileNotFoundException

class MainHook : IXposedHookLoadPackage {
    private var mXSharedPreferences = XSharedPreferences(BuildConfig.APPLICATION_ID)

    private val packageNames = HashSet<String>()

    override fun handleLoadPackage(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        if (mXSharedPreferences.hasFileChanged()) {
            packageNames.clear()
            val packageNames = mXSharedPreferences.getStringSet(Key_PackageNames, HashSet())
            if (packageNames !== null) this@MainHook.packageNames.addAll(packageNames)
        }
        XposedHelpers.findAndHookMethod(Application::class.java, "attach", Context::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam?) {
                val context = param!!.args[0] as Context

                loadPackageParam.classLoader = context.classLoader

                for (packageName in packageNames) {
                    val apkFile = findApkFile(context, packageName)

                    if (apkFile != null) {
                        val pathClassLoader = PathClassLoader(apkFile.absolutePath, ClassLoader.getSystemClassLoader())
                        val moduleHookClassName = ""
                        val moduleHookClass = Class.forName(moduleHookClassName, true, pathClassLoader)
                        val instance = moduleHookClass.newInstance()
                        val method = moduleHookClass.getDeclaredMethod("handleLoadPackage", XC_LoadPackage.LoadPackageParam::class.java)
                        method.invoke(instance, loadPackageParam)
                    }
                }
            }
        })
    }


    /**
     * 安装app以后，系统会在/data/app/下备份了一份.apk文件，通过动态加载这个apk文件，调用相应的方法
     * 这样就可以实现，只需要第一次重启，以后修改hook代码就不用重启了
     * @param context context参数
     * @param modulePackageName 当前模块的packageName
     * @param handleHookClass   指定由哪一个类处理相关的hook逻辑
     * @param loadPackageParam  传入XC_LoadPackage.LoadPackageParam参数
     * @throws Throwable 抛出各种异常,包括具体hook逻辑的异常,寻找apk文件异常,反射加载Class异常等
     */
    @Throws(Throwable::class)
    private fun invokeHandleHookMethod(context: Context, modulePackageName: String, handleHookClass: String, handleHookMethod: String, loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        //        File apkFile = findApkFileBySDK(modulePackageName);//会受其它Xposed模块hook 当前宿主程序的SDK_INT的影响
        //        File apkFile = findApkFile(modulePackageName);
        //原来的两种方式不是很好,改用这种新的方式
        val apkFile = findApkFile(context, modulePackageName) ?: throw RuntimeException("寻找模块apk失败")
//加载指定的hook逻辑处理类，并调用它的handleHook方法
        val pathClassLoader = PathClassLoader(apkFile.absolutePath, ClassLoader.getSystemClassLoader())
        val cls = Class.forName(handleHookClass, true, pathClassLoader)
        val instance = cls.newInstance()
        val method = cls.getDeclaredMethod(handleHookMethod, XC_LoadPackage.LoadPackageParam::class.java)
        method.invoke(instance, loadPackageParam)
    }

    /**
     * 根据包名构建目标Context,并调用getPackageCodePath()来定位apk
     * @param context context参数
     * @param modulePackageName 当前模块包名
     * @return return apk file
     */
    private fun findApkFile(context: Context?, modulePackageName: String): File? {
        if (context == null) return null
        try {
            val moudleContext = context.createPackageContext(modulePackageName, Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY)
            val apkPath = moudleContext.packageCodePath
            return File(apkPath)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 寻找这个Android设备上的当前apk文件,不受其它Xposed模块hook SDK_INT的影响
     *
     * @param modulePackageName 当前模块包名
     * @return File 返回apk文件
     * @throws FileNotFoundException 在/data/app/下的未找到本模块apk文件,请检查本模块包名配置是否正确.
     * 具体检查build.gradle中的applicationId和AndroidManifest.xml中的package
     */
    @Deprecated("")
    @Throws(FileNotFoundException::class)
    private fun findApkFile(modulePackageName: String): File {
        var apkFile: File? = null
        try {
            apkFile = findApkFileAfterSDK21(modulePackageName)
        } catch (e: Exception) {
            try {
                apkFile = findApkFileBeforeSDK21(modulePackageName)
            } catch (e2: Exception) {
                //忽略这个异常
            }

        }

        if (apkFile == null) {
            throw FileNotFoundException("没在/data/app/下找到文件对应的apk文件")
        }
        return apkFile
    }

    /**
     * 根据当前的SDK_INT寻找这个Android设备上的当前apk文件
     *
     * @param modulePackageName 当前模块包名
     * @return File 返回apk文件
     * @throws FileNotFoundException 在/data/app/下的未找到本模块apk文件,请检查本模块包名配置是否正确.
     * 具体检查build.gradle中的applicationId和AndroidManifest.xml中的package
     */
    @Deprecated("")
    @Throws(FileNotFoundException::class)
    private fun findApkFileBySDK(modulePackageName: String): File {
        val apkFile: File
        //当前Xposed模块hook了Build.VERSION.SDK_INT不用担心，因为这是发生在hook之前，不会有影响
        //但是其它的Xposed模块hook了当前宿主的这个值以后，就会有影响了,所以这里没有使用这个方法
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            apkFile = findApkFileAfterSDK21(modulePackageName)
        } else {
            apkFile = findApkFileBeforeSDK21(modulePackageName)
        }
        return apkFile
    }

    /**
     * 寻找apk文件(api_21之后)
     * 在Android sdk21以及之后，apk文件的路径发生了变化
     *
     * @param packageName 当前模块包名
     * @return File 返回apk文件
     * @throws FileNotFoundException apk文件未找到
     */
    @Deprecated("")
    @Throws(FileNotFoundException::class)
    private fun findApkFileAfterSDK21(packageName: String): File {
        val apkFile: File
        var path = File(String.format("/data/app/%s-%s", packageName, "1"))
        if (!path.exists()) {
            path = File(String.format("/data/app/%s-%s", packageName, "2"))
        }
        if (!path.exists() || !path.isDirectory) {
            throw FileNotFoundException(String.format("没找到目录/data/app/%s-%s", packageName, "1/2"))
        }
        apkFile = File(path, "base.apk")
        if (!apkFile.exists() || apkFile.isDirectory) {
            throw FileNotFoundException(String.format("没找到文件/data/app/%s-%s/base.apk", packageName, "1/2"))
        }
        return apkFile
    }

    /**
     * 寻找apk文件(api_21之前)
     *
     * @param packageName 当前模块包名
     * @return File 返回apk文件
     * @throws FileNotFoundException apk文件未找到
     */
    @Deprecated("")
    @Throws(FileNotFoundException::class)
    private fun findApkFileBeforeSDK21(packageName: String): File {
        var apkFile = File(String.format("/data/app/%s-%s.apk", packageName, "1"))
        if (!apkFile.exists()) {
            apkFile = File(String.format("/data/app/%s-%s.apk", packageName, "2"))
        }
        if (!apkFile.exists() || apkFile.isDirectory) {
            throw FileNotFoundException(String.format("没找到文件/data/app/%s-%s.apk", packageName, "1/2"))
        }
        return apkFile
    }
}
