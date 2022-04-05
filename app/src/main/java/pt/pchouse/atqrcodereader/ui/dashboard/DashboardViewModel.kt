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

package pt.pchouse.atqrcodereader.ui.dashboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.pchouse.atqrcodereader.R
import pt.pchouse.atqrcodereader.ServiceLocator
import pt.pchouse.atqrcodereader.logic.QRCode
import pt.pchouse.atqrcodereader.logic.certificate.Certificate

class DashboardViewModel : ViewModel() {

    companion object {
        val LOG_TAG: String = DashboardViewModel::class.qualifiedName!!
    }

    fun handleCertificate(qrCode: QRCode) {
        viewModelScope.launch {
            try {
                qrCode.softwareCertificationInfoAsync()
                certificate = qrCode.certificate

                if (certificate == null) {
                    _certNameVisible.postValue(false)
                    _certInfoVisible.postValue(false)
                    _certSourceVisible.postValue(false)
                    return@launch
                }

                if (certificate!!.name.isBlank()) {
                    _certNameVisible.postValue(false)
                } else {
                    _certName.postValue(certificate!!.name)
                    _certNameVisible.postValue(true)
                }

                if (certificate!!.producer.isBlank()) {
                    _certInfoVisible.postValue(false)
                    _certSourceVisible.postValue(false)
                } else {
                    val context = ServiceLocator.locate.getMainContext()
                    _certInfo.postValue(
                        StringBuilder("\n")
                            .append(context.getString(R.string.producer))
                            .append(": ")
                            .append(certificate?.producer ?: "")
                            .append("\n\n")
                            .append(context.getString(R.string.status))
                            .append(": ")
                            .append(certificate?.status ?: "")
                            .append("\n")
                            .append(context.getString(R.string.status_date))
                            .append(": ")
                            .append(certificate?.statusDate ?: "")
                            .toString()
                    )
                    _certInfoVisible.postValue(true)
                    _certSourceVisible.postValue(true)
                }

            } catch (e: Throwable) {
                Log.e(LOG_TAG, e.message ?: "")
            }
        }
    }

    fun handleIssuer(qrCode: QRCode) {
        viewModelScope.launch {
            qrCode.validateIssuerTinAsync()

            val check = qrCode.issuerVatCheckerResponse

            if (check == null || !check.isValid || check.isError) {
                _issuerInfoVisible.postValue(false)
                _issuerNameVisible.postValue(false)
                _issuerSourceVisible.postValue(false)
                return@launch
            }

            _issuerName.postValue(check.name)
            _issuerInfo.postValue(check.address)
            _issuerNameVisible.postValue(true)
            _issuerInfoVisible.postValue(true)
            _issuerSourceVisible.postValue(true)
        }
    }

    fun handleBuyer(qrCode: QRCode) {
        viewModelScope.launch {
            qrCode.validateBuyerTinAsync()

            val check = qrCode.buyerVatCheckerResponse

            if (check == null || !check.isValid || check.isError) {
                _buyerInfoVisible.postValue(false)
                _buyerNameVisible.postValue(false)
                _buyerSourceVisible.postValue(false)
                return@launch
            }

            _buyerName.postValue(check.name)
            _buyerInfo.postValue(check.address)
            _buyerNameVisible.postValue(true)
            _buyerInfoVisible.postValue(true)
            _buyerSourceVisible.postValue(true)
        }
        }

    var certificate: Certificate? = null

    private val _certInfo = MutableLiveData<String>()
    private val _certName = MutableLiveData<String>()
    private val _certSourceVisible = MutableLiveData<Boolean>()
    private val _certInfoVisible = MutableLiveData<Boolean>()
    private val _certNameVisible = MutableLiveData<Boolean>()

    val certInfo: LiveData<String> = _certInfo
    val certName: LiveData<String> = _certName
    val certSourceVisible: LiveData<Boolean> = _certSourceVisible
    val certInfoVisible: LiveData<Boolean> = _certInfoVisible
    val certNameVisible: LiveData<Boolean> = _certNameVisible

    private val _issuerInfo = MutableLiveData<String>()
    private val _issuerName = MutableLiveData<String>()
    private val _issuerSourceVisible = MutableLiveData<Boolean>()
    private val _issuerInfoVisible = MutableLiveData<Boolean>()
    private val _issuerNameVisible = MutableLiveData<Boolean>()

    val issuerInfo: LiveData<String> = _issuerInfo
    val issuerName: LiveData<String> = _issuerName
    val issuerSourceVisible: LiveData<Boolean> = _issuerSourceVisible
    val issuerInfoVisible: LiveData<Boolean> = _issuerInfoVisible
    val issuerNameVisible: LiveData<Boolean> = _issuerNameVisible


    private val _buyerInfo = MutableLiveData<String>()
    private val _buyerName = MutableLiveData<String>()
    private val _buyerSourceVisible = MutableLiveData<Boolean>()
    private val _buyerInfoVisible = MutableLiveData<Boolean>()
    private val _buyerNameVisible = MutableLiveData<Boolean>()

    val buyerInfo: LiveData<String> = _buyerInfo
    val buyerName: LiveData<String> = _buyerName
    val buyerSourceVisible: LiveData<Boolean> = _buyerSourceVisible
    val buyerInfoVisible: LiveData<Boolean> = _buyerInfoVisible
    val buyerNameVisible: LiveData<Boolean> = _buyerNameVisible


}