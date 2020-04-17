package ch.jschneider.playstoreapidemo

import com.dragons.aurora.playstoreapiv2.AuthException
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI
import com.dragons.aurora.playstoreapiv2.GooglePlayException
import com.dragons.aurora.playstoreapiv2.HttpClientAdapter
import org.omg.CORBA.portable.UnknownException
import java.awt.PageAttributes.MediaType
import javax.xml.ws.Response


class OkHttpClientAdapter(context: Context?) : HttpClientAdapter() {
    private val client: OkHttpClient

    @Throws(IOException::class)
    override fun get(
        url: String,
        params: Map<String, String>,
        headers: Map<String, String>
    ): ByteArray {
        val requestBuilder: Request.Builder = Builder()
            .url(buildUrl(url, params))
            .get()
        return request(requestBuilder, headers)
    }

    @Throws(IOException::class)
    override fun getEx(
        url: String,
        params: Map<String, List<String>>,
        headers: Map<String, String>
    ): ByteArray {
        return request(Builder().url(buildUrlEx(url, params)).get(), headers)
    }

    @Throws(IOException::class)
    override fun postWithoutBody(
        url: String,
        urlParams: Map<String, String>,
        headers: Map<String, String>
    ): ByteArray {
        return post(buildUrl(url, urlParams), HashMap(), headers)
    }

    @Throws(IOException::class)
    override fun post(
        url: String,
        params: Map<String, String>,
        headers: MutableMap<String, String>
    ): ByteArray {
        headers["Content-Type"] = "application/x-www-form-urlencoded; charset=UTF-8"
        val bodyBuilder: FormBody.Builder = Builder()
        if (null != params && !params.isEmpty()) {
            for (name in params.keys) {
                bodyBuilder.add(name, params[name])
            }
        }
        val requestBuilder: Request.Builder = Builder()
            .url(url)
            .post(bodyBuilder.build())
        return post(url, requestBuilder, headers)
    }

    @Throws(IOException::class)
    override fun post(
        url: String,
        body: ByteArray,
        headers: MutableMap<String, String>
    ): ByteArray {
        if (!headers.containsKey("Content-Type")) {
            headers["Content-Type"] = "application/x-protobuf"
        }
        val requestBuilder: Request.Builder = Builder()
            .url(url)
            .post(RequestBody.create(MediaType.parse("application/x-protobuf"), body))
        return post(url, requestBuilder, headers)
    }

    @Throws(IOException::class)
    private override fun post(
        url: String,
        requestBuilder: Request.Builder,
        headers: Map<String, String>
    ): ByteArray {
        requestBuilder.url(url)
        return request(requestBuilder, headers)
    }

    @Throws(IOException::class)
    private fun request(
        requestBuilder: Request.Builder,
        headers: Map<String, String>
    ): ByteArray {
        val request: Request = requestBuilder
            .headers(Headers.of(headers))
            .build()
        val response: Response = client.newCall(request).execute()
        val code: Int = response.code()
        val content: ByteArray = response.body().bytes()
        if (code == 401 || code == 403) {
            val authException = AuthException("Auth error", code)
            val authResponse =
                GooglePlayAPI.parseResponse(String(content))
            if (authResponse.containsKey("Error") && authResponse["Error"] == "NeedsBrowser") {
                authException.twoFactorUrl = authResponse["Url"]
            }
            throw authException
        } else if (code == 404) {
            val authResponse =
                GooglePlayAPI.parseResponse(String(content))
            if (authResponse.containsKey("Error") && authResponse["Error"] == "UNKNOWN_ERR") {
                throw UnknownException("Unknown error occurred", code)
            } else throw AppNotFoundException("App not found", code)
        } else if (code == 429) {
            throw TooManyRequestsException(
                "Rate-limiting enabled, you are making too many requests",
                code
            )
        } else if (code >= 500) {
            throw GooglePlayException("Server error", code)
        } else if (code >= 400) {
            throw MalformedRequestException("Malformed Request", code)
        }
        return content
    }

    override fun buildUrl(
        url: String,
        params: Map<String, String>
    ): String {
        val urlBuilder: HttpUrl.Builder = HttpUrl.parse(url).newBuilder()
        if (null != params && !params.isEmpty()) {
            for (name in params.keys) {
                urlBuilder.addQueryParameter(name, params[name])
            }
        }
        return urlBuilder.build().toString()
    }

    override fun buildUrlEx(
        url: String,
        params: Map<String, List<String>>
    ): String {
        val urlBuilder: HttpUrl.Builder = HttpUrl.parse(url).newBuilder()
        if (null != params && !params.isEmpty()) {
            for (name in params.keys) {
                for (value in params[name]!!) {
                    urlBuilder.addQueryParameter(name, value)
                }
            }
        }
        return urlBuilder.build().toString()
    }

    init {
        val builder: OkHttpClient.Builder = Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .cookieJar(object : CookieJar() {
                private val cookieStore: HashMap<HttpUrl, List<Cookie>> =
                    HashMap()

                fun saveFromResponse(
                    url: HttpUrl,
                    cookies: List<Cookie>
                ) {
                    cookieStore[url] = cookies
                }

                fun loadForRequest(url: HttpUrl): List<Cookie> {
                    val cookies: List<Cookie>? = cookieStore[url]
                    return cookies ?: ArrayList()
                }
            })
        //if (Util.isNetworkProxyEnabled(context)) builder.proxy(Util.getNetworkProxy(context));
        client = builder.build()
    }
}

