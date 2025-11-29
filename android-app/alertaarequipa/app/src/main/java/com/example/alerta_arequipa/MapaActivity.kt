// ===============================================
// MapaActivity.kt
// ===============================================
package com.example.alerta_arequipa

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.alerta_arequipa.databinding.ActivityMapaBinding
import com.example.alerta_arequipa.models.UbicacionPeligrosa
import com.example.alerta_arequipa.network.ApiService
import com.example.alerta_arequipa.network.UbicacionesRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MapaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapaBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var repository: UbicacionesRepository
    private var ubicacionesPeligrosas = listOf<UbicacionPeligrosa>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = UbicacionesRepository(ApiService.create())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        configurarUI()
    }

    private fun configurarUI() {
        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.btnActualizar.setOnClickListener {
            cargarUbicacionesPeligrosas()
        }

        actualizarHora()
    }

    private fun actualizarHora() {
        val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.tvHoraActualizacion.text = "Última actualización: ${formato.format(Date())}"
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
                }
            }
        }

        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        cargarUbicacionesPeligrosas()
    }

    private fun cargarUbicacionesPeligrosas() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = repository.obtenerTodasLasUbicaciones()

                result.onSuccess { ubicaciones ->
                    ubicacionesPeligrosas = ubicaciones
                    mostrarUbicacionesEnMapa()

                    binding.tvMonitoreoActivo.text = "Monitoreo Activo"
                    binding.tvCantidadAlertas.text = "${ubicaciones.size} zonas peligrosas"
                    actualizarHora()

                    Snackbar.make(
                        binding.root,
                        "Cargadas ${ubicaciones.size} ubicaciones",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }

                result.onFailure { error ->
                    Snackbar.make(
                        binding.root,
                        "Error: ${error.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Error de conexión: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun mostrarUbicacionesEnMapa() {
        mMap.clear()

        for (ubicacion in ubicacionesPeligrosas) {
            val latLng = LatLng(ubicacion.latitud, ubicacion.longitud)

            val markerColor = when (ubicacion.nivelPeligro.uppercase()) {
                "ALTO" -> BitmapDescriptorFactory.HUE_RED
                "MEDIO" -> BitmapDescriptorFactory.HUE_ORANGE
                else -> BitmapDescriptorFactory.HUE_YELLOW
            }

            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(ubicacion.tipoCrimen)
                    .snippet(ubicacion.descripcion ?: "Sin descripción")
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            )

            mMap.addCircle(
                CircleOptions()
                    .center(latLng)
                    .radius(ubicacion.radioMetros)
                    .strokeColor(Color.RED)
                    .strokeWidth(2f)
                    .fillColor(Color.argb(30, 255, 0, 0))
            )
        }
    }
}