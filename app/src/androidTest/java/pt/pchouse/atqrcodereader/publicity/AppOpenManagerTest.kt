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
import com.google.ads.consent.DebugGeography
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppOpenManagerTest {

    @Test
    fun testSetApplicationContentStatusPersonalized() {
        val context: Context = ApplicationProvider.getApplicationContext()
        AppOpenManager.setApplicationContentStatus(context, ConsentStatus.PERSONALIZED)

        Assert.assertTrue(ConsentInformation.getInstance(context).consentStatus == ConsentStatus.PERSONALIZED)
        Assert.assertTrue(AppOpenManager.personalized)
        Assert.assertTrue(AppOpenManager.isPersonalizedAddsInSettings(context) ?: false)
    }

    @Test
    fun testSetApplicationContentStatusNotPersonalized() {
        val context: Context = ApplicationProvider.getApplicationContext()

        listOf(ConsentStatus.NON_PERSONALIZED, ConsentStatus.UNKNOWN).forEach {

            AppOpenManager.setApplicationContentStatus(context, it)

            Assert.assertTrue(ConsentInformation.getInstance(context).consentStatus == it)
            Assert.assertFalse(AppOpenManager.personalized)
            Assert.assertFalse(AppOpenManager.isPersonalizedAddsInSettings(context) ?: false)
        }
    }

    @Test
    fun testIsPersonalizedAddsInSettings(){
        val context: Context = ApplicationProvider.getApplicationContext()
        listOf(true, false, true).forEach {
            Assert.assertEquals(it, AppOpenManager.isPersonalizedAddsInSettings(context, it))
            Assert.assertEquals(it, AppOpenManager.isPersonalizedAddsInSettings(context))
        }
    }

    @Test
    fun testIsRequestLocationInEeaOrUnknown_True(){
        val context: Context = ApplicationProvider.getApplicationContext()
        ConsentInformation.getInstance(context).debugGeography = DebugGeography.DEBUG_GEOGRAPHY_EEA
        Assert.assertTrue(AppOpenManager.isRequestLocationInEeaOrUnknown(context))
    }

    /**
    @Test
    fun testIsRequestLocationInEeaOrUnknown_False(){
        val context: Context = ApplicationProvider.getApplicationContext()
        ConsentInformation.getInstance(context).addTestDevice("B4BB811D278DC05C611F01976D60F939");
        ConsentInformation.getInstance(context).debugGeography = DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA
        Assert.assertFalse(AppOpenManager.isRequestLocationInEeaOrUnknown(context))
    }
    */
}