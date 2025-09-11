#!/usr/bin/env kotlin

@file:DependsOn("com.google.code.gson:gson:2.10.1")

import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Entry(val id: Int, val message: String)

fun main() {
    val file = File("data.json")
    val gson = Gson()
    val listType = object : TypeToken<MutableList<Entry>>() {}.type

    // 读取已有 JSON
    val entries: MutableList<Entry> = if (file.exists() && file.readText().isNotBlank()) {
        gson.fromJson(file.readText(), listType)
    } else {
        mutableListOf()
    }

    // 生成新的条目
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
    val newEntry = Entry(entries.size + 1, "Hello at $timestamp")

    entries.add(newEntry)

    // 写回 JSON
    file.writeText(gson.toJson(entries))

    println("✅ Updated data.json with: $newEntry")
}

main()
