package nhom3.chatbln.android

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.gson.GsonBuilder
import nhom3.chatbln.android.data.local.SessionPreferences
import nhom3.chatbln.android.data.remote.AuthInterceptor
import nhom3.chatbln.android.data.remote.ChatBlnApiService
import nhom3.chatbln.android.data.remote.DynamicBaseUrlInterceptor
import nhom3.chatbln.android.data.remote.StompRealtimeClient
import nhom3.chatbln.android.data.repository.ChatRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    val gson = GsonBuilder().setLenient().create()

    val sessionPreferences = SessionPreferences(appContext)
    val dynamicBaseUrlInterceptor = DynamicBaseUrlInterceptor(sessionPreferences)
    val authInterceptor = AuthInterceptor(sessionPreferences)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(dynamicBaseUrlInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    val apiService: ChatBlnApiService = Retrofit.Builder()
        .baseUrl(SessionPreferences.RETROFIT_PLACEHOLDER_API_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ChatBlnApiService::class.java)

    val stompRealtimeClient = StompRealtimeClient(
        okHttpClient = okHttpClient,
        sessionPreferences = sessionPreferences,
        gson = gson
    )

    val chatRepository = ChatRepository(
        context = appContext,
        apiService = apiService,
        sessionPreferences = sessionPreferences,
        gson = gson
    )
}

class ChatBlnApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(container.okHttpClient)
            .crossfade(true)
            .build()
    }
}
