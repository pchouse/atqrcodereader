/*
 * Copyright (C) 2022  Reflexão Sistemas e Estudos Informáticos, LDA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pt.pchouse.atqrcodereader.ui.settings

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.pchouse.atqrcodereader.ServiceLocator
import java.lang.Exception

class SettingsViewModel : ViewModel() {

    private var settings: Model? = null

    companion object {
        val LOG_TAG: String = SettingsViewModel::class.qualifiedName!!
        val db: Dao = ServiceLocator.locate.db().settings()
    }

    /**
     * Initiate the field values with the values from database or default if no exist
     */
    fun initFields() {
        try {
            Log.d(LOG_TAG, "Going to read or create settings in database")
            viewModelScope.launch {
                val model = db.getAsync()
                if (model == null) {
                    Log.d(LOG_TAG, "Settings does not exist in database, it will be create")
                    this@SettingsViewModel.settings = Model(1, false, "")
                    db.saveAsync(this@SettingsViewModel.getSettings()!!)
                    Log.d(LOG_TAG, "Settings was created in database")
                } else {
                    Log.d(LOG_TAG, "Settings exist in database and was loaded")
                    settings = model
                }
                updateFieldsValues()
            }

        } catch (e: Exception) {
            settings = Model(1, true, "")
            Log.e(LOG_TAG, e.message ?: "unknown error")
        }
    }

    private fun updateFieldsValues() {
        Log.d(LOG_TAG, "Going to update fields values")
        if (settings == null) {
            Log.d(LOG_TAG, "Settings does not exist, will not update field values")
            return
        }
        _showAllFields.postValue(settings!!.showAllFields)
        _apiUrl.postValue(settings!!.apiUrl)

        Log.d(LOG_TAG, "Fields values was updated")
    }

    /**
     * Get settings
     */
    fun getSettings(): Model? {
        return settings
    }

    fun updateDb() {
        Log.d(LOG_TAG, "Going to update settings in database")

        if (settings == null) {
            Log.d(LOG_TAG, "Settings does not exist, will not update in database")
            return
        }
        viewModelScope.launch {
            db.saveAsync(settings!!)
            Log.d(LOG_TAG, "Settings was update in database")
        }
    }

    private val _apiUrl = MutableLiveData<String>().apply {
        value = settings?.apiUrl ?: ""
    }

    val apiUrl: LiveData<String> = _apiUrl

    private val _showAllFields = MutableLiveData<Boolean>().apply {
        value = settings?.showAllFields ?: false
    }

    val showAllFields: LiveData<Boolean> = _showAllFields

}