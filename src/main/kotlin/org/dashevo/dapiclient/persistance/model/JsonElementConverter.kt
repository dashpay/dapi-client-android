package org.dashevo.dapiclient.persistance.model

import com.google.gson.JsonElement
import com.google.gson.JsonParser

import io.objectbox.converter.PropertyConverter

internal class JsonElementConverter : PropertyConverter<JsonElement, String> {

    override fun convertToEntityProperty(databaseValue: String): JsonElement {
        val parser = JsonParser()
        return parser.parse(databaseValue)
    }

    override fun convertToDatabaseValue(entityProperty: JsonElement): String {
        return entityProperty.toString()
    }
}
