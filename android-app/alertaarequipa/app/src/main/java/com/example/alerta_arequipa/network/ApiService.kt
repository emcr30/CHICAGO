package com.example.alerta_arequipa.network

import com.example.alerta_arequipa.models.ApiResponse
import com.example.alerta_arequipa.models.UbicacionPeligrosa
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {

    @GET("api/ubicaciones")
    suspend fun obtenerUbicacionesPeligrosas(): Response<ApiResponse<List<UbicacionPeligrosa>>>

    @GET("api/ubicaciones/cercanas")
    suspend fun obtenerUbicacionesCercanas(
        @Query("lat") latitud: Double,
        @Query("lng") longitud: Double,
        @Query("radio") radioMetros: Double = 5000.0
    ): Response<ApiResponse<List<UbicacionPeligrosa>>>

    @GET("api/ubicaciones/{id}")
    suspend fun obtenerUbicacion(
        @Path("id") id: Int
    ): Response<ApiResponse<UbicacionPeligrosa>>

    companion object {
        private const val BASE_URL = "https://crimengo2025.azurewebsites.net/"

        fun create(): ApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}

class UbicacionesRepository(private val apiService: ApiService? = null) {

    private val dbHelper = PostgreSQLHelper()

    // Metodo principal: intenta primero con PostgreSQL, si falla usa la API
    suspend fun obtenerTodasLasUbicaciones(): Result<List<UbicacionPeligrosa>> {
        return try {
            // Intenta primero con PostgreSQL (más confiable)
            val ubicaciones = dbHelper.obtenerUbicacionesPeligrosas()

            if (ubicaciones.isNotEmpty()) {
                Result.success(ubicaciones)
            } else {
                // Si no hay datos en PostgreSQL, intenta con la API
                obtenerDesdeLaAPI()
            }
        } catch (e: Exception) {
            // Si falla PostgreSQL, intenta con la API
            obtenerDesdeLaAPI()
        }
    }

    private suspend fun obtenerDesdeLaAPI(): Result<List<UbicacionPeligrosa>> {
        return try {
            apiService?.let {
                val response = it.obtenerUbicacionesPeligrosas()
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(response.body()?.data ?: emptyList())
                } else {
                    Result.failure(Exception("Error al obtener ubicaciones de la API"))
                }
            } ?: Result.failure(Exception("API no disponible"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerUbicacionesCercanas(
        lat: Double,
        lng: Double,
        radio: Double
    ): Result<List<UbicacionPeligrosa>> {
        return try {
            val ubicaciones = dbHelper.obtenerUbicacionesCercanas(lat, lng, radio)

            if (ubicaciones.isNotEmpty()) {
                Result.success(ubicaciones)
            } else {
                obtenerCercanasDesdeAPI(lat, lng, radio)
            }
        } catch (e: Exception) {
            obtenerCercanasDesdeAPI(lat, lng, radio)
        }
    }

    private suspend fun obtenerCercanasDesdeAPI(
        lat: Double,
        lng: Double,
        radio: Double
    ): Result<List<UbicacionPeligrosa>> {
        return try {
            apiService?.let {
                val response = it.obtenerUbicacionesCercanas(lat, lng, radio)
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(response.body()?.data ?: emptyList())
                } else {
                    Result.failure(Exception("Error al obtener ubicaciones cercanas de la API"))
                }
            } ?: Result.failure(Exception("API no disponible"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}