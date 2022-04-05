/*
 * Copyright (c) 2022. Reflexão Estudos e Sistemas Informáticos, Lda.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package pt.pchouse.atqrcodereader.publicity


import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.pchouse.atqrcodereader.MainActivity
import pt.pchouse.atqrcodereader.ServiceLocator


/** Prefetches App Open Ads.  */
class AppOpenManager(private val mainActivity: MainActivity) : DefaultLifecycleObserver,
    Application.ActivityLifecycleCallbacks {

    companion object {
        private const val LOG_TAG = "AppOpenManager"
        private const val AD_UNIT_ID = "app_opening"
        private var isShowingAd: Boolean = false
        private var wasShow: Boolean = false
        private const val MAX_RETRIES = 2
        private var retries = 0
    }

    private var appOpenAd: AppOpenAd? = null
    private var loadCallback: AppOpenAdLoadCallback? = null
    private lateinit var lifecycleOwner : LifecycleOwner

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mainActivity.registerActivityLifecycleCallbacks(this)
        }else{
            mainActivity.application.registerActivityLifecycleCallbacks(this)
        }
        mainActivity.lifecycle.addObserver(this)
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    /** Request an ad  */
    private fun fetchAd() {
        if (isAdAvailable) return

        loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {

            override fun onAdLoaded(add: AppOpenAd) {
                this@AppOpenManager.appOpenAd = add
                this@AppOpenManager.showAddIfAvailable()
            }

            override fun onAdFailedToLoad(loadError: LoadAdError) {
                Log.d(LOG_TAG, loadError.message)
            }
        }

        mainActivity.lifecycleScope.launch {
            AppOpenAd.load(
                mainActivity,
                ServiceLocator.locate.getString(AD_UNIT_ID),
                adRequest,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                loadCallback!!
            )
        }
    }

    /** Creates and returns ad request.  */
    private val adRequest: AdRequest
        get() = AdRequest.Builder().build()

    /** Utility method that checks if ad exists and can be shown.  */
    private val isAdAvailable: Boolean
        get() = appOpenAd != null

    /**
     * Show the ad
     */
    fun showAddIfAvailable() {
        if (!isShowingAd && isAdAvailable && !wasShow) {
            Log.d(LOG_TAG, "The open app ad wil be showed")

            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    this@AppOpenManager.appOpenAd = null
                    isShowingAd = false
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(LOG_TAG, adError.message)
                    if (++retries > MAX_RETRIES) return
                    lifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.Default) {
                            delay(1000 * 60)
                            this@AppOpenManager.showAddIfAvailable()
                        }
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                    wasShow = true
                }
            }

            appOpenAd?.show(mainActivity)

        } else {
            Log.d(LOG_TAG, "The add will not be loaded, will be fetch first")
            fetchAd()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        this.lifecycleOwner = owner
        owner.lifecycleScope.launch {
            showAddIfAvailable()
            Log.d(LOG_TAG, "On start, showAddIfAvailable was call")
        }
    }

}
