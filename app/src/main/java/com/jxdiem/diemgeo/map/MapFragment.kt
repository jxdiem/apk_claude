package com.jxdiem.diemgeo.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.jxdiem.diemgeo.R
import com.jxdiem.diemgeo.databinding.FragmentMapBinding
import com.jxdiem.diemgeo.db.MapSourceType
import com.jxdiem.diemgeo.location.TrustLevel
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment(R.layout.fragment_map) {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()

    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var polygonOverlay: Polyline? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(17.0)

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), binding.mapView).apply {
            enableMyLocation()
            runOnFirstFix {
                activity?.runOnUiThread {
                    myLocation?.let { binding.mapView.controller.animateTo(it) }
                }
            }
        }
        binding.mapView.overlays.add(myLocationOverlay)

        polygonOverlay = Polyline().apply { outlinePaint.strokeWidth = 8f }
        binding.mapView.overlays.add(polygonOverlay)

        binding.buttonAddSource.setOnClickListener { showAddSourceDialog() }
        binding.buttonLayers.setOnClickListener { showLayerMenu() }
        binding.buttonStartPolygon.setOnClickListener { viewModel.startTracking() }
        binding.buttonAddVertex.setOnClickListener { onAddVertexClicked() }
        binding.buttonFinishPolygon.setOnClickListener { onFinishClicked() }

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
                        binding.textSatellites.text = getString(
                            R.string.label_satellites, state.satellitesInView, state.satellitesUsedInFix
                        )
                        val levelLabel = when (state.trustLevel) {
                            TrustLevel.HIGH -> getString(R.string.label_trust_high)
                            TrustLevel.MEDIUM -> getString(R.string.label_trust_medium)
                            TrustLevel.LOW -> getString(R.string.label_trust_low)
                        }
                        binding.textTrust.text =
                            getString(R.string.label_trust_score, state.trustScore) + " – $levelLabel"
                        val color = when (state.trustLevel) {
                            TrustLevel.HIGH -> R.color.trust_high
                            TrustLevel.MEDIUM -> R.color.trust_medium
                            TrustLevel.LOW -> R.color.trust_low
                        }
                        binding.textTrust.setTextColor(ContextCompat.getColor(requireContext(), color))
                    }
                }
                launch {
                    viewModel.trackingState.collect { tracking ->
                        binding.buttonStartPolygon.visibility = if (tracking.isTracking) View.GONE else View.VISIBLE
                        binding.trackingControls.visibility = if (tracking.isTracking) View.VISIBLE else View.GONE

                        binding.textPolygonProgress.visibility = if (tracking.isTracking) View.VISIBLE else View.GONE
                        binding.textPolygonProgress.text = getString(R.string.polygon_area_label, tracking.liveAreaSquareMeters) +
                            "  •  " + getString(R.string.polygon_points_label, tracking.points.size)

                        val geoPoints = tracking.points.map { GeoPoint(it.latitude, it.longitude) }
                        polygonOverlay?.setPoints(if (geoPoints.size >= 2) geoPoints + geoPoints.first() else geoPoints)
                        binding.mapView.invalidate()
                    }
                }
                launch {
                    viewModel.mapSources.collect { /* consumed on demand via showLayerMenu */ }
                }
            }
        }
    }

    private fun onAddVertexClicked() {
        val added = viewModel.addVertex()
        if (!added) {
            Snackbar.make(binding.root, R.string.polygon_vertex_too_close, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun onFinishClicked() {
        showSavePolygonDialog()
    }

    private fun showSavePolygonDialog() {
        val input = TextInputEditText(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_stop_polygon)
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = input.text?.toString()?.ifBlank { "Poligono" } ?: "Poligono"
                viewModel.stopAndSavePolygon(
                    name = name,
                    onSaved = {},
                    onError = {
                        Snackbar.make(binding.root, R.string.polygon_need_more_points, Snackbar.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> viewModel.cancelTracking() }
            .setCancelable(false)
            .show()
    }

    private fun showAddSourceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_map_source, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.input_name)
        val urlInput = dialogView.findViewById<TextInputEditText>(R.id.input_url)
        val radioGroup = dialogView.findViewById<android.widget.RadioGroup>(R.id.radio_type)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.wms_dialog_title)
            .setView(dialogView)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = nameInput.text?.toString().orEmpty()
                val url = urlInput.text?.toString().orEmpty()
                if (name.isBlank() || url.isBlank()) return@setPositiveButton
                val type = if (radioGroup.checkedRadioButtonId == R.id.radio_wms) {
                    MapSourceType.WMS
                } else {
                    MapSourceType.WMTS_XYZ
                }
                viewModel.addMapSource(name, url, type)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showLayerMenu() {
        val popup = PopupMenu(requireContext(), binding.buttonLayers)
        val sources = viewModel.mapSources.value
        sources.forEachIndexed { index, source -> popup.menu.add(0, index, index, source.name) }
        popup.setOnMenuItemClickListener { item ->
            val source = sources.getOrNull(item.itemId) ?: return@setOnMenuItemClickListener false
            binding.mapView.setTileSource(TileSourceFactory.from(source))
            binding.mapView.invalidate()
            true
        }
        popup.show()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        myLocationOverlay?.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        myLocationOverlay?.disableMyLocation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
