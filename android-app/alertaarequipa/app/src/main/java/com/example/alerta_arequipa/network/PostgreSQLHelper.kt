package com.example.alerta_arequipa.network

import android.util.Log
import com.example.alerta_arequipa.models.UbicacionPeligrosa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class PostgreSQLHelper {

    companion object {
        private const val HOST = "crimengo-db.postgres.database.azure.com"
        private const val DATABASE = "postgres"
        private const val USER = "chic1"
        private const val PASSWORD = "Atlantis31"
        private const val PORT = "5432"

        private const val CONNECTION_URL =
            "jdbc:postgresql://$HOST:$PORT/$DATABASE?sslmode=require"
    }

    private fun getConnection(): Connection? {
        return try {
            Class.forName("org.postgresql.Driver")

            val props = Properties()
            props.setProperty("user", USER)
            props.setProperty("password", PASSWORD)
            props.setProperty("ssl", "true")
            props.setProperty("sslmode", "require")

            DriverManager.getConnection(CONNECTION_URL, props)
        } catch (e: Exception) {
            Log.e("PostgreSQL", "Error conectando: ${e.message}", e)
            null
        }
    }

    private fun calcularNivelPeligro(primaryType: String, arrest: Boolean, domestic: Boolean): String {
        return when {
            primaryType.contains("HOMICIDE", ignoreCase = true) -> "ALTO"
            primaryType.contains("ROBBERY", ignoreCase = true) -> "ALTO"
            primaryType.contains("ASSAULT", ignoreCase = true) -> "ALTO"
            primaryType.contains("BATTERY", ignoreCase = true) -> "ALTO"
            primaryType.contains("KIDNAPPING", ignoreCase = true) -> "ALTO"
            primaryType.contains("SEXUAL", ignoreCase = true) -> "ALTO"
            primaryType.contains("BURGLARY", ignoreCase = true) -> "MEDIO"
            primaryType.contains("THEFT", ignoreCase = true) -> "MEDIO"
            primaryType.contains("CRIMINAL DAMAGE", ignoreCase = true) -> "BAJO"
            domestic -> "MEDIO"
            arrest -> "BAJO"
            else -> "MEDIO"
        }
    }

    suspend fun obtenerUbicacionesPeligrosas(limite: Int = 1000): List<UbicacionPeligrosa> = withContext(Dispatchers.IO) {
        val ubicaciones = mutableListOf<UbicacionPeligrosa>()
        var connection: Connection? = null

        try {
            connection = getConnection()
            val statement = connection?.createStatement()

            // Obtiene los crímenes más recientes con ubicación válida
            val query = """
                SELECT id, case_number, date, block, primary_type, description, 
                       location_description, arrest, domestic, district, year,
                       latitude, longitude
                FROM crimes
                WHERE latitude IS NOT NULL 
                  AND longitude IS NOT NULL
                  AND latitude BETWEEN -90 AND 90
                  AND longitude BETWEEN -180 AND 180
                ORDER BY date DESC
                LIMIT $limite
            """.trimIndent()

            val resultSet: ResultSet? = statement?.executeQuery(query)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            while (resultSet?.next() == true) {
                val latitude = resultSet.getDouble("latitude")
                val longitude = resultSet.getDouble("longitude")
                val primaryType = resultSet.getString("primary_type") ?: "UNKNOWN"
                val arrest = resultSet.getBoolean("arrest")
                val domestic = resultSet.getBoolean("domestic")

                // Formatea la fecha
                val timestamp = resultSet.getTimestamp("date")
                val fechaFormateada = if (timestamp != null) {
                    dateFormat.format(Date(timestamp.time))
                } else {
                    null
                }

                val ubicacion = UbicacionPeligrosa(
                    id = resultSet.getString("id") ?: UUID.randomUUID().toString(),
                    latitud = latitude,
                    longitud = longitude,
                    tipoCrimen = primaryType,
                    descripcion = resultSet.getString("description"),
                    fecha = fechaFormateada,
                    nivelPeligro = calcularNivelPeligro(primaryType, arrest, domestic),
                    radioMetros = when (calcularNivelPeligro(primaryType, arrest, domestic)) {
                        "ALTO" -> 800.0
                        "MEDIO" -> 500.0
                        else -> 300.0
                    },
                    caseNumber = resultSet.getString("case_number"),
                    block = resultSet.getString("block"),
                    locationDescription = resultSet.getString("location_description"),
                    arrest = arrest,
                    domestic = domestic,
                    district = resultSet.getString("district"),
                    year = resultSet.getInt("year")
                )
                ubicaciones.add(ubicacion)
            }

            resultSet?.close()
            statement?.close()

            Log.d("PostgreSQL", "✅ Crímenes obtenidos: ${ubicaciones.size}")
        } catch (e: Exception) {
            Log.e("PostgreSQL", "❌ Error obteniendo crímenes: ${e.message}", e)
        } finally {
            try {
                connection?.close()
            } catch (e: Exception) {
                Log.e("PostgreSQL", "Error cerrando conexión: ${e.message}")
            }
        }

        return@withContext ubicaciones
    }

    suspend fun obtenerUbicacionesCercanas(
        lat: Double,
        lng: Double,
        radioKm: Double = 5.0,
        limite: Int = 100
    ): List<UbicacionPeligrosa> = withContext(Dispatchers.IO) {
        val ubicaciones = mutableListOf<UbicacionPeligrosa>()
        var connection: Connection? = null

        try {
            connection = getConnection()

            // Fórmula de Haversine para calcular distancia
            val query = """
                SELECT id, case_number, date, block, primary_type, description, 
                       location_description, arrest, domestic, district, year,
                       latitude, longitude,
                    (6371 * acos(
                        cos(radians(?)) * cos(radians(latitude)) * 
                        cos(radians(longitude) - radians(?)) + 
                        sin(radians(?)) * sin(radians(latitude))
                    )) AS distancia
                FROM crimes
                WHERE latitude IS NOT NULL 
                  AND longitude IS NOT NULL
                  AND latitude BETWEEN -90 AND 90
                  AND longitude BETWEEN -180 AND 180
                  AND (6371 * acos(
                        cos(radians(?)) * cos(radians(latitude)) * 
                        cos(radians(longitude) - radians(?)) + 
                        sin(radians(?)) * sin(radians(latitude))
                    )) < ?
                ORDER BY distancia
                LIMIT $limite
            """.trimIndent()

            val preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setDouble(1, lat)
            preparedStatement?.setDouble(2, lng)
            preparedStatement?.setDouble(3, lat)
            preparedStatement?.setDouble(4, lat)
            preparedStatement?.setDouble(5, lng)
            preparedStatement?.setDouble(6, lat)
            preparedStatement?.setDouble(7, radioKm)

            val resultSet = preparedStatement?.executeQuery()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            while (resultSet?.next() == true) {
                val primaryType = resultSet.getString("primary_type") ?: "UNKNOWN"
                val arrest = resultSet.getBoolean("arrest")
                val domestic = resultSet.getBoolean("domestic")

                val timestamp = resultSet.getTimestamp("date")
                val fechaFormateada = if (timestamp != null) {
                    dateFormat.format(Date(timestamp.time))
                } else {
                    null
                }

                val ubicacion = UbicacionPeligrosa(
                    id = resultSet.getString("id") ?: UUID.randomUUID().toString(),
                    latitud = resultSet.getDouble("latitude"),
                    longitud = resultSet.getDouble("longitude"),
                    tipoCrimen = primaryType,
                    descripcion = resultSet.getString("description"),
                    fecha = fechaFormateada,
                    nivelPeligro = calcularNivelPeligro(primaryType, arrest, domestic),
                    radioMetros = when (calcularNivelPeligro(primaryType, arrest, domestic)) {
                        "ALTO" -> 800.0
                        "MEDIO" -> 500.0
                        else -> 300.0
                    },
                    caseNumber = resultSet.getString("case_number"),
                    block = resultSet.getString("block"),
                    locationDescription = resultSet.getString("location_description"),
                    arrest = arrest,
                    domestic = domestic,
                    district = resultSet.getString("district"),
                    year = resultSet.getInt("year")
                )
                ubicaciones.add(ubicacion)
            }

            resultSet?.close()
            preparedStatement?.close()

            Log.d("PostgreSQL", "✅ Crímenes cercanos: ${ubicaciones.size}")
        } catch (e: Exception) {
            Log.e("PostgreSQL", "❌ Error obteniendo crímenes cercanos: ${e.message}", e)
        } finally {
            try {
                connection?.close()
            } catch (e: Exception) {
                Log.e("PostgreSQL", "Error cerrando conexión: ${e.message}")
            }
        }

        return@withContext ubicaciones
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        return@withContext try {
            connection = getConnection()
            val statement = connection?.createStatement()
            val result = statement?.executeQuery("SELECT COUNT(*) as total FROM crimes")
            result?.next()
            val count = result?.getInt("total") ?: 0
            result?.close()
            statement?.close()

            Log.d("PostgreSQL", "✅ Conexión exitosa. Total de crímenes: $count")
            true
        } catch (e: Exception) {
            Log.e("PostgreSQL", "❌ Test de conexión falló: ${e.message}", e)
            false
        } finally {
            try {
                connection?.close()
            } catch (e: Exception) {
                Log.e("PostgreSQL", "Error cerrando conexión: ${e.message}")
            }
        }
    }
}