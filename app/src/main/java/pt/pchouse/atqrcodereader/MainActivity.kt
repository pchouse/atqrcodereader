/*
 * Copyright (C) 2022  Reflexão Sistemas e Estudos Informáticos, LDA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pt.pchouse.atqrcodereader

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.material.bottomnavigation.BottomNavigationView
import pt.pchouse.atqrcodereader.databinding.ActivityMainBinding
import pt.pchouse.atqrcodereader.publicity.AppOpenManager

class MainActivity : AppCompatActivity() {


    companion object {
        val LOG_TAG : String = MainActivity::class.qualifiedName!!
        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null
        private lateinit var bottomNavView: BottomNavigationView
        private lateinit var appOpenManager: AppOpenManager

        /**
         * Get the bottom Navigation View
         */
        fun getBottomNavView(): BottomNavigationView{
            return bottomNavView
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(LOG_TAG, "onCreate called")
        super.onCreate(savedInstanceState)
        context = applicationContext
        val binding = ActivityMainBinding.inflate(layoutInflater)
        Log.i(LOG_TAG, "bidding is set")
        setContentView(binding.root)

        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(
                mutableListOf(AdRequest.DEVICE_ID_EMULATOR).also {
                    it.addAll(resources.getStringArray(R.array.admob_test_device_ids))
                }
            ).build()
        )

        MobileAds.initialize(this){}
        appOpenManager = AppOpenManager(this)

        Log.i(LOG_TAG, "setContentView is set")
        bottomNavView = binding.navView

        Log.i(LOG_TAG, "Going to set Navigation Controller")

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_settings))
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNavView.setupWithNavController(navController)

        Log.d(LOG_TAG, "Navigation Controller is set")
    }

}