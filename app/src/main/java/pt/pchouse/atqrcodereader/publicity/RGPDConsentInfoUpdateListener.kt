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
import com.google.ads.consent.ConsentInfoUpdateListener
import com.google.ads.consent.ConsentStatus
import pt.pchouse.atqrcodereader.MainActivity

class RGPDConsentInfoUpdateListener(private var context: Context) : ConsentInfoUpdateListener {

    override fun onConsentInfoUpdated(consentStatus: ConsentStatus?) {
        MainActivity.IsRGPDChecked = true

        if (!AppOpenManager.isRequestLocationInEeaOrUnknown(context)) {
            AppOpenManager.setApplicationContentStatus(context, ConsentStatus.PERSONALIZED)
            return
        }

        if (consentStatus == null) {
            AppOpenManager.personalized = AppOpenManager.isPersonalizedAddsInSettings(
                context.applicationContext
            ) ?: false
            return
        }

        AppOpenManager.setApplicationContentStatus(context, consentStatus)
    }

    override fun onFailedToUpdateConsentInfo(reason: String?) {
        AppOpenManager.personalized = AppOpenManager.isPersonalizedAddsInSettings(
            context.applicationContext
        ) ?: false
    }

}