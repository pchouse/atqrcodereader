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

package pt.pchouse.atqrcodereader.logic.certificate

import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import pt.pchouse.atqrcodereader.AServiceLocator
import pt.pchouse.atqrcodereader.ServiceLocator

class ProxyTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            // Mock Service locator get verison
            ServiceLocator.locate = object : AServiceLocator() {
                override fun appVersion(): String {
                    return "0.0.0"
                }
            }
        }
    }

    @Test
    fun testGet() {

            val valuesStack = listOf(
                Response(
                    ResponseStatus.OK,
                    Certificate(
                        "PCHGestex",
                        "1",
                        "REFLEXAO ESTUDOS E SISTEMAS INFORMATICOS LDA",
                        "1408",
                        "Certificado",
                        "2011-12-15"
                    )
                ),
                Response(
                    ResponseStatus.OK,
                    Certificate(
                        "ActiveSell",
                        "1",
                        "REFLEXAO ESTUDOS E SISTEMAS INFORMATICOS LDA",
                        "2479",
                        "Certificado",
                        "2016-01-13"
                    )

                ),
                Response(
                    ResponseStatus.OK,
                    Certificate(
                        "Activ2Sell",
                        "2",
                        "REFLEXAO ESTUDOS E SISTEMAS INFORMATICOS LDA",
                        "2870",
                        "Certificado",
                        "2020-11-27"
                    )

                ),
            )

            for (value in valuesStack) {
                val response = Proxy.get(value.certificate!!.certificate.padStart(9, '0'))
                assertEquals(value, response)
            }

    }

    @Test
    fun testNotExist(){
           assertEquals(
               Response(ResponseStatus.CERTIFICATE_NOT_EXIST, null),
               Proxy.get("9999")
           )

    }
}