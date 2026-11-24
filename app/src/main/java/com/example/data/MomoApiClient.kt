package com.example.data

import android.content.Context
import coil.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

private class InMemoryCookieJar : CookieJar {
    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val key = url.host
        val existing = store.getOrPut(key) { mutableListOf() }
        synchronized(existing) {
            cookies.forEach { incoming ->
                existing.removeAll { it.name == incoming.name && it.domain == incoming.domain && it.path == incoming.path }
                if (incoming.expiresAt > System.currentTimeMillis()) {
                    existing.add(incoming)
                }
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val matching = mutableListOf<Cookie>()
        store.values.forEach { list ->
            synchronized(list) {
                list.removeAll { it.expiresAt <= now }
                list.filterTo(matching) { it.matches(url) }
            }
        }
        return matching
    }

    fun clear() {
        store.clear()
    }
}

data class AuthResult(
    val user: MomoUser,
    val token: String
)

data class ModelsPayload(
    val models: List<MomoModel>,
    val defaultModelId: String?
)

class MomoApiClient(private val context: Context) {

    @Volatile
    private var authToken: String? = null

    private val cookieJar = InMemoryCookieJar()

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Accept", "application/json, text/event-stream")
        val token = authToken
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        if (original.body != null && original.header("Content-Type") == null) {
            builder.header("Content-Type", "application/json")
        }
        chain.proceed(builder.build())
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(authInterceptor)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val streamingClient: OkHttpClient = httpClient.newBuilder()
        .readTimeout(0, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .build()

    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context.applicationContext)
            .okHttpClient(httpClient)
            .crossfade(true)
            .build()
    }

    fun setToken(token: String?) {
        authToken = token?.takeIf { it.isNotBlank() }
    }

    fun clearSession() {
        authToken = null
        cookieJar.clear()
    }

    suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("email", email.trim())
            .put("password", password)
        val json = executeJson("POST", "/api/auth/login", payload)
        val token = json.optString("token", "")
        if (token.isNotBlank()) {
            authToken = token
        }
        val userObj = json.optJSONObject("user") ?: throw Exception(json.optString("error", "Gagal masuk"))
        AuthResult(user = parseUser(userObj), token = token)
    }

    suspend fun register(name: String, email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("name", name.trim())
            .put("email", email.trim())
            .put("password", password)
        val json = executeJson("POST", "/api/auth/register", payload)
        val token = json.optString("token", "")
        if (token.isNotBlank()) {
            authToken = token
        }
        val userObj = json.optJSONObject("user") ?: throw Exception(json.optString("error", "Gagal mendaftar"))
        AuthResult(user = parseUser(userObj), token = token)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            executeJson("POST", "/api/auth/logout", JSONObject())
        } catch (_: Exception) {
            // Clear local session regardless of server network errors
        } finally {
            clearSession()
        }
    }

    suspend fun getMe(): MomoUser? = withContext(Dispatchers.IO) {
        val json = executeJsonOrNull("GET", "/api/auth/me", null, allow401Null = true) ?: return@withContext null
        val userObj = json.optJSONObject("user") ?: return@withContext null
        parseUser(userObj)
    }

    suspend fun getBots(): List<MomoBot> = withContext(Dispatchers.IO) {
        val json = executeJson("GET", "/api/bots", null)
        val arr = json.optJSONArray("bots") ?: JSONArray()
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(parseBot(obj))
            }
        }
    }

    suspend fun createBot(name: String, look: String, job: String): MomoBot = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("name", name.trim())
            .put("look", look.trim())
            .put("job", job.trim())
        val json = executeJson("POST", "/api/bots", payload)
        val botObj = json.optJSONObject("bot") ?: json
        parseBot(botObj)
    }

    suspend fun updateBot(id: String, name: String, look: String, job: String): MomoBot = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("id", id)
            .put("name", name.trim())
            .put("look", look.trim())
            .put("job", job.trim())
        val json = executeJson("PATCH", "/api/bots", payload)
        val botObj = json.optJSONObject("bot") ?: json
        parseBot(botObj)
    }

    suspend fun deleteBot(id: String) = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(id, "UTF-8")
        executeJson("DELETE", "/api/bots?id=$encoded", null)
        Unit
    }

    suspend fun getModels(): ModelsPayload = withContext(Dispatchers.IO) {
        val json = executeJson("GET", "/api/models", null)
        val arr = json.optJSONArray("models") ?: JSONArray()
        val list = buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val ec = obj.optJSONObject("effortCosts")
                add(
                    MomoModel(
                        id = obj.optString("id"),
                        label = obj.optString("label").ifBlank { obj.optString("name", obj.optString("id")) },
                        description = obj.optString("description", ""),
                        brand = obj.optString("brand", ""),
                        kind = obj.optString("kind", "chat"),
                        supportsImageEdit = obj.optBoolean("supportsImageEdit", false),
                        creditCost = obj.optInt("creditCost", 1).coerceAtLeast(1),
                        effortCosts = EffortCosts(
                            low = ec?.optDouble("low", 1.0)?.takeIf { !it.isNaN() } ?: 1.0,
                            medium = ec?.optDouble("medium", 1.5)?.takeIf { !it.isNaN() } ?: 1.5,
                            high = ec?.optDouble("high", 2.5)?.takeIf { !it.isNaN() } ?: 2.5
                        )
                    )
                )
            }
        }
        ModelsPayload(
            models = list,
            defaultModelId = json.optString("defaultModelId").takeIf { it.isNotBlank() }
        )
    }

    suspend fun getGeneratedImages(): List<GeneratedImageItem> = withContext(Dispatchers.IO) {
        val json = executeJson("GET", "/api/generated-images", null)
        val arr = json.optJSONArray("images")
            ?: json.optJSONArray("items")
            ?: json.optJSONArray("data")
            ?: JSONArray()
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val rawUrl = obj.optString("url")
                    .ifBlank { obj.optString("imageUrl") }
                    .ifBlank { obj.optString("src") }
                if (rawUrl.isBlank()) continue
                add(
                    GeneratedImageItem(
                        id = obj.optString("id").ifBlank { "$i-${rawUrl.hashCode()}" },
                        url = QuotaFormatters.resolveMomoUrl(rawUrl),
                        prompt = obj.optString("prompt", obj.optString("caption", "")),
                        model = obj.optString("model", obj.optString("modelLabel", obj.optString("modelId", ""))),
                        size = obj.optString("size", obj.optString("imageSize", "1024x1024")),
                        createdAt = obj.optString("createdAt").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    suspend fun getPlans(currentPlanId: String = "", currentPlanName: String = ""): List<MomoPlan> = withContext(Dispatchers.IO) {
        val json = executeJson("GET", "/api/plans", null)
        val arr = json.optJSONArray("plans") ?: json.optJSONArray("data") ?: JSONArray()
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", obj.optString("slug", "plan_$i"))
                val name = obj.optString("name", obj.optString("label", id))
                val featuresArr = obj.optJSONArray("features")
                val cleanFeatures = buildList {
                    if (featuresArr != null) {
                        for (f in 0 until featuresArr.length()) {
                            val raw = featuresArr.optString(f, "")
                            if (raw.isNotBlank() && !QuotaFormatters.containsRawTokenNumber(raw)) {
                                add(raw)
                            }
                        }
                    }
                }
                val rawDesc = obj.optString("description", "")
                val cleanDesc = if (QuotaFormatters.containsRawTokenNumber(rawDesc)) {
                    QuotaFormatters.sanitizeNoRawTokens(rawDesc)
                } else {
                    rawDesc
                }
                val price = when {
                    obj.has("priceIdr") -> obj.optLong("priceIdr", 0L)
                    obj.has("price") -> obj.optLong("price", 0L)
                    obj.has("amount") -> obj.optLong("amount", 0L)
                    else -> 0L
                }
                val imageQuota = when {
                    obj.has("imageLimit") -> obj.optInt("imageLimit", 0)
                    obj.has("imageQuota") -> obj.optInt("imageQuota", 0)
                    obj.has("images") -> obj.optInt("images", 0)
                    else -> 0
                }
                val maxBots = when {
                    obj.has("maxBots") -> obj.optInt("maxBots", 1)
                    obj.has("botLimit") -> obj.optInt("botLimit", 1)
                    else -> 1
                }
                val isCurrent = obj.optBoolean("isCurrent", false) ||
                    (currentPlanId.isNotBlank() && id.equals(currentPlanId, ignoreCase = true)) ||
                    (currentPlanName.isNotBlank() && name.equals(currentPlanName, ignoreCase = true))

                add(
                    MomoPlan(
                        id = id,
                        name = name,
                        description = cleanDesc,
                        priceIdr = price,
                        billingCycle = obj.optString("billingCycle", obj.optString("interval", "bulan")),
                        imageQuota = imageQuota,
                        maxBots = maxBots,
                        badge = obj.optString("badge").takeIf { it.isNotBlank() },
                        features = cleanFeatures,
                        isCurrent = isCurrent
                    )
                )
            }
        }
    }

    suspend fun checkoutQris(planId: String, planName: String, fallbackAmount: Long): QrisCheckoutSession =
        withContext(Dispatchers.IO) {
            val payload = JSONObject().put("planId", planId)
            val json = executeJson("POST", "/api/billing/checkout", payload)
            val orderObj = json.optJSONObject("order") ?: json
            val orderId = orderObj.optString("orderId")
                .ifBlank { orderObj.optString("order_id") }
                .ifBlank { orderObj.optString("id") }
                .ifBlank { json.optString("orderId") }
            val amount = when {
                orderObj.has("amount") -> orderObj.optLong("amount", fallbackAmount)
                orderObj.has("total") -> orderObj.optLong("total", fallbackAmount)
                orderObj.has("grossAmount") -> orderObj.optLong("grossAmount", fallbackAmount)
                json.has("amount") -> json.optLong("amount", fallbackAmount)
                else -> fallbackAmount
            }
            val rawQrUrl = listOf(
                orderObj.optString("qrUrl"),
                orderObj.optString("qrImageUrl"),
                orderObj.optString("qrCodeUrl"),
                orderObj.optString("qrisUrl"),
                orderObj.optString("qrisImageUrl"),
                json.optString("qrUrl"),
                json.optString("qrImageUrl"),
                json.optString("qrCodeUrl")
            ).firstOrNull { it.isNotBlank() }

            val rawQrData = listOf(
                orderObj.optString("qrDataUrl"),
                orderObj.optString("qrisImage"),
                orderObj.optString("qrImage"),
                json.optString("qrDataUrl"),
                json.optString("qrImage")
            ).firstOrNull { it.isNotBlank() }

            val qrString = listOf(
                orderObj.optString("qrString"),
                orderObj.optString("qrisString"),
                orderObj.optString("qrisPayload"),
                json.optString("qrString")
            ).firstOrNull { it.isNotBlank() }

            QrisCheckoutSession(
                orderId = orderId,
                planId = planId,
                planName = planName,
                amountIdr = amount,
                qrImageUrl = rawQrUrl?.let { QuotaFormatters.resolveMomoUrl(it) },
                qrDataUrl = rawQrData,
                qrString = qrString,
                expiresAt = orderObj.optString("expiresAt", json.optString("expiresAt")).takeIf { it.isNotBlank() },
                status = orderObj.optString("status", json.optString("status", "pending")).ifBlank { "pending" }
            )
        }

    suspend fun getBillingStatus(orderId: String): String = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(orderId, "UTF-8")
        val json = executeJson("GET", "/api/billing/status?orderId=$encoded", null)
        val orderObj = json.optJSONObject("order")
        val status = json.optString("status")
            .ifBlank { orderObj?.optString("status").orEmpty() }
            .ifBlank { if (json.optBoolean("paid", false)) "paid" else "pending" }
        status.lowercase()
    }

    suspend fun changePlan(planId: String) = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("planId", planId)
        executeJson("POST", "/api/billing/change-plan", payload)
        Unit
    }

    suspend fun downloadImageBytes(url: String): Pair<ByteArray, String> = withContext(Dispatchers.IO) {
        val resolved = QuotaFormatters.resolveMomoUrl(url)
        val req = Request.Builder()
            .url(resolved)
            .get()
            .header("Accept", "image/*,*/*")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw Exception("Gagal mengunduh gambar (HTTP ${resp.code})")
            }
            val bytes = resp.body?.bytes() ?: throw Exception("Data gambar kosong")
            val mime = resp.header("Content-Type")?.substringBefore(";")?.trim()
                ?.takeIf { it.startsWith("image/") } ?: "image/png"
            bytes to mime
        }
    }

    fun streamBrain(
        messages: List<ChatMessage>,
        modelId: String,
        effort: ReasoningEffort,
        bot: MomoBot,
        needsTitle: Boolean,
        imageSize: ImageSizeOption,
        attachments: List<ChatAttachment>
    ): Flow<SseEvent> = callbackFlow {
        val messagesArr = JSONArray()
        messages.forEach { msg ->
            messagesArr.put(
                JSONObject()
                    .put("role", msg.role)
                    .put("text", msg.text)
            )
        }

        val attachmentsArr = JSONArray()
        attachments.forEach { att ->
            val obj = JSONObject()
                .put("name", att.name)
                .put("mimeType", att.mimeType)
                .put("type", when (att.kind) {
                    AttachmentKind.PHOTO -> "image"
                    AttachmentKind.NOTE -> "note"
                    AttachmentKind.FILE -> "file"
                })
            if (!att.dataUrl.isNullOrBlank()) {
                obj.put("dataUrl", att.dataUrl)
                obj.put("url", att.dataUrl)
            }
            if (!att.textContent.isNullOrBlank()) {
                obj.put("content", att.textContent)
                obj.put("text", att.textContent)
            }
            attachmentsArr.put(obj)
        }

        val pinnedBotObj = JSONObject()
            .put("id", bot.id)
            .put("name", bot.name)
            .put("job", bot.job)
            .put("look", bot.look)

        val bodyJson = JSONObject()
            .put("messages", messagesArr)
            .put("modelId", modelId)
            .put("reasoningEffort", effort.apiValue)
            .put("reasoningEnabled", !bot.isImageStudio)
            .put("activeLook", bot.look)
            .put("pinnedBot", pinnedBotObj)
            .put("workspaceView", if (bot.isImageStudio) "imagine" else "chat")
            .put("needsTitle", needsTitle)
            .put("imageSize", imageSize.apiValue)
            .put("attachments", attachmentsArr)

        val request = Request.Builder()
            .url("$MOMO_API_BASE/api/brain")
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .build()

        val call = streamingClient.newCall(request)
        val thread = Thread {
            try {
                call.execute().use { response ->
                    if (response.code == 401) {
                        close(UnauthorizedException("Sesi login Anda berakhir. Silakan masuk kembali."))
                        return@use
                    }
                    if (response.code == 429) {
                        val errBody = response.body?.string().orEmpty()
                        val errJson = runCatching { JSONObject(errBody) }.getOrNull()
                        val msg = errJson?.optString("error")
                            ?.ifBlank { errJson.optString("message") }
                            ?.ifBlank { "Kuota sementara tercapai. Tunggu reset atau tingkatkan paket." }
                            ?: "Kuota sementara tercapai. Tunggu reset atau tingkatkan paket."
                        val resetAt = errJson?.optString("resetAt")
                            ?.ifBlank { errJson.optString("chat5hResetAt") }
                            ?.ifBlank { errJson.optString("chatWeeklyResetAt") }
                            ?.takeIf { it.isNotBlank() }
                        val reason = errJson?.optString("chatBlockedReason")
                            ?.ifBlank { errJson.optString("reason") }
                            ?.takeIf { it.isNotBlank() }
                        close(RateLimitedException(QuotaFormatters.sanitizeNoRawTokens(msg), resetAt, reason))
                        return@use
                    }
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string().orEmpty()
                        val errJson = runCatching { JSONObject(errBody) }.getOrNull()
                        val msg = errJson?.optString("error")
                            ?.ifBlank { errJson.optString("message") }
                            ?.ifBlank { "Permintaan gagal (HTTP ${response.code})" }
                            ?: "Permintaan gagal (HTTP ${response.code})"
                        trySend(SseEvent.TurnError(QuotaFormatters.sanitizeNoRawTokens(msg)))
                        close()
                        return@use
                    }

                    val stream = response.body?.byteStream()
                    if (stream == null) {
                        trySend(SseEvent.TurnError("Respons stream kosong"))
                        close()
                        return@use
                    }

                    val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                    var currentEventName: String? = null
                    val dataBuilder = StringBuilder()

                    fun flushEvent() {
                        val rawData = dataBuilder.toString().trim()
                        dataBuilder.setLength(0)
                        val evtName = currentEventName
                        currentEventName = null
                        if (rawData.isEmpty() || rawData == "[DONE]") return
                        parseSsePayload(evtName, rawData)?.let { trySend(it) }
                    }

                    while (true) {
                        val line = reader.readLine() ?: break
                        when {
                            line.isEmpty() -> flushEvent()
                            line.startsWith("event:") -> currentEventName = line.removePrefix("event:").trim()
                            line.startsWith("data:") -> {
                                if (dataBuilder.isNotEmpty()) dataBuilder.append('\n')
                                dataBuilder.append(line.removePrefix("data:").trimStart())
                            }
                        }
                    }
                    flushEvent()
                    close()
                }
            } catch (e: Exception) {
                if (!call.isCanceled()) {
                    close(e)
                } else {
                    close()
                }
            }
        }
        thread.start()

        awaitClose {
            call.cancel()
        }
    }

    private fun parseSsePayload(eventName: String?, rawData: String): SseEvent? {
        val obj = runCatching { JSONObject(rawData) }.getOrNull() ?: return null
        val type = obj.optString("type")
            .ifBlank { obj.optString("event") }
            .ifBlank { eventName.orEmpty() }
            .lowercase()

        return when (type) {
            "turn_meta" -> SseEvent.TurnMeta(rawData)
            "activity_update", "activity" -> {
                val label = obj.optString("label")
                    .ifBlank { obj.optString("message") }
                    .ifBlank { obj.optString("text") }
                    .ifBlank { obj.optString("status") }
                if (label.isNotBlank()) SseEvent.ActivityUpdate(label) else null
            }
            "reasoning_delta", "thinking_delta" -> {
                val delta = extractDelta(obj)
                if (delta.isNotEmpty()) SseEvent.ReasoningDelta(delta) else null
            }
            "part_start" -> {
                val partObj = obj.optJSONObject("part")
                val partType = obj.optString("partType")
                    .ifBlank { partObj?.optString("type").orEmpty() }
                    .ifBlank { obj.optString("kind") }
                val imgUrl = extractImageUrl(obj) ?: partObj?.let { extractImageUrl(it) }
                SseEvent.PartStart(partType, imgUrl?.let { QuotaFormatters.resolveMomoUrl(it) })
            }
            "text_delta", "delta", "content_delta" -> {
                val delta = extractDelta(obj)
                if (delta.isNotEmpty()) SseEvent.TextDelta(delta) else null
            }
            "part_end" -> {
                val partObj = obj.optJSONObject("part")
                val partType = obj.optString("partType")
                    .ifBlank { partObj?.optString("type").orEmpty() }
                val imgUrl = extractImageUrl(obj) ?: partObj?.let { extractImageUrl(it) }
                SseEvent.PartEnd(partType, imgUrl?.let { QuotaFormatters.resolveMomoUrl(it) })
            }
            "image", "image_ready", "image_generated" -> {
                val imgUrl = extractImageUrl(obj)
                if (!imgUrl.isNullOrBlank()) {
                    SseEvent.PartEnd("image", QuotaFormatters.resolveMomoUrl(imgUrl))
                } else null
            }
            "session_title", "title" -> {
                val title = obj.optString("title").ifBlank { obj.optString("sessionTitle") }
                if (title.isNotBlank()) SseEvent.SessionTitle(title) else null
            }
            "turn_done", "done" -> SseEvent.TurnDone
            "turn_error", "error" -> {
                val msg = obj.optString("error")
                    .ifBlank { obj.optString("message") }
                    .ifBlank { "Terjadi kesalahan pada respons AI." }
                SseEvent.TurnError(QuotaFormatters.sanitizeNoRawTokens(msg), obj.optString("code").takeIf { it.isNotBlank() })
            }
            else -> {
                val fallbackDelta = extractDelta(obj)
                if (fallbackDelta.isNotEmpty()) SseEvent.TextDelta(fallbackDelta) else null
            }
        }
    }

    private fun extractDelta(obj: JSONObject): String {
        return when {
            obj.has("delta") && obj.opt("delta") is String -> obj.optString("delta")
            obj.has("text") && obj.opt("text") is String -> obj.optString("text")
            obj.has("content") && obj.opt("content") is String -> obj.optString("content")
            else -> ""
        }
    }

    private fun extractImageUrl(obj: JSONObject): String? {
        return listOf(
            obj.optString("imageUrl"),
            obj.optString("url"),
            obj.optString("src")
        ).firstOrNull { it.isNotBlank() }
    }

    private fun executeJson(
        method: String,
        path: String,
        body: JSONObject?
    ): JSONObject {
        return executeJsonOrNull(method, path, body, allow401Null = false) ?: JSONObject()
    }

    private fun executeJsonOrNull(
        method: String,
        path: String,
        body: JSONObject?,
        allow401Null: Boolean = false
    ): JSONObject? {
        val url = "$MOMO_API_BASE$path"
        val reqBuilder = Request.Builder().url(url)
        val reqBody = body?.toString()?.toRequestBody(JSON_MEDIA_TYPE)
        when (method.uppercase()) {
            "GET" -> reqBuilder.get()
            "POST" -> reqBuilder.post(reqBody ?: "{}".toRequestBody(JSON_MEDIA_TYPE))
            "PATCH" -> reqBuilder.patch(reqBody ?: "{}".toRequestBody(JSON_MEDIA_TYPE))
            "DELETE" -> if (reqBody != null) reqBuilder.delete(reqBody) else reqBuilder.delete()
        }

        httpClient.newCall(reqBuilder.build()).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (resp.code == 401) {
                if (allow401Null) return null
                throw UnauthorizedException("Sesi login Anda berakhir. Silakan masuk kembali.")
            }
            if (resp.code == 429) {
                val errJson = runCatching { JSONObject(raw) }.getOrNull()
                val msg = errJson?.optString("error")
                    ?.ifBlank { errJson.optString("message") }
                    ?.ifBlank { "Kuota sementara tercapai." }
                    ?: "Kuota sementara tercapai."
                throw RateLimitedException(
                    message = QuotaFormatters.sanitizeNoRawTokens(msg),
                    resetAt = errJson?.optString("resetAt")?.takeIf { it.isNotBlank() },
                    reason = errJson?.optString("chatBlockedReason")?.takeIf { it.isNotBlank() }
                )
            }
            val parsed = runCatching {
                if (raw.isBlank()) JSONObject() else JSONObject(raw)
            }.getOrElse { JSONObject().put("raw", raw) }

            if (!resp.isSuccessful) {
                val errMsg = parsed.optString("error")
                    .ifBlank { parsed.optString("message") }
                    .ifBlank { "Permintaan gagal (HTTP ${resp.code})" }
                throw Exception(QuotaFormatters.sanitizeNoRawTokens(errMsg))
            }
            return parsed
        }
    }

    private fun parseUser(obj: JSONObject): MomoUser {
        val quotaObj = obj.optJSONObject("quota")
        val quota = if (quotaObj != null) {
            MomoQuota(
                chat5hRemainingPct = quotaObj.optInt("chat5hRemainingPct", 100).coerceIn(0, 100),
                chat5hResetAt = quotaObj.optString("chat5hResetAt").takeIf { it.isNotBlank() && it != "null" },
                chatWeeklyRemainingPct = quotaObj.optInt("chatWeeklyRemainingPct", 100).coerceIn(0, 100),
                chatWeeklyResetAt = quotaObj.optString("chatWeeklyResetAt").takeIf { it.isNotBlank() && it != "null" },
                chatBlockedReason = quotaObj.optString("chatBlockedReason", "none").ifBlank { "none" },
                imageRemaining = quotaObj.optInt("imageRemaining", 0).coerceAtLeast(0),
                imageLimit = quotaObj.optInt("imageLimit", 0).coerceAtLeast(0),
                imageBonusQuota = quotaObj.optInt("imageBonusQuota", 0).coerceAtLeast(0),
                planName = quotaObj.optString("planName", "Gratis").ifBlank { "Gratis" },
                planId = quotaObj.optString("planId", ""),
                maxBots = quotaObj.optInt("maxBots", 1).coerceAtLeast(1),
                botCount = quotaObj.optInt("botCount", 0).coerceAtLeast(0)
            )
        } else {
            MomoQuota()
        }

        return MomoUser(
            id = obj.optString("id"),
            name = obj.optString("name", "Pengguna Momo"),
            email = obj.optString("email", ""),
            role = obj.optString("role", "user"),
            status = obj.optString("status", "active"),
            quota = quota
        )
    }

    private fun parseBot(obj: JSONObject): MomoBot {
        return MomoBot(
            id = obj.optString("id"),
            name = obj.optString("name", "Momo"),
            look = obj.optString("look", "rest"),
            job = obj.optString("job", ""),
            createdAt = obj.optString("createdAt").takeIf { it.isNotBlank() }
        )
    }
}
