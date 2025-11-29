package com.example.alerta_arequipa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.alerta_arequipa.databinding.ActivityMainBinding
import com.example.alerta_arequipa.network.PostgreSQLHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSIONS_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verificarPermisos()
        configurarBotones()

        // 🔥 PRUEBA LA CONEXIÓN
        lifecycleScope.launch {
            val helper = PostgreSQLHelper()
            val conectado = helper.testConnection()
            Log.d("TEST_BD", "Conexión a BD: $conectado")

            if (conectado) {
                val crimes = helper.obtenerUbicacionesPeligrosas(limite = 10)
                Log.d("TEST_BD", "Crímenes obtenidos: ${crimes.size}")
                crimes.forEach {
                    Log.d("TEST_BD", "Crimen: ${it.tipoCrimen} - ${it.block} - Peligro: ${it.nivelPeligro}")
                }
            }
        }
    }

    private fun verificarPermisos() {
        val permisos = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permisos.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisos.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permisosNecesarios = permisos.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permisosNecesarios.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permisosNecesarios.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun configurarBotones() {
        binding.btnPermisos.setOnClickListener {
            startActivity(Intent(this, PermisosActivity::class.java))
        }

        binding.btnDeteccion.setOnClickListener {
            startActivity(Intent(this, DeteccionActivity::class.java))
        }

        binding.btnMapa.setOnClickListener {
            startActivity(Intent(this, MapaActivity::class.java))
        }

        binding.btnAlertaSonora.setOnClickListener {
            startActivity(Intent(this, AlertaSonoraActivity::class.java))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val todosOtorgados = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!todosOtorgados) {
                // Mostrar mensaje al usuario
                startActivity(Intent(this, PermisosActivity::class.java))
            }
        }
    }
}