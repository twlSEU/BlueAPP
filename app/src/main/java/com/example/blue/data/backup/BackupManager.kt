package com.example.blue.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.blue.core.database.AppDatabase
import com.example.blue.core.util.AmountUtils
import com.example.blue.data.local.DiaryImageStorage
import com.example.blue.data.local.TimeImageStorage
import com.example.blue.data.local.entity.AccountCategoryEntity
import com.example.blue.data.local.entity.AccountEntryEntity
import com.example.blue.data.local.entity.DiaryEntity
import com.example.blue.data.local.entity.DiaryImageEntity
import com.example.blue.data.local.entity.DiaryMoodEntity
import com.example.blue.data.local.entity.DiaryMoodIds
import com.example.blue.data.local.entity.SleepRecordEntity
import com.example.blue.data.local.entity.SleepSource
import com.example.blue.data.local.entity.TimeEventEntity
import com.example.blue.data.local.entity.TimeEventType
import com.example.blue.data.local.entity.TimeProfileEntity
import com.example.blue.model.AccountType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupInfo(
    val exportedAt: Long,
    val diaryCount: Int,
    val accountCount: Int,
    val imageCount: Int,
    val sleepCount: Int = 0,
    val timeEventCount: Int = 0,
)

enum class RestoreMode { MERGE, REPLACE }

class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val imageStorage: DiaryImageStorage,
    private val timeImageStorage: TimeImageStorage,
) {
    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val diaryDao = database.diaryDao()
        val accountDao = database.accountDao()
        val sleepRecordDao = database.sleepRecordDao()
        val diaries = diaryDao.getAllDiaries()
        val availableImages = diaryDao.getAllImages().filter { image ->
            imageStorage.fileFor(image.localPath).isFile
        }
        val moods = diaryDao.getAllMoods()
        val accounts = accountDao.getAllEntries()
        val categories = accountDao.getAllCategories()
        val sleeps = sleepRecordDao.getAllRecords()
        val timeDao = database.timeDao()
        val timeProfile = timeDao.getProfile()
        val timeEvents = timeDao.getAllEvents()
        val availableTimeImages = timeEvents.mapNotNull(TimeEventEntity::imagePath).distinct().mapNotNull { path ->
            timeImageStorage.fileFor(path).takeIf(File::isFile)
        }
        val manifest = JSONObject().apply {
            put("formatVersion", FORMAT_VERSION)
            put(
                "appVersion",
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown",
            )
            put("exportedAt", System.currentTimeMillis())
            put("diaryCount", diaries.size)
            put("accountCount", accounts.size)
            put("sleepCount", sleeps.size)
            put("timeEventCount", timeEvents.size)
            put("imageCount", availableImages.size + availableTimeImages.size)
        }
        context.contentResolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                writeText(zip, "manifest.json", manifest.toString())
                writeText(zip, "diaries.json", diariesJson(diaries, availableImages, moods).toString())
                writeText(zip, "accounts.json", JSONArray().also { result ->
                    accounts.forEach { result.put(accountJson(it)) }
                }.toString())
                writeText(zip, "categories.json", JSONArray().also { result ->
                    categories.forEach { result.put(categoryJson(it)) }
                }.toString())
                writeText(zip, "sleeps.json", JSONArray().also { result ->
                    sleeps.forEach { result.put(sleepJson(it)) }
                }.toString())
                writeText(
                    zip,
                    "time.json",
                    timeJson(profile = timeProfile, events = timeEvents, availableImages = availableTimeImages).toString(),
                )
                writeText(zip, "accounts.csv", accountsCsv(accounts, categories.associateBy { it.id }))
                availableImages.forEach { image ->
                    val file = imageStorage.fileFor(image.localPath)
                    require(file.length() <= MAX_IMAGE_BYTES) { "图片文件过大，无法导出：${file.name}" }
                    zip.putNextEntry(ZipEntry("images/${file.name}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                availableTimeImages.forEach { file ->
                    require(file.length() <= MAX_IMAGE_BYTES) { "图片文件过大，无法导出：${file.name}" }
                    zip.putNextEntry(ZipEntry("time_images/${file.name}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: error("无法创建备份文件")
    }

    suspend fun inspect(uri: Uri): BackupInfo = withContext(Dispatchers.IO) {
        val file = copyToTemp(uri)
        try {
            ZipFile(file).use { zip -> parseManifest(readText(zip, "manifest.json")).info }
        } finally {
            file.delete()
        }
    }

    suspend fun restore(uri: Uri, mode: RestoreMode) = withContext(Dispatchers.IO) {
        val backup = copyToTemp(uri)
        val staging = File(context.cacheDir, "restore_${UUID.randomUUID()}").also(File::mkdirs)
        val diaryStaging = File(staging, "diaries").also(File::mkdirs)
        val timeStaging = File(staging, "time").also(File::mkdirs)
        val imageDirectory = imageStorage.fileFor(DiaryImageStorage.DIRECTORY_NAME)
        val timeImageDirectory = timeImageStorage.fileFor(TimeImageStorage.DIRECTORY_NAME)
        val filesPresentBeforeRestore = imageDirectory.listFiles().orEmpty().toSet()
        val timeFilesPresentBeforeRestore = timeImageDirectory.listFiles().orEmpty().toSet()
        val installedFiles = mutableListOf<File>()
        var databaseCommitted = false
        try {
            ZipFile(backup).use { zip ->
                val manifest = parseManifest(readText(zip, "manifest.json"))
                val categories = parseCategories(JSONArray(readText(zip, "categories.json")))
                val accounts = parseAccounts(JSONArray(readText(zip, "accounts.json")))
                val diaries = parseDiaries(JSONArray(readText(zip, "diaries.json")), zip, diaryStaging)
                val sleeps = if (manifest.formatVersion >= SLEEP_BACKUP_VERSION) {
                    parseSleeps(JSONArray(readText(zip, "sleeps.json")))
                } else {
                    emptyList()
                }
                val timeData = if (manifest.formatVersion >= TIME_BACKUP_VERSION) {
                    parseTimeData(JSONObject(readText(zip, "time.json")), zip, timeStaging)
                } else {
                    TimeBackupData(profile = null, events = emptyList())
                }

                diaryStaging.listFiles().orEmpty().forEach { source ->
                    val destination = imageStorage.fileFor(
                        "${DiaryImageStorage.DIRECTORY_NAME}/${source.name}",
                    )
                    destination.parentFile?.mkdirs()
                    source.copyTo(destination, overwrite = false)
                    installedFiles += destination
                }
                timeStaging.listFiles().orEmpty().forEach { source ->
                    val destination = timeImageStorage.fileFor(
                        "${TimeImageStorage.DIRECTORY_NAME}/${source.name}",
                    )
                    destination.parentFile?.mkdirs()
                    source.copyTo(destination, overwrite = false)
                    installedFiles += destination
                }

                val obsoleteMergedImagePaths = mutableSetOf<String>()
                val obsoleteMergedTimeImagePaths = mutableSetOf<String>()
                database.withTransaction {
                    val accountDao = database.accountDao()
                    val diaryDao = database.diaryDao()
                    val sleepRecordDao = database.sleepRecordDao()
                    val timeDao = database.timeDao()
                    if (mode == RestoreMode.REPLACE) {
                        accountDao.clearAllEntries()
                        accountDao.clearAllCategories()
                        diaryDao.clearAllDiaries()
                        sleepRecordDao.clearAllRecords()
                        timeDao.clearEvents()
                        timeDao.clearProfile()
                    }
                    categories.forEach { accountDao.upsertCategory(it) }
                    accounts.forEach { accountDao.upsertEntry(it) }
                    diaries.forEach { item ->
                        if (mode == RestoreMode.MERGE) {
                            obsoleteMergedImagePaths += diaryDao.getImages(item.diary.id)
                                .map(DiaryImageEntity::localPath)
                        }
                        diaryDao.upsertDiary(item.diary)
                        diaryDao.deleteImages(item.diary.id)
                        if (item.images.isNotEmpty()) diaryDao.upsertImages(item.images)
                        diaryDao.deleteMoods(item.diary.id)
                        if (item.moods.isNotEmpty()) diaryDao.upsertMoods(item.moods)
                    }
                    if (mode == RestoreMode.REPLACE) {
                        sleeps.forEach { sleepRecordDao.upsertRecord(it) }
                    } else {
                        sleeps.forEach { imported ->
                            val existingOnDate = sleepRecordDao.getRecord(imported.recordDate)
                            when {
                                existingOnDate == null -> {
                                    val idCollision = sleepRecordDao.getRecordById(imported.id)
                                    sleepRecordDao.upsertRecord(
                                        if (idCollision == null) imported else imported.copy(id = UUID.randomUUID().toString()),
                                    )
                                }
                                imported.updatedAt >= existingOnDate.updatedAt -> {
                                    sleepRecordDao.upsertRecord(
                                        imported.copy(
                                            id = existingOnDate.id,
                                            createdAt = existingOnDate.createdAt,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    timeData.profile?.let { profile -> timeDao.upsertProfile(profile) }
                    timeData.events.forEach { imported ->
                        val existing = timeDao.getEvent(imported.id)
                        when {
                            existing == null -> timeDao.upsertEvent(imported)
                            mode == RestoreMode.REPLACE || imported.updatedAt >= existing.updatedAt -> {
                                existing.imagePath?.let(obsoleteMergedTimeImagePaths::add)
                                timeDao.upsertEvent(imported.copy(createdAt = existing.createdAt))
                            }
                        }
                    }
                }
                databaseCommitted = true

                if (mode == RestoreMode.REPLACE) {
                    filesPresentBeforeRestore.forEach { oldFile -> runCatching { oldFile.delete() } }
                    timeFilesPresentBeforeRestore.forEach { oldFile -> runCatching { oldFile.delete() } }
                } else {
                    val importedPaths = diaries.flatMap { item -> item.images.map(DiaryImageEntity::localPath) }.toSet()
                    (obsoleteMergedImagePaths - importedPaths).forEach { path ->
                        runCatching { imageStorage.fileFor(path).delete() }
                    }
                    val referencedTimePaths = database.timeDao().getAllEvents()
                        .mapNotNull(TimeEventEntity::imagePath)
                        .toSet()
                    val importedTimePaths = timeData.events.mapNotNull(TimeEventEntity::imagePath).toSet()
                    (obsoleteMergedTimeImagePaths + importedTimePaths - referencedTimePaths).forEach { path ->
                        runCatching { timeImageStorage.fileFor(path).delete() }
                    }
                }
            }
        } finally {
            if (!databaseCommitted) {
                installedFiles.forEach { file -> runCatching { file.delete() } }
            }
            backup.delete()
            staging.deleteRecursively()
        }
    }

    private fun diariesJson(
        diaries: List<DiaryEntity>,
        images: List<DiaryImageEntity>,
        moods: List<DiaryMoodEntity>,
    ): JSONArray {
        val byDiary = images.groupBy { it.diaryId }
        val moodsByDiary = moods.groupBy { it.diaryId }
        return JSONArray().also { result ->
            diaries.forEach { diary ->
                result.put(JSONObject().apply {
                    put("id", diary.id)
                    put("date", diary.diaryDate.toString())
                    put("time", diary.diaryTime.toString())
                    put("content", diary.content)
                    put("mood", diary.mood ?: JSONObject.NULL)
                    put("moods", JSONArray().also { moodArray ->
                        moodsByDiary[diary.id]
                            .orEmpty()
                            .map(DiaryMoodEntity::mood)
                            .filter(DiaryMoodIds::isValid)
                            .distinct()
                            .sorted()
                            .forEach(moodArray::put)
                    })
                    put("createdAt", diary.createdAt)
                    put("updatedAt", diary.updatedAt)
                    put("images", JSONArray().also { imageArray ->
                        byDiary[diary.id].orEmpty().forEach { image ->
                            imageArray.put(JSONObject().apply {
                                put("id", image.id)
                                put("path", "images/${File(image.localPath).name}")
                                put("sortOrder", image.sortOrder)
                                put("createdAt", image.createdAt)
                            })
                        }
                    })
                })
            }
        }
    }

    private fun parseDiaries(
        array: JSONArray,
        zip: ZipFile,
        staging: File,
    ): List<ImportedDiary> {
        val result = mutableListOf<ImportedDiary>()
        for (index in 0 until array.length()) {
            val json = array.getJSONObject(index)
            val diaryId = json.getString("id")
            val legacyMood = json.optInt("mood", 0).takeIf { it in LEGACY_MOOD_RANGE }
            val moodArray = json.optJSONArray("moods")
            val importedMoodValues = if (moodArray == null) {
                listOfNotNull(legacyMood)
            } else {
                buildList {
                    for (moodIndex in 0 until moodArray.length()) {
                        moodArray.optInt(moodIndex, 0)
                            .takeIf(DiaryMoodIds::isValid)
                            ?.let(::add)
                    }
                }.distinct()
            }
            val importedImages = mutableListOf<DiaryImageEntity>()
            val imageArray = json.optJSONArray("images") ?: JSONArray()
            for (imageIndex in 0 until imageArray.length()) {
                val image = imageArray.getJSONObject(imageIndex)
                val backupPath = image.getString("path")
                val sourceName = File(backupPath).name
                require(backupPath == "images/$sourceName" && sourceName.isNotBlank()) {
                    "备份包含不安全的图片路径"
                }
                val entry = zip.getEntry(backupPath) ?: continue
                require(!entry.isDirectory) { "图片条目无效" }
                require(entry.size < 0L || entry.size <= MAX_IMAGE_BYTES) { "图片文件过大" }
                val fileName = "${UUID.randomUUID()}_$sourceName"
                zip.getInputStream(entry).use { input ->
                    File(staging, fileName).outputStream().use { output ->
                        copyWithLimit(input, output, MAX_IMAGE_BYTES)
                    }
                }
                importedImages += DiaryImageEntity(
                    id = image.optString("id", UUID.randomUUID().toString()),
                    diaryId = diaryId,
                    localPath = "${DiaryImageStorage.DIRECTORY_NAME}/$fileName",
                    sortOrder = image.optInt("sortOrder", imageIndex),
                    createdAt = image.optLong("createdAt", System.currentTimeMillis()),
                )
            }
            result += ImportedDiary(
                diary = DiaryEntity(
                    id = diaryId,
                    diaryDate = LocalDate.parse(json.getString("date")),
                    diaryTime = LocalTime.parse(json.getString("time")),
                    content = json.optString("content"),
                    createdAt = json.optLong("createdAt"),
                    updatedAt = json.optLong("updatedAt"),
                    mood = importedMoodValues.minOrNull(),
                ),
                images = importedImages,
                moods = importedMoodValues.map { mood ->
                    DiaryMoodEntity(diaryId = diaryId, mood = mood)
                },
            )
        }
        return result
    }

    private fun parseCategories(array: JSONArray): List<AccountCategoryEntity> =
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            AccountCategoryEntity(
                id = item.getString("id"),
                name = item.getString("name"),
                type = AccountType.valueOf(item.getString("type")),
                isDefault = item.optBoolean("isDefault"),
                isActive = item.optBoolean("isActive", true),
                createdAt = item.optLong("createdAt"),
                updatedAt = item.optLong("updatedAt"),
            )
        }

    private fun parseAccounts(array: JSONArray): List<AccountEntryEntity> =
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            AccountEntryEntity(
                id = item.getString("id"),
                entryDate = LocalDate.parse(item.getString("date")),
                entryTime = LocalTime.parse(item.getString("time")),
                type = AccountType.valueOf(item.getString("type")),
                amountInCents = item.getLong("amountInCents"),
                name = item.getString("name"),
                categoryId = item.getString("categoryId"),
                note = item.optString("note").ifBlank { null },
                createdAt = item.optLong("createdAt"),
                updatedAt = item.optLong("updatedAt"),
            )
        }

    private fun parseSleeps(array: JSONArray): List<SleepRecordEntity> {
        val records = List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val sleepDateTime = LocalDateTime.parse(item.getString("sleepDateTime"))
            val wakeDateTime = item.optString("wakeDateTime")
                .takeIf { it.isNotBlank() && it != "null" }
                ?.let(LocalDateTime::parse)
            require(wakeDateTime == null || wakeDateTime > sleepDateTime) {
                "睡眠记录的起床时间无效"
            }
            SleepRecordEntity(
                id = item.getString("id"),
                recordDate = LocalDate.parse(item.getString("recordDate")),
                sleepDateTime = sleepDateTime,
                wakeDateTime = wakeDateTime,
                source = SleepSource.valueOf(item.getString("source")),
                isEstimated = item.optBoolean("isEstimated"),
                note = item.optString("note").ifBlank { null },
                createdAt = item.optLong("createdAt"),
                updatedAt = item.optLong("updatedAt"),
            )
        }
        require(records.map(SleepRecordEntity::recordDate).distinct().size == records.size) {
            "备份包含重复日期的睡眠记录"
        }
        require(records.map(SleepRecordEntity::id).distinct().size == records.size) {
            "备份包含重复的睡眠记录 ID"
        }
        return records
    }

    private fun timeJson(
        profile: TimeProfileEntity?,
        events: List<TimeEventEntity>,
        availableImages: List<File>,
    ): JSONObject {
        val availableNames = availableImages.map(File::getName).toSet()
        return JSONObject().apply {
            put("age", profile?.age ?: JSONObject.NULL)
            put("birthday", profile?.birthday?.toString() ?: JSONObject.NULL)
            put(
                "events",
                JSONArray().also { result ->
                    events.forEach { event ->
                        val imageName = event.imagePath?.let(::File)?.name?.takeIf(availableNames::contains)
                        result.put(
                            JSONObject().apply {
                                put("id", event.id)
                                put("title", event.title)
                                put("date", event.eventDate.toString())
                                put("type", event.type.name)
                                put("imagePath", imageName?.let { "time_images/$it" } ?: JSONObject.NULL)
                                put("createdAt", event.createdAt)
                                put("updatedAt", event.updatedAt)
                            },
                        )
                    }
                },
            )
        }
    }

    private fun parseTimeData(
        json: JSONObject,
        zip: ZipFile,
        staging: File,
    ): TimeBackupData {
        val age = if (json.isNull("age")) null else json.getInt("age").also {
            require(it in 0..80) { "备份中的年龄无效" }
        }
        val birthday = json.optString("birthday")
            .takeIf { it.isNotBlank() && it != "null" }
            ?.let(LocalDate::parse)
            ?.also { require(!it.isAfter(LocalDate.now())) { "备份中的生日无效" } }
        val profile = if (age != null || birthday != null) {
            TimeProfileEntity(age = age ?: 0, birthday = birthday)
        } else {
            null
        }
        val array = json.optJSONArray("events") ?: JSONArray()
        val events = List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val imagePath = item.optString("imagePath")
                .takeIf { it.isNotBlank() && it != "null" }
                ?.let { backupPath ->
                    val sourceName = File(backupPath).name
                    require(backupPath == "time_images/$sourceName" && sourceName.isNotBlank()) {
                        "备份包含不安全的时光图片路径"
                    }
                    val entry = zip.getEntry(backupPath) ?: return@let null
                    require(!entry.isDirectory) { "时光图片条目无效" }
                    require(entry.size < 0L || entry.size <= MAX_IMAGE_BYTES) { "时光图片文件过大" }
                    val fileName = "${UUID.randomUUID()}_$sourceName"
                    zip.getInputStream(entry).use { input ->
                        File(staging, fileName).outputStream().use { output ->
                            copyWithLimit(input, output, MAX_IMAGE_BYTES)
                        }
                    }
                    "${TimeImageStorage.DIRECTORY_NAME}/$fileName"
                }
            TimeEventEntity(
                id = item.getString("id"),
                title = item.getString("title").trim().also { require(it.isNotBlank()) { "时光事件名称不能为空" } },
                eventDate = LocalDate.parse(item.getString("date")),
                type = TimeEventType.valueOf(item.getString("type")),
                imagePath = imagePath,
                createdAt = item.optLong("createdAt"),
                updatedAt = item.optLong("updatedAt"),
            )
        }
        require(events.map(TimeEventEntity::id).distinct().size == events.size) {
            "备份包含重复的时光事件 ID"
        }
        return TimeBackupData(profile = profile, events = events)
    }

    private fun parseManifest(text: String): ParsedManifest {
        val json = JSONObject(text)
        val formatVersion = json.getInt("formatVersion")
        require(formatVersion in MIN_SUPPORTED_FORMAT_VERSION..FORMAT_VERSION) { "不支持的备份版本" }
        return ParsedManifest(
            formatVersion = formatVersion,
            info = BackupInfo(
                exportedAt = json.getLong("exportedAt"),
                diaryCount = json.optInt("diaryCount"),
                accountCount = json.optInt("accountCount"),
                sleepCount = json.optInt("sleepCount"),
                timeEventCount = json.optInt("timeEventCount"),
                imageCount = json.optInt("imageCount"),
            ),
        )
    }

    private fun copyToTemp(uri: Uri): File {
        val target = File(context.cacheDir, "backup_${UUID.randomUUID()}.zip")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    copyWithLimit(input, output, MAX_BACKUP_BYTES)
                }
            } ?: error("无法读取备份文件")
            return target
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    private fun readText(zip: ZipFile, path: String): String {
        val entry = zip.getEntry(path) ?: error("备份缺少 $path")
        require(!entry.isDirectory) { "备份条目无效：$path" }
        require(entry.size < 0L || entry.size <= MAX_JSON_BYTES) { "备份数据过大" }
        val output = ByteArrayOutputStream()
        zip.getInputStream(entry).use { input -> copyWithLimit(input, output, MAX_JSON_BYTES) }
        return output.toString(Charsets.UTF_8.name())
    }

    private fun writeText(zip: ZipOutputStream, path: String, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        require(bytes.size.toLong() <= MAX_JSON_BYTES) { "导出数据过大：$path" }
        zip.putNextEntry(ZipEntry(path))
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun accountJson(item: AccountEntryEntity) = JSONObject().apply {
        put("id", item.id)
        put("date", item.entryDate.toString())
        put("time", item.entryTime.toString())
        put("type", item.type.name)
        put("amountInCents", item.amountInCents)
        put("name", item.name)
        put("categoryId", item.categoryId)
        put("note", item.note)
        put("createdAt", item.createdAt)
        put("updatedAt", item.updatedAt)
    }

    private fun categoryJson(item: AccountCategoryEntity) = JSONObject().apply {
        put("id", item.id)
        put("name", item.name)
        put("type", item.type.name)
        put("isDefault", item.isDefault)
        put("isActive", item.isActive)
        put("createdAt", item.createdAt)
        put("updatedAt", item.updatedAt)
    }

    private fun sleepJson(item: SleepRecordEntity) = JSONObject().apply {
        put("id", item.id)
        put("recordDate", item.recordDate.toString())
        put("sleepDateTime", item.sleepDateTime.toString())
        put("wakeDateTime", item.wakeDateTime?.toString() ?: JSONObject.NULL)
        put("source", item.source.name)
        put("isEstimated", item.isEstimated)
        put("note", item.note)
        put("createdAt", item.createdAt)
        put("updatedAt", item.updatedAt)
    }

    private fun accountsCsv(
        accounts: List<AccountEntryEntity>,
        categories: Map<String, AccountCategoryEntity>,
    ) = buildString {
        append("日期,时间,类型,金额,名称,分类,备注\n")
        accounts.forEach { item ->
            append(
                listOf(
                    item.entryDate,
                    item.entryTime,
                    if (item.type == AccountType.INCOME) "收入" else "支出",
                    AmountUtils.formatCents(item.amountInCents),
                    item.name,
                    categories[item.categoryId]?.name.orEmpty(),
                    item.note.orEmpty(),
                ).joinToString(",") { value ->
                    "\"${value.toString().replace("\"", "\"\"")}\""
                },
            )
            append('\n')
        }
    }

    private data class ImportedDiary(
        val diary: DiaryEntity,
        val images: List<DiaryImageEntity>,
        val moods: List<DiaryMoodEntity>,
    )

    private data class ParsedManifest(
        val formatVersion: Int,
        val info: BackupInfo,
    )

    private data class TimeBackupData(
        val profile: TimeProfileEntity?,
        val events: List<TimeEventEntity>,
    )

    private companion object {
        const val FORMAT_VERSION = 5
        const val MIN_SUPPORTED_FORMAT_VERSION = 1
        const val SLEEP_BACKUP_VERSION = 2
        const val TIME_BACKUP_VERSION = 3
        val LEGACY_MOOD_RANGE = 1..6
        const val MAX_JSON_BYTES = 64L * 1024 * 1024
        const val MAX_IMAGE_BYTES = 100L * 1024 * 1024
        const val MAX_BACKUP_BYTES = 2L * 1024 * 1024 * 1024

        fun copyWithLimit(
            input: InputStream,
            output: OutputStream,
            maxBytes: Long,
        ): Long {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= maxBytes) { "备份数据超过允许大小" }
                output.write(buffer, 0, read)
            }
            return total
        }
    }
}
