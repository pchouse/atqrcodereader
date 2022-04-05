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

package pt.pchouse.atqrcodereader.api

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import pt.pchouse.atqrcodereader.AServiceLocator
import pt.pchouse.atqrcodereader.ServiceLocator
import pt.pchouse.atqrcodereader.logic.FieldCode
import pt.pchouse.atqrcodereader.logic.QRCodeTest
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class RequestTest {

    companion object {
        const val TEST_PORT = 8999
        const val ENDPOINT = "http://localhost:$TEST_PORT"
        const val F_STATUS = "status"
        const val F_MESSAGE = "message"
        const val F_FIELDS_ERROR = "fieldsError"
        const val F_FIELD_CODE = "field"
        const val F_VALUE = "value"

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

    @Test
    fun testRequest() {

        val valuesStack = listOf(
            Response(Status.OK, "", null),
            Response(Status.OK, "", listOf()),
            Response(Status.ERROR, "The error message", null),
            Response(Status.ERROR, "The error message", listOf()),
            Response(
                Status.ERROR, "The error message", listOf(
                    FieldError(FieldCode.A, null)
                )
            ),
            Response(
                Status.ERROR, "The error message", listOf(
                    FieldError(FieldCode.A, "")
                )
            ),
            Response(
                Status.ERROR, "The error message", listOf(
                    FieldError(FieldCode.A, "The error field")
                )
            ),
            Response(
                Status.ERROR, "The error message", listOf(
                    FieldError(FieldCode.A, "The error field A"),
                    FieldError(FieldCode.B, "The error field B")
                )
            ),
        )

        val mockWebServer = MockWebServer()

        val baseBuildStr = "\"%s\":\"%s\","
        val baseBuild = "\"%s\": %s ,"
        valuesStack.forEach {

            val stringBuilder = StringBuffer("{")
            stringBuilder.append(String.format(baseBuildStr, F_STATUS, it.status))
            stringBuilder.append(String.format(baseBuildStr, F_MESSAGE, it.message))

            when {
                // when null
                it.fieldsError == null -> {
                    stringBuilder.append(
                        String.format(
                            baseBuild.trimEnd(','),
                            F_FIELDS_ERROR,
                            "null"
                        )
                    )
                }
                // when empty
                it.fieldsError!!.isEmpty() -> {
                    stringBuilder.append(
                        String.format(
                            baseBuild.trimEnd(','),
                            F_FIELDS_ERROR,
                            "[]"
                        )
                    )
                }
                // if has values
                else -> {
                    stringBuilder.append(String.format(baseBuild.trimEnd(','), F_FIELDS_ERROR, "["))
                    var index = 0
                    val size = it.fieldsError!!.size
                    it.fieldsError!!.forEach { fr ->
                        stringBuilder.append("{")
                        stringBuilder.append(String.format(baseBuildStr, F_FIELD_CODE, fr.field))
                        if (fr.value == null) {
                            stringBuilder.append("\"$F_VALUE\": null")
                        } else {
                            stringBuilder.append(
                                String.format(
                                    baseBuildStr.trimEnd(','),
                                    F_VALUE,
                                    fr.value
                                )
                            )
                        }
                        stringBuilder.append("}")
                        if (size > ++index) stringBuilder.append(",")
                    }
                    stringBuilder.append("]")
                }
            }

            stringBuilder.append("}")

            mockWebServer.enqueue(
                MockResponse().setBody(stringBuilder.toString())
            )

        }

        mockWebServer.start(TEST_PORT)

        try {
            val request = ServiceLocator.locate.getIRequest()
            valuesStack.forEach {
                runBlocking {
                    val response = request.validate(ENDPOINT, "DUMMY_QR_CODE")
                    assertEquals(it.fieldsError, response.fieldsError)
                }
            }
        } finally {
            mockWebServer.shutdown()
        }
    }

    @Test
    fun testErrorEmptyResponse() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setBody(""))
        mockWebServer.start(TEST_PORT)
        try {
            runBlocking {
                val response = Request().validate(ENDPOINT, "DUMMY_QR_CODE")
                fail("Should fail but: $response")
            }
        } catch (e: Throwable) {
            assertEquals(ResponseException::class, e::class)
            assertTrue(resourcesStringExists(e.message!!))
        } finally {
            mockWebServer.shutdown()
        }
    }

    @Test
    fun testErrorMalFormattedResponse() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setBody("{\"stat\" : \"\"}"))
        mockWebServer.start(TEST_PORT)
        try {
            runBlocking {
                val response = Request().validate(ENDPOINT, "DUMMY_QR_CODE")
                fail("Should fail but: $response")
            }

        } catch (e: Throwable) {
            assertEquals(ResponseException::class, e::class)
            assertTrue(resourcesStringExists(e.message!!))
        } finally {
            mockWebServer.shutdown()
        }
    }

    @Test
    fun testErrorHttpResponseCode400() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody(""))
        mockWebServer.start(TEST_PORT)
        try {
            runBlocking {
                val response = Request().validate(ENDPOINT, "DUMMY_QR_CODE")
                fail("Should fail but: $response")
            }

        } catch (e: Throwable) {
            assertEquals(ResponseException::class, e::class)
        } finally {
            mockWebServer.shutdown()
        }
    }

    @Test
    fun testErrorMalFormattedResponseStatus() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setBody(
            "{\"status\" : \"Z\", \"message\" : \"msg\", \"fieldsError\" : null}"
        ))
        mockWebServer.start(TEST_PORT)
        try {
            runBlocking {
                val response = Request().validate(ENDPOINT, "DUMMY_QR_CODE")
                fail("Should fail but: $response")
            }

        } catch (e: Throwable) {
            assertEquals(ResponseException::class, e::class)
            assertTrue(resourcesStringExists(e.message!!))
        } finally {
            mockWebServer.shutdown()
        }
    }

}