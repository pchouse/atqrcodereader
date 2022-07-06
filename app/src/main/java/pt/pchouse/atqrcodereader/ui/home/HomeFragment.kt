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

package pt.pchouse.atqrcodereader.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.ads.consent.ConsentInformation
import com.google.ads.consent.ConsentStatus
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.pchouse.atqrcodereader.MainActivity
import pt.pchouse.atqrcodereader.R
import pt.pchouse.atqrcodereader.ServiceLocator
import pt.pchouse.atqrcodereader.databinding.FragmentHomeBinding
import pt.pchouse.atqrcodereader.logic.ImageAnalyzer
import pt.pchouse.atqrcodereader.publicity.AppOpenManager
import pt.pchouse.atqrcodereader.ui.settings.Model
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HomeFragment : Fragment() {

    companion object {
        val TAG_LOG: String? = HomeFragment::class.qualifiedName
        /**
         * The last qr code text read
         */
        var qrText : String? = null

        /**
         * Define if are a not proceeded qr code text
         */
        var qrTextNew: Boolean = false
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var _cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var _cameraExecutor: ExecutorService
    private var preview: Preview = Preview.Builder().build()
    private var camaraGranted = false
    private lateinit var mAdView : AdView

    /**
     * Activity result launcher of permission
     */
    private lateinit var _launcher: ActivityResultLauncher<String>


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        Log.d(TAG_LOG, "Going to registerForActivityResult for RequestPermission")
        _launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                Log.d(TAG_LOG, "Camera permission granted")
                camaraGranted = true
            } else {
                Log.d(TAG_LOG, "Camera permission denied")
                camaraGranted = false
                shouldShowRequestPermissionRationaleDialog()
            }
        }

        checkCameraPermission()

        viewLifecycleOwner.lifecycleScope.launch{
            withContext(Dispatchers.IO){
                if(ServiceLocator.locate.db().settings().getAsync() == null){
                    ServiceLocator.locate.db().settings().saveAsync(Model(1,false, ""))
                }
            }
        }

        mAdView = binding.fragmentHome.findViewById(R.id.add_home_banner)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

       if(AppOpenManager.isPersonalizedAddsInSettings(requireContext()) == null) {
           AppOpenManager.checkRGPD(requireContext())
           AppOpenManager.openConsentForm(requireContext())
           MainActivity.IsRGPDChecked = true
       }else if(!MainActivity.IsRGPDChecked) {
           AppOpenManager.checkRGPD(requireContext())
           if(ConsentInformation.getInstance(context).consentStatus == ConsentStatus.UNKNOWN){
               AppOpenManager.openConsentForm(requireContext())
           }
           MainActivity.IsRGPDChecked = true
       }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Create the camera executor
     */
    private suspend fun createCameraExecutor() {
            withContext(Dispatchers.Default) {
                _cameraExecutor = Executors.newSingleThreadExecutor()
                _cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
                _cameraProviderFuture.addListener({
                    val cameraProvider = _cameraProviderFuture.get()
                    bindPreview(cameraProvider)
                }, ContextCompat.getMainExecutor(requireContext()))
            }
    }

    /**
     * Camera preview
     */
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        Log.d(TAG_LOG, "Camera selector was build")

        preview.setSurfaceProvider(binding.previewView.surfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        Log.d(TAG_LOG, "Image analyses was build")
        imageAnalysis.setAnalyzer(_cameraExecutor, ImageAnalyzer())

        Log.d(TAG_LOG, "Camera executor was set to image analyser")

        cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageAnalysis,
            preview
        )

        Log.d(TAG_LOG, "Camera provider was bind to life cycle")
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
           if (camaraGranted) createCameraExecutor()
        }
    }


    override fun onPause() {
        try {
            _cameraProviderFuture.get().unbindAll()
            Log.d(TAG_LOG, "Camera provider was unbounded all")
            if (!_cameraExecutor.isShutdown) {
                _cameraExecutor.shutdown()
                Log.d(TAG_LOG, "Camera executor was shutdown")
            }
        } catch (e: Exception) {
            Log.e(TAG_LOG, e.message ?: "Unknown exception message")
        }
        super.onPause()
    }
    /**
     * Check for camera permissions
     */
    private fun checkCameraPermission() {
        Log.d(TAG_LOG, "Checking for camera permission")

        val cameraPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        )

        Log.d(TAG_LOG, String.format("Camera permission is %s", cameraPermission))

        when {

            cameraPermission == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG_LOG, "Camera permission is granted, going to create camera executor")
                camaraGranted = true
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Log.d(TAG_LOG, "Camera permission request should show rational message")
                this.shouldShowRequestPermissionRationaleDialog()
            }

            else -> {
                Log.d(TAG_LOG, "Camera permission request launch")
                (_launcher).launch(Manifest.permission.CAMERA)
            }
        }

    }

    /**
     * Show a rational dialog for the permission
     */
    private fun shouldShowRequestPermissionRationaleDialog() {

        Log.d(TAG_LOG, "Start build a rational dialog for camera permissions")

        val builder: AlertDialog.Builder = activity.let {
            AlertDialog.Builder(it)
        }

        builder.setTitle(R.string.request_for_camera_permission)
            ?.setMessage(R.string.camera_permission_rational_message)

        builder.apply {

            setNegativeButton(
                R.string.no
            ) { _, _ ->
                this@HomeFragment.activity?.finish()
            }

            setPositiveButton(R.string.ok) { _, _ ->
                    _launcher.launch(Manifest.permission.CAMERA)
            }
        }
        Log.d(TAG_LOG, "Showing a rational dialog for camera permissions")
        builder.create().show()
    }

}