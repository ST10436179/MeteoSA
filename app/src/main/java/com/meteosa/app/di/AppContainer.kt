package com.meteosa.app.di

import android.content.Context
import com.meteosa.app.BuildConfig
import com.meteosa.app.data.local.SessionManager
import com.meteosa.app.data.local.ThemePreferences
import com.meteosa.app.data.remote.AuthInterceptor
import com.meteosa.app.data.remote.BackendApi
import com.meteosa.app.data.remote.WeatherApi
import com.meteosa.app.data.repository.AuthRepository
import com.meteosa.app.data.repository.ReportsRepository
import com.meteosa.app.data.repository.WeatherRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Hand-rolled dependency container (no Hilt/Dagger). We skip an annotation-processor based DI
 * framework on purpose: it would add a kapt/ksp step to every build, which is exactly the kind
 * of extra build-time cost we want to avoid on a lower-spec dev machine building straight to a
 * physical phone. Everything below is a cheap lazy singleton instead.
 */
class AppContainer(context: Context) {

    val sessionManager = SessionManager(context.applicationContext)
    val themePreferences = ThemePreferences(context.applicationContext)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val backendHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sessionManager))
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val weatherHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val backendApi: BackendApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_BASE_URL)
        .client(backendHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BackendApi::class.java)

    private val weatherApi: WeatherApi = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .client(weatherHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApi::class.java)

    val authRepository = AuthRepository(backendApi, sessionManager)
    val reportsRepository = ReportsRepository(backendApi, sessionManager)
    val weatherRepository = WeatherRepository(weatherApi)
}
