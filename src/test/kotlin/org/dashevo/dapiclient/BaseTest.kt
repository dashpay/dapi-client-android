package org.dashevo.dapiclient

import java.io.File

open class BaseTest {

    fun getJson(fileName: String): String {
        return File(this.javaClass.getResource("/$fileName.json").path).readText()
    }

}