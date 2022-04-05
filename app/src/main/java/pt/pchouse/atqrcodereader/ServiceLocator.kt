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

package pt.pchouse.atqrcodereader

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.Room
import pt.pchouse.atqrcodereader.api.IRequest
import pt.pchouse.atqrcodereader.api.IRequestApi
import pt.pchouse.atqrcodereader.api.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

abstract class AServiceLocator : Application() {

    companion object {
        val LOG_TAG: String = AServiceLocator::class.qualifiedName!!
        private var db: AppDatabase? = null

        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null
    }

    open fun getString(name: String): String {
        return try {
            val id: Int = getMainContext().resources.getIdentifier(
                name,
                "string",
                getMainContext().packageName
            )

            if (id == 0) name else getMainContext().resources.getString(id)
        }catch (e : Throwable){
            Log.e(LOG_TAG, e.message ?: "")
            name
        }
    }

    /**
     * Get the app context
     */
    open fun getMainContext(): Context {

        if (context == null) {
            val field = MainActivity::class.java.declaredFields.first { field ->
                field.name == "context"
            }
            field.isAccessible = true
            context = field.get(null) as Context
        }

        return context!!
    }

    open fun getIRequestApi(): IRequestApi {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(IRequestApi::class.java)
    }

    /**
     * Request the validation to the validation server
     */
    open fun getIRequest(): IRequest {
        return Request()
    }

    /**
     * Get the database acess
     */
    open fun db(): AppDatabase {
        try {
            if (db == null) {
                db = Room.databaseBuilder(
                    getMainContext(),
                    AppDatabase::class.java,
                    "pchouse_qrcode"
                ).build()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, e.message ?: "unknown error")
        }

        return db!!
    }

    /**
     * Get app version
     */
    open fun appVersion(): String {
        return try {
            getMainContext().packageManager.getPackageInfo(
                getMainContext().packageName, 0
            ).versionName
        }catch (_: Throwable){
            ""
        }
    }

    @Suppress("DEPRECATION")
    private fun hasInternetConnectionOld(): Boolean {
        val cm =
            getMainContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: android.net.NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasInternetConnectionAfterM(): Boolean {
        val cm =
            getMainContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    /**
     * https://stackoverflow.com/a/65106280/6397645
     */
    open fun hasInternetConnection(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                hasInternetConnectionAfterM()
            else
                hasInternetConnectionOld()
        } catch (_: Throwable) {
            false
        }
    }

}

class ServiceLocator {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var locate: AServiceLocator = Locator
    }

    @SuppressLint("StaticFieldLeak")
    private object Locator : AServiceLocator()
}