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

import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import pt.pchouse.atqrcodereader.AServiceLocator
import pt.pchouse.atqrcodereader.ServiceLocator
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

class QRCodeTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            // Mock Service locator get string resource
            ServiceLocator.locate = object : AServiceLocator() {
                override fun getString(name: String): String {
                    return name
                }
            }
        }

        @AfterClass
        @JvmStatic
        fun afterClass() {
            // Remove Mock Service locator
            ServiceLocator.locate = object : AServiceLocator() {}
        }

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

    @Before
    fun before() {
        ServiceLocator.locate = object : AServiceLocator() {
            override fun hasInternetConnection(): Boolean {
                return true
            }

            override fun getString(name: String): String {
                return name
            }
        }
    }

    @After
    fun after() {
        ServiceLocator.locate = object : AServiceLocator() {}
    }

    /**
     * Get the values instance fo QR code
     */
    private fun getValuesField(qrCode: QRCode): MutableMap<FieldCode, ParsedCode> {
        val field = QRCode::class.java.getDeclaredField("values")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(qrCode) as MutableMap<FieldCode, ParsedCode>
    }

    /**
     * Get the values instance fo QR code
     */
    private fun setValuesField(qrCode: QRCode, values: MutableMap<FieldCode, ParsedCode>) {
        val field = QRCode::class.java.getDeclaredField("values")
        field.isAccessible = true
        field.set(qrCode, values)
    }

    /**
     * Invoke the validateField method
     */
    private fun invokeValidateField(qrCode: QRCode, fieldCode: FieldCode) {
        QRCode::class.memberFunctions.first { it.name == "validateField" }.apply {
            isAccessible = true
            call(qrCode, fieldCode)
        }
    }

    @Test
    fun testFieldA() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.A
        val qrCode = QRCode()

        // Test of tin
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With no tin should be invalid because is mandatory", parsedCode.isValid)
        assertEquals("", parsedCode.value)
        assertEquals("field_is_mandatory_but_not_exist", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // Test a valid TIN
        val validTin = "129792659"
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(validTin, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertTrue("The tin $validTin should be validated has valid", parsedCode.isValid)
        assertEquals(validTin, parsedCode.value)
        assertTrue(parsedCode.errors.isEmpty())

        // Test wrong tin format
        val tinWrongFormat = "1297926999"
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(tinWrongFormat, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("The tin $tinWrongFormat should be validated has not valid", parsedCode.isValid)
        assertEquals(tinWrongFormat, parsedCode.value)
        assertEquals("value_format_does_not_respect_regexp", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // Test tin empty
        val tinEmpty = ""
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(tinEmpty, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With empty tin should be validated has not valid", parsedCode.isValid)
        assertEquals(tinEmpty, parsedCode.value)
        assertEquals("field_without_value", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // verify final consumer as issuer. must invalidate
        val tinFinalConsumerFormat = "999999990"
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(tinFinalConsumerFormat, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse(
            "The tin $tinFinalConsumerFormat should be validated has not valid",
            parsedCode.isValid
        )
        assertEquals(tinFinalConsumerFormat, parsedCode.value)
        assertEquals("value_not_valid", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

    }

    @Test
    fun testFieldB() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.B
        val qrCode = QRCode()

        // Test of tin
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With no tin should be invalid because is mandatory", parsedCode.isValid)
        assertEquals("", parsedCode.value)
        assertEquals("field_is_mandatory_but_not_exist", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // Test a valid TIN
        for (validTin in listOf("129792659", "999999990")) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(validTin, true, mutableListOf()),
                    FieldCode.C to ParsedCode("PT", true, mutableListOf())
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue("The tin $validTin should be validated has valid", parsedCode.isValid)
            assertEquals(validTin, parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

        // Test wrong tin format
        val tinWrongFormat = "1297926999"
        setValuesField(
            qrCode,
            mutableMapOf(
                fieldCode to ParsedCode(tinWrongFormat, true, mutableListOf()),
                FieldCode.C to ParsedCode("PT", true, mutableListOf())
            )
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("The tin $tinWrongFormat should be validated has not valid", parsedCode.isValid)
        assertEquals(tinWrongFormat, parsedCode.value)
        assertEquals("value_not_valid", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // wrong tin
        val tinFinalConsumerFormat = "999999999"
        setValuesField(
            qrCode,
            mutableMapOf(
                fieldCode to ParsedCode(tinFinalConsumerFormat, true, mutableListOf()),
                FieldCode.C to ParsedCode("PT", true, mutableListOf())
            )
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse(
            "The tin $tinFinalConsumerFormat should be validated has not valid",
            parsedCode.isValid
        )
        assertEquals(tinFinalConsumerFormat, parsedCode.value)
        assertEquals("value_not_valid", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // test oft PT tin
        for (validTin in listOf(
            "129792659",
            "999999990",
            "999999999",
            "1",
            "KO999",
            "A".padEnd(30, '9')
        )) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(validTin, true, mutableListOf()),
                    FieldCode.C to ParsedCode("KO", true, mutableListOf())
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue("The tin $validTin should be validated has valid", parsedCode.isValid)
            assertEquals(validTin, parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

        // wrong max length
        val tinWrongMaxLength = "A".padEnd(31, '9')
        setValuesField(
            qrCode,
            mutableMapOf(
                fieldCode to ParsedCode(tinWrongMaxLength, true, mutableListOf()),
                FieldCode.C to ParsedCode("KO", true, mutableListOf())
            )
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse(
            "The tin $tinWrongMaxLength should be validated has not valid",
            parsedCode.isValid
        )
        assertEquals(tinWrongMaxLength, parsedCode.value)
        assertEquals("value_length_greater_than_max_length", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

    }

    @Test
    fun testFieldC() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.C
        val qrCode = QRCode()

        // Test of country
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With no tin should be invalid because is mandatory", parsedCode.isValid)
        assertEquals("", parsedCode.value)
        assertEquals("field_is_mandatory_but_not_exist", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // Test a valid country
        for (country in arrayOf(
            "AD",
            "AE",
            "AF",
            "AG",
            "AI",
            "AL",
            "AM",
            "AO",
            "AQ",
            "AR",
            "AS",
            "AT",
            "AU",
            "AW",
            "AX",
            "AZ",
            "BA",
            "BB",
            "BD",
            "BE",
            "BF",
            "BG",
            "BH",
            "BI",
            "BJ",
            "BL",
            "BM",
            "BN",
            "BO",
            "BQ",
            "BR",
            "BS",
            "BT",
            "BV",
            "BW",
            "BY",
            "BZ",
            "CA",
            "CC",
            "CD",
            "CF",
            "CG",
            "CH",
            "CI",
            "CK",
            "CL",
            "CM",
            "CN",
            "CO",
            "CR",
            "CU",
            "CV",
            "CW",
            "CX",
            "CY",
            "CZ",
            "DE",
            "DJ",
            "DK",
            "DM",
            "DO",
            "DZ",
            "EC",
            "EE",
            "EG",
            "EH",
            "ER",
            "ES",
            "ET",
            "FI",
            "FJ",
            "FK",
            "FM",
            "FO",
            "FR",
            "GA",
            "GB",
            "GD",
            "GE",
            "GF",
            "GG",
            "GH",
            "GI",
            "GL",
            "GM",
            "GN",
            "GP",
            "GQ",
            "GR",
            "GS",
            "GT",
            "GU",
            "GW",
            "GY",
            "HK",
            "HM",
            "HN",
            "HR",
            "HT",
            "HU",
            "ID",
            "IE",
            "IL",
            "IM",
            "IN",
            "IO",
            "IQ",
            "IR",
            "IS",
            "IT",
            "JE",
            "JM",
            "JO",
            "JP",
            "KE",
            "KG",
            "KH",
            "KI",
            "KM",
            "KN",
            "KP",
            "KR",
            "KW",
            "KY",
            "KZ",
            "LA",
            "LB",
            "LC",
            "LI",
            "LK",
            "LR",
            "LS",
            "LT",
            "LU",
            "LV",
            "LY",
            "MA",
            "MC",
            "MD",
            "ME",
            "MF",
            "MG",
            "MH",
            "MK",
            "ML",
            "MM",
            "MN",
            "MO",
            "MP",
            "MQ",
            "MR",
            "MS",
            "MT",
            "MU",
            "MV",
            "MW",
            "MX",
            "MY",
            "MZ",
            "NA",
            "NC",
            "NE",
            "NF",
            "NG",
            "NI",
            "NL",
            "NO",
            "NP",
            "NR",
            "NU",
            "NZ",
            "OM",
            "PA",
            "PE",
            "PF",
            "PG",
            "PH",
            "PK",
            "PL",
            "PM",
            "PN",
            "PR",
            "PS",
            "PT",
            "PW",
            "PY",
            "QA",
            "RE",
            "RO",
            "RS",
            "RU",
            "RW",
            "SA",
            "SB",
            "SC",
            "SD",
            "SE",
            "SG",
            "SH",
            "SI",
            "SJ",
            "SK",
            "SL",
            "SM",
            "SN",
            "SO",
            "SR",
            "SS",
            "ST",
            "SV",
            "SX",
            "SY",
            "SZ",
            "TC",
            "TD",
            "TF",
            "TG",
            "TH",
            "TJ",
            "TK",
            "TL",
            "TM",
            "TN",
            "TO",
            "TR",
            "TT",
            "TV",
            "TW",
            "TZ",
            "UA",
            "UG",
            "UM",
            "US",
            "UY",
            "UZ",
            "VA",
            "VC",
            "VE",
            "VG",
            "VI",
            "VN",
            "VU",
            "WF",
            "WS",
            "XK",
            "YE",
            "YT",
            "ZA",
            "ZM",
            "ZW",
            "Desconhecido"
        )) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(country, true, mutableListOf())
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue("The country $country should be validated has valid", parsedCode.isValid)
            assertEquals(country, parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

        // Test invalid country
        for (notValidCountry in listOf("XX", "PT-MA", "PT-AC")) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(notValidCountry, true, mutableListOf())
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse(
                "The tin $notValidCountry should be validated has not valid",
                parsedCode.isValid
            )
            assertEquals(notValidCountry, parsedCode.value)
            assertEquals("value_format_does_not_respect_regexp", parsedCode.errors.first())
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }

    }

    @Test
    fun testFieldD() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.D
        val qrCode = QRCode()

        // Test of document type
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse(
            "With no document type should be invalid because is mandatory",
            parsedCode.isValid
        )
        assertEquals("", parsedCode.value)
        assertEquals("field_is_mandatory_but_not_exist", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        val typesForTest = arrayOf(
            "FT",
            "FS",
            "FR",
            "ND",
            "NC",
            "GR",
            "GT",
            "GA",
            "GC",
            "GD",
            "CM",
            "CC",
            "FC",
            "FO",
            "NE",
            "OU",
            "OR",
            "PF",
            "RC",
            "RG",
            "RP",
            "RE",
            "CS",
            "LD",
            "RA"
        )

        assertTrue(
            "The document types array for test hasn't the same size as the Types enumeration",
            typesForTest.size == Type.values().size
        )

        for (type in typesForTest) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(type, true, mutableListOf())
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue("The document type $type should be validated has valid", parsedCode.isValid)
            assertEquals(type, parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

        // Test invalid country
        for (notValidType in listOf("XX", "PT")) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(notValidType, true, mutableListOf())
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse(
                "The type $notValidType should be validated has not valid",
                parsedCode.isValid
            )
            assertEquals(notValidType, parsedCode.value)
            assertEquals("value_not_valid", parsedCode.errors.first())
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }
    }

    @Test
    fun testFieldE() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.E
        val qrCode = QRCode()

        // Test of document status
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse(
            "With no document status should be invalid because is mandatory",
            parsedCode.isValid
        )
        assertEquals("", parsedCode.value)
        assertEquals("field_is_mandatory_but_not_exist", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        val statusStack = arrayOf("N", "A", "T", "S", "R", "F")

        assertTrue(
            "The array status size is no teh same as to test",
            statusStack.size == Status.values().size
        )

        for (status in statusStack) {

            // Test status for invoice
            for (invoiceType in listOf(Type.FT, Type.FS, Type.FR, Type.ND, Type.NC)) {

                setValuesField(
                    qrCode,
                    mutableMapOf(
                        fieldCode to ParsedCode(status, true, mutableListOf()),
                        FieldCode.D to ParsedCode(invoiceType.toString(), true, mutableListOf())
                    )
                )

                invokeValidateField(qrCode, fieldCode)
                parsedCode = getValuesField(qrCode)[fieldCode]!!
                assertEquals(status, parsedCode.value)

                if (status != "T") {
                    assertTrue(
                        "The document status $status for $invoiceType should be validated has valid",
                        parsedCode.isValid
                    )
                    assertTrue(parsedCode.errors.isEmpty())
                } else {
                    assertFalse(
                        "The document status $status for $invoiceType should be validated has not valid",
                        parsedCode.isValid
                    )
                    assertEquals("value_not_valid", parsedCode.errors.first())
                    assertTrue(
                        "Not exists in string resources",
                        resourcesStringExists(parsedCode.errors.first())
                    )
                }
            }

            // Test status for movement of goods
            for (movementType in listOf(Type.GR, Type.GT, Type.GA, Type.GC, Type.GD)) {

                setValuesField(
                    qrCode,
                    mutableMapOf(
                        fieldCode to ParsedCode(status, true, mutableListOf()),
                        FieldCode.D to ParsedCode(movementType.toString(), true, mutableListOf())
                    )
                )

                invokeValidateField(qrCode, fieldCode)
                parsedCode = getValuesField(qrCode)[fieldCode]!!
                assertEquals(status, parsedCode.value)

                if (status != "S") {
                    assertTrue(
                        "The document status $status for $movementType should be validated has valid",
                        parsedCode.isValid
                    )
                    assertTrue(parsedCode.errors.isEmpty())
                } else {
                    assertFalse(
                        "The document status $status for $movementType should be validated has not valid",
                        parsedCode.isValid
                    )
                    assertEquals("value_not_valid", parsedCode.errors.first())
                    assertTrue(
                        "Not exists in string resources",
                        resourcesStringExists(parsedCode.errors.first())
                    )
                }
            }

            // Test status for working type
            for (workingType in listOf(
                Type.CM,
                Type.CC,
                Type.FC,
                Type.FO,
                Type.NE,
                Type.OU,
                Type.OR,
                Type.PF
            )) {

                setValuesField(
                    qrCode,
                    mutableMapOf(
                        fieldCode to ParsedCode(status, true, mutableListOf()),
                        FieldCode.D to ParsedCode(workingType.toString(), true, mutableListOf())
                    )
                )

                invokeValidateField(qrCode, fieldCode)
                parsedCode = getValuesField(qrCode)[fieldCode]!!
                assertEquals(status, parsedCode.value)

                if (!listOf("T", "S", "R").contains(status)) {
                    assertTrue(
                        "The document status $status for $workingType should be validated has valid",
                        parsedCode.isValid
                    )
                    assertTrue(parsedCode.errors.isEmpty())
                } else {
                    assertFalse(
                        "The document status $status for $workingType should be validated has not valid",
                        parsedCode.isValid
                    )
                    assertEquals("value_not_valid", parsedCode.errors.first())
                    assertTrue(
                        "Not exists in string resources",
                        resourcesStringExists(parsedCode.errors.first())
                    )
                }
            }

            // Test status for payments
            for (paymentType in listOf(Type.RC, Type.RG)) {

                setValuesField(
                    qrCode,
                    mutableMapOf(
                        fieldCode to ParsedCode(status, true, mutableListOf()),
                        FieldCode.D to ParsedCode(paymentType.toString(), true, mutableListOf())
                    )
                )

                invokeValidateField(qrCode, fieldCode)
                parsedCode = getValuesField(qrCode)[fieldCode]!!
                assertEquals(status, parsedCode.value)

                if (listOf("N", "A").contains(status)) {
                    assertTrue(
                        "The document status $status for $paymentType should be validated has valid",
                        parsedCode.isValid
                    )
                    assertTrue(parsedCode.errors.isEmpty())
                } else {
                    assertFalse(
                        "The document status $status for $paymentType should be validated has not valid",
                        parsedCode.isValid
                    )
                    assertEquals("value_not_valid", parsedCode.errors.first())
                    assertTrue(
                        "Not exists in string resources",
                        resourcesStringExists(parsedCode.errors.first())
                    )
                }
            }

            // Test status for Insurance
            for (insuranceType in listOf(Type.RP, Type.RE, Type.CS, Type.LD, Type.RA)) {

                setValuesField(
                    qrCode,
                    mutableMapOf(
                        fieldCode to ParsedCode(status, true, mutableListOf()),
                        FieldCode.D to ParsedCode(
                            insuranceType.toString(),
                            true,
                            mutableListOf()
                        )
                    )
                )

                invokeValidateField(qrCode, fieldCode)
                parsedCode = getValuesField(qrCode)[fieldCode]!!
                assertEquals(status, parsedCode.value)

                if (status != "T") {
                    assertTrue(
                        "The document status $status for $insuranceType should be validated has valid",
                        parsedCode.isValid
                    )
                    assertTrue(parsedCode.errors.isEmpty())
                } else {
                    assertFalse(
                        "The document status $status for $insuranceType should be validated has not valid",
                        parsedCode.isValid
                    )
                    assertEquals("value_not_valid", parsedCode.errors.first())
                    assertTrue(
                        "Not exists in string resources",
                        resourcesStringExists(parsedCode.errors.first())
                    )
                }
            }

        }

    }

    @Test
    fun testFieldF() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.F
        val qrCode = QRCode()

        // Test of document date
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse(
            "With no document date should be invalid because is mandatory",
            parsedCode.isValid
        )
        assertEquals("", parsedCode.value)
        assertEquals("field_is_mandatory_but_not_exist", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // Test a valid Date
        for (validDate in listOf("20210101", "20221005")) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(validDate, true, mutableListOf())
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue("The date $validDate should be validated has valid", parsedCode.isValid)
            assertEquals(validDate, parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

        // wrong date
        for (dateNotValid in arrayOf("20201231", "02102022")) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(dateNotValid, true, mutableListOf()),
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse(
                "The date $dateNotValid should be validated has not valid",
                parsedCode.isValid
            )
            assertEquals(dateNotValid, parsedCode.value)
            assertEquals("value_not_valid", parsedCode.errors.first())
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }
    }

    @Test
    fun testFieldG() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.G
        val qrCode = QRCode()

        // Test of no document unique identifier
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse(
            "With no document unique identifier should be invalid because is mandatory",
            parsedCode.isValid
        )
        assertEquals("", parsedCode.value)
        assertEquals("field_is_mandatory_but_not_exist", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // Test a valid unique identifier
        val validUniqueIdStack = listOf(
            "999!#$ 999#$%/999",
            "FT FT/999", "A 9/9",
            "b 9/9".padEnd(60, '9'),
            "b 9/9".padStart(60, '9'),
            "C " + "a".padEnd(56, '9') + "/9"
        )

        for (validUniqueId in validUniqueIdStack) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(validUniqueId, true, mutableListOf())
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue(
                "The unique identifier $validUniqueId should be validated has valid",
                parsedCode.isValid
            )
            assertEquals(validUniqueId, parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

        // test wrong unique identifier
        val wrongIdStack = mapOf(
            "field_without_value" to "",
            "value_length_greater_than_max_length" to "A A/9".padEnd(61, '9'),
            "value_format_does_not_respect_regexp" to "A A A/99"
        )

        for (wrongId in wrongIdStack) {

            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(wrongId.value, true, mutableListOf()),
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse(
                "The date ${wrongId.value} should be validated has not valid",
                parsedCode.isValid
            )
            assertEquals(wrongId.value, parsedCode.value)
            assertEquals(wrongId.key, parsedCode.errors.first())
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }

    }

    @Test
    fun testFieldH() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.H
        val qrCode = QRCode()

        // Test of no atcud
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse(
            "With no ATCUD should be invalid because is mandatory",
            parsedCode.isValid
        )
        assertEquals("", parsedCode.value)
        assertEquals("field_is_mandatory_but_not_exist", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // Test a valid ATCUD identifier
        val validAtcudStack = mapOf(
            "0" to Pair("FT FT/9", "20221231"),
            "A9B999A9-999" to Pair("FT FT/999", "20221231"),
            "A9B999A9-999" to Pair("FT FT/999", "20230101"),
            "A9B999A9-".padEnd(70, '9') to
                    Pair("FT FT/".padEnd(67, '9'), "20230101"),
        )

        for (validAtcud in validAtcudStack) {

            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(validAtcud.key, true, mutableListOf()),
                    FieldCode.G to ParsedCode(validAtcud.value.first, true, mutableListOf()),
                    FieldCode.F to ParsedCode(validAtcud.value.second, true, mutableListOf())
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue(
                "The ATCUD ${validAtcud.key} for document " +
                        "${validAtcud.value.first} issued at ${validAtcud.value.second} " +
                        "should be validated has valid",
                parsedCode.isValid
            )
            assertEquals(validAtcud.key, parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

        // Test a invalid ATCUD
        val notValidAtcudStack = mapOf(
            "0" to Pair("FT FT/9", "20230101"),
            "A9B999A9-999" to Pair("FT FT/9999", "20221231"),
            "A9B999A9-9999" to Pair("FT FT/999", "20230101"),
            "A9B999A9-9999" to Pair("FT FT/999", "2023"),
            "A9B999A9-9999" to Pair("999", "2023"),
            "A9B999A9-".padEnd(71, '9') to
                    Pair("FT FT/".padEnd(61, '9'), "20230101"),
        )

        for (wrongAtcud in notValidAtcudStack) {

            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(wrongAtcud.key, true, mutableListOf()),
                    FieldCode.G to ParsedCode(wrongAtcud.value.first, true, mutableListOf()),
                    FieldCode.F to ParsedCode(wrongAtcud.value.second, true, mutableListOf())
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse(
                "The ATCUD ${wrongAtcud.key} should be validated has not valid",
                parsedCode.isValid
            )
            assertEquals(wrongAtcud.key, parsedCode.value)
            assertEquals("value_not_valid", parsedCode.errors.first())
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }
    }

    @Test
    fun testFieldI1() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.I1
        val qrCode = QRCode()

        // Test of no I1
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse(
            "With no I1 should be invalid because is mandatory",
            parsedCode.isValid
        )
        assertEquals("", parsedCode.value)
        assertEquals("field_is_mandatory_but_not_exist", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // Valid 0
        setValuesField(
            qrCode,
            mutableMapOf(
                fieldCode to ParsedCode("0", true, mutableListOf()),
            )
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertTrue(
            "The I1 value '0' should be valid if no iva apply",
            parsedCode.isValid
        )
        assertEquals("0", parsedCode.value)
        assertTrue(parsedCode.errors.isEmpty())


        for (fieldLetter in 'I'..'K') {
            for (index in 2..8) {
                // Valid PT
                setValuesField(
                    qrCode,
                    mutableMapOf(
                        fieldCode to ParsedCode("PT", true, mutableListOf()),
                        FieldCode.valueOf(fieldLetter + index.toString()) to
                                ParsedCode("999.99", true, mutableListOf()),
                    )
                )
                invokeValidateField(qrCode, fieldCode)
                parsedCode = getValuesField(qrCode)[fieldCode]!!
                assertTrue(
                    "The I1 value 'PT' should be valid because ${fieldLetter}${index} has value",
                    parsedCode.isValid
                )
                assertEquals("PT", parsedCode.value)
                assertTrue(parsedCode.errors.isEmpty())
            }
        }
    }

    @Test
    fun testNumericsFromFieldItoK() {

        var parsedCode: ParsedCode
        var fieldCode: FieldCode
        val qrCode = QRCode()

        for (fieldLetter in 'I'..'K') {

            for (index in 2..8) {

                fieldCode = FieldCode.valueOf(fieldLetter + index.toString())

                val dependencyValue: String = when (fieldLetter) {
                    'J' -> "PT-AC"
                    'K' -> "PT-MA"
                    else -> "PT"
                }

                // Test empty
                val empty = ""
                setValuesField(
                    qrCode,
                    mutableMapOf(fieldCode to ParsedCode(empty, true, mutableListOf()))
                )
                invokeValidateField(qrCode, fieldCode)
                parsedCode = getValuesField(qrCode)[fieldCode]!!
                assertFalse("With empty should be validated has not valid", parsedCode.isValid)
                assertEquals(empty, parsedCode.value)
                assertEquals(
                    "field_optional_without_value_cannot_be_in_code",
                    parsedCode.errors.first()
                )
                assertTrue(
                    "Not exists in string resources",
                    resourcesStringExists(parsedCode.errors.first())
                )

                // validate valid
                for (value in listOf("0.01", "9.99", "999.99", "0.00", "9.99".padStart(16, '9'))) {
                    setValuesField(
                        qrCode,
                        mutableMapOf(
                            fieldCode to ParsedCode(value, true, mutableListOf()),
                            FieldCode.valueOf("${fieldLetter}1") to ParsedCode(
                                dependencyValue,
                                true,
                                mutableListOf()
                            )
                        )
                    )
                    invokeValidateField(qrCode, fieldCode)
                    parsedCode = getValuesField(qrCode)[fieldCode]!!
                    assertTrue("With $value should be validated has valid", parsedCode.isValid)
                    assertEquals(value, parsedCode.value)
                    assertTrue(parsedCode.errors.isEmpty())
                }

                // validate invalid because empty dependency
                for (value in listOf("0.01", "9.99", "999.99", "0.00", "9.99".padStart(16, '9'))) {
                    setValuesField(
                        qrCode,
                        mutableMapOf(fieldCode to ParsedCode(value, true, mutableListOf()))
                    )
                    invokeValidateField(qrCode, fieldCode)
                    parsedCode = getValuesField(qrCode)[fieldCode]!!
                    assertFalse(
                        "With $value should be validated has not valid because empty dependency",
                        parsedCode.isValid
                    )
                    assertEquals(value, parsedCode.value)
                    assertEquals("value_not_valid", parsedCode.errors.first())
                    assertTrue(
                        "Not exists in string resources",
                        resourcesStringExists(parsedCode.errors.first())
                    )
                }

                for (notValid in listOf(
                    "999",
                    "99.9",
                    "1.",
                    "0.9",
                    ".40",
                    "9.99".padStart(17, '9')
                )) {
                    setValuesField(
                        qrCode,
                        mutableMapOf(fieldCode to ParsedCode(notValid, true, mutableListOf()))
                    )
                    invokeValidateField(qrCode, fieldCode)
                    parsedCode = getValuesField(qrCode)[fieldCode]!!
                    assertFalse(
                        "With $notValid should be validated has not valid",
                        parsedCode.isValid
                    )
                    assertEquals(notValid, parsedCode.value)
                    assertTrue(
                        parsedCode.errors.first()
                            .matches(
                                Regex(
                                    "value_not_valid" +
                                            "|value_format_does_not_respect_regexp" +
                                            "|value_length_greater_than_max_length"
                                )
                            )
                    )
                    assertTrue(
                        "Not exists in string resources",
                        resourcesStringExists(parsedCode.errors.first())
                    )
                }
            }
        }

    }

    @Test
    fun testFieldJ1() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.J1
        val qrCode = QRCode()

        // Test empty
        val empty = ""
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(empty, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With empty should be validated has not valid", parsedCode.isValid)
        assertEquals(empty, parsedCode.value)
        assertEquals("field_optional_without_value_cannot_be_in_code", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        //Invalid
        for (notValid in listOf("0", "1", "PT", "PT-MA")) {
            setValuesField(
                qrCode,
                mutableMapOf(fieldCode to ParsedCode(notValid, true, mutableListOf()))
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse("With $notValid should be validated has not valid", parsedCode.isValid)
            assertEquals(notValid, parsedCode.value)
            assertEquals("value_format_does_not_respect_regexp", parsedCode.errors.first())
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }

        //valid
        for (index in 2..8) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode("PT-AC", true, mutableListOf()),
                    FieldCode.valueOf("J$index") to ParsedCode("9.99", true, mutableListOf())
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue(
                "Should be validated has valid because 'J$index' has value",
                parsedCode.isValid
            )
            assertEquals("PT-AC", parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

    }

    @Test
    fun testFieldK1() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.K1
        val qrCode = QRCode()

        // Test empty
        val empty = ""
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(empty, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With empty should be validated has not valid", parsedCode.isValid)
        assertEquals(empty, parsedCode.value)
        assertEquals("field_optional_without_value_cannot_be_in_code", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        //Invalid
        for (notValid in listOf("0", "1", "PT", "PT-AC")) {
            setValuesField(
                qrCode,
                mutableMapOf(fieldCode to ParsedCode(notValid, true, mutableListOf()))
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse("With $notValid should be validated has not valid", parsedCode.isValid)
            assertEquals(notValid, parsedCode.value)
            assertEquals("value_format_does_not_respect_regexp", parsedCode.errors.first())
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }

        //valid
        for (index in 2..8) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode("PT-MA", true, mutableListOf()),
                    FieldCode.valueOf("K$index") to ParsedCode("9.99", true, mutableListOf())
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue(
                "Should be validated has valid because 'K$index' has value",
                parsedCode.isValid
            )
            assertEquals("PT-MA", parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

    }

    @Test
    fun testFieldL() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.L
        val qrCode = QRCode()

        // Empty
        val empty = ""
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(empty, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With empty tin should be validated has not valid", parsedCode.isValid)
        assertEquals(empty, parsedCode.value)
        assertEquals("field_optional_without_value_cannot_be_in_code", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        for (value in listOf("0.01", "9.99", "999.99", "0.00", "9.99".padStart(16, '9'))) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(value, true, mutableListOf()),
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue("With $value should be validated has valid", parsedCode.isValid)
            assertEquals(value, parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

        for (notValid in listOf(
            "999",
            "99.9",
            "1.",
            "0.9",
            ".40",
            "9.99".padStart(17, '9')
        )) {
            setValuesField(
                qrCode,
                mutableMapOf(fieldCode to ParsedCode(notValid, true, mutableListOf()))
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse(
                "With $notValid should be validated has not valid",
                parsedCode.isValid
            )
            assertEquals(notValid, parsedCode.value)
            assertTrue(
                parsedCode.errors.first().matches(
                    Regex("value_length_greater_than_max_length|value_format_does_not_respect_regexp")
                )
            )
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }

    }

    @Test
    fun testFieldM() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.M
        val qrCode = QRCode()

        // Empty
        val empty = ""
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(empty, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With empty tin should be validated has not valid", parsedCode.isValid)
        assertEquals(empty, parsedCode.value)
        assertEquals("field_optional_without_value_cannot_be_in_code", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        for (value in listOf("0.01", "9.99", "999.99", "0.00", "9.99".padStart(16, '9'))) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(value, true, mutableListOf()),
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue("With $value should be validated has valid", parsedCode.isValid)
            assertEquals(value, parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

        for (notValid in listOf(
            "999",
            "99.9",
            "1.",
            "0.9",
            ".40",
            "9.99".padStart(17, '9')
        )) {
            setValuesField(
                qrCode,
                mutableMapOf(fieldCode to ParsedCode(notValid, true, mutableListOf()))
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse(
                "With $notValid should be validated has not valid",
                parsedCode.isValid
            )
            assertEquals(notValid, parsedCode.value)
            assertTrue(
                parsedCode.errors.first().matches(
                    Regex("value_length_greater_than_max_length|value_format_does_not_respect_regexp")
                )
            )
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }

    }

    @Test
    fun testFieldN() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.N
        val qrCode = QRCode()

        // Empty
        val empty = ""
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(empty, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With empty tin should be validated has not valid", parsedCode.isValid)
        assertEquals(empty, parsedCode.value)
        assertEquals("field_without_value", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        for (notValid in listOf(
            "999",
            "99.9",
            "1.",
            "0.9",
            ".40",
            "9.99".padStart(17, '9')
        )) {
            setValuesField(
                qrCode,
                mutableMapOf(fieldCode to ParsedCode(notValid, true, mutableListOf()))
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse(
                "With $notValid should be validated has not valid",
                parsedCode.isValid
            )
            assertEquals(notValid, parsedCode.value)
            assertTrue(
                parsedCode.errors.first().matches(
                    Regex("value_length_greater_than_max_length|value_format_does_not_respect_regexp")
                )
            )
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }

        //Wrong sum
        setValuesField(
            qrCode,
            mutableMapOf(
                fieldCode to ParsedCode("999.99", true, mutableListOf()),
                FieldCode.M to ParsedCode("999.99", true, mutableListOf()),
                FieldCode.I4 to ParsedCode("0.01", true, mutableListOf())
            )
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse(
            "Should be validated has not valid",
            parsedCode.isValid
        )
        assertEquals("999.99", parsedCode.value)
        assertTrue(
            parsedCode.errors.first().matches(
                Regex("value_not_valid")
            )
        )
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // Valid sum
        setValuesField(
            qrCode,
            mutableMapOf(
                fieldCode to ParsedCode("0.10", true, mutableListOf()),
                FieldCode.M to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.I4 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.I6 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.I8 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.J4 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.J6 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.J8 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.K4 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.K6 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.K8 to ParsedCode("0.01", true, mutableListOf()),
            )
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertTrue(
            "Should be validated has valid",
            parsedCode.isValid
        )
        assertEquals("0.10", parsedCode.value)
        assertTrue(parsedCode.errors.isEmpty())
    }

    @Test
    fun testFieldO() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.O
        val qrCode = QRCode()

        // Empty
        val empty = ""
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(empty, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With empty tin should be validated has not valid", parsedCode.isValid)
        assertEquals(empty, parsedCode.value)
        assertEquals("field_without_value", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // Valid sum
        setValuesField(
            qrCode,
            mutableMapOf(
                fieldCode to ParsedCode("9.99".padStart(16, '9'), true, mutableListOf()),
                FieldCode.I2 to ParsedCode("9.99".padStart(16, '9'), true, mutableListOf()),
            )
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertTrue("Should be validated has valid", parsedCode.isValid)
        assertEquals("9.99".padStart(16, '9'), parsedCode.value)
        assertTrue(parsedCode.errors.isEmpty())

        // Valid sum
        setValuesField(
            qrCode,
            mutableMapOf(
                fieldCode to ParsedCode("0.14", true, mutableListOf()),
                FieldCode.I2 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.I3 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.I5 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.I7 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.J2 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.J3 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.J5 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.J7 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.K2 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.K3 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.K5 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.K7 to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.L to ParsedCode("0.01", true, mutableListOf()),
                FieldCode.N to ParsedCode("0.01", true, mutableListOf()),
            )
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertTrue("Should be validated has valid", parsedCode.isValid)
        assertEquals("0.14", parsedCode.value)
        assertTrue(parsedCode.errors.isEmpty())

        // Not valid
        for (notValid in listOf(
            "999",
            "99.9",
            "1.",
            "0.9",
            ".40",
            "9.99".padStart(17, '9')
        )) {
            setValuesField(
                qrCode,
                mutableMapOf(fieldCode to ParsedCode(notValid, true, mutableListOf()))
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse(
                "With $notValid should be validated has not valid",
                parsedCode.isValid
            )
            assertEquals(notValid, parsedCode.value)
            assertTrue(
                parsedCode.errors.first().matches(
                    Regex("value_length_greater_than_max_length|value_format_does_not_respect_regexp")
                )
            )
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }

    }

    @Test
    fun testFieldP() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.P
        val qrCode = QRCode()

        // Empty
        val empty = ""
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(empty, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With empty tin should be validated has not valid", parsedCode.isValid)
        assertEquals(empty, parsedCode.value)
        assertEquals("field_optional_without_value_cannot_be_in_code", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        for (value in listOf("0.01", "9.99", "999.99", "0.00", "9.99".padStart(16, '9'))) {
            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(value, true, mutableListOf()),
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue("With $value should be validated has valid", parsedCode.isValid)
            assertEquals(value, parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

        for (notValid in listOf(
            "999",
            "99.9",
            "1.",
            "0.9",
            ".40",
            "9.99".padStart(17, '9')
        )) {

            setValuesField(
                qrCode,
                mutableMapOf(fieldCode to ParsedCode(notValid, true, mutableListOf()))
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse(
                "With $notValid should be validated has not valid",
                parsedCode.isValid
            )
            assertEquals(notValid, parsedCode.value)
            assertTrue(
                parsedCode.errors.first().matches(
                    Regex("value_length_greater_than_max_length|value_format_does_not_respect_regexp")
                )
            )
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }

    }

    @Test
    fun testFieldQ() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.Q
        val qrCode = QRCode()

        // Test of no hash
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With no hash should be invalid because is mandatory", parsedCode.isValid)
        assertEquals("", parsedCode.value)
        assertEquals("field_is_mandatory_but_not_exist", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // Test a valid Hash
        for (validHash in listOf("#!\"\$", "A12w", "Lk%&")) {
            setValuesField(
                qrCode,
                mutableMapOf(fieldCode to ParsedCode(validHash, true, mutableListOf()))
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue("The hash $validHash should be validated has valid", parsedCode.isValid)
            assertEquals(validHash, parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

        // Test wrong hash
        for (hashWrongFormat in listOf("AAA", "AAAAA")) {
            setValuesField(
                qrCode,
                mutableMapOf(fieldCode to ParsedCode(hashWrongFormat, true, mutableListOf()))
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse(
                "The hash $hashWrongFormat should be validated has not valid",
                parsedCode.isValid
            )
            assertEquals(hashWrongFormat, parsedCode.value)
            assertTrue(
                parsedCode.errors.first().matches(
                    Regex(
                        "value_length_greater_than_max_length|value_length_less_than_min_length"
                    )
                )
            )
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }
        // Test hash empty
        val hashEmpty = ""
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(hashEmpty, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With empty hash should be validated has not valid", parsedCode.isValid)
        assertEquals(hashEmpty, parsedCode.value)
        assertEquals("field_without_value", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

    }

    @Test
    fun testFieldR() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.R
        val qrCode = QRCode()

        // Test of no number
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With no hash should be invalid because is mandatory", parsedCode.isValid)
        assertEquals("", parsedCode.value)
        assertEquals("field_is_mandatory_but_not_exist", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        // Test a valid Hash
        for (number in (1..9)) {
            for (length in 1..4) {
                val validCertificate = "$number".padEnd(length, '9')
                setValuesField(
                    qrCode,
                    mutableMapOf(fieldCode to ParsedCode(validCertificate, true, mutableListOf()))
                )
                invokeValidateField(qrCode, fieldCode)
                parsedCode = getValuesField(qrCode)[fieldCode]!!
                assertTrue(
                    "The certificate $validCertificate should be validated has valid",
                    parsedCode.isValid
                )
                assertEquals(validCertificate, parsedCode.value)
                assertTrue(parsedCode.errors.isEmpty())
            }
        }

        // Test wrong hash
        for (wrongCertificate in listOf("99999", "A111")) {
            setValuesField(
                qrCode,
                mutableMapOf(fieldCode to ParsedCode(wrongCertificate, true, mutableListOf()))
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse(
                "The certificate number $wrongCertificate should be validated has not valid",
                parsedCode.isValid
            )
            assertEquals(wrongCertificate, parsedCode.value)
            assertEquals("value_format_does_not_respect_regexp", parsedCode.errors.first())
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }
        // Test certificate number empty
        val certificateEmpty = ""
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(certificateEmpty, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse(
            "With empty certificate number should be validated has not valid",
            parsedCode.isValid
        )
        assertEquals(certificateEmpty, parsedCode.value)
        assertEquals("field_without_value", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )
    }

    @Test
    fun testFieldS() {

        var parsedCode: ParsedCode
        val fieldCode = FieldCode.S
        val qrCode = QRCode()

        // Empty
        val empty = ""
        setValuesField(
            qrCode,
            mutableMapOf(fieldCode to ParsedCode(empty, true, mutableListOf()))
        )
        invokeValidateField(qrCode, fieldCode)
        parsedCode = getValuesField(qrCode)[fieldCode]!!
        assertFalse("With empty tin should be validated has not valid", parsedCode.isValid)
        assertEquals(empty, parsedCode.value)
        assertEquals("field_optional_without_value_cannot_be_in_code", parsedCode.errors.first())
        assertTrue(
            "Not exists in string resources",
            resourcesStringExists(parsedCode.errors.first())
        )

        for (value in listOf(
            "A", "A:B", "TB;PT00000000000000000000000;513500.58", "MB10999;999999999;999.99",
            "TB;PT00000000000000000000000;513500.58,MB10999;999999999;999.99",
            "9".padStart(65, '9')
        )
        ) {

            setValuesField(
                qrCode,
                mutableMapOf(
                    fieldCode to ParsedCode(value, true, mutableListOf()),
                )
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertTrue("With $value should be validated has valid", parsedCode.isValid)
            assertEquals(value, parsedCode.value)
            assertTrue(parsedCode.errors.isEmpty())
        }

        for (notValid in listOf(
            "a*9", "A".padStart(66, '9')
        )) {

            setValuesField(
                qrCode,
                mutableMapOf(fieldCode to ParsedCode(notValid, true, mutableListOf()))
            )
            invokeValidateField(qrCode, fieldCode)
            parsedCode = getValuesField(qrCode)[fieldCode]!!
            assertFalse(
                "With $notValid should be validated has not valid",
                parsedCode.isValid
            )
            assertEquals(notValid, parsedCode.value)
            assertTrue(
                parsedCode.errors.first().matches(
                    Regex("value_length_greater_than_max_length|value_format_does_not_respect_regexp")
                )
            )
            assertTrue(
                "Not exists in string resources",
                resourcesStringExists(parsedCode.errors.first())
            )
        }

    }

    @Test
    fun testParseField() {
        val qrCode = QRCode()
        val method = QRCode::class.java.declaredMethods.first { it.name == "parseField" }
        method.isAccessible = true

        val valuesStack = listOf(
            Pair(FieldCode.A, "999999999"),
            Pair(FieldCode.A, "99999:999"),
        )

        for (values in valuesStack) {
            val arg = "${values.first}:${values.second}"
            val parsed = method.invoke(qrCode, arg) as Pair<*, *>

            assertEquals(values.first, parsed.first)
            assertEquals(values.second, (parsed.second as ParsedCode).value)
            assertTrue((parsed.second as ParsedCode).isValid)
            assertTrue((parsed.second as ParsedCode).errors.isEmpty())
        }

    }

    @Test
    fun testParseFieldEmpty() {

        val qrCode = QRCode()
        val method = QRCode::class.java.declaredMethods.first { it.name == "parseField" }
        method.isAccessible = true

        try {
            QRCode::class.memberFunctions.first { it.name == "parseField" }.apply {
                isAccessible = true
                call(qrCode, "")
            }
            fail("Parse empty field should throw ParseException")
        } catch (e: Throwable) {
            assertEquals(ParseException::class, e.cause!!::class)
            assertEquals("qr_code_field_is_not_valid", e.cause!!.message)
        }
    }

    @Test
    fun testParseFieldNotValid() {

        val qrCode = QRCode()
//        val method = QRCode::class.java.declaredMethods.first { it.name == "parseField" }
//        method.isAccessible = true

        for (field in listOf("AA:99999999", "Z:999")) {
            try {
                QRCode::class.memberFunctions.first { it.name == "parseField" }.apply {
                    isAccessible = true
                    call(qrCode, field)
                }
                fail("Parse field $field should throw ParseException")
            } catch (e: Throwable) {
                assertEquals(ParseException::class, e.cause!!::class)
                assertEquals("qr_code_field_is_not_valid", e.cause!!.message)
            }
        }
    }

    @Test
    fun testValidateVatBaseDependency() {

        val method =
            QRCode::class.java.declaredMethods.first { it.name == "validateVatBaseDependency" }
        method.isAccessible = true

        val parsedVal: MutableMap<FieldCode, ParsedCode> = mutableMapOf()

        for (codeLetter in listOf("I", "J", "K")) {

            (3..7 step 2).forEach { index ->

                val qrCode = QRCode()
                val fieldCodeOdd = FieldCode.valueOf("$codeLetter$index")
                val fieldCodeEven = FieldCode.valueOf("$codeLetter${index + 1}")
                parsedVal[fieldCodeOdd] = ParsedCode("0.0", true)
                parsedVal[fieldCodeEven] = ParsedCode("0.0", true)
                setValuesField(qrCode, parsedVal)

                method.invoke(qrCode)

                qrCode.getValues().forEach {
                    assertTrue(it.value.isValid)
                }
            }
        }
    }

    @Test
    fun testValidateVatBaseDependencyError() {

        val method =
            QRCode::class.java.declaredMethods.first { it.name == "validateVatBaseDependency" }
        method.isAccessible = true

        val parsedVal: MutableMap<FieldCode, ParsedCode> = mutableMapOf()

        for (codeLetter in listOf("I", "J", "K")) {

            listOf(3, 6, 7).forEach { index ->

                val qrCode = QRCode()
                val fieldCodeOdd = FieldCode.valueOf("$codeLetter$index")
                parsedVal[fieldCodeOdd] = ParsedCode("0.0", true)
                setValuesField(qrCode, parsedVal)
                method.invoke(qrCode)
                qrCode.getValues().forEach {
                    assertFalse(it.value.isValid)
                }
            }
        }
    }

    @Test
    fun testParse() {

        val valuesStack = listOf(
            mapOf(
                FieldCode.A to "123456789",
                FieldCode.B to "999999990",
                FieldCode.C to "PT",
                FieldCode.D to "FT",
                FieldCode.E to "N",
                FieldCode.F to "20211231",
                FieldCode.G to "FT AB2019/0035",
                FieldCode.H to "CSDF7T5H-0035",
                FieldCode.I1 to "PT",
                FieldCode.I2 to "12000.00",
                FieldCode.I3 to "15000.00",
                FieldCode.I4 to "900.00",
                FieldCode.I5 to "50000.00",
                FieldCode.I6 to "6500.00",
                FieldCode.I7 to "80000.00",
                FieldCode.I8 to "18400.00",
                FieldCode.J1 to "PT-AC",
                FieldCode.J2 to "10000.00",
                FieldCode.J3 to "25000.56",
                FieldCode.J4 to "1000.02",
                FieldCode.J5 to "75000.00",
                FieldCode.J6 to "6750.00",
                FieldCode.J7 to "100000.00",
                FieldCode.J8 to "18000.00",
                FieldCode.K1 to "PT-MA",
                FieldCode.K2 to "5000.00",
                FieldCode.K3 to "12500.00",
                FieldCode.K4 to "625.00",
                FieldCode.K5 to "25000.00",
                FieldCode.K6 to "3000.00",
                FieldCode.K7 to "40000.00",
                FieldCode.K8 to "8800.00",
                FieldCode.L to "100.00",
                FieldCode.M to "25.00",
                FieldCode.N to "64000.02",
                FieldCode.O to "513600.58",
                FieldCode.P to "100.00",
                FieldCode.Q to "kLp0",
                FieldCode.R to "9999",
                FieldCode.S to "TB;PT00000000000000000000000;513500.58",
            ),
            mapOf(
                FieldCode.A to "123456789",
                FieldCode.B to "999999990",
                FieldCode.C to "PT",
                FieldCode.D to "FS",
                FieldCode.E to "N",
                FieldCode.F to "20230812",
                FieldCode.G to "FS CDVF/12345",
                FieldCode.H to "CDF7T5HD-12345",
                FieldCode.I1 to "PT",
                FieldCode.I7 to "0.65",
                FieldCode.I8 to "0.15",
                FieldCode.N to "0.15",
                FieldCode.O to "0.80",
                FieldCode.Q to "YhGV",
                FieldCode.R to "9999",
                FieldCode.S to "NU;0.80",
            ),
            mapOf(
                FieldCode.A to "123456789",
                FieldCode.B to "999999990",
                FieldCode.C to "PT",
                FieldCode.D to "FS",
                FieldCode.E to "N",
                FieldCode.F to "20230812",
                FieldCode.G to "FS CDVF/12345",
                FieldCode.H to "CDF7T5HD-12345",
                FieldCode.I1 to "PT",
                FieldCode.I7 to "0.00",
                FieldCode.I8 to "0.00",
                FieldCode.N to "0.00",
                FieldCode.O to "0.00",
                FieldCode.Q to "YhGV",
                FieldCode.R to "9999"
            )
        )

        valuesStack.forEach { itStack ->

            val qrCode = QRCode()
            var rawCode = ""

            itStack.forEach {
                rawCode += "${it.key}:${it.value}*"
            }

            runBlocking { qrCode.parse(rawCode.trimEnd('*')) }

            val values = qrCode.getValues()

            assertTrue(values.isNotEmpty())

            values.forEach {

                assertEquals(
                    "The field ${it.key} failed",
                    itStack[it.key],
                    it.value.value
                )

                assertTrue(
                    "The field ${it.key} errors should be empty but ${it.value.errors.joinToString("; ")}",
                    it.value.errors.isEmpty()
                )

                assertTrue(
                    "The field ${it.key} failed should be valid",
                    it.value.isValid
                )
            }

            assertEquals(itStack.size, values.size)
        }
    }

    @Test
    fun testParseError() {

        val valuesStack = listOf(
            mapOf(
                //FieldCode.A to "123456789",
                FieldCode.B to "999999990",
                FieldCode.C to "PT",
                FieldCode.D to "FT",
                FieldCode.E to "N",
                FieldCode.F to "20211231",
                FieldCode.G to "FT AB2019/0035",
                FieldCode.H to "CSDF7T5H-0035",
                FieldCode.I1 to "PT",
                FieldCode.I2 to "12000.00",
                FieldCode.I3 to "15000.00",
                FieldCode.I4 to "900.00",
                FieldCode.I5 to "50000.00",
                FieldCode.I6 to "6500.00",
                FieldCode.I7 to "80000.00",
                FieldCode.I8 to "18400.00",
                FieldCode.J1 to "PT-AC",
                FieldCode.J2 to "10000.00",
                FieldCode.J3 to "25000.56",
                FieldCode.J4 to "1000.02",
                FieldCode.J5 to "75000.00",
                FieldCode.J6 to "6750.00",
                FieldCode.J7 to "100000.00",
                FieldCode.J8 to "18000.00",
                FieldCode.K1 to "PT-MA",
                FieldCode.K2 to "5000.00",
                FieldCode.K3 to "12500.00",
                FieldCode.K4 to "625.00",
                FieldCode.K5 to "25000.00",
                FieldCode.K6 to "3000.00",
                FieldCode.K7 to "40000.00",
                //FieldCode.K8 to "8800.00",
                FieldCode.L to "100.00",
                FieldCode.M to "25.00",
                FieldCode.N to "64000.02",
                FieldCode.O to "513600.58",
                FieldCode.P to "100.00",
                FieldCode.Q to "kLp0",
                FieldCode.R to "9999",
                FieldCode.S to "TB;PT00000000000000000000000;513500.58",
            ),
        )

        valuesStack.forEach { itStack ->

            val qrCode = QRCode()
            var rawCode = ""

            itStack.forEach {
                rawCode += "${it.key}:${it.value}*"
            }

            runBlocking { qrCode.parse(rawCode.trimEnd('*')) }

            val values = qrCode.getValues()

            assertTrue(values.isNotEmpty())

            values.forEach {

                if (it.key == FieldCode.A || it.key == FieldCode.K7) {
                    assertFalse(it.value.isValid)
                    assertFalse(it.value.errors.isEmpty())
                    return
                }

                assertEquals(
                    "The field ${it.key} failed",
                    itStack[it.key],
                    it.value.value
                )

                assertTrue(
                    "The field ${it.key} errors should be empty but ${it.value.errors.joinToString("; ")}",
                    it.value.errors.isEmpty()
                )

                assertTrue(
                    "The field ${it.key} failed should be valid",
                    it.value.isValid
                )

            }

            assertEquals(itStack.size + 1, values.size)
        }
    }

    @Test
    fun validateIssuerTin() {

        ServiceLocator.locate = object : AServiceLocator() {
            override fun hasInternetConnection(): Boolean {
                return true
            }
        }

        try {
            val qrCode = QRCode()

            setValuesField(
                qrCode, mutableMapOf(FieldCode.A to ParsedCode("502178604", true))
            )
            runBlocking {
                QRCode::class.memberFunctions.first { it.name == "validateIssuerTinAsync" }.also {
                    it.isAccessible = true
                    it.callSuspend(qrCode)
                }
            }

            assertTrue(qrCode.issuerVatCheckerResponse!!.isValid)


        } finally {
            ServiceLocator.locate = object : AServiceLocator() {}
        }
    }

    @Test
    fun validateInvalidIssuerTin() {

        val qrCode = QRCode()

        setValuesField(
            qrCode, mutableMapOf(FieldCode.A to ParsedCode("999999999", true))
        )

        runBlocking {
            QRCode::class.memberFunctions.first { it.name == "validateIssuerTinAsync" }.also {
                it.isAccessible = true
                it.callSuspend(qrCode)
            }
        }

        assertFalse(qrCode.issuerVatCheckerResponse!!.isValid)

    }

    @Test
    fun validateBuyerTin() {

        ServiceLocator.locate = object : AServiceLocator() {
            override fun hasInternetConnection(): Boolean {
                return true
            }
        }

        try {
            listOf(
                Pair("PT", "502178604"),
                Pair("ES", "A78098308"),
            ).forEach { pair ->

                val qrCode = QRCode()

                setValuesField(
                    qrCode,
                    mutableMapOf(
                        FieldCode.B to ParsedCode(pair.second, true),
                        FieldCode.C to ParsedCode(pair.first, true)
                    )
                )

                runBlocking {
                    QRCode::class.memberFunctions.first { it.name == "validateBuyerTinAsync" }
                        .also {
                            it.isAccessible = true
                            it.callSuspend(qrCode)
                        }
                }

                assertTrue(qrCode.buyerVatCheckerResponse!!.isValid)

            }
        } finally {
            ServiceLocator.locate = object : AServiceLocator() {}
        }
    }

    @Test
    fun validateInvalidBuyerTinWithVatCheckResponseNull() {

        ServiceLocator.locate = object : AServiceLocator() {
            override fun hasInternetConnection(): Boolean {
                return true
            }
        }

        try {

            listOf(
                Pair("PT", "999999990"),
                Pair("PT", "502178604"),
                Pair("KO", "999"),
                Pair("ES", "123456789")

            ).forEach {

                for (parseValid in listOf(true, false)) {
                    (0..1).forEach { _ ->
                        val qrCode = QRCode()

                        setValuesField(
                            qrCode,
                            mutableMapOf(
                                FieldCode.B to ParsedCode(it.second, parseValid),
                                FieldCode.C to ParsedCode(it.first, !parseValid)
                            )
                        )

                        runBlocking {
                            QRCode::class.memberFunctions.first { it.name == "validateBuyerTinAsync" }
                                .also {
                                    it.isAccessible = true
                                    it.callSuspend(qrCode)
                                }
                        }

                        assertNull(qrCode.buyerVatCheckerResponse)
                    }
                }
            }
        } finally {
            ServiceLocator.locate = object : AServiceLocator() {}
        }
    }

    @Test
    fun validateInvalidBuyerTinWithVatCheckResponseNotValid() {

        ServiceLocator.locate = object : AServiceLocator() {
            override fun hasInternetConnection(): Boolean {
                return true
            }
        }

        try {
            listOf(
                Pair("ES", "999999990"),
                Pair("FR", "502178604"),
                Pair("PT", "999999999")
            ).forEach { pair ->
                val qrCode = QRCode()

                setValuesField(
                    qrCode,
                    mutableMapOf(
                        FieldCode.B to ParsedCode(pair.second, true),
                        FieldCode.C to ParsedCode(pair.first, true)
                    )
                )

                runBlocking {
                    QRCode::class.memberFunctions.first { it.name == "validateBuyerTinAsync" }
                        .also {
                            it.isAccessible = true
                            it.callSuspend(qrCode)
                        }
                }

                assertFalse(qrCode.buyerVatCheckerResponse!!.isValid)
            }
        } finally {
            ServiceLocator.locate = object : AServiceLocator() {}
        }
    }

    @Test
    fun softwareCertificationInfoAsync() {

        val serviceLocator = ServiceLocator.locate

        ServiceLocator.locate = object : AServiceLocator() {
            override fun getString(name: String): String {
                return name
            }

            override fun appVersion(): String {
                return "0.0.0"
            }
        }

        try {
            val qrCode = QRCode()
            val number = "1408"
            setValuesField(
                qrCode, mutableMapOf(
                    FieldCode.R to ParsedCode(number, true)
                )
            )

            runBlocking {
                QRCode::class.memberFunctions.first {
                    it.name == "softwareCertificationInfoAsync"
                }.also {
                    it.isAccessible = true
                    it.callSuspend(qrCode)
                }
            }
            assertEquals(number, qrCode.certificate?.certificate)
        } finally {
            ServiceLocator.locate = serviceLocator
        }
    }

}