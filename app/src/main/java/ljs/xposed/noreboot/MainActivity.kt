package ljs.xposed.noreboot

import android.os.Bundle
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import kotlinx.android.synthetic.main.activity_main.*
import ljs.android.activity.BaseActivity

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navController = Navigation.findNavController(this, R.id.nav_fragment)

        setSupportActionBar(toolbar)

        NavigationUI.setupActionBarWithNavController(this, navController)
    }
}
