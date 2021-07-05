package org.dashj.platform.dapiclient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.dashj.platform.dapiclient.model.GrpcExceptionInfo
import org.junit.Test

class ErrorTest {

    @Test
    fun invalidContractTest() {
        val error = "Metadata(server=nginx/1.19.6,date=Thu, 21 Jan 2021 18:21:43 GMT,content-type=application/grpc,content-length=0,errors=[{\"name\":\"InvalidContractIdError\",\"contractId\":\"2DAncD4YTjfhSQZYrsQ659xbM7M5dNEkyfBEAg9SsS3W\"}])"
        val eo = GrpcExceptionInfo(error)
        assertTrue(eo.errors[0].containsKey("name"))
        assertEquals(eo.errors[0]["name"], "InvalidContractIdError")
    }

    @Test
    fun emptyErrorTest() {
        val error = "Status{code=NOT_FOUND, description=Identity not found, cause=null}: Metadata(server=nginx/1.19.6,date=Thu, 21 Jan 2021 18:21:21 GMT,content-type=application/grpc,content-length=0)"
        val eo = GrpcExceptionInfo(error)
        assertTrue(eo.errors.isEmpty())
    }

    @Test
    fun internalErrorTest() {
        val error = "Metadata(server=nginx/1.19.6,date=Thu, 21 Jan 2021 18:22:46 GMT,content-type=application/grpc,content-length=0,stack=\"Error: connect ECONNREFUSED 172.22.0.5:26657\n    at TCPConnectWrap.afterConnect [as oncomplete] (net.js:1144:16)\"";
        val eo = GrpcExceptionInfo(error)
        assertTrue(eo.errors.isEmpty())
    }

    @Test
    fun jsonSchemaValidationError() {
        val error = "Metadata(server=nginx/1.19.6,date=Sun, 10 Jan 2021 13:36:37 GMT,content-type=application/grpc,content-length=0,errors=[{\"name\":\"JsonSchemaValidationError\",\"keyword\":\"enum\",\"dataPath\":\".where[0][1]\",\"schemaPath\":\"#/properties/where/items/oneOf/0/items/1/enum\",\"params\":{\"allowedValues\":[\"<\",\"<=\",\"==\",\">\",\">=\"]}},{\"name\":\"JsonSchemaValidationError\",\"keyword\":\"enum\",\"dataPath\":\".where[0][0]\",\"schemaPath\":\"#/properties/where/items/oneOf/1/items/0/enum\",\"params\":{\"allowedValues\":[\"\$createdAt\",\"\$updatedAt\"]}},{\"name\":\"JsonSchemaValidationError\",\"keyword\":\"minItems\",\"dataPath\":\".where[0][2]\",\"schemaPath\":\"#/properties/where/items/oneOf/2/items/2/minItems\",\"params\":{\"limit\":1}},{\"name\":\"JsonSchemaValidationError\",\"keyword\":\"const\",\"dataPath\":\".where[0][1]\",\"schemaPath\":\"#/properties/where/items/oneOf/3/items/1/const\",\"params\":{\"allowedValue\":\"startsWith\"}},{\"name\":\"JsonSchemaValidationError\",\"keyword\":\"const\",\"dataPath\":\".where[0][1]\",\"schemaPath\":\"#/properties/where/items/oneOf/4/items/1/const\",\"params\":{\"allowedValue\":\"elementMatch\"}},{\"name\":\"JsonSchemaValidationError\",\"keyword\":\"const\",\"dataPath\":\".where[0][1]\",\"schemaPath\":\"#/properties/where/items/oneOf/5/items/1/const\",\"params\":{\"allowedValue\":\"length\"}},{\"name\":\"JsonSchemaValidationError\",\"keyword\":\"const\",\"dataPath\":\".where[0][1]\",\"schemaPath\":\"#/properties/where/items/oneOf/6/items/1/const\",\"params\":{\"allowedValue\":\"contains\"}},{\"name\":\"JsonSchemaValidationError\",\"keyword\":\"oneOf\",\"dataPath\":\".where[0]\",\"schemaPath\":\"#/properties/where/items/oneOf\",\"params\":{\"passingSchemas\":null}}])\n"
        val eo = GrpcExceptionInfo(error)
        assertTrue(eo.errors[0].containsKey("name"))
        assertEquals(eo.errors[0]["name"], "JsonSchemaValidationError")
    }
}