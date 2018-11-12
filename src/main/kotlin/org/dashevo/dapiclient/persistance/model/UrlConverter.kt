package org.dashevo.dapiclient.persistance.model

import java.net.MalformedURLException
import java.net.URL

import io.objectbox.converter.PropertyConverter

internal class UrlConverter : PropertyConverter<URL, String> {

    override fun convertToEntityProperty(databaseValue: String): URL {
        try {
            return URL(databaseValue)
        } catch (e: MalformedURLException) {
            throw IllegalStateException()
        }

    }

    override fun convertToDatabaseValue(entityProperty: URL): String {
        return entityProperty.toString()
    }
}
