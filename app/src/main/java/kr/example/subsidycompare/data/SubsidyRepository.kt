package kr.example.subsidycompare.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kr.example.subsidycompare.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 지원금 데이터를 가져오는 곳.
 *
 * 1) 마지막으로 받아 둔 데이터(캐시)가 있으면 그것을 먼저 보여주고
 * 2) 없으면 앱에 내장된 샘플 데이터(assets/sample_subsidies.json)를 보여주며
 * 3) 백그라운드에서 DATA_URL 의 최신 데이터를 받아 캐시를 갱신합니다.
 */
class SubsidyRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile: File get() = File(context.filesDir, CACHE_NAME)

    /** DATA_URL 을 아직 설정하지 않았으면 네트워크를 쓰지 않고 샘플만 사용 */
    val isRemoteConfigured: Boolean
        get() = !BuildConfig.DATA_URL.contains("YOUR_GITHUB_ID")

    suspend fun loadLocal(): SubsidyData = withContext(Dispatchers.IO) {
        val cached = runCatching {
            if (isRemoteConfigured && cacheFile.exists()) parse(cacheFile.readText()) else null
        }.getOrNull()
        cached ?: parse(context.assets.open(SAMPLE_ASSET).bufferedReader().use { it.readText() })
    }

    /** 최신 데이터를 받아 캐시에 저장. 실패하면 Result.failure */
    suspend fun refresh(): Result<SubsidyData> = withContext(Dispatchers.IO) {
        if (!isRemoteConfigured) {
            return@withContext Result.failure(IllegalStateException("데이터 주소가 아직 설정되지 않았습니다"))
        }
        runCatching {
            val conn = (URL(BuildConfig.DATA_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                useCaches = false
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    error("서버 응답 오류 (${conn.responseCode})")
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val data = parse(body)
                require(data.devices.isNotEmpty() && data.plans.isNotEmpty()) { "데이터가 비어 있습니다" }
                cacheFile.writeText(body)
                data
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun parse(text: String): SubsidyData = json.decodeFromString(SubsidyData.serializer(), text)

    private companion object {
        const val CACHE_NAME = "subsidies_cache.json"
        const val SAMPLE_ASSET = "sample_subsidies.json"
    }
}
