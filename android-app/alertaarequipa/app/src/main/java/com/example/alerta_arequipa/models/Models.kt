package com.example.alerta_arequipa.models

import com.google.gson.annotations.SerializedName

data class UbicacionPeligrosa(
    @SerializedName("id")
    val id: String,  // ⚠️ Cambiado de Int a String

    @SerializedName("latitud")
    val latitud: Double,

    @SerializedName("longitud")
    val longitud: Double,

    @SerializedName("tipo_crimen")
    val tipoCrimen: String,

    @SerializedName("descripcion")
    val descripcion: String? = null,

    @SerializedName("fecha")
    val fecha: String? = null,

    @SerializedName("nivel_peligro")
    val nivelPeligro: String = "MEDIO",

    @SerializedName("radio_metros")
    val radioMetros: Double = 500.0,

    // Campos adicionales de la BD
    val caseNumber: String? = null,
    val block: String? = null,
    val locationDescription: String? = null,
    val arrest: Boolean = false,
    val domestic: Boolean = false,
    val district: String? = null,
    val year: Int? = null
)

// Mantén las demás clases igual...
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("message")
    val message: String? = null
)

data class ConfiguracionAlerta(
    var volumen: Int = 80,
    var sonidoActivo: Boolean = true,
    var vibracionActiva: Boolean = true,
    var distanciaAlerta: Double = 500.0
)

data class Alerta(
    val ubicacion: UbicacionPeligrosa,
    val distancia: Double,
    val timestamp: Long = System.currentTimeMillis()
)