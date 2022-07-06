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
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.ads.consent.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import kotlinx.coroutines.*
import pt.pchouse.atqrcodereader.MainActivity
import pt.pchouse.atqrcodereader.R
import pt.pchouse.atqrcodereader.ServiceLocator
import java.net.MalformedURLException
import java.net.URL

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
        var personalized = false
        var consentForm: ConsentForm? = null

        /**
         * Set the application RGPD status consent, to be called
         * after close RGPD consent form or verify that is out side of UE
         */
        fun setApplicationContentStatus(context: Context, consentStatus: ConsentStatus) {
            ConsentInformation.getInstance(context).consentStatus = consentStatus
            personalized = consentStatus == ConsentStatus.PERSONALIZED
            isPersonalizedAddsInSettings(
                context.applicationContext, personalized
            )
        }

        /**
         * Open consent form
         */
        fun openConsentForm(context: Context) {

                var privacyUrl: URL? = null

                try {
                    privacyUrl = URL(context.getString(R.string.privacy_url))
                } catch (e: MalformedURLException) {
                    personalized = false
                }

                consentForm = ConsentForm.Builder(context, privacyUrl)
                    .withListener(RGPDConsentFormListener(context))
                    .withPersonalizedAdsOption()
                    .withNonPersonalizedAdsOption()
                    //.withAdFreeOption()
                    .build()

                consentForm!!.load()

        }

        /**
         * Gets or set in preferences if the adds are personalized
         */
        fun isPersonalizedAddsInSettings(
            context: Context,
            isPersonalized: Boolean? = null
        ): Boolean? {
            val sharedPref: SharedPreferences = context.getSharedPreferences(
                context.getString(R.string.publicity_shared_file),
                AppCompatActivity.MODE_PRIVATE
            )

            val key = context.resources.getString(R.string.ads_personalized)

            if (isPersonalized == null) {
                if (!sharedPref.contains(key)) {
                    return null
                }
                return sharedPref.getBoolean(key, false)
            }

            val editor = sharedPref.edit()
            editor.putBoolean(key, isPersonalized)
            editor.commit()

            return isPersonalized
        }

        /**
         * Check if is mandatory to open RGPD form and if yes open it
         */
        fun checkRGPD(context: Context) {

            val consentInformation: ConsentInformation = ConsentInformation.getInstance(context.applicationContext)
            val publisherIds = arrayOf(context.getString(R.string.publisherID))

            consentInformation.requestConsentInfoUpdate(
                publisherIds, RGPDConsentInfoUpdateListener(context)
            )
        }

        /**
         * Get if call is from UE
         */
        fun isRequestLocationInEeaOrUnknown(context: Context): Boolean{
            return ConsentInformation.getInstance(context.applicationContext).isRequestLocationInEeaOrUnknown
        }

    }

    private var appOpenAd: AppOpenAd? = null
    private var loadCallback: AppOpenAdLoadCallback? = null
    private lateinit var lifecycleOwner: LifecycleOwner

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mainActivity.registerActivityLifecycleCallbacks(this)
        } else {
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
        get() {
            val buildRequest = AdRequest.Builder()
            if (!personalized) {
                val extras = Bundle()
                extras.putString("npa", "1")
                buildRequest.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            }
            return buildRequest.build()
        }

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
