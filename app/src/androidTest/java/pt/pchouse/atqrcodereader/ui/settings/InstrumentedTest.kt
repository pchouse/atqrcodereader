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

package pt.pchouse.atqrcodereader.ui.settings

import android.content.Context
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pt.pchouse.atqrcodereader.AServiceLocator
import pt.pchouse.atqrcodereader.MainActivity
import pt.pchouse.atqrcodereader.R
import pt.pchouse.atqrcodereader.ServiceLocator

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class InstrumentedTest {

    @Before
    fun before() {
        // Mock Context - Only absolute necessary to test fragments in isolation mode
        ServiceLocator.locate = object : AServiceLocator() {
            override fun getMainContext(): Context {
                return InstrumentationRegistry.getInstrumentation().targetContext
            }
        }

        runBlocking {
            if (ServiceLocator.locate.db().settings().getAsync() == null) {
                ServiceLocator.locate.db().settings().saveAsync(
                    Model(1, true, "http://localhost")
                )
            }
        }
    }

    @After
    fun after() {
        // Remove Mock Context
        ServiceLocator.locate = object : AServiceLocator() {}
    }

    @Test
    fun useAppContext() {

        val db = ServiceLocator.locate.db().settings()
        val testUrl = "https://testapi/androidUnitTest"
        var oldApiUrlUi: String? = null
        var oldShowAllUi: Boolean? = null

        val scenario = ActivityScenario.launch(MainActivity::class.java).onActivity {
            MainActivity.getBottomNavView().selectedItemId = R.id.navigation_settings
        }

        scenario.onActivity {
            oldApiUrlUi = it.findViewById<TextView>(R.id.settings_api_url).text.toString()
            oldShowAllUi = it.findViewById<SwitchCompat>(R.id.settings_show_all_fields).isChecked
        }

        runBlocking {

            val oldSettings = db.getAsync()!!

            assertEquals(oldSettings.apiUrl, oldApiUrlUi)
            assertEquals(oldSettings.showAllFields, oldShowAllUi)

            Espresso.onView(ViewMatchers.withId(R.id.settings_api_url))
                .perform(ViewActions.replaceText(testUrl))

            Espresso.onView(ViewMatchers.withId(R.id.settings_show_all_fields))
                .perform(ViewActions.click())

            val settings = db.getAsync()

            assertEquals(testUrl, settings?.apiUrl)
            assertEquals(!oldShowAllUi!!, settings?.showAllFields)

            db.saveAsync(oldSettings)
        }
    }
}