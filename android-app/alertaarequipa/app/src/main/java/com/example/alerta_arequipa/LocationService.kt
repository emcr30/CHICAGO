// ===============================================
// LocationService.kt
// ===============================================
package com.example.alerta_arequipa

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.alerta_arequipa.models.UbicacionPeligrosa
import com.example.alerta_arequipa.network.ApiService
import com.example.alerta_arequipa.network.UbicacionesRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: UbicacionesRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var ubicacionesPeligrosas = listOf<UbicacionPeligrosa>()
    private val NOTIFICATION_CHANNEL_ID = "AlertasChannel"
    private val NOTIFICATION_ID = 1001
    private val ALERT_NOTIFICATION_ID = 1002

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        repository = UbicacionesRepository(ApiService.create())

        crearCanalNotificacion()
        iniciarForegroundService()
        cargarUbicacionesPeligrosas()
        iniciarMonitoreoUbicacion()
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Alertas de Seguridad",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de zonas peligrosas cercanas"
                enableVibration(true)
                enableLights(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun iniciarForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Monitoreo Activo")
            .setContentText("Vigilando zonas peligrosas cercanas")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun cargarUbicacionesPeligrosas() {
        serviceScope.launch {
            try {
                val result = repository.obtenerTodasLasUbicaciones()
                result.onSuccess { ubicaciones ->
                    ubicacionesPeligrosas = ubicaciones
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun iniciarMonitoreoUbicacion() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30000L
        ).apply {
            setMinUpdateIntervalMillis(15000L)
            setMaxUpdateDelayMillis(60000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    verificarProximidadZonasPeligrosas(location)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun verificarProximidadZonasPeligrosas(ubicacionActual: Location) {
        val prefs = getSharedPreferences("AlertasPrefs", MODE_PRIVATE)
        val distanciaAlerta = prefs.getFloat("distancia_alerta", 500f)

        for (ubicacionPeligrosa in ubicacionesPeligrosas) {
            val ubicacionPeligro = Location("").apply {
                latitude = ubicacionPeligrosa.latitud
                longitude = ubicacionPeligrosa.longitud
            }

            val distancia = ubicacionActual.distanceTo(ubicacionPeligro)

            if (distancia <= distanciaAlerta) {
                mostrarAlerta(ubicacionPeligrosa, distancia)
                break
            }
        }
    }

    private fun mostrarAlerta(ubicacion: UbicacionPeligrosa, distancia: Float) {
        val prefs = getSharedPreferences("AlertasPrefs", MODE_PRIVATE)
        val ultimaAlerta = prefs.getLong("ultima_alerta_${ubicacion.id.hashCode()}", 0)
        val tiempoActual = System.currentTimeMillis()

        if (tiempoActual - ultimaAlerta < 300000) {
            return
        }

        prefs.edit().putLong("ultima_alerta_${ubicacion.id.hashCode()}", tiempoActual).apply()

        val intent = Intent(this, AlertaSonoraActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("⚠️ Zona Peligrosa Cercana")
            .setContentText("${ubicacion.tipoCrimen} - ${distancia.toInt()}m de distancia")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}