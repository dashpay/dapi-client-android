package org.dashevo.dapiclient.persistance.model

import com.google.gson.JsonElement
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Unique
import java.net.URL

@Entity
internal class CacheEntry(
        @field:Id var id: Long?,
        @field:Convert(converter = UrlConverter::class, dbType = String::class) @field:Unique var uri: URL,
        @field:Convert(converter = JsonElementConverter::class, dbType = String::class) var data: JsonElement
)
