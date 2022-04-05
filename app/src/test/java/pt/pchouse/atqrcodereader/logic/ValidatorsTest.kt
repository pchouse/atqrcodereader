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

package pt.pchouse.atqrcodereader.logic

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class ValidatorsTest {

    companion object {
        val invoices = arrayOf(Type.FT, Type.FS, Type.FR, Type.NC, Type.ND)
        val movements = arrayOf(Type.GR, Type.GT, Type.GA, Type.GC, Type.GD)

        //val payments = arrayOf(Type.RC, Type.RG)
        val working =
            arrayOf(Type.CM, Type.CC, Type.FC, Type.FO, Type.NE, Type.OU, Type.OR, Type.PF)
        val insurance = arrayOf(Type.RP, Type.RE, Type.CS, Type.LD, Type.RA)

        private fun getResValuesPath(): Path {
            return Paths.get(
                QRCodeTest::class.java.classLoader!!.getResource("./").file.toString()
                    .split("app/build/tmp")[0].trim('/'),
                "app/src/main/res/values"
            )!!
        }

        fun resourcesStringExists(name: String): Boolean {
            val stringFile = File(
                Paths.get(
                    getResValuesPath().toString(),
                    "strings.xml"
                )!!.toString()
            )

            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringFile)
            return XPathFactory.newInstance().newXPath().evaluate(
                "//@name='$name'", doc, XPathConstants.BOOLEAN
            ) as Boolean
        }

    }

    @Test
    fun testConvertStringTypeDocumentToDocumentTypeGroup() {

        val groupStack: Map<Type, Group> = mapOf(
            // Invoice
            Type.FT to Group.Invoice,
            Type.FS to Group.Invoice,
            Type.FR to Group.Invoice,
            Type.ND to Group.Invoice,
            Type.NC to Group.Invoice,
            // Movement
            Type.GR to Group.Movement,
            Type.GT to Group.Movement,
            Type.GA to Group.Movement,
            Type.GC to Group.Movement,
            Type.GD to Group.Movement,
            // Working
            Type.CM to Group.Working,
            Type.CC to Group.Working,
            Type.FC to Group.Working,
            Type.FO to Group.Working,
            Type.NE to Group.Working,
            Type.OU to Group.Working,
            Type.OR to Group.Working,
            Type.PF to Group.Working,
            // Payment
            Type.RC to Group.Payment,
            Type.RG to Group.Payment,
            // Insurance
            Type.RP to Group.Insurance,
            Type.RE to Group.Insurance,
            Type.CS to Group.Insurance,
            Type.LD to Group.Insurance,
            Type.RA to Group.Insurance,
        )

        for (type in groupStack) {
            assertEquals(
                type.value,
                Validators.convertToDocumentGroup(type.key)
            )
        }
    }

    @Test
    fun testDocumentStatus() {

        val valuesStack: MutableMap<Status, MutableMap<Type, Boolean>> = mutableMapOf(
            Status.N to mutableMapOf(),
            Status.A to mutableMapOf(),
            Status.F to mutableMapOf(),
            Status.R to mutableMapOf(),
            Status.S to mutableMapOf(),
            Status.T to mutableMapOf(),
        )

        for (type in Type.values()) {

            valuesStack[Status.N]!![type] = true

            valuesStack[Status.A]!![type] = true

            valuesStack[Status.T]!![type] = movements.contains(type)

            valuesStack[Status.S]!![type] = invoices.contains(type) || insurance.contains(type)

            valuesStack[Status.R]!![type] = invoices.contains(type)
                    || insurance.contains(type)
                    || movements.contains(type)

            valuesStack[Status.F]!![type] = invoices.contains(type)
                    || insurance.contains(type)
                    || movements.contains(type)
                    || working.contains(type)

        }

        for (statusStack in valuesStack) {
            val key = statusStack.key
            for (typeValues in statusStack.value) {
                assertEquals(
                    typeValues.value,
                    Validators.status(key, typeValues.key).isValid
                )

                assertEquals(
                    typeValues.value,
                    Validators.status(key.toString(), typeValues.key.toString()).isValid
                )

            }
        }
    }

    @Test
    fun testType() {
        for (type in Type.values()) {
            assertTrue(Validators.type(type.toString()).isValid)
        }

        val invalid = Validators.type("AA")
        assertFalse(invalid.isValid)
        assertFalse(invalid.errors.isEmpty())
        assertEquals(invalid.errors.first(), "value_not_valid")
        assertTrue(resourcesStringExists(invalid.errors.first()))
    }

    @Test
    fun testDate() {
        val valuesStack = arrayOf(
            Pair("20210101", true),
            Pair("20221005", true),
            Pair("20201231", false),
            Pair("05102010", false), //ddMMyyyy
            Pair("12312002", false), //MMddyyyy
        )

        for (pair in valuesStack) {
            val validatorResult = Validators.date(pair.first)
            assertEquals(
                "Fail: " + pair.first,
                pair.second,
                validatorResult.isValid
            )

            if (!pair.second) {
                assertTrue(validatorResult.errors.isNotEmpty())
                assertTrue(resourcesStringExists(validatorResult.errors.first()))
            }
        }
    }

    @Test
    fun testAtcud() {

        val valuesStack = mapOf(
            "0" to mapOf(
                true to listOf(Pair("FT A/999", "20221231")),
                false to listOf(Pair("FT A/999", "20231231"))
            ),
            "A".padEnd(9, 'A') + "-999" to mapOf(
                false to listOf(Pair("FT A/999", "20231231"))
            ),
            "A".padEnd(7, 'A') + "-999" to mapOf(
                false to listOf(Pair("FT A/999", "20231231"))
            ),
            "A".padEnd(8, 'A') + "-999" to mapOf(
                false to listOf(Pair("FT A/9999", "20231231")),
                true to listOf(Pair("FT A/999", "20231231")),
            ),
            "C".padEnd(8, 'A') + "-" + "9".padEnd(62, '9') to mapOf(
                false to listOf(Pair("FT A/" + "9".padEnd(62, '9'), "20231231")),
            ),
            "D".padEnd(8, 'A') + "-" + "9".padEnd(61, '9') to mapOf(
                true to listOf(Pair("FT A/" + "9".padEnd(61, '9'), "20231231")),
            ),
        )

        for (value in valuesStack) {
            val atcud = value.key
            for (resultStack in value.value) {
                val result = resultStack.key
                for (pair in resultStack.value) {

                    val validatorResult = Validators.atcud(atcud, pair.first, pair.second)

                    assertEquals(
                        "Fail ATCUD: $atcud, DocUniId: ${pair.first}, Date: ${pair.second}",
                        result,
                        validatorResult.isValid
                    )

                    if (!result) {
                        assertTrue(validatorResult.errors.isNotEmpty())
                        assertTrue(resourcesStringExists(validatorResult.errors.first()))
                    }
                }
            }
        }
    }

    @Test
    fun testCustomerTin() {

        val values = mapOf(
            true to mapOf(
                "999999990" to "PT",
                "129792659" to "PT",
                "106493485" to "PT",
                "275646530" to "PT",
                "387598294" to "PT",
                "453585035" to "PT",
                "578837455" to "PT",
                "677364725" to "PT",
                "718430328" to "PT",
                "712104976" to "PT",
                "726638242" to "PT",
                "745770363" to "PT",
                "759962413" to "PT",
                "777196832" to "PT",
                "786222468" to "PT",
                "795520298" to "PT",
                "805022635" to "PT",
                "904361381" to "PT",
                "915885948" to "PT",
                "981784054" to "PT",
                "995207968" to "PT",
                "995207999" to "ES",
            ),
            false to mapOf(
                "999999999" to "PT",
                "129792699" to "PT",
                "106499485" to "PT",
                "275946530" to "PT",
                "99999990" to "PT",
                "9999999909" to "PT",
            ),
        )

        for (expected in values)
            for (value in expected.value) {

                val validatorResult = Validators.customerTin(value.key, value.value)

                assertEquals(
                    "Tin ${value.key} from country ${value.value}",
                    expected.key,
                    validatorResult.isValid
                )

                if (!expected.key) {
                    assertTrue(validatorResult.errors.isNotEmpty())
                    assertTrue(resourcesStringExists(validatorResult.errors.first()))
                }
            }
    }

    @Test
    fun testIssuerTin() {

        val values = mapOf(
            true to arrayOf(
                "129792659",
                "106493485",
                "275646530",
                "387598294",
                "453585035",
                "578837455",
                "677364725",
                "718430328",
                "712104976",
                "726638242",
                "745770363",
                "759962413",
                "777196832",
                "786222468",
                "795520298",
                "805022635",
                "904361381",
                "915885948",
                "981784054",
                "995207968",
            ),
            false to arrayOf(
                "999999990",
                "999999999",
                "129792699",
                "106499485",
                "275946530",
                "995207999",
                "99999990",
                "1297926599",
            ),
        )

        for (expected in values)
            for (value in expected.value) {

                val validatorResult = Validators.issuerTin(value)

                assertEquals(
                    "Tin $value ",
                    expected.key,
                    validatorResult.isValid
                )

                if (!expected.key) {
                    assertTrue(validatorResult.errors.isNotEmpty())
                    assertTrue(resourcesStringExists(validatorResult.errors.first()))
                }
            }
    }

    @Test
    fun testFiscalRegion() {

        var validatorResult: ValidatorResult

        for (notValid in listOf("1", "PT-AM", "PT-AC")) {

            validatorResult = Validators.fiscalRegion(notValid, listOf())

            assertFalse(
                "Value $notValid should not validate because doesn't is not equal to '0' or 'PT'",
                validatorResult.isValid
            )

            assertTrue(validatorResult.errors.isNotEmpty())
            assertTrue(resourcesStringExists(validatorResult.errors.first()))
        }

        assertTrue(
            Validators.fiscalRegion("0", listOf()).isValid
        )

        assertTrue(
            Validators.fiscalRegion("0", listOf("")).isValid
        )

        validatorResult = Validators.fiscalRegion("0", listOf("", "0.0"))
        assertFalse(validatorResult.isValid)

        validatorResult = Validators.fiscalRegion("PT", listOf("", "0.0"))
        assertTrue(validatorResult.isValid)

        validatorResult = Validators.fiscalRegion("PT", listOf("0.0", "1.0", ""))
        assertTrue(validatorResult.isValid)

        validatorResult = Validators.fiscalRegion("PT", listOf("0.09"))
        assertTrue(validatorResult.isValid)

        validatorResult = Validators.fiscalRegion("0", listOf("", "0.09"))
        assertFalse(validatorResult.isValid)

        validatorResult = Validators.fiscalRegion("PT", listOf("", ""))
        assertFalse(validatorResult.isValid)

    }

    @Test
    fun testSumEqual() {

        val valuesStack = mapOf(
            true to listOf(
                Pair("0.00", listOf("")),
                Pair("999.99", listOf("", "900.99", "", "99.00")),
                Pair("99.99", listOf("", "99.99"))
            ),

            false to listOf(
                Pair("0.00", listOf("1.0")),
                Pair("1.00", listOf("0.0")),
                Pair("1.00", listOf("")),
                Pair("", listOf("")),
                Pair("999.99", listOf("", "901.99", "", "99.00")),
                Pair("99.99", listOf("", "990.99"))
            )
        )

        for (value in valuesStack) {
            for (toSumStack in value.value) {
                val validatorResult = Validators.sumEqual(toSumStack.first, toSumStack.second)
                assertEquals(value.key, validatorResult.isValid)

                if (!value.key) {
                    assertTrue(validatorResult.errors.isNotEmpty())
                    assertTrue(
                        "Resource: ${validatorResult.errors.first()}",
                        resourcesStringExists(validatorResult.errors.first())
                    )
                }
            }
        }
    }

    @Test
    fun testEuroTin() {
        assertTrue(
            Validators.euroTin(
                "PT", "191809276"
            ).isValid
        )

        val validatorResult = Validators.euroTin("PT", "191809279")
        assertFalse(validatorResult.isValid)
        assertTrue(validatorResult.errors.isNotEmpty())
        assertTrue(resourcesStringExists(validatorResult.errors.first()))

    }

}