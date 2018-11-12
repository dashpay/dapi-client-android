package org.dashevo.dapiclient.persistance

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.objectbox.BoxStore
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import org.dashevo.dapiclient.persistance.model.CacheEntry
import org.dashevo.dapiclient.persistance.model.CacheEntry_
import java.net.URL


class PersistenceInterceptor : Interceptor {

    lateinit var boxStore: BoxStore

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (::boxStore.isInitialized) {
            val responseBody = response.body()!!
            val bodyString = responseBody.string()

            val url = request.url().url()
            val parser = JsonParser()
            val bodyJson = parser.parse(bodyString)
            persist(url, bodyJson)

            return response.newBuilder()
                    .body(ResponseBody.create(responseBody.contentType(), bodyString))
                    .build()
        } else {
            return response
        }
    }

    private fun persist(url: URL, newData: JsonElement) {
        val box = boxStore.boxFor(CacheEntry::class.java)
        val existingCacheEntry = box.query().equal(CacheEntry_.uri, url.toString()).build().find()
        val cacheEntry = when (existingCacheEntry.size) {
            0 -> CacheEntry(0, url, newData)
            1 -> existingCacheEntry[0].apply {
                data = newData
            }
            else -> throw IllegalStateException()
        }
        box.put(cacheEntry)
    }

}