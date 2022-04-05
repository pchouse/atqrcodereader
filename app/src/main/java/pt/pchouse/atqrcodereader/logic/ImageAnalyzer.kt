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

package pt.pchouse.atqrcodereader.logic

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import pt.pchouse.atqrcodereader.MainActivity
import pt.pchouse.atqrcodereader.R
import pt.pchouse.atqrcodereader.ui.home.HomeFragment

class ImageAnalyzer : ImageAnalysis.Analyzer {

    companion object{
        val LOG_TAG = ImageAnalyzer::class.qualifiedName!!
    }

    override fun analyze(imageProxy: ImageProxy) {
        scanBarcode(imageProxy)
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private fun scanBarcode(imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()
            scanner.process(inputImage)
                .addOnCompleteListener {
                    imageProxy.close()
                    if (it.isSuccessful) {
                        readBarcodeData(it.result as List<Barcode>)
                    } else {
                        it.exception?.printStackTrace()
                    }
                }
        }
    }

    /**
     * Read the qrcode and start dashboard
     */
    private fun readBarcodeData(barcodes: List<Barcode>) {
        for (barcode in barcodes) {
            when (barcode.valueType) {
                Barcode.TYPE_TEXT -> {

                    if(barcode.rawValue?.startsWith("A:") != true) return
                    if(barcode.rawValue?.indexOf('*') == -1 ) return

                    Log.d(LOG_TAG, String.format("QR code text found '%s'", barcode.rawValue))

                    HomeFragment.qrText = barcode.rawValue
                    HomeFragment.qrTextNew = true
                    Log.d(LOG_TAG, "Going to open Dashboard fragment")
                    MainActivity.getBottomNavView().selectedItemId = R.id.navigation_dashboard
                    return
                }
            }
        }
    }

}