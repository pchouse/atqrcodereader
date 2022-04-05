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

import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import pt.pchouse.atqrcodereader.ServiceLocator
import java.io.IOException

object Proxy {

    private val LOG_TAG = Proxy::class.qualifiedName!!

    const val END_POINT =
        "https://www.portaldasfinancas.gov.pt/pt/consultaProgCertificadosM24.action"

    fun get(number: String): Response {

        Log.d(LOG_TAG, "Going to get the certificate information for $number")

        try {
            Log.d(LOG_TAG, "Going to connect to $END_POINT")

            val doc = Jsoup.connect(END_POINT).userAgent(
                "PChouse QR code ${ServiceLocator.locate.appVersion()}"
            ).get()

            Log.d(LOG_TAG, "Connection with $END_POINT was successful")

            val elementTable = doc.getElementById("m24Table")
                ?.childNodes()?.filter {
                    it.nodeName() == "tbody"
                }

            if (elementTable?.isNotEmpty() != true) {
                Log.d(
                    LOG_TAG,
                    "The returned endpoint page was not has expected, probably connection error"
                )
                return Response(ResponseStatus.CONNECTION_ERROR, null)
            }

            var element: Node? = null

            try {
                element = elementTable[0]?.childNodes()?.first {
                    val nodeName = it.nodeName()
                    if (nodeName != "tr") {
                        false
                    } else {
                        it.childNode(4).childNode(0).toString() == number.trimStart('0')
                    }
                }
            } catch (_: java.util.NoSuchElementException) {

            }

            if (element == null) {
                Log.d(
                    LOG_TAG,
                    "The certificate $number was not found, probably certificate not exist"
                )
                return Response(ResponseStatus.CERTIFICATE_NOT_EXIST, null)
            }

            Log.d(
                LOG_TAG,
                "The certificate $number was found"
            )

            return Response(
                ResponseStatus.OK,
                Certificate(
                    element.childNode(1).childNode(0).toString().trim(),
                    element.childNode(2).childNode(0).toString().trim(),
                    element.childNode(3).childNode(0).toString().trim(),
                    element.childNode(4).childNode(0).toString().trim(),
                    element.childNode(5).childNode(0).toString().trim(),
                    element.childNode(6).childNode(0).toString().trim(),
                )
            )
        } catch (e: IOException) {
            Log.e(
                LOG_TAG,
                "The connection with $END_POINT throws error '${e.message}'"
            )
            return Response(ResponseStatus.CONNECTION_ERROR, null)
        } catch (e: Throwable) {
            Log.e(
                LOG_TAG,
                "Unknown error '${e.message}'"
            )
            return Response(ResponseStatus.UNKNOWN, null)
        }
    }

}
