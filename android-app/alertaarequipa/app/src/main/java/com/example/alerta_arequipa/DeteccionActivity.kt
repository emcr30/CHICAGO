// ===============================================
// DeteccionActivity.kt
// ===============================================
package com.example.alerta_arequipa

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.alerta_arequipa.databinding.ActivityDeteccionBinding
import com.example.alerta_arequipa.network.ApiService
import com.example.alerta_arequipa.network.UbicacionesRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class DeteccionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeteccionBinding
    private var deteccionActiva = false
    private lateinit var repository: UbicacionesRepository
    private var escuchandoCambios = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeteccionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = UbicacionesRepository(ApiService.create())

        configurarUI()
    }


    private fun configurarUI() {
        binding.switchDeteccion.setOnCheckedChangeListener { _, isChecked ->
            if (!escuchandoCambios) return@setOnCheckedChangeListener  // ❗ Ignora cambios automáticos

            deteccionActiva = isChecked
            actualizarEstado()

            if (isChecked) {
                iniciarDeteccion()
            } else {
                detenerDeteccion()
            }
        }

        binding.btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun actualizarEstado() {
        if (deteccionActiva) {
            binding.estadoDeteccion.text = "Detección Activa"
            binding.descripcionEstado.text = "Mantén esta opción activa para recibir alertas en tiempo real"
            binding.estadoDeteccion.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            binding.estadoDeteccion.text = "Detección Inactiva"
            binding.descripcionEstado.text = "Activa la detección para monitorear zonas peligrosas cercanas"
            binding.estadoDeteccion.setTextColor(getColor(android.R.color.darker_gray))
        }
    }

    private fun iniciarDeteccion() {
        val serviceIntent = Intent(this, LocationService::class.java)
        startService(serviceIntent)

        lifecycleScope.launch {
            try {
                val result = repository.obtenerTodasLasUbicaciones()
                result.onSuccess { ubicaciones ->
                    Snackbar.make(
                        binding.root,
                        "Cargadas ${ubicaciones.size} ubicaciones peligrosas",
                        Snackbar.LENGTH_SHORT
                    ).show()

                    val prefs = getSharedPreferences("AlertasPrefs", MODE_PRIVATE)
                    prefs.edit().putBoolean("deteccion_activa", true).apply()
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
            }
        }
    }

    private fun detenerDeteccion() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)

        val prefs = getSharedPreferences("AlertasPrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("deteccion_activa", false).apply()

        Snackbar.make(
            binding.root,
            "Detección detenida",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("AlertasPrefs", MODE_PRIVATE)
        deteccionActiva = prefs.getBoolean("deteccion_activa", false)

        escuchandoCambios = false
        binding.switchDeteccion.isChecked = deteccionActiva
        escuchandoCambios = true  //

        actualizarEstado()
    }


}