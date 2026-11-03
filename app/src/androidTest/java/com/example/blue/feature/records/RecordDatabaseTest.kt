package com.example.blue.feature.records

import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.blue.core.database.AppDatabase
import com.example.blue.data.backup.BackupManager
import com.example.blue.data.backup.RestoreMode
import com.example.blue.data.local.DiaryImageStorage
import com.example.blue.data.local.TimeImageStorage
import com.example.blue.data.local.entity.NumericRecordEntity
import com.example.blue.data.local.entity.RecordTypeEntity
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordDatabaseTest {
    private lateinit var database: AppDatabase
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDatabase() { database.close() }

    @Test
    fun projectsHaveIndependentHistoryAndMeasurementTimesDetermineLatestValue() = runBlocking {
        val dao = database.recordDao()
        dao.upsertType(type("weight")); dao.upsertType(type("water"))
        dao.upsertRecord(record("first", "weight", 100, 65.0))
        dao.upsertRecord(record("latest", "weight", 200, 64.5))
        dao.upsertRecord(record("backfill", "weight", 50, 70.0).copy(createdAt = 1000))
        dao.upsertRecord(record("water", "water", 300, 500.0))
        val overview = dao.observeTypes().first().first { it.id == "weight" }
        assertEquals(3, overview.recordCount)
        assertEquals(64.5, overview.latestValue!!, 0.0)
        assertEquals(listOf("latest", "first", "backfill"), dao.observeRecords("weight").first().map { it.id })
    }

    @Test
    fun editingAndDeletingMeasurementsUpdatesOverviewAndOnlyProjectDeletionCascades() = runBlocking {
        val dao = database.recordDao()
        dao.upsertType(type("weight")); dao.upsertType(type("water"))
        dao.upsertRecord(record("entry", "weight", 100, 65.0))
        dao.upsertRecord(record("other", "water", 100, 500.0))
        dao.upsertRecord(record("entry", "weight", 100, 64.0))
        assertEquals(1, dao.observeTypes().first().first { it.id == "weight" }.recordCount)
        assertEquals(64.0, dao.getRecord("entry")!!.value, 0.0)
        dao.deleteType("weight")
        assertNull(dao.getRecord("entry"))
        assertNotNull(dao.getRecord("other"))
        dao.deleteRecord("other")
        assertNull(dao.observeTypes().first().single().latestValue)
    }

    @Test
    fun exportedBackupRestoresProjectsAndMeasurementsWithoutDuplicatingIds() = runBlocking {
        val dao = database.recordDao()
        dao.upsertType(type("weight"))
        dao.upsertRecord(record("entry", "weight", 100, 65.25))
        val manager = BackupManager(context, database, DiaryImageStorage(context), TimeImageStorage(context))
        val file = File.createTempFile("record-backup-", ".zip", context.cacheDir)
        try {
            val uri = Uri.fromFile(file)
            manager.exportTo(uri)
            assertEquals(1, manager.inspect(uri).numericRecordCount)
            dao.clearTypes()
            manager.restore(uri, RestoreMode.MERGE)
            manager.restore(uri, RestoreMode.MERGE)
            assertEquals(1, dao.getAllTypes().size)
            assertEquals(1, dao.getAllRecords().size)
            assertEquals(65.25, dao.getRecord("entry")!!.value, 0.0)
            dao.upsertType(type("temporary"))
            manager.restore(uri, RestoreMode.REPLACE)
            assertNull(dao.getType("temporary"))
            assertNotNull(dao.getRecord("entry"))
        } finally { file.delete() }
    }

    private fun type(id: String) = RecordTypeEntity(id, id, "kg", 1, 1)
    private fun record(id: String, typeId: String, time: Long, value: Double) = NumericRecordEntity(id, typeId, value, time, "", time, time)
}
