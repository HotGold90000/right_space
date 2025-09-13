package utils

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.lang.reflect.Type

/**
 * Json 工具类（基于 Gson）
 *
 * 功能：
 *  - toJson/fromJson（支持 reified 泛型 与 Type）
 *  - prettyPrint / minify
 *  - 校验 JSON 有效性
 *  - 对 JsonElement 的键进行深度排序
 *  - 合并两个 JsonObject（可控制覆盖规则）
 *  - 文件读写（支持原子写入 .tmp -> rename）
 *  - 从 assets 读取 JSON（Android）
 */
object JsonUtils {

    // 常用 Gson 实例（可复用）
    val GSON: Gson by lazy { Gson() }
    private val GSON_PRETTY: Gson by lazy { GsonBuilder().setPrettyPrinting().create() }

    // ---------------------------
    // 基本序列化 / 反序列化
    // ---------------------------

    /** 将对象转为 json 字符串，pretty = true 则格式化输出 */
    fun toJson(obj: Any, pretty: Boolean = false): String =
        if (pretty) GSON_PRETTY.toJson(obj) else GSON.toJson(obj)

    /**
     * 反序列化到具体类型（支持泛型）
     * 使用示例：
     *  val type = object : TypeToken<Map<String, List<MyItem>>>(){}.type
     *  val map: Map<String, List<MyItem>> = JsonUtils.fromJson(jsonString, type)
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> fromJson(json: String, type: Type): T = GSON.fromJson(json, type) as T

    /** reified 便捷版本，适合非复杂泛型（或用 TypeToken） */
    inline fun <reified T> fromJson(json: String): T =
        GSON.fromJson(json, object : TypeToken<T>() {}.type)

    // ---------------------------
    // 验证 / 格式化 / 压缩
    // ---------------------------

    /** 判断字符串是否为合法 JSON */
    fun isValidJson(json: String): Boolean =
        try {
            JsonParser.parseString(json)
            true
        } catch (e: Exception) {
            false
        }

    /** 将任意合法 JSON 字符串格式化为可读的 pretty JSON（抛出异常给调用方如果不是 JSON） */
    fun prettyPrint(json: String): String {
        val element = JsonParser.parseString(json)
        return GSON_PRETTY.toJson(element)
    }

    /** 将任意合法 JSON 字符串压缩成单行 JSON（minify） */
    fun minify(json: String): String {
        val element = JsonParser.parseString(json)
        return GSON.toJson(element)
    }

    // ---------------------------
    // 键排序（深度排序 JsonObject 的 key）
    // ---------------------------

    /** 深度排序 JsonElement（返回一个新的 JsonElement，JsonObject 的键按字典序排序） */
    private fun sortJsonElementDeep(element: JsonElement): JsonElement {
        return when {
            element.isJsonObject -> {
                val obj = element.asJsonObject
                val entries = obj.entrySet().sortedBy { it.key } // 字典序
                val newObj = JsonObject()
                for (entry in entries) {
                    newObj.add(entry.key, sortJsonElementDeep(entry.value))
                }
                newObj
            }
            element.isJsonArray -> {
                val arr = element.asJsonArray
                val newArr = JsonArray()
                arr.forEach { newArr.add(sortJsonElementDeep(it)) }
                newArr
            }
            else -> element.deepCopy()
        }
    }

    /**
     * 对字符串形式的 JSON 进行键排序（深度），返回字符串。
     * pretty = true 将返回格式化后的字符串，否则返回 minified（紧凑）形式。
     */
    fun sortJsonKeys(json: String, pretty: Boolean = false): String {
        val element = JsonParser.parseString(json)
        val sorted = sortJsonElementDeep(element)
        return if (pretty) GSON_PRETTY.toJson(sorted) else GSON.toJson(sorted)
    }

    // ---------------------------
    // 合并 JsonObject
    // ---------------------------

    /** 递归合并两个 JsonObject（返回新的 JsonObject） */
    private fun mergeJsonObjects(base: JsonObject, override: JsonObject, overwrite: Boolean): JsonObject {
        val result = JsonObject()
        // 先拷贝 base
        for ((k, v) in base.entrySet()) {
            result.add(k, v.deepCopy())
        }
        // 遍历 override
        for ((k, v) in override.entrySet()) {
            if (!result.has(k)) {
                result.add(k, v.deepCopy())
            } else {
                val baseVal = result.get(k)
                when {
                    baseVal.isJsonObject && v.isJsonObject -> {
                        result.add(k, mergeJsonObjects(baseVal.asJsonObject, v.asJsonObject, overwrite))
                    }
                    baseVal.isJsonArray && v.isJsonArray -> {
                        // 数组合并：简单地 append override 的元素到 base 后面
                        val newArr = JsonArray()
                        baseVal.asJsonArray.forEach { newArr.add(it.deepCopy()) }
                        v.asJsonArray.forEach { newArr.add(it.deepCopy()) }
                        result.add(k, newArr)
                    }
                    else -> {
                        if (overwrite) {
                            result.add(k, v.deepCopy())
                        }
                        // overwrite == false 时保留 base 的值（忽略 override）
                    }
                }
            }
        }
        return result
    }

    /**
     * 合并两个 json 对象字符串（都必须是 object）。返回 pretty 格式字符串（可根据需要再 minify）。
     * @param baseJson base json 字符串
     * @param overrideJson 需要合并进 base 的 json 字符串
     * @param overwrite true 表示 override 的标量会覆盖 base 中的相同 key；false 则保留 base 的值
     */
    fun mergeJson(baseJson: String, overrideJson: String, overwrite: Boolean = true, pretty: Boolean = true): String {
        val baseEl = JsonParser.parseString(baseJson)
        val overrideEl = JsonParser.parseString(overrideJson)

        if (!baseEl.isJsonObject || !overrideEl.isJsonObject) {
            throw IllegalArgumentException("Both baseJson and overrideJson must be JSON objects")
        }

        val merged = mergeJsonObjects(baseEl.asJsonObject, overrideEl.asJsonObject, overwrite)
        return if (pretty) GSON_PRETTY.toJson(merged) else GSON.toJson(merged)
    }

    // ---------------------------
    // 文件相关：读写（支持原子写入）
    // ---------------------------

    /** 读取文件内容（UTF-8），异常会向上抛出 */
    fun readFile(file: File): String = file.readText(Charsets.UTF_8)

    /** 读取文件，失败返回 null */
    fun readFileOrNull(file: File): String? = try {
        readFile(file)
    } catch (e: Exception) {
        null
    }

    /** 从 assets 读取文本（Android 场景） */
//    @Throws(IOException::class)
//    fun readAsset(context: Context, assetPath: String): String =
//        context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }

    /**
     * 将对象写入文件（UTF-8），默认 pretty=false。可选：
     *  - pretty：格式化输出（更易读）
     *  - sortKeys：输出前对 JSON 的键进行深度排序（便于查看）
     *  - atomic：启用原子写入（先写 tmp 文件，再重命名），减少写入中途被截断的风险
     *
     * 注意：大量数据请在后台线程执行。
     */
    @Throws(IOException::class)
    fun writeJsonToFile(
        file: File,
        obj: Any,
        pretty: Boolean = false,
        sortKeys: Boolean = false,
        atomic: Boolean = true
    ) {
        val rawJson = toJson(obj, pretty)
        val outJson = if (sortKeys) sortJsonKeys(rawJson, pretty) else rawJson

        // 确保父目录存在
        file.parentFile?.let { if (!it.exists()) it.mkdirs() }

        if (atomic) {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(outJson, Charsets.UTF_8)
            // 尝试重命名：如果失败则覆盖原文件
            if (!tmp.renameTo(file)) {
                // 退到更强硬的方式：删除目标，重试
                if (file.exists() && !file.delete()) {
                    // 仍然不能删除的话，抛出异常
                    throw IOException("Failed to delete existing file: ${file.absolutePath}")
                }
                if (!tmp.renameTo(file)) {
                    throw IOException("Failed to move tmp file to target: ${file.absolutePath}")
                }
            }
        } else {
            file.writeText(outJson, Charsets.UTF_8)
        }
    }

    // ---------------------------
    // File 扩展便捷方法（可直接在项目内使用）
    // ---------------------------

    fun FileWriteJson(file: File, obj: Any, pretty: Boolean = false, sortKeys: Boolean = false) {
        writeJsonToFile(file, obj, pretty, sortKeys, atomic = true)
    }

    // ---------------------------
    // 其他便捷方法/示例
    // ---------------------------

    /** 返回一个按字典序排序并格式化的 JSON（方便在日志/控制台查看） */
    fun tidyJsonForDisplay(obj: Any): String {
        val raw = toJson(obj, pretty = false)
        return sortJsonKeys(raw, pretty = true)
    }
}
