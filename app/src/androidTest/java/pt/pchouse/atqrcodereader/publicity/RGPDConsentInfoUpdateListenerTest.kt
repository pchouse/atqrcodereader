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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.consent.ConsentInformation
import com.google.ads.consent.ConsentStatus
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RGPDConsentInfoUpdateListenerTest {

    @Test
    fun testOnConsentFormClosedStatusPersonalized(){
        val context: Context = ApplicationProvider.getApplicationContext()
        val listener = RGPDConsentInfoUpdateListener(context)
        listener.onConsentInfoUpdated(ConsentStatus.PERSONALIZED)

        Assert.assertEquals(
            ConsentStatus.PERSONALIZED,
            ConsentInformation.getInstance(context).consentStatus
        )

        Assert.assertTrue(AppOpenManager.personalized)
        Assert.assertTrue(
            AppOpenManager.isPersonalizedAddsInSettings(context.applicationContext) ?: false
        )
    }

    @Test
    fun testOnConsentFormClosedStatusNonPersonalized(){
        val context: Context = ApplicationProvider.getApplicationContext()
        val listener = RGPDConsentInfoUpdateListener(context)
        listener.onConsentInfoUpdated(ConsentStatus.NON_PERSONALIZED)

        Assert.assertEquals(
            ConsentStatus.NON_PERSONALIZED,
            ConsentInformation.getInstance(context).consentStatus
        )

        Assert.assertFalse(AppOpenManager.personalized)
        Assert.assertFalse(
            AppOpenManager.isPersonalizedAddsInSettings(context.applicationContext) ?: false
        )
    }

    @Test
    fun testOnConsentFormClosedStatusUnknown(){
        val context: Context = ApplicationProvider.getApplicationContext()
        val listener = RGPDConsentInfoUpdateListener(context)
        listener.onConsentInfoUpdated(ConsentStatus.UNKNOWN)

        Assert.assertEquals(
            ConsentStatus.UNKNOWN,
            ConsentInformation.getInstance(context).consentStatus
        )

        Assert.assertFalse(AppOpenManager.personalized)
        Assert.assertFalse(
            AppOpenManager.isPersonalizedAddsInSettings(context.applicationContext) ?: false
        )
    }

    @Test
    fun testOnFailedToUpdateConsentInfo(){
        val context: Context = ApplicationProvider.getApplicationContext()
        val listener = RGPDConsentInfoUpdateListener(context)
        listOf(true, false, true).forEach {
            AppOpenManager.isPersonalizedAddsInSettings(context, it)
            listener.onFailedToUpdateConsentInfo("Error description")
            Assert.assertEquals(it, AppOpenManager.personalized)
        }
    }

}