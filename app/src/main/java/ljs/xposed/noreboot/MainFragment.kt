package ljs.xposed.noreboot

import android.content.pm.PackageInfo
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_main_layout.*
import ljs.SingletonHolder
import ljs.android.alert
import ljs.android.fragment.BaseFragment
import ljs.android.preferences
import ljs.android.toast
import ljs.io.IOUtil
import ljs.recyclerview.SimpleItemDecoration
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream


open class MainFragment : BaseFragment() {

    inner class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private val tv_appName = itemView.findViewById<AppCompatTextView>(R.id.tv_appName)
        private val tv_packageName = itemView.findViewById<AppCompatTextView>(R.id.tv_packageName)
        private val iv_appIcon = itemView.findViewById<AppCompatImageView>(R.id.iv_appIcon)
        private val tv_moduleInfo = itemView.findViewById<AppCompatTextView>(R.id.tv_moduleInfo)
        private val tv_appVersion = itemView.findViewById<AppCompatTextView>(R.id.tv_appVersion)
        var packageInfo: PackageInfo? = null
        var appName = ""
        var packageName = ""

        init {
            itemView.setOnClickListener(this@ModuleViewHolder)
        }

        override fun onClick(view: View) {
            val snackbarText = if (isSeted(packageName)) {
                packageNames.remove(packageName)
                "已取消\"$appName\""
            } else {
                packageNames.add(packageName)
                val mainHookClass = getMainHookClass(packageName)
                baseActivity.alert(mainHookClass)
                "将主动加载模块\"$appName\",直到下一次重启后"

            }
            Snackbar.make(view, snackbarText, Snackbar.LENGTH_LONG).show()
            baseActivity.preferences.getStringSet(Key_PackageNames, packageNames)
            recyclerView.adapter?.notifyDataSetChanged()
        }

        fun bindView(packageInfo: PackageInfo) {
            this.packageInfo = packageInfo

            val appInfo = packageInfo.applicationInfo

            packageName = appInfo.packageName

            appName = appInfo.loadLabel(baseActivity.packageManager).toString()
            tv_appName.text = appName
            tv_packageName.text = packageName
            @Suppress("DEPRECATION")
            iv_appIcon.setBackgroundDrawable(appInfo.loadIcon(baseActivity.packageManager))
            tv_moduleInfo.text = appInfo.metaData.getString("xposeddescription")
            tv_appVersion.text = packageInfo.versionName

            if (isSeted(packageName)) itemView.setBackgroundColor(ContextCompat.getColor(baseActivity, R.color.checkedColor))
            else {
                val typeArray = baseActivity.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                val selectableItemBackground = typeArray.getDrawable(0)
                typeArray.recycle()
                @Suppress("DEPRECATION")
                itemView.setBackgroundDrawable(selectableItemBackground)
            }
        }
    }

    override val layoutId = R.layout.fragment_main_layout
    private val packageNames: HashSet<String> = HashSet()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packages = baseActivity.preferences.getStringSet(Key_PackageNames, HashSet<String>())
        if (packages !== null) packageNames.addAll(packages)
    }

    fun getMainHookClass(packageName: String): String {
        val apkFile = baseActivity.getApkFile(packageName)
        val zipFile = ZipFile(apkFile)
        val assets = zipFile.getEntry("assets")
        println(assets)
        val initFile = zipFile.getEntry("assets/xposed_init")

        return IOUtil.toString(zipFile.getInputStream(initFile), "UTF-8", true).toString()
    }

    fun isSeted(packageName: String): Boolean = packageNames.contains(packageName)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        val modules = baseActivity.installedXposedModules
        recyclerView.layoutManager = LinearLayoutManager(baseActivity)
        recyclerView.addItemDecoration(SimpleItemDecoration(baseActivity))
        recyclerView.adapter = object : RecyclerView.Adapter<ModuleViewHolder>() {

            override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int) =
                    ModuleViewHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.item_module_layout, viewGroup, false))

            override fun getItemCount(): Int = modules.size
            override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) = holder.bindView(modules[position])
        }

        helpButton.setOnClickListener {
            Snackbar.make(it, "请选择需要重新加载的模块,不要选择未更新的模块,否则将有可能导致不可预料错误!", Snackbar.LENGTH_LONG).setActionTextColor(Color.RED).show()
        }
    }
}