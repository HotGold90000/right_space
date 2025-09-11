//package src.main.kotlin

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

data class Entry(val id: Int, val message: String)

fun main() {
    val gson = Gson()
    val listType = object : TypeToken<MutableList<Entry>>() {}.type
    val file = File("data.json")

    val entries: MutableList<Entry> = if (file.exists()) {
        gson.fromJson(file.readText(), listType)
    } else {
        mutableListOf()
    }

    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
    val newEntry = Entry(entries.size + 1, "Hello at $timestamp")
    entries.add(newEntry)

    file.writeText(gson.toJson(entries))
}
