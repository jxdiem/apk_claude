package com.jxdiem.diemgeo.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.jxdiem.diemgeo.R
import com.jxdiem.diemgeo.databinding.FragmentSavedBinding
import com.jxdiem.diemgeo.share.ShareHelper
import com.jxdiem.diemgeo.stego.Steganography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SavedFragment : Fragment(R.layout.fragment_saved) {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SavedViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val polygonAdapter = PolygonAdapter(
            onShare = { polygon ->
                polygon.geoJsonFilePath?.let { ShareHelper.shareGeoJson(requireContext(), File(it)) }
            },
            onDelete = { viewModel.deletePolygon(it) }
        )
        binding.recyclerPolygons.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPolygons.adapter = polygonAdapter

        val photoAdapter = PhotoAdapter(
            onShare = { photo -> ShareHelper.sharePhoto(requireContext(), File(photo.filePath)) },
            onDelete = { viewModel.deletePhoto(it) },
            onVerify = { photo, updateText -> verifyPhoto(photo.filePath, updateText) }
        )
        binding.recyclerPhotos.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPhotos.adapter = photoAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.polygons.collect { polygonAdapter.submitList(it) } }
                launch { viewModel.photos.collect { photoAdapter.submitList(it) } }
            }
        }
    }

    private fun verifyPhoto(filePath: String, updateText: (String) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeFile(filePath) ?: return@withContext null
                Steganography.extractAndVerify(bitmap)
            }
            val text = when (result) {
                is Steganography.VerificationResult.Valid -> getString(R.string.stego_verified)
                is Steganography.VerificationResult.ChecksumMismatch -> getString(R.string.stego_tampered)
                is Steganography.VerificationResult.NoSignatureFound, null ->
                    "Nessuna firma nascosta trovata (file non originale di diem_geo?)"
            }
            updateText(text)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
