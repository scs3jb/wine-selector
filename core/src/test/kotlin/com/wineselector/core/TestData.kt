package com.wineselector.core

import com.wineselector.core.db.XWinesDatabase

object TestData {
    private fun stream(name: String) = TestData::class.java.classLoader!!.getResourceAsStream(name)!!

    val bundledDb: XWinesDatabase by lazy {
        XWinesDatabase().apply { loadFromStreams(stream("xwines.csv"), stream("xwines_ratings.csv")) }
    }

    val slimDb: XWinesDatabase by lazy {
        XWinesDatabase().apply { loadFromStreams(stream("xwines_slim_wines.csv"), stream("xwines_slim_ratings.csv")) }
    }

    fun menuText(name: String): String = stream("menus/$name.txt").bufferedReader().readText()
}
