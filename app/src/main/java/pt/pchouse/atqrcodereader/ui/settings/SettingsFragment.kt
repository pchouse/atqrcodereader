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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import pt.pchouse.atqrcodereader.databinding.FragmentSettingsBinding
import pt.pchouse.atqrcodereader.publicity.AppOpenManager
import kotlin.system.exitProcess

class SettingsFragment : Fragment() {

    companion object {
        val LOG_TAG: String = SettingsFragment::class.qualifiedName!!
    }

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var mAdView: AdView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        try {
            Log.e(LOG_TAG, "On create view start")

            settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

            Log.e(LOG_TAG, "ViewModel was initiated")

            settingsViewModel.initFields()

            _binding = FragmentSettingsBinding.inflate(inflater, container, false)
            mAdView = binding.addSettingsBannerBottom
            val root: View = binding.root

            Log.e(LOG_TAG, "Fragment was inflated")

            setListeners()
            return root
        } catch (e: Exception) {
            Log.e(LOG_TAG, e.message ?: "unknown error")
            exitProcess(1)
        }
    }

    override fun onStart() {
        super.onStart()
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    /**
     * Create the event listeners
     */
    private fun setListeners() {
        try {

            Log.e(LOG_TAG, "Creating listeners")

            val apiUrl: EditText = binding.settingsApiUrl
            settingsViewModel.apiUrl.observe(viewLifecycleOwner) {
                apiUrl.setText(it)
            }

            apiUrl.doAfterTextChanged {
                settingsViewModel.getSettings()?.apiUrl = it.toString()
                settingsViewModel.updateDb()
            }

            val showAllFields: SwitchCompat = binding.settingsShowAllFields
            settingsViewModel.showAllFields.observe(viewLifecycleOwner) {
                showAllFields.isChecked = it
            }

            showAllFields.setOnCheckedChangeListener { _, isChecked ->
                settingsViewModel.getSettings()?.showAllFields = isChecked
                settingsViewModel.updateDb()
            }

            if (AppOpenManager.isRequestLocationInEeaOrUnknown(requireContext())) {
                _binding!!.openRgpdForm.visibility = View.VISIBLE

                _binding!!.openRgpdForm.setOnClickListener {
                        AppOpenManager.openConsentForm(requireContext())
                }
            }else {
                _binding!!.openRgpdForm.visibility = View.INVISIBLE
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, e.message ?: "unknown error")
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}