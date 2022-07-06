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

import android.content.Context
import android.util.Log
import com.google.ads.consent.ConsentFormListener
import com.google.ads.consent.ConsentInformation
import com.google.ads.consent.ConsentStatus

class RGPDConsentFormListener(private val context: Context) : ConsentFormListener() {

    companion object {
        private const val LOG_TAG = "RGPDConsentFormListener"
    }

    override fun onConsentFormLoaded() {
        try {
            AppOpenManager.consentForm?.show()
        } catch (e: Exception) {
            Log.d(LOG_TAG, e.toString())
        }
    }

    override fun onConsentFormOpened() {}

    override fun onConsentFormClosed(
        consentStatus: ConsentStatus?, userPrefersAdFree: Boolean?
    ) {
        try {

            if (consentStatus == null) {
                AppOpenManager.personalized = AppOpenManager.isPersonalizedAddsInSettings(context)
                    ?: false
                ConsentInformation.getInstance(context).consentStatus = ConsentStatus.UNKNOWN
                return
            }

            ConsentInformation.getInstance(context).consentStatus = consentStatus
            AppOpenManager.personalized = consentStatus == ConsentStatus.PERSONALIZED
            AppOpenManager.isPersonalizedAddsInSettings(
                context.applicationContext, AppOpenManager.personalized
            )

        } catch (e: Exception) {
            Log.d(LOG_TAG, e.toString())
        }
    }

    override fun onConsentFormError(errorDescription: String) {
        AppOpenManager.personalized = false
        Log.d(LOG_TAG, errorDescription)
    }

}