import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) {
    val gson = Gson()
    val gsonPretty: Gson = GsonBuilder().setPrettyPrinting().create()

    // 当前时间
    val now = LocalDateTime.now()
    val yearMonth = now.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val day = now.format(DateTimeFormatter.ofPattern("dd"))
    val time = now.format(DateTimeFormatter.ofPattern("HH:mm"))

    // 从环境变量中读取写入内容（来自 GitHub Actions payload）
    val content = System.getenv("CONTENT") ?: ""

    // 月份文件路径
    val file = File("dataSource/$yearMonth.json")

    // 如果文件不存在，初始化一个空的 { "01":[], "02":[], ... }
    if (!file.exists()) {
        val emptyMonth = (1..31).associate { String.format("%02d", it) to mutableListOf<Map<String, String>>() }
        file.parentFile.mkdirs()
        file.writeText(gsonPretty.toJson(emptyMonth)) // 初始化时也用格式化输出
    }

    // 读取 JSON
    val type = object : TypeToken<MutableMap<String, MutableList<Map<String, String>>>>() {}.type
    val monthData: MutableMap<String, MutableList<Map<String, String>>> =
        gson.fromJson(file.readText(), type)

    // 获取当天列表
    val todayList = monthData.getOrPut(day) { mutableListOf() }

    // 添加数据
    todayList.add(mapOf(time to content))

    // 写回 JSON 文件（格式化）
    file.writeText(gsonPretty.toJson(monthData))

    println("✅ 已写入: $yearMonth-$day $time $content")
}



//import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
//import java.io.File
//import java.time.LocalDateTime
//import java.time.format.DateTimeFormatter
//
//fun main(args: Array<String>) {
//    val gson = Gson()
//
//    // 当前时间
//    val now = LocalDateTime.now()
//    val yearMonth = now.format(DateTimeFormatter.ofPattern("yyyy-MM"))
//    val day = now.format(DateTimeFormatter.ofPattern("dd"))
//    val time = now.format(DateTimeFormatter.ofPattern("HH:mm"))
//
//    // 从环境变量中读取写入内容（来自 GitHub Actions payload）
//    val content = System.getenv("CONTENT") ?: ""
//
//    // 月份文件路径
//    val file = File("dataSource/$yearMonth.json")
//
//    // 如果文件不存在，初始化一个空的 { "01":[], "02":[], ... }
//    if (!file.exists()) {
//        val emptyMonth = (1..31).associate { String.format("%02d", it) to mutableListOf<Map<String, String>>() }
//        file.parentFile.mkdirs()
//        file.writeText(gson.toJson(emptyMonth))
//    }
//
//    // 读取 JSON
//    val type = object : TypeToken<MutableMap<String, MutableList<Map<String, String>>>>() {}.type
//    val monthData: MutableMap<String, MutableList<Map<String, String>>> =
//        gson.fromJson(file.readText(), type)
//
//    // 获取当天列表
//    val todayList = monthData.getOrPut(day) { mutableListOf() }
//
//    // 添加数据
//    todayList.add(mapOf(time to content))
//
//    // 写回 JSON 文件
//    file.writeText(gson.toJson(monthData))
//
//    println("✅ 已写入: $yearMonth-$day $time $content")
//}