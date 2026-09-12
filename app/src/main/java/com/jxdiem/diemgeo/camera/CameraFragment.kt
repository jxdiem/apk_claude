package com.jxdiem.diemgeo.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jxdiem.diemgeo.R
import com.jxdiem.diemgeo.databinding.FragmentCameraBinding
import com.jxdiem.diemgeo.location.TrustLevel
import com.jxdiem.diemgeo.share.ShareHelper
import kotlinx.coroutines.launch
import java.io.File

class CameraFragment : Fragment(R.layout.fragment_camera) {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModels()
    private var imageCapture: ImageCapture? = null
    private var lastPhotoFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startCamera()

        binding.buttonCapture.setOnClickListener { takePhoto() }
        binding.buttonSharePhoto.setOnClickListener {
            lastPhotoFile?.let { ShareHelper.sharePhoto(requireContext(), it) }
        }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.trustEngine.state.collect { state ->
                        val accuracyText = state.location?.takeIf { it.hasAccuracy() }
                            ?.let { getString(R.string.label_gps_accuracy, it.accuracy) }
                            ?: "Precisione GPS: n/d"
                        binding.textAccuracy.text = accuracyText
                        val levelLabel = when (state.trustLevel) {
                            TrustLevel.HIGH -> getString(R.string.label_trust_high)
                            TrustLevel.MEDIUM -> getString(R.string.label_trust_medium)
                            TrustLevel.LOW -> getString(R.string.label_trust_low)
                        }
                        binding.textTrust.text =
                            getString(R.string.label_trust_score, state.trustScore) + " – $levelLabel"
                    }
                }
                launch {
                    viewModel.lastCapture.collect { photo ->
                        lastPhotoFile = File(photo.filePath)
                        binding.resultCard.visibility = View.VISIBLE
                        binding.textAiResult.text = if (photo.aiLabels.isNotEmpty()) {
                            getString(R.string.ai_label_result, photo.aiLabels.joinToString { it.text })
                        } else {
                            getString(R.string.ai_label_none)
                        }
                    }
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (_: Exception) {
                // camera not available on this device/emulator; UI stays but capture is disabled
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        binding.textAiResult.text = getString(R.string.ai_label_analyzing)
        binding.resultCard.visibility = View.VISIBLE

        capture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    if (bitmap != null) viewModel.capture(bitmap)
                }

                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                    binding.textAiResult.text = "Errore scatto: ${exception.message}"
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val rotation = image.imageInfo.rotationDegrees
        if (rotation == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
