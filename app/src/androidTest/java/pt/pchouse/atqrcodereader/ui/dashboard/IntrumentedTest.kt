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

package pt.pchouse.atqrcodereader.ui.dashboard

import android.content.Context
import android.os.SystemClock
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pt.pchouse.atqrcodereader.AServiceLocator
import pt.pchouse.atqrcodereader.MainActivity
import pt.pchouse.atqrcodereader.R
import pt.pchouse.atqrcodereader.ServiceLocator
import pt.pchouse.atqrcodereader.logic.FieldCode
import pt.pchouse.atqrcodereader.ui.home.HomeFragment
import pt.pchouse.atqrcodereader.ui.settings.Model

@RunWith(AndroidJUnit4::class)
class InstrumentedTest {

    companion object {

        val fullFields = mutableMapOf(
            FieldCode.valueOf("A") to "123456789",
            FieldCode.valueOf("B") to "999999990",
            FieldCode.valueOf("C") to "PT",
            FieldCode.valueOf("D") to "FT",
            FieldCode.valueOf("E") to "N",
            FieldCode.valueOf("F") to "20191231",
            FieldCode.valueOf("G") to "FT AB2019/0035",
            FieldCode.valueOf("H") to "CSDF7T5H-0035",
            FieldCode.valueOf("I1") to "PT",
            FieldCode.valueOf("I2") to "12000.00",
            FieldCode.valueOf("I3") to "15000.00",
            FieldCode.valueOf("I4") to "900.00",
            FieldCode.valueOf("I5") to "50000.00",
            FieldCode.valueOf("I6") to "6500.00",
            FieldCode.valueOf("I7") to "80000.00",
            FieldCode.valueOf("I8") to "18400.00",
            FieldCode.valueOf("J1") to "PT-AC",
            FieldCode.valueOf("J2") to "10000.00",
            FieldCode.valueOf("J3") to "25000.56",
            FieldCode.valueOf("J4") to "1000.02",
            FieldCode.valueOf("J5") to "75000.00",
            FieldCode.valueOf("J6") to "6750.00",
            FieldCode.valueOf("J7") to "100000.00",
            FieldCode.valueOf("J8") to "18000.00",
            FieldCode.valueOf("K1") to "PT-MA",
            FieldCode.valueOf("K2") to "5000.00",
            FieldCode.valueOf("K3") to "12500.00",
            FieldCode.valueOf("K4") to "625.00",
            FieldCode.valueOf("K5") to "25000.00",
            FieldCode.valueOf("K6") to "3000.00",
            FieldCode.valueOf("K7") to "40000.00",
            FieldCode.valueOf("K8") to "8800.00",
            FieldCode.valueOf("L") to "100.00",
            FieldCode.valueOf("M") to "25.00",
            FieldCode.valueOf("N") to "64000.02",
            FieldCode.valueOf("O") to "513600.58",
            FieldCode.valueOf("P") to "100.002",
            FieldCode.valueOf("Q") to "kLp0",
            FieldCode.valueOf("R") to "9999",
            FieldCode.valueOf("S") to "TB;PT00000000000000000000000;513500.58"
        )
    }

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

    private fun mapToRawText(fields: MutableMap<FieldCode, String>): String {

        val string = StringBuilder()

        fields.forEach {
            string.append(it.key.name)
                .append(":")
                .append(it.value)
                .append("*")
        }

        return string.toString().trimEnd('*')
    }

    @Test
    fun testFullQrCode() {

        val fieldToTest = fullFields
        val rawText = mapToRawText(fieldToTest)

        ActivityScenario.launch(MainActivity::class.java).onActivity {
            HomeFragment.qrTextNew = true
            HomeFragment.qrText = rawText
            MainActivity.getBottomNavView().selectedItemId = R.id.navigation_dashboard
        }

        fieldToTest.forEach { (fieldCode, value) ->
            val start = SystemClock.currentThreadTimeMillis()
            do {
                var cont = false
                try {
                    Espresso.onView(
                        ViewMatchers.withTagValue(
                            Matchers.equalTo("${DashboardFragment.TAG_FIELD}_${fieldCode.name}")
                        )
                    ).check { view, noViewFoundException ->

                        if (noViewFoundException != null) {
                            throw noViewFoundException
                        }

                        Assert.assertEquals(
                            "Test field ${fieldCode.name}",
                            fieldCode.name,
                            (view as TextView).text
                        )
                    }

                    Espresso.onView(
                        ViewMatchers.withTagValue(
                            Matchers.equalTo("${DashboardFragment.TAG_VALUE}_${fieldCode.name}")
                        )
                    ).check { view, noViewFoundException ->

                        if (noViewFoundException != null) {
                            throw noViewFoundException
                        }

                        Assert.assertEquals(
                            "Test field $value",
                            value,
                            (view as TextView).text
                        )
                    }
                } catch (e: NoMatchingViewException) {
                    if ((SystemClock.currentThreadTimeMillis() - start) > 1000 * 60 * 2) {
                        Assert.fail(e.message)
                    }
                    cont = true
                }
            } while (cont)
        }

        Espresso.onView(ViewMatchers.withId(R.id.qr_code_raw_value))
            .check { view, _ ->
                Assert.assertEquals(
                    rawText,
                    (view as TextView).text
                )
            }
    }

    @Test
    fun testQrCodeWithWrongValue() {

        val fieldToTest = fullFields
        fullFields[FieldCode.A] = "999999990"

        ActivityScenario.launch(MainActivity::class.java).onActivity {
            HomeFragment.qrTextNew = true
            HomeFragment.qrText = mapToRawText(fieldToTest)
            MainActivity.getBottomNavView().selectedItemId = R.id.navigation_dashboard
        }

        fieldToTest.forEach { (fieldCode, value) ->
            val start = SystemClock.currentThreadTimeMillis()
            do {
                var cont = false
                try {

                    Espresso.onView(
                        ViewMatchers.withTagValue(
                            Matchers.equalTo("${DashboardFragment.TAG_FIELD}_${fieldCode.name}")
                        )
                    ).check { view, noViewFoundException ->

                        if (noViewFoundException != null) {
                            throw noViewFoundException
                        }

                        Assert.assertEquals(
                            "Test field ${fieldCode.name}",
                            fieldCode.name,
                            (view as TextView).text
                        )
                    }

                    Espresso.onView(
                        ViewMatchers.withTagValue(
                            Matchers.equalTo("${DashboardFragment.TAG_VALUE}_${fieldCode.name}")
                        )
                    ).check { view, noViewFoundException ->

                        if (noViewFoundException != null) {
                            throw noViewFoundException
                        }

                        Assert.assertEquals(
                            "Test field $value",
                            value,
                            (view as TextView).text
                        )
                    }
                } catch (e: NoMatchingViewException) {
                    if ((SystemClock.currentThreadTimeMillis() - start) > 1000 * 60 * 2) {
                        Assert.fail(e.message)
                    }
                    cont = true
                }
            } while (cont)
        }

        Espresso.onView(
            ViewMatchers.withTagValue(
                Matchers.equalTo("${DashboardFragment.TAG_FIELD}_${FieldCode.A}")
            )
        ).check { view, _ ->

            val expected = ContextCompat.getColor(
                InstrumentationRegistry.getInstrumentation().targetContext,
                R.color.field_error
            )

            val result = (view as TextView).currentTextColor

            Assert.assertEquals(
                "Test field ${FieldCode.A.name}",
                expected,
                result
            )
        }

    }

    @Test
    fun testWithApiError() {

        var settings: Model?

        runBlocking {
            settings = ServiceLocator.locate.db().settings().getAsync()
            settings?.apiUrl = "http://localhost:8999"
        }

        try {

            val fieldToTest = fullFields
            val rawText = mapToRawText(fieldToTest)

            ActivityScenario.launch(MainActivity::class.java).onActivity {
                HomeFragment.qrTextNew = true
                HomeFragment.qrText = rawText
                MainActivity.getBottomNavView().selectedItemId = R.id.navigation_dashboard
            }

            fieldToTest.forEach { (fieldCode, value) ->
                val start = SystemClock.currentThreadTimeMillis()
                do {
                    var cont = false
                    try {
                        Espresso.onView(
                            ViewMatchers.withTagValue(
                                Matchers.equalTo("${DashboardFragment.TAG_FIELD}_${fieldCode.name}")
                            )
                        ).check { view, noViewFoundException ->

                            if (noViewFoundException != null) {
                                throw noViewFoundException
                            }

                            Assert.assertEquals(
                                "Test field ${fieldCode.name}",
                                fieldCode.name,
                                (view as TextView).text
                            )
                        }

                        Espresso.onView(
                            ViewMatchers.withTagValue(
                                Matchers.equalTo("${DashboardFragment.TAG_VALUE}_${fieldCode.name}")
                            )
                        ).check { view, noViewFoundException ->

                            if (noViewFoundException != null) {
                                throw noViewFoundException
                            }

                            Assert.assertEquals(
                                "Test field $value",
                                value,
                                (view as TextView).text
                            )
                        }
                    } catch (e: NoMatchingViewException) {
                        if ((SystemClock.currentThreadTimeMillis() - start) > 1000 * 60 * 2) {
                            Assert.fail(e.message)
                        }
                        cont = true
                    }
                } while (cont)
            }

            Espresso.onView(ViewMatchers.withId(R.id.qr_code_raw_value))
                .check { view, _ ->
                    Assert.assertEquals(
                        rawText,
                        (view as TextView).text
                    )
                }

        } finally {
            settings?.apiUrl = ""
            runBlocking {
                if (settings != null) {
                    ServiceLocator.locate.db().settings().saveAsync(settings!!)
                }
            }
        }


    }

}
