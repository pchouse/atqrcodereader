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

import ch.digitalfondue.vatchecker.EUTinChecker
import pt.pchouse.atqrcodereader.ServiceLocator
import java.lang.reflect.Field
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


object Validators {

    const val monetaryRegExp = "^(([1-9][0-9]{0,12})|0)\\.[0-9]{2}\$"
    val rawDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    /**
     * Validate for status string and value string
     */
    fun status(status: String, type: String): ValidatorResult {
        return try {
            status(Status.valueOf(status), Type.valueOf(type))
        } catch (e: IllegalArgumentException) {
            return ValidatorResult(false)
        }
    }

    /**
     * Validate document status for document type
     */
    fun status(status: Status, type: Type): ValidatorResult {
        return try {
            status(status, convertToDocumentGroup(type))
        } catch (e: Throwable) {
            return ValidatorResult(false)
        }
    }

    /**
     * Validate document status for document group
     */
    fun status(status: Status, group: Group): ValidatorResult {
        var field: Field? = null
        try {
            field = status.javaClass.getField(status.toString())
        } catch (_: NoSuchFieldException) {

        }

        if (field == null) {
            return ValidatorResult(
                false, mutableListOf(
                    ServiceLocator.locate.getString("value_not_valid")
                )
            )
        }

        if (field.getAnnotation(StatusProperties::class.java)?.valid?.contains(group) == true) {
            return ValidatorResult(true)
        }

        return ValidatorResult(
            false, mutableListOf(
                ServiceLocator.locate.getString("value_not_valid_for_type_of_document")
            )
        )
    }

    /**
     *  Convert document  to Document type group
     */
    @Throws(ParseException::class)
    fun convertToDocumentGroup(type: Type): Group {
        for (field: Field in Group::class.java.declaredFields) {
            if (field.getAnnotation(GroupProperties::class.java)?.types?.contains(type) == true) {
                return Group.valueOf(field.name)
            }
        }

        throw ParseException("document_type_not_exist")
    }

    /**
     * Validate the document type string
     */
    fun type(type: String): ValidatorResult {
        return try {
            Type.valueOf(type)
            ValidatorResult(true)
        } catch (_: Exception) {
            ValidatorResult(
                false, mutableListOf(
                    ServiceLocator.locate.getString("value_not_valid")
                )
            )
        }
    }

    /**
     * Validate raw date string
     */
    fun date(date: String): ValidatorResult {
        try {
            val year = LocalDate.parse(date, rawDateFormatter).year
            if (year < 2021) {
                return ValidatorResult(
                    false, mutableListOf(
                        ServiceLocator.locate.getString("qr_code_not_implemented_before_2021")
                    )
                )
            }
            return ValidatorResult(true)
        } catch (e: DateTimeParseException) {
            return ValidatorResult(
                false, mutableListOf(
                    ServiceLocator.locate.getString("value_not_valid")
                )
            )
        }
    }

    /**
     * Validate the atcud
     * @param atcud The field H
     * @param docUniId The document unique identifier, EX: FT A/999. Field G
     * @param date The raw document date. Field F
     */
    fun atcud(atcud: String, docUniId: String, date: String): ValidatorResult {
        return try {
            atcud(atcud, docUniId, LocalDate.parse(date, rawDateFormatter))
        } catch (e: DateTimeParseException) {
            atcud(atcud, docUniId, null)
        }
    }

    /**
     * Validate the atcud
     * @param atcud The field H
     * @param docUniId The document unique identifier, EX: FT A/999. Field G
     * @param date The raw document date. Field F
     */
    private fun atcud(atcud: String, docUniId: String, date: LocalDate?): ValidatorResult {
        try {
            if (atcud == "0" && date == null) {
                return ValidatorResult(
                    false, mutableListOf(
                        ServiceLocator.locate.getString("only_can_validate_with_correct_doc_date")
                    )
                )
            }

            if (atcud == "0" && date!!.year < 2023) {
                return ValidatorResult(true)
            }


            if (atcud.matches(Regex("^0-[0-9]+\$"))) {
                return ValidatorResult(
                    false, mutableListOf(
                        ServiceLocator.locate.getString("without_series_register_id_should_only_zero")
                    )
                )
            }

            if (atcud.length > 70 || !atcud.matches(Regex("^[0-9A-Z]{8,}-[0-9]+\$"))) {
                return ValidatorResult(
                    false, mutableListOf(
                        ServiceLocator.locate.getString("value_not_valid")
                    )
                )
            }

            if (docUniId.split("/")[1] == atcud.split("-")[1]) {
                return ValidatorResult(true)
            }

            return ValidatorResult(
                false, mutableListOf(
                    ServiceLocator.locate.getString("wrong_atcud_doc_number")
                )
            )

        } catch (_: Throwable) {
            return ValidatorResult(
                false, mutableListOf(
                    ServiceLocator.locate.getString("value_not_valid")
                )
            )
        }
    }

    /**
     * Validate the issuer TIN, must be a Portuguese valid TIN
     */
    fun issuerTin(tin: String): ValidatorResult {
        return if (tin == "999999990")
            ValidatorResult(
                false, mutableListOf(
                    ServiceLocator.locate.getString("issuer_tin_can_not_be_final_consumer")
                )
            )
        else tinPt(tin)
    }

    /**
     * Validate customer TIN, only validate portuguese TIN all other will evaluated as valid
     */
    fun customerTin(tin: String, country: String): ValidatorResult {
        return if (country == "PT") tinPt(tin) else ValidatorResult(true)
    }

    fun euroTin(tin: String, country: String): ValidatorResult {
        val response = EUTinChecker.doCheck(tin, country)

        if (response.isValidStructure && response.isValidSyntax) {
            return ValidatorResult(true)
        }

        return ValidatorResult(
            false, mutableListOf(
                ServiceLocator.locate.getString("value_not_valid")
            )
        )
    }

    /**
     * Validate the Portuguese Tax Identification Number (TIN)
     */
    private fun tinPt(tin: String): ValidatorResult {
        var checkSum = 0
        val startRegExp = Regex("^(1|2|3|45|5|6|70|71|72|74|75|77|78|79|8|90|91|98|99)[0-9]{7,8}\$")

        if (tin.length != 9 || !tin.matches(startRegExp)) {
            return ValidatorResult(
                false, mutableListOf(
                    ServiceLocator.locate.getString("value_not_valid")
                )
            )
        }

        for (n in 1..8) {
            checkSum += Integer.valueOf(tin.substring(n - 1, n)) * (10 - n)
        }

        val modulo11: Int = 11 - (checkSum % 11)
        val checkDigit = if (modulo11 > 9) 0 else modulo11

        if (Integer.valueOf(tin.substring(8, 9)) == checkDigit) {
            return ValidatorResult(true)
        }
        return ValidatorResult(
            false, mutableListOf(
                ServiceLocator.locate.getString("value_not_valid")
            )
        )
    }

    /**
     * Validate the Fiscal Region of field I1
     */
    fun fiscalRegion(region: String, args: List<String>): ValidatorResult {
        var allEmpty = true

        for (vat in args) {
            if (vat.isNotBlank()) allEmpty = false
        }

        if (region == "0" && allEmpty) {
            return ValidatorResult(true)
        }

        if (region == "0" && !allEmpty) {
            return ValidatorResult(
                false, mutableListOf(
                    ServiceLocator.locate.getString("value_not_valid")
                )
            )
        }

        if (allEmpty) {
            return ValidatorResult(
                false, mutableListOf(
                    ServiceLocator.locate.getString("document_without_vat_must_be_zero")
                )
            )
        }

        return ValidatorResult(true)
    }

    /**
     * Validate id the dependency is empty or not
     */
    @Suppress("UNUSED", "UNUSED_PARAMETER")
    fun dependencyEmpty(fieldValue: String, dependency: String): ValidatorResult {
        return if (dependency.isNotBlank())
            ValidatorResult(true)
        else
            ValidatorResult(
            false, mutableListOf(
                ServiceLocator.locate.getString("dependencies_without_values")
            )
        )
    }

    /**
     * Verify if the field values is equals to the sum of list
     */
    fun sumEqual(field: String, toSum: List<String>): ValidatorResult {
        try {
            var sum = BigDecimal(0.00).setScale(2, RoundingMode.HALF_UP)
            for (value in toSum) {
                if (value.isBlank()) continue
                sum = sum.add(BigDecimal(value).setScale(2, RoundingMode.HALF_UP))
            }


            if (sum == BigDecimal(field).setScale(2, RoundingMode.HALF_UP)) {
                return ValidatorResult(true)
            }

            return ValidatorResult(
                false, mutableListOf(
                    ServiceLocator.locate.getString("dependencies_wrong_sum")
                )
            )
        } catch (e: Throwable) {
            return ValidatorResult(
                false, mutableListOf(
                    ServiceLocator.locate.getString("not_possible_perform_sum_dependencies")
                )
            )
        }
    }

}