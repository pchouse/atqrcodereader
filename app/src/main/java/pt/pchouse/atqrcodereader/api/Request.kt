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


import android.util.Log
import pt.pchouse.atqrcodereader.ServiceLocator
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class Request : IRequest {

    companion object {
       val  LOG_TAG = Request::class.qualifiedName!!
    }
    /**
     * Validate through API
     */
    override suspend fun validate(url: String, qrCodeText: String): Response {
        try {

            val retrofit = Retrofit.Builder()
                .baseUrl("http://localhost/dummy/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            Log.d(LOG_TAG, "Request to: $url")

            val service = retrofit.create(IRequestApi::class.java)
                .validate(url, RequestValidation(qrCodeText))

            val response = service.await()


            @Suppress("SENSELESS_COMPARISON")
            if (response.status == null || response.message == null) {
                throw ResponseException(
                    ServiceLocator.locate.getString("api_response_mal_formatted")
                )
            }

            return response

        } catch (e: java.io.EOFException) {
            Log.e(LOG_TAG, e.message!!)
            throw ResponseException(
                ServiceLocator.locate.getString("api_response_mal_formatted")
            )
        } catch (e: Throwable) {
            Log.e(LOG_TAG, e.message!!)
            throw ResponseException(e.message)
        }
    }

}