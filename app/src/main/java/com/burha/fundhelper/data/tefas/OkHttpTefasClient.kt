package com.burha.fundhelper.data.tefas

import com.burha.fundhelper.domain.FundSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpTefasClient @Inject constructor(
    private val http: OkHttpClient,
) : TefasClient {

    override suspend fun fetchYatCatalog(): List<FundSnapshot> {
        val body = """
            {"dil":"TR","fonTipi":"YAT","kurucuKodu":null,"sfonTurKod":null,"fonTurAciklama":null,
             "islem":1,"fonTurKod":null,"fonGrubu":null,"donemGetiri1a":"1","donemGetiri3a":"1",
             "donemGetiri6a":"1","donemGetiri1y":"1","donemGetiriyb":"1","donemGetiri3y":"1",
             "donemGetiri5y":"1","basTarih":null,"bitTarih":null,"calismaTipi":2,"getiriOrani":"1"}
        """.trimIndent()
        return TefasJsonMapper.parseCatalog(post(TefasEndpoints.CATALOG, body))
    }

    override suspend fun fetchLatestYatPrices(): List<FundSnapshot> {
        val end = LocalDate.now()
        val start = end.minusDays(7)
        val fmt = DateTimeFormatter.BASIC_ISO_DATE
        val body = """
            {"fonTipi":"YAT","fonKodu":null,"aramaMetni":null,"fonTurKod":null,"fonGrubu":null,
             "sfonTurKod":null,"fonTurAciklama":null,"kurucuKod":null,
             "basTarih":"${start.format(fmt)}","bitTarih":"${end.format(fmt)}",
             "basSira":1,"bitSira":100000,"dil":"TR","sFonTurKod":"","fonKod":"","fonGrup":"","fonUnvanTip":""}
        """.trimIndent()
        return TefasJsonMapper.parseLatestPrices(post(TefasEndpoints.PRICES, body))
    }

    private suspend fun post(url: String, jsonBody: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", TefasEndpoints.USER_AGENT)
            .header("Accept", "application/json, text/plain, */*")
            .header("Origin", TefasEndpoints.ORIGIN)
            .header("Referer", TefasEndpoints.REFERER)
            .post(jsonBody.toRequestBody(JSON))
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw TefasFetchException("HTTP ${response.code}")
            }
            body
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}