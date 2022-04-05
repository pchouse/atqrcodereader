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

import android.app.AlertDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import ch.digitalfondue.vatchecker.EUTinChecker
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.launch
import pt.pchouse.atqrcodereader.MainActivity
import pt.pchouse.atqrcodereader.R
import pt.pchouse.atqrcodereader.ServiceLocator
import pt.pchouse.atqrcodereader.databinding.FragmentDashboardBinding
import pt.pchouse.atqrcodereader.logic.*
import pt.pchouse.atqrcodereader.logic.certificate.Proxy
import pt.pchouse.atqrcodereader.ui.home.HomeFragment
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class DashboardFragment : Fragment() {

    companion object {
        val TAG_LOG: String = DashboardFragment::class.qualifiedName!!
        private var qrCode: QRCode? = null
        const val TAG_CARD = "DASHBOARD_CARD"
        const val TAG_FIELD = "DASHBOARD_CARD_FIELD"
        const val TAG_VALUE = "DASHBOARD_CARD_VALUE"
    }

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // The values are immutable, does not need viewModel
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var mAdView: AdView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        mAdView =
            binding.addDashboardHeadBannerContainer.findViewById(R.id.add_dashboard_head_banner)
        initComponents()
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Initiate the components
     */
    private fun initComponents() {

        viewLifecycleOwner.lifecycleScope.launch {
            try {

                if (HomeFragment.qrText == null) {
                    throw ParseException(requireContext().getString(R.string.no_qr_code_text))
                }

                if (HomeFragment.qrTextNew || qrCode == null) {
                    qrCode = QRCode()
                    qrCode!!.parse(HomeFragment.qrText ?: "")
                    HomeFragment.qrTextNew = false
                }

                FieldCode.values().forEach { fieldCode ->

                    val settings = ServiceLocator.locate.db().settings().getAsync()!!
                    val parsedCode = qrCode!!.getValues()[fieldCode]

                    if (!settings.showAllFields && parsedCode == null) {
                        return@forEach
                    }

                    val fieldNameColor = when (parsedCode?.isValid) {
                        true -> ContextCompat.getColor(requireContext(), R.color.field_valid)
                        false -> ContextCompat.getColor(requireContext(), R.color.field_error)
                        else -> null
                    }

                    val card = layoutInflater.inflate(
                        R.layout.field_card, binding.root, false
                    ) as CardView

                    card.tag = "${TAG_CARD}_${fieldCode.name}"

                    card.findViewById<TextView>(R.id.field_code).also {
                        it.text = fieldCode.name
                        if (fieldNameColor != null) it.setTextColor(fieldNameColor)
                        it.tag = "${TAG_FIELD}_${fieldCode.name}"
                    }

                    card.findViewById<TextView>(R.id.field_value).also {
                        it.text = parsedCode?.value
                            ?.replace("\n", "\\n")
                            ?.replace("\r", "\\r")
                            ?.replace("\t", "\\t")
                            ?: ""

                        it.tag = "${TAG_VALUE}_${fieldCode.name}"
                    }

                    card.findViewById<TextView>(R.id.field_description).text =
                        ServiceLocator.locate.getString("field_${fieldCode.name}")

                    val cardSkeleton = card.findViewById(R.id.card_skeleton) as LinearLayout

                    @Suppress("UNUSED_EXPRESSION")
                    when (fieldCode) {
                        FieldCode.A -> fillIssuerNifInfo(
                            qrCode!!,
                            cardSkeleton
                        )
                        FieldCode.B -> fillBuyerNifInfo(
                            qrCode!!, cardSkeleton
                        )
                        FieldCode.D -> if (parsedCode?.isValid == true) fillTypeOfDocument(
                            parsedCode.value,
                            card
                        )
                        FieldCode.E -> if (parsedCode?.isValid == true) fillStatusOfDocument(
                            parsedCode.value,
                            card
                        )
                        FieldCode.F -> if (parsedCode?.isValid == true) fillDocDate(
                            parsedCode.value,
                            card
                        )
                        FieldCode.I1 -> if (parsedCode?.isValid == true) fillPTRegimeInfo(
                            qrCode!!,
                            card
                        )
                        FieldCode.I2 -> fillVatRate(VatRate.pt(VatRate.Type.ISE), card)
                        FieldCode.I3 -> fillVatRate(VatRate.pt(VatRate.Type.RED), card)
                        FieldCode.I5 -> fillVatRate(VatRate.pt(VatRate.Type.INT), card)
                        FieldCode.I7 -> fillVatRate(VatRate.pt(VatRate.Type.NOR), card)
                        FieldCode.J2 -> fillVatRate(VatRate.ptAz(VatRate.Type.ISE), card)
                        FieldCode.J3 -> fillVatRate(VatRate.ptAz(VatRate.Type.RED), card)
                        FieldCode.J5 -> fillVatRate(VatRate.ptAz(VatRate.Type.INT), card)
                        FieldCode.J7 -> fillVatRate(VatRate.ptAz(VatRate.Type.NOR), card)
                        FieldCode.K2 -> fillVatRate(VatRate.ptAm(VatRate.Type.ISE), card)
                        FieldCode.K3 -> fillVatRate(VatRate.ptAm(VatRate.Type.RED), card)
                        FieldCode.K5 -> fillVatRate(VatRate.ptAm(VatRate.Type.INT), card)
                        FieldCode.K7 -> fillVatRate(VatRate.ptAm(VatRate.Type.NOR), card)
                        FieldCode.R -> if (parsedCode?.isValid == true) fillCertificateNumber(
                            cardSkeleton
                        )
                        else -> null
                    }

                    if (parsedCode?.errors?.isNotEmpty() == true) {
                        parsedCode.errors.forEach { error ->
                            cardSkeleton.addView(
                                (layoutInflater.inflate(
                                    R.layout.error_text,
                                    binding.root,
                                    false
                                ) as TextView).also {
                                    it.text = error
                                }
                            )
                        }
                    }

                    binding.fragmentDashboard.addView(card)
                }

                addRawValue(HomeFragment.qrText!!)

                if (qrCode?.apiValidationResponse?.message?.isNotBlank() == true) {
                    addMessageFromApiValidator(qrCode?.apiValidationResponse?.message!!)
                }

            } catch (e: Throwable) {
                qrCode = null

                val builder: AlertDialog.Builder = activity.let {
                    AlertDialog.Builder(it)
                }

                builder.setTitle(R.string.attention)
                builder.setMessage(e.message ?: "")
                builder.apply {

                    setCancelable(false)

                    setNeutralButton(
                        R.string.ok
                    ) { _, _ ->
                        MainActivity.getBottomNavView().selectedItemId = R.id.navigation_home
                    }
                }
                Log.d(HomeFragment.TAG_LOG, "Show dialog of QR code parse erros")
                Log.e(HomeFragment.TAG_LOG, e.message ?: "")
                builder.create().show()
            } finally {
                binding.dashboardLoader.visibility = View.GONE
            }
        }
    }

    /**
     * Add the qr code raw value card
     */
    private fun addRawValue(rawQrCode: String) {

        val card = layoutInflater.inflate(
            R.layout.qr_code_raw_card, binding.root, false
        ) as CardView

        card.findViewById<TextView>(R.id.qr_code_raw_value).apply {
            text = rawQrCode
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .trim()
        }

        val errors = StringBuilder()

        qrCode?.qrCodeRawTextErrors?.forEach {
            errors.append(it)
            errors.append("\n")
        }

        if (qrCode?.qrCodeRawTextErrors?.isNotEmpty() == true) {
            card.findViewById<TextView>(R.id.qr_code_raw_errors).apply {
                text = errors.toString().removeSuffix("\n")
            }

            card.findViewById<TextView>(R.id.qr_code_raw_label).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setTextColor(requireContext().getColor(R.color.field_error))
                } else {
                    setTextColor(Color.RED)
                }
            }
        }

        binding.fragmentDashboard.addView(card)
    }

    /**
     * Fill the message return from validation API
     */
    private fun addMessageFromApiValidator(message: String) {
        val card =
            layoutInflater.inflate(R.layout.api_response_message, binding.root, false) as CardView
        card.findViewById<TextView>(R.id.api_response_message).apply {
            text = message
        }
        binding.fragmentDashboard.addView(card)
    }

    /**
     * Fill the Fiscal region PT info
     */
    private fun fillPTRegimeInfo(qrCode: QRCode, cardView: CardView) {
        var textInfo: String? = null
        val value = qrCode.getValues()[FieldCode.I1]?.value
        if (value == "0") {
            textInfo = getString(R.string.document_without_vat)
        }

        (2..8).forEach {
            if (qrCode.getValues()[FieldCode.valueOf("I$it")]?.isValid == true) {
                textInfo = getString(R.string.vat_fiscal_region_continental)
            }
        }

        cardView.findViewById<TextView>(R.id.field_description).apply {
            this.text = textInfo ?: getString(R.string.document_without_vat)
        }
    }

    /**
     * Populate the Issuer information if exists
     */
    private fun fillIssuerNifInfo(qrCode: QRCode, view: LinearLayout) {
        try {
            val tinInfoLayout =
                layoutInflater.inflate(R.layout.tin_info, binding.root, false) as LinearLayout

            val source = tinInfoLayout.findViewById<TextView>(R.id.tin_source_info).also {
                it.text = StringBuilder(getString(R.string.info_source))
                    .append(": ").append(EUTinChecker.ENDPOINT)
                it.visibility = View.INVISIBLE
            }

            dashboardViewModel.issuerSourceVisible.observe(viewLifecycleOwner) {
                source.visibility = if (it) View.VISIBLE else View.GONE
            }

            val name = tinInfoLayout.findViewById<TextView>(R.id.tin_info_name)
            dashboardViewModel.issuerName.observe(viewLifecycleOwner) {
                name.text = it
            }
            dashboardViewModel.issuerNameVisible.observe(viewLifecycleOwner) {
                name.visibility = if (it) View.VISIBLE else View.GONE
            }

            val info = tinInfoLayout.findViewById<TextView>(R.id.tin_info_address)
            dashboardViewModel.issuerInfo.observe(viewLifecycleOwner) {
                info.text = it
            }
            dashboardViewModel.issuerInfoVisible.observe(viewLifecycleOwner) {
                info.visibility = if (it) View.VISIBLE else View.GONE
            }

            view.addView(tinInfoLayout)
            dashboardViewModel.handleIssuer(qrCode)

        } catch (e: Throwable) {
            Log.e(TAG_LOG, e.message ?: "")
        }
    }

    /**
     * Populate the Issuer information if exists
     */
    private fun fillBuyerNifInfo(qrCode: QRCode, view: LinearLayout) {
        try {
            val tinInfoLayout =
                layoutInflater.inflate(R.layout.tin_info, binding.root, false) as LinearLayout

            val source = tinInfoLayout.findViewById<TextView>(R.id.tin_source_info).also {
                it.text = StringBuilder(getString(R.string.info_source)).append(": ")
                    .append(EUTinChecker.ENDPOINT)
                it.visibility = View.INVISIBLE
            }
            dashboardViewModel.buyerSourceVisible.observe(viewLifecycleOwner) {
                source.visibility = if (it) View.VISIBLE else View.GONE
            }

            val name = tinInfoLayout.findViewById<TextView>(R.id.tin_info_name)
            dashboardViewModel.buyerName.observe(viewLifecycleOwner) {
                name.text = it
            }
            dashboardViewModel.buyerNameVisible.observe(viewLifecycleOwner) {
                name.visibility = if (it) View.VISIBLE else View.GONE
            }

            val info = tinInfoLayout.findViewById<TextView>(R.id.tin_info_address)
            dashboardViewModel.buyerInfo.observe(viewLifecycleOwner) {
                info.text = it
            }
            dashboardViewModel.buyerInfoVisible.observe(viewLifecycleOwner) {
                info.visibility = if (it) View.VISIBLE else View.GONE
            }

            view.addView(tinInfoLayout)
            dashboardViewModel.handleBuyer(qrCode)

        } catch (e: Throwable) {
            Log.e(TAG_LOG, e.message ?: "")
        }
    }

    /**
     * Fill document date info
     */
    private fun fillDocDate(date: String, cardView: CardView) {
        try {

            val dateFormatted: String = LocalDate
                .parse(date, Validators.rawDateFormatter)
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))

            cardView.findViewById<TextView>(R.id.field_description_info).apply {
                text = StringBuilder("  ").append(dateFormatted)
            }
        } catch (e: Throwable) {
            Log.e(TAG_LOG, e.message ?: "")
        }
    }

    /**
     * Fill with the document type name
     */
    private fun fillTypeOfDocument(type: String, cardView: CardView) {
        try {
            cardView.findViewById<TextView>(R.id.field_description_info).apply {
                text = StringBuilder(" -> ").append(
                    resources.getString(
                        resources.getIdentifier(
                            "doc_type_${type}", "string", "pt.pchouse.atqrcodereader"
                        )
                    )
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG_LOG, e.message ?: "")
        }
    }

    /**
     * Fill with the document status name
     */
    private fun fillStatusOfDocument(status: String, cardView: CardView) {
        try {
            cardView.findViewById<TextView>(R.id.field_description_info).apply {
                text = StringBuilder(" -> ").append(
                    resources.getString(
                        resources.getIdentifier(
                            "status_${status}", "string", "pt.pchouse.atqrcodereader"
                        )
                    )
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG_LOG, e.message ?: "")
        }
    }

    /**
     * Fill with the document status name
     */
    private fun fillVatRate(rate: String, cardView: CardView) {
        try {
            cardView.findViewById<TextView>(R.id.field_description_info).apply {
                text = StringBuilder(" ").append(rate)
            }
        } catch (e: Throwable) {
            Log.e(TAG_LOG, e.message ?: "")
        }
    }

    /**
     * Fill the certification information
     */
    private fun fillCertificateNumber(view: LinearLayout) {
        try {
            val infoLayout =
                layoutInflater.inflate(R.layout.tin_info, binding.root, false) as LinearLayout

            val source = infoLayout.findViewById<TextView>(R.id.tin_source_info).also {
                it.text = StringBuilder(getString(R.string.info_source))
                    .append(": ").append(Proxy.END_POINT).toString()
                it.visibility = View.INVISIBLE
            }

            dashboardViewModel.certSourceVisible.observe(viewLifecycleOwner) {
                source.visibility = if (it) View.VISIBLE else View.GONE
            }

            val name = infoLayout.findViewById<TextView>(R.id.tin_info_name)
            dashboardViewModel.certName.observe(viewLifecycleOwner) {
                name.text = it
            }
            dashboardViewModel.certNameVisible.observe(viewLifecycleOwner) {
                name.visibility = if (it) View.VISIBLE else View.GONE
            }

            val info = infoLayout.findViewById<TextView>(R.id.tin_info_address)
            dashboardViewModel.certInfo.observe(viewLifecycleOwner) {
                info.text = it

            }
            dashboardViewModel.certInfoVisible.observe(viewLifecycleOwner) {
                info.visibility = if (it) View.VISIBLE else View.GONE
            }

            view.addView(infoLayout)

            dashboardViewModel.handleCertificate(qrCode!!)

        } catch (e: Throwable) {
            Log.e(TAG_LOG, e.message ?: "")
        }
    }
}


