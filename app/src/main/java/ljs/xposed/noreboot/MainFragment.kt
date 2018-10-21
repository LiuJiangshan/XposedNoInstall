package ljs.xposed.noreboot

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_main_layout.*
import ljs.android.alert
import ljs.android.fragment.BaseFragment
import ljs.android.preferences
import ljs.android.toast
import ljs.exception.KnowException
import ljs.recyclerview.SimpleItemDecoration
import java.io.File
import java.lang.Exception


open class MainFragment : BaseFragment() {

    inner class ModuleViewHolder(itemView: View, private var module: Module) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private val tv_appName = itemView.findViewById<AppCompatTextView>(R.id.tv_appName)
        private val tv_packageName = itemView.findViewById<AppCompatTextView>(R.id.tv_packageName)
        private val iv_appIcon = itemView.findViewById<AppCompatImageView>(R.id.iv_appIcon)
        private val tv_moduleInfo = itemView.findViewById<AppCompatTextView>(R.id.tv_moduleInfo)
        private val tv_appVersion = itemView.findViewById<AppCompatTextView>(R.id.tv_appVersion)
        private var icon: Drawable? = null

        init {
            itemView.setOnClickListener(this@ModuleViewHolder)
        }

        override fun onClick(view: View) {

            AlertDialog.Builder(baseActivity).setMessage("移除模块\"${module.appName}\"?")
                    .setPositiveButton("确定") { _, _ ->
                        removeModule(module)
                        Snackbar.make(view, "已移除", Snackbar.LENGTH_SHORT).show()
                    }.setNegativeButton("取消") { _, _ ->
                        Snackbar.make(view, "已取消", Snackbar.LENGTH_SHORT).show()
                    }.show()

            recyclerView.adapter?.notifyDataSetChanged()
        }


        fun bindView(module: Module) {
            this@ModuleViewHolder.module = module

            icon = module.loadModuleInfo(baseActivity.packageManager)

            tv_appName.text = module.appName
            tv_packageName.text = module.packageName
            @Suppress("DEPRECATION")
            iv_appIcon.setBackgroundDrawable(icon)
            tv_moduleInfo.text = module.info
            tv_appVersion.text = module.versionName
        }

    }

    override val layoutId = R.layout.fragment_main_layout
    val modules = ArrayList<Module>()
    val Request_File = 0
    val adapter = object : RecyclerView.Adapter<ModuleViewHolder>() {

        override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int) =
                ModuleViewHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.item_module_layout, viewGroup, false), modules[position])

        override fun getItemCount(): Int = modules.size
        override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) = holder.bindView(modules[position])
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        modules.addAll(baseActivity.preferences.getString(Key_SelectedModules, "[]").toModules())

        recyclerView.layoutManager = LinearLayoutManager(baseActivity)
        recyclerView.addItemDecoration(SimpleItemDecoration(baseActivity))
        recyclerView.adapter = adapter
        addButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/vnd.android.package-archive"
            startActivityForResult(intent, Request_File)
        }
    }

    fun removeModule(module: Module) {
        modules.remove(module)
        baseActivity.preferences.edit().putString(Key_SelectedModules, modules.toJson()).apply()
        adapter.notifyDataSetChanged()
    }

    fun addModule(module: Module) {
        try {

            if (modules.any { it.apkFile == module.apkFile }) throw KnowException("已经存在")

            module.loadModuleInfo(baseActivity.packageManager)
            if (!File(module.apkFile).exists()) throw KnowException("文件不存在:${module.apkFile}")
            //不能添加自己
            else if (module.packageName == BuildConfig.APPLICATION_ID) throw KnowException("不能添加${module.appName}")
            else if (!module.isXposed) throw KnowException("${module.appName}不是xposed模块")

            module.mainHookClass = getMainHookClass(File(module.apkFile))
            if (module.mainHookClass.isEmpty()) throw KnowException("不能获取模块入口")

            modules.add(module)

            baseActivity.preferences.edit().putString(Key_SelectedModules, modules.toJson()).apply()
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            baseActivity.alert("添加失败:\"${e.message}\"")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Request_File && data != null) {
            val module = Module()
            val apkFile = File(data.data.path)
            module.apkFile = apkFile.absolutePath
            addModule(module)
        }
    }
}