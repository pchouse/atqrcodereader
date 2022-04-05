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

import android.util.Log
import ch.digitalfondue.vatchecker.EUVatCheckResponse
import ch.digitalfondue.vatchecker.EUVatChecker.*
import kotlinx.coroutines.*
import pt.pchouse.atqrcodereader.ServiceLocator
import pt.pchouse.atqrcodereader.api.Response
import pt.pchouse.atqrcodereader.api.Status
import pt.pchouse.atqrcodereader.logic.certificate.Certificate
import pt.pchouse.atqrcodereader.logic.certificate.Proxy
import pt.pchouse.atqrcodereader.logic.certificate.ResponseStatus


class QRCode {

    companion object {
        val LOG_TAG = QRCode::class.qualifiedName!!
    }

    private val serviceLocator = ServiceLocator.locate
    private val values: MutableMap<FieldCode, ParsedCode> = mutableMapOf()
    private val ptTinNotUseEuVatCheck = Regex("(^([1-3]|45|70|74|75|8))|999999990")
    val qrCodeRawTextErrors = mutableListOf<String>()

    var certificate: Certificate? = null
        private set

    var apiValidationResponse: Response? = null
        private set
    var issuerVatCheckerResponse: EUVatCheckResponse? = null
        private set

    var buyerVatCheckerResponse: EUVatCheckResponse? = null
        private set


    /**
     * Get the parsed values
     */
    fun getValues(): Map<FieldCode, ParsedCode> {
        return values
    }

    /**
     * Parse the QR code
     */
    suspend fun parse(qrCodeRawText: String) {

        coroutineScope {

            if (qrCodeRawText.isBlank()) {
                throw ParseException(serviceLocator.getString("qr_code_is_not_valid"))
            }

            if (qrCodeRawText.endsWith(" ")) {
                qrCodeRawTextErrors.add(
                    serviceLocator.getString("qr_code_can_not_end_with_blank_space")
                )
            }

            if (qrCodeRawText.trim().endsWith("*")) {
                qrCodeRawTextErrors.add(
                    serviceLocator.getString("qr_code_can_not_end_with_asterisk")
                )
            }

            if (qrCodeRawText.contains(
                    Regex(
                        "[\\r\\n\\t]", setOf(
                            RegexOption.MULTILINE, RegexOption.IGNORE_CASE
                        )
                    )
                )
            ) {
                qrCodeRawTextErrors.add(
                    serviceLocator.getString("qr_code_can_not_have_line_control_character")
                )
            }


            val rawTextSplit = qrCodeRawText.trim('*').split("*")

            if (rawTextSplit.isEmpty()) {
                throw ParseException(serviceLocator.getString("qr_code_is_not_valid"))
            }

            var last: String? = null

            for (field in rawTextSplit) {

                val parsed = parseField(field)

                if (values.containsKey(parsed.first)) {
                    values[parsed.first]?.errors?.add(
                        String.format(serviceLocator.getString("duplicated_filed"), parsed.first)
                    )
                    continue
                }

                values[parsed.first] = parsed.second
                if (last != null && last > parsed.toString()) {
                    values[parsed.first]?.errors?.add(
                        serviceLocator.getString("filed_code_is_out_of_order")
                    )
                }

                last = parsed.toString()
            }

            FieldCode.values().forEach {
                validateField(it)
            }

            validateVatBaseDependency()
            validateApiAsync(qrCodeRawText)
        }
    }

    /**
     * Parse each code field
     */
    private fun parseField(field: String): Pair<FieldCode, ParsedCode> {

        val fieldCode: FieldCode
        val split = field.split(":")

        if (split.isEmpty()) {
            Log.e(LOG_TAG, "Field '$field' is not valid")
            throw ParseException(
                String.format(serviceLocator.getString("qr_code_field_is_not_valid"), field)
            )
        }

        try {
            fieldCode = FieldCode.valueOf(split[0])
        } catch (e: IllegalArgumentException) {
            Log.e(LOG_TAG, "Field '${split[0]}' is not a valid to parse as field")
            throw ParseException(
                String.format(serviceLocator.getString("qr_code_field_is_not_valid"), split[0])
            )
        }

        val fieldValue = if (split.size > 2)
            split.drop(1).joinToString(":")
        else
            split[1]

        try {

            return Pair(fieldCode, ParsedCode(fieldValue, true))

        } catch (e: IllegalArgumentException) {
            Log.e(LOG_TAG, "Field '$field' is not valid")
            throw ParseException(
                String.format(
                    serviceLocator.getString("qr_code_field_code_is_not_valid"),
                    fieldCode
                )
            )
        }
    }

    /**
     * Validate field
     */
    private fun validateField(fieldCode: FieldCode) {

        val fieldProperties = FieldCode.getProperties()[fieldCode]!!
        val fieldValidator = FieldCode.getValidators()[fieldCode]
        val parsedCode = values[fieldCode]

        if (parsedCode == null && !fieldProperties.mandatory) {
            return
        }

        if (parsedCode == null && fieldProperties.mandatory) {
            values[fieldCode] = ParsedCode("", false).apply {
                this.errors.add(serviceLocator.getString("field_is_mandatory_but_not_exist"))
            }
            return
        }

        if (parsedCode!!.value.isBlank()) {
            parsedCode.isValid = false
            parsedCode.errors.add(
                serviceLocator.getString(
                    if (fieldProperties.mandatory)
                        "field_without_value"
                    else
                        "field_optional_without_value_cannot_be_in_code"
                )
            )
            return
        }

        val fieldValue = parsedCode.value

        // The value is valid by default

        if (fieldProperties.maxLength > 0 && fieldValue.length > fieldProperties.maxLength) {
            parsedCode.isValid = false
            parsedCode.errors.add(serviceLocator.getString("value_length_greater_than_max_length"))
        }

        if (fieldProperties.minLength > 0 && fieldValue.length < fieldProperties.minLength) {
            parsedCode.isValid = false
            parsedCode.errors.add(serviceLocator.getString("value_length_less_than_min_length"))
        }

        if (fieldProperties.regex.isNotBlank() && !fieldValue.matches(Regex(fieldProperties.regex))) {
            parsedCode.isValid = false
            parsedCode.errors.add(serviceLocator.getString("value_format_does_not_respect_regexp"))
        }

        if (fieldValidator == null || !parsedCode.isValid) {
            return
        }

        val args: MutableList<Any> = mutableListOf(fieldValue)
        val types: MutableList<Class<Any>> = mutableListOf(fieldValue.javaClass)

        if (fieldValidator.args.isNotEmpty()) {
            for (fieldCodeArg in fieldValidator.args) {
                val arg = values[FieldCode.valueOf(fieldCodeArg)]?.value ?: ""
                args.add(arg)
                types.add(arg.javaClass)
            }
        }

        var spread: Boolean

        val method = try {
            spread = true
            Class.forName(fieldValidator.kClass.qualifiedName!!)
                .getDeclaredMethod(
                    fieldValidator.method, *types.toTypedArray()
                )
        } catch (_: Throwable) {
            spread = false
            Class.forName(fieldValidator.kClass.qualifiedName!!)
                .getDeclaredMethod(
                    fieldValidator.method, String::class.java, List::class.java
                )
        }

        try {
            @Suppress("UNCHECKED_CAST")
            val validateResult = if (spread)
                method.invoke(Validators, *args.toTypedArray()) as ValidatorResult
            else
                method.invoke(Validators, args[0], args.drop(1) as List<String>) as ValidatorResult



            if (!validateResult.isValid) {
                parsedCode.isValid = false
                validateResult.errors.forEach {
                    parsedCode.errors.add(it)
                }
            }
        } catch (e: Throwable) {
            parsedCode.isValid = false
            parsedCode.errors.add(serviceLocator.getString("value_not_valid"))
        }
    }

    /**
     * Validate the dependencies if Vat base and Vat value
     */
    private fun validateVatBaseDependency() {
        listOf("I", "J", "K").forEach {
            (3..7 step 2).forEach { index ->
                val fieldCodeOdd = FieldCode.valueOf("$it$index")
                val fieldCodeEven = FieldCode.valueOf("$it${index + 1}")

                if (values.containsKey(fieldCodeOdd) != values.containsKey(fieldCodeEven)) {
                    val key = if (values.containsKey(fieldCodeOdd)) fieldCodeOdd else fieldCodeEven
                    values[key]?.isValid = false
                    values[key]?.errors?.add(
                        String.format(
                            ServiceLocator.locate.getString("when_field_X_or_X_exist_both_must_exist"),
                            fieldCodeOdd,
                            fieldCodeEven
                        )
                    )
                }
            }
        }
    }

    /**
     * Validate through api
     */
    private suspend fun validateApiAsync(rawQrCode: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = ServiceLocator.locate.db().settings().getAsync()?.apiUrl

                if (url == null || url.isBlank()) {
                    Log.d(LOG_TAG, "API Url in settings is blank")
                    return@withContext
                }

                if (!ServiceLocator.locate.hasInternetConnection()) {
                    Log.d(LOG_TAG, "No internet connection")
                    return@withContext
                }

                apiValidationResponse =
                    ServiceLocator.locate.getIRequest().validate(url, rawQrCode)

                apiValidationResponse?.fieldsError?.forEach {
                    if (values.containsKey(it.field)) {
                        values[it.field]!!.isValid = false
                        values[it.field]!!.errors.add(it.value ?: "")
                        return@forEach
                    }

                    values[it.field] = ParsedCode(
                        "",
                        false,
                        mutableListOf(it.value ?: "")
                    )

                }
            } catch (e: Throwable) {
                apiValidationResponse = Response(
                    Status.ERROR,
                    e.message ?: "",
                    null
                )
            }
        }
    }

    /**
     * Validate the issuer's TIN
     */
    suspend fun validateIssuerTinAsync() {
        withContext(Dispatchers.IO) {
            try {
                if (values[FieldCode.A]?.isValid != true) return@withContext
                if (values[FieldCode.A]!!.value.matches(this@QRCode.ptTinNotUseEuVatCheck)) return@withContext
                if (!ServiceLocator.locate.hasInternetConnection()) return@withContext

                this@QRCode.issuerVatCheckerResponse = doCheck(
                    "PT", values[FieldCode.A]!!.value
                )

            } catch (e: Throwable) {
                Log.e(LOG_TAG, e.message ?: "")
            }
            return@withContext
        }
    }


    /**
     * Validate the buyer's TIN
     */
    suspend fun validateBuyerTinAsync() {
        withContext(Dispatchers.IO) {
            try {
                val euCountryCodes = listOf(
                    "bg",
                    "CS",
                    "DA",
                    "DE",
                    "EL",
                    "EN",
                    "ES",
                    "ET",
                    "FI",
                    "FR",
                    "HR",
                    "HU",
                    "IT",
                    "LT",
                    "LV",
                    "MT",
                    "NL",
                    "PL",
                    "PT",
                    "RO",
                    "SK",
                    "SL",
                    "SV",
                )

                val parsedCode = values[FieldCode.B]!!

                if (!parsedCode.isValid) return@withContext

                val tin = parsedCode.value

                val countryParseCode = values[FieldCode.C]!!

                if (!countryParseCode.isValid) return@withContext

                val country = countryParseCode.value
                if (!euCountryCodes.contains(country)) return@withContext

                if (country == "PT" && tin.matches(ptTinNotUseEuVatCheck)) return@withContext

                if (!ServiceLocator.locate.hasInternetConnection()) return@withContext

                this@QRCode.buyerVatCheckerResponse = doCheck(country, tin)

            } catch (e: Throwable) {
                Log.e(LOG_TAG, e.message ?: "")
            }

            return@withContext
        }
    }

    /**
     * Get the software certification information
     */
    suspend fun softwareCertificationInfoAsync() {
        withContext(Dispatchers.IO) {
            try {
                if (values[FieldCode.R]?.isValid != true) return@withContext

                val value = values[FieldCode.R]!!.value

                if (value.matches(Regex("^(0{1,4}|9999)"))) {
                    certificate = Certificate(
                        ServiceLocator.locate.getString("program_not_certified"),
                        "", "", "", "", ""
                    )

                    return@withContext
                }

                val response = Proxy.get(value)

                if (response.responseStatus == ResponseStatus.CONNECTION_ERROR
                    || response.responseStatus == ResponseStatus.UNKNOWN
                ) return@withContext

                if (response.responseStatus == ResponseStatus.CERTIFICATE_NOT_EXIST) {
                    values[FieldCode.R]?.isValid = false
                    certificate = Certificate(
                        ServiceLocator.locate.getString("program_certified_not_exist"),
                        "", "", "", "", ""
                    )
                    return@withContext
                }

                certificate = response.certificate
                return@withContext
            } catch (e: Throwable) {
                Log.e(LOG_TAG, e.message ?: "")
                return@withContext
            }
        }
    }
}

