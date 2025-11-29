// ===============================================
// AlertaSonoraActivity.kt (corregido)
// ===============================================
package com.example.alerta_arequipa

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.example.alerta_arequipa.databinding.ActivityAlertaSonoraBinding
import com.google.android.material.snackbar.Snackbar

class AlertaSonoraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertaSonoraBinding
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private var sonidoActivo = true
    private var volumen = 80 // porcentaje 0..100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertaSonoraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarConfiguracion()     // carga volumen y sonidoActivo primero
        inicializarAudio()        // inicializa vibrator y toneGenerator con el volumen cargado
        configurarUI()
    }

    private fun inicializarAudio() {
        // Inicializar vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Crear ToneGenerator con el volumen actual
        crearToneGenerator()
    }

    // Crea (o recrea) el ToneGenerator usando el valor actual de 'volumen' (0..100)
    private fun crearToneGenerator() {
        try {
            toneGenerator?.release()
        } catch (t: Throwable) {
            // ignorar
        }
        // ToneGenerator segundo parámetro: volumen 0..100
        val vg = volumen.coerceIn(0, 100)
        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, vg)
    }

    private fun cargarConfiguracion() {
        val prefs = getSharedPreferences("AlertasPrefs", MODE_PRIVATE)
        volumen = prefs.getInt("volumen_alerta", 80)
        sonidoActivo = prefs.getBoolean("sonido_activo", true)

        binding.seekBarVolumen.progress = volumen
        binding.switchSonido.isChecked = sonidoActivo
    }

    private fun guardarConfiguracion() {
        val prefs = getSharedPreferences("AlertasPrefs", MODE_PRIVATE)
        prefs.edit().apply {
            putInt("volumen_alerta", volumen)
            putBoolean("sonido_activo", sonidoActivo)
            apply()
        }
    }

    private fun configurarUI() {
        binding.tvVolumen.text = "Volumen: $volumen%"

        binding.seekBarVolumen.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volumen = progress
                binding.tvVolumen.text = "Volumen: $volumen%"
                // Opcional: si quieres que el volumen se cambie en tiempo real mientras arrastras,
                // puedes llamar crearToneGenerator() aquí (pero se recreará muchas veces).
                // crearToneGenerator()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Al terminar de mover el SeekBar, guardamos y recreamos el ToneGenerator
                guardarConfiguracion()
                crearToneGenerator()
            }
        })

        binding.switchSonido.setOnCheckedChangeListener { _, isChecked ->
            sonidoActivo = isChecked
            guardarConfiguracion()

            if (isChecked) {
                binding.descripcionSonido.text =
                    "Recibirás alertas auditivas cuando se detecte un evento"
            } else {
                binding.descripcionSonido.text = "Solo recibirás alertas visuales"
            }
        }

        binding.btnProbarAlerta.setOnClickListener {
            reproducirAlerta()
        }

        binding.btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun reproducirAlerta() {
        try {
            if (sonidoActivo) {
                // Si toneGenerator no fuera válido por alguna razón, recreamos con valor actual
                if (toneGenerator == null) crearToneGenerator()
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 500, 200, 500),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 200, 500), -1)
            }

            Snackbar.make(
                binding.root,
                "Alerta de prueba reproducida",
                Snackbar.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Snackbar.make(
                binding.root,
                "Error al reproducir alerta: ${e.message}",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            toneGenerator?.release()
        } catch (t: Throwable) {
            // ignorar
        }
        toneGenerator = null
    }
}
