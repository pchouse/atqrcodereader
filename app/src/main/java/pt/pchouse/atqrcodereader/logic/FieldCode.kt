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

enum class FieldCode {

    /**
     * Field: A (This field is mandatory)<br>
     * Issuer TIN (NIF)<br>
     * The issuer's TIN (NIF) without Country prefix<br>
     */
    @FieldProperties(true, regex = "^[1-9][0-9]{8}$")
    @FieldValidator("issuerTin")
    A,

    /**
     * Field: B (This field is mandatory)<br>
     * Buyer's TIN (NIF)<br>
     * The buyer's TIN (NIF)
     */
    @FieldProperties(true, maxLength = 30)
    @FieldValidator("customerTin", ["C"])
    B,

    /**
     * Field: C (This field is mandatory)<br>
     * Buyer's Country<br>
     */
    @FieldProperties(
        true,
        regex = "^(AD|AE|AF|AG|AI|AL|AM|AO|AQ|AR|AS|AT|AU|AW|AX|AZ|BA|BB|BD|BE|BF|BG|BH|BI|BJ|BL|BM|BN|BO|BQ|BR|BS|BT|BV|BW|BY|BZ|CA|CC|CD|CF|CG|CH|CI|CK|CL|CM|CN|CO|CR|CU|CV|CW|CX|CY|CZ|DE|DJ|DK|DM|DO|DZ|EC|EE|EG|EH|ER|ES|ET|FI|FJ|FK|FM|FO|FR|GA|GB|GD|GE|GF|GG|GH|GI|GL|GM|GN|GP|GQ|GR|GS|GT|GU|GW|GY|HK|HM|HN|HR|HT|HU|ID|IE|IL|IM|IN|IO|IQ|IR|IS|IT|JE|JM|JO|JP|KE|KG|KH|KI|KM|KN|KP|KR|KW|KY|KZ|LA|LB|LC|LI|LK|LR|LS|LT|LU|LV|LY|MA|MC|MD|ME|MF|MG|MH|MK|ML|MM|MN|MO|MP|MQ|MR|MS|MT|MU|MV|MW|MX|MY|MZ|NA|NC|NE|NF|NG|NI|NL|NO|NP|NR|NU|NZ|OM|PA|PE|PF|PG|PH|PK|PL|PM|PN|PR|PS|PT|PW|PY|QA|RE|RO|RS|RU|RW|SA|SB|SC|SD|SE|SG|SH|SI|SJ|SK|SL|SM|SN|SO|SR|SS|ST|SV|SX|SY|SZ|TC|TD|TF|TG|TH|TJ|TK|TL|TM|TN|TO|TR|TT|TV|TW|TZ|UA|UG|UM|US|UY|UZ|VA|VC|VE|VG|VI|VN|VU|WF|WS|XK|YE|YT|ZA|ZM|ZW|Desconhecido)\$"
    )
    C,

    /**
     * Field: D (This field is mandatory)<br>
     * Document type<br>
     * According to the type of SAF-T (PT).<br>
     */
    @FieldProperties(true)
    @FieldValidator("type")
    D,

    /**
     * Field: E (This field is mandatory)<br>
     * Document status<br>
     * According to the type of SAF-T (PT).<br>
     */
    @FieldProperties(true)
    @FieldValidator("status", ["D"])
    E,

    /**
     * Field: F (This field is mandatory)<br>
     * Document date<br>
     * Format 'yyyyMMdd'<br>
     */
    @FieldProperties(true)
    @FieldValidator("date")
    F,

    /**
     * Field: G (This field is mandatory)<br>
     * Unique identification of the document<br>
     * According to the type of SAF-T (PT).<br>
     * Ex: 'FT A/999'<br>
     */
    @FieldProperties(true, "[^ ]+ [^/^ ]+/[0-9]+", 60)
    G,

    /**
     * Field: H (This field is mandatory)<br>
     * ATCUD<br>
     * Ex: 'CSDF7T5H-999'<br>
     */
    @FieldProperties(true, maxLength = 70)
    @FieldValidator("atcud", ["G", "F"])
    H,

    /**
     * Field: I1<br>
     * Verify if is a document
     * without an indication of the VAT rate,
     * which should appear in table 4.2, 4.3 or 4.4 of the SAF-T (PT)
     */
    @FieldProperties(true, "^0|PT\$")
    @FieldValidator(
        "fiscalRegion", [
            "I2", "I3", "I4", "I5", "I6", "I7", "I8",
            "J1", "J2", "J3", "J4", "J5", "J6", "J7", "J8",
            "K1", "K2", "K3", "K4", "K5", "K6", "K7", "K8"
        ]
    )
    I1,

    /**
     * Field: I2<br>
     * VAT-exempt tax base of PT fiscal region<br>
     * Total value of the tax base exempt from VAT
     * (including taxable transactions under Stamp Duty,
     * whether or not they are exempt from Stamp Duty).
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["I1"])
    I2,

    /**
     * Field: I3<br>
     * VAT tax base at reduced rate<br>
     * Total value of the tax base subject to the reduced VAT rate.
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["I1"])
    I3,

    /**
     * Field: I4<br>
     * Total VAT at reduced rate<br>
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["I1"])
    I4,

    /**
     * Field: I5<br>
     * VAT tax base at intermediate rate<br>
     * Total value of the tax base subject to the intermediate VAT rate.
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["I1"])
    I5,

    /**
     * Field: I6<br>
     * Total VAT at intermediate rate<br>
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["I1"])
    I6,

    /**
     * Field: I7<br>
     * VAT tax base at normal rate<br>
     * Total value of the tax base subject to the normal VAT rate.
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["I1"])
    I7,

    /**
     * Field: I8<br>
     * Total VAT at normal rate<br>
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["I1"])
    I8,

    /**
     * If the document has VAT from the fiscal region of Azores must be PT-AC
     */
    @FieldProperties(false, "^PT-AC\$")
    @FieldValidator(
        "fiscalRegion", [
            "J2", "J3", "J4", "J5", "J6", "J7", "J8",
        ]
    )
    J1,

    /**
     * Field: J2<br>
     * VAT-exempt tax base of PTAC fiscal region<br>
     * Total value of the tax base exempt from VAT
     * (including taxable transactions under Stamp Duty,
     * whether or not they are exempt from Stamp Duty).
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["J1"])
    J2,

    /**
     * Field: J3<br>
     * VAT tax base at reduced rate<br>
     * Total value of the tax base subject to the reduced VAT rate.
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["J1"])
    J3,

    /**
     * Field: J4<br>
     * Total VAT at reduced rate<br>
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["J1"])
    J4,

    /**
     * Field: J5<br>
     * VAT tax base at intermediate rate<br>
     * Total value of the tax base subject to the intermediate VAT rate.
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["J1"])
    J5,

    /**
     * Field: J6<br>
     * Total VAT at intermediate rate<br>
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["J1"])
    J6,

    /**
     * Field: J7<br>
     * VAT tax base at normal rate<br>
     * Total value of the tax base subject to the normal VAT rate.
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["J1"])
    J7,

    /**
     * Field: J8<br>
     * Total VAT at normal rate<br>
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["J1"])
    J8,

    /**
     * If the document has VAT from the fiscal region of Madeira must be PT-MA
     */
    @FieldProperties(false, "^PT-MA\$", maxLength = 16)
    @FieldValidator(
        "fiscalRegion", [
            "K2", "K3", "K4", "K5", "K6", "K7", "K8"
        ]
    )
    K1,

    /**
     * Field: K2<br>
     * VAT-exempt tax base of PTMA fiscal region<br>
     * Total value of the tax base exempt from VAT
     * (including taxable transactions under Stamp Duty,
     * whether or not they are exempt from Stamp Duty).
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["K1"])
    K2,

    /**
     * Field: K3<br>
     * VAT tax base at reduced rate<br>
     * Total value of the tax base subject to the reduced VAT rate.
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["K1"])
    K3,

    /**
     * Field: K4<br>
     * Total VAT at reduced rate<br>
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["K1"])
    K4,

    /**
     * Field: K5<br>
     * VAT tax base at intermediate rate<br>
     * Total value of the tax base subject to the intermediate VAT rate.
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["K1"])
    K5,

    /**
     * Field: K6<br>
     * Total VAT at intermediate rate<br>
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["K1"])
    K6,

    /**
     * Field: K7<br>
     * VAT tax base at normal rate<br>
     * Total value of the tax base subject to the normal VAT rate.
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["K1"])
    K7,

    /**
     * Field: K8<br>
     * Total VAT at normal rate<br>
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator("dependencyEmpty", ["K1"])
    K8,

    /**
     * Field: L<br>
     * Not subject / non-taxable in VAT (IVA)<br>
     * Total value related to non-subject / non-taxable transactions in VAT.
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    L,

    /**
     * Field: M<br>
     * Stamp tax<br>
     * Total value of stamp tax.
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    M,

    /**
     * Field: N<br>
     * TaxPayable<br>
     * Total value of VAT and Stamp Tax - TaxPayable field of SAF-T (PT)
     */
    @FieldProperties(true, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator(
        "sumEqual", [
            "I4", "I6", "I8","J4", "J6", "J8","K4", "K6", "K8", "M"
        ]
    )
    N,

    /**
     * Field: O<br>
     * Gross total<br>
     * Total document value - GrossTotal field of SAF-T (PT).
     */
    @FieldProperties(true, Validators.monetaryRegExp, maxLength = 16)
    @FieldValidator(
        "sumEqual", [
            "I2", "I3", "I5","I7", "J2", "J3", "J5","J7", "K2", "K3", "K5","K7", "L", "N"
        ]
    )
    O,

    /**
     * Field: P<br>
     * Withholding Tax Amount
     */
    @FieldProperties(false, Validators.monetaryRegExp, maxLength = 16)
    P,

    /**
     * Field: Q<br>
     * 4 Hash characters
     */
    @FieldProperties(true, minLength = 4, maxLength = 4)
    Q,

    /**
     * Field: R<br>
     * Program certificate number
     */
    @FieldProperties(true, "[0-9]{1,4}", maxLength = 4)
    R,

    /**
     * Field: S<br>
     * Other info<br>
     * Field of free filling, in which, for example,
     * information for payment can be indicated
     * (ex: from IBAN or Ref MB, with the tab «;»).
     * This field cannot contain the asterisk character (*).
     */
    @FieldProperties(false, "^[^\\*]+\$", minLength = 1, maxLength = 65)
    S;

    companion object {

        /**
         * Stack of all field properties
         */
        private var properties: MutableMap<FieldCode, FieldProperties>? = null

        /**
         * Stack of all field validator
         */
        private var validators: MutableMap<FieldCode, FieldValidator>? = null

        /**
         * Parse the properties and validators annotations fo each field
         */
        private fun parseAnnotations() {

            properties = mutableMapOf()
            validators = mutableMapOf()

            for (fieldCode in values()) {
                val field = FieldCode::class.java.getDeclaredField(fieldCode.toString())

                // parse properties
                val fieldProperties = field.getAnnotation(FieldProperties::class.java)
                    ?: throw Exception(
                        "Field '${fieldCode}' must be annotated with FieldProperties and is not"
                    )

                properties!![fieldCode] = fieldProperties

                // parse annotations
                val fieldValidator = field.getAnnotation(FieldValidator::class.java)
                    ?: continue

                validators!![fieldCode] = fieldValidator
            }
        }

        /**
         * Get all field properties
         */
        fun getProperties(): MutableMap<FieldCode, FieldProperties> {
            if (properties == null) parseAnnotations()
            return properties!!
        }

        /**
         * Get all field validators
         */
        fun getValidators(): MutableMap<FieldCode, FieldValidator> {
            if (validators == null) parseAnnotations()
            return validators!!
        }

    }

}