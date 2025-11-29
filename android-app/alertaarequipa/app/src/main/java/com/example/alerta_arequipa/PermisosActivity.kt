// ===============================================
// PermisosActivity.kt
// ===============================================
package com.example.alerta_arequipa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.alerta_arequipa.databinding.ActivityPermisosBinding
import com.google.android.material.snackbar.Snackbar

class PermisosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermisosBinding

    private val REQUEST_FOREGROUND_AND_NOTIF = 1001
    private val REQUEST_BACKGROUND = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermisosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarUI()
        actualizarEstadoPermisos()
    }

    private fun configurarUI() {
        binding.btnSolicitarPermisos.setOnClickListener {
            solicitarPermisosForeground()
        }

        binding.btnCancelar.setOnClickListener {
            finish()
        }

        binding.btnConfigurar.setOnClickListener {
            abrirConfiguracion()
        }
    }

    // --------------------- SOLICITUD 1 ----------------------
    private fun solicitarPermisosForeground() {
        val permisos = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisos.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        ActivityCompat.requestPermissions(
            this,
            permisos.toTypedArray(),
            REQUEST_FOREGROUND_AND_NOTIF
        )
    }

    // --------------------- SOLICITUD 2 ----------------------
    private fun solicitarPermisoBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_BACKGROUND
            )
        }
    }

    private fun abrirConfiguracion() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun actualizarEstadoPermisos() {
        val fine = tienePermiso(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = tienePermiso(Manifest.permission.ACCESS_COARSE_LOCATION)

        val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tienePermiso(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else true

        val notificaciones = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            tienePermiso(Manifest.permission.POST_NOTIFICATIONS)
        } else true

        val todos = fine && coarse && background && notificaciones

        binding.estadoPermisos.text = if (todos) "✓ Todos los permisos otorgados" else "✗ Permisos pendientes"
        binding.btnSolicitarPermisos.isEnabled = !todos

        // Cuando foreground está OK, solicitar background automáticamente
        if ((fine && coarse) && !background) {
            solicitarPermisoBackground()
        }
    }

    private fun tienePermiso(permiso: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_FOREGROUND_AND_NOTIF || requestCode == REQUEST_BACKGROUND) {
            actualizarEstadoPermisos()
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarEstadoPermisos()
    }
}
