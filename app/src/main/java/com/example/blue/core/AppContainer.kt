package com.example.blue.core

import android.content.Context
import androidx.room.Room
import com.example.blue.core.database.AppDatabase
import com.example.blue.data.local.DiaryImageStorage
import com.example.blue.data.local.TimeImageStorage
import com.example.blue.data.backup.BackupManager
import com.example.blue.data.repository.AccountRepository
import com.example.blue.data.repository.DiaryRepository
import com.example.blue.data.repository.HomeRepository
import com.example.blue.data.repository.OfflineAccountRepository
import com.example.blue.data.repository.OfflineDiaryRepository
import com.example.blue.data.repository.OfflineHomeRepository
import com.example.blue.data.repository.OfflineSleepRepository
import com.example.blue.data.repository.OfflineTimeRepository
import com.example.blue.data.repository.SleepRepository
import com.example.blue.data.repository.TimeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

interface AppContainer {
    val homeRepository: HomeRepository
    val diaryRepository: DiaryRepository
    val accountRepository: AccountRepository
    val diaryImageStorage: DiaryImageStorage
    val sleepRepository: SleepRepository
    val timeRepository: TimeRepository
    val timeImageStorage: TimeImageStorage
    val backupManager: BackupManager

    fun initialize()
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initializationStarted = AtomicBoolean(false)

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        )
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()
    }

    override val homeRepository: HomeRepository by lazy {
        OfflineHomeRepository(database.homeDao())
    }

    override val diaryRepository: DiaryRepository by lazy {
        OfflineDiaryRepository(
            database = database,
            diaryDao = database.diaryDao(),
        )
    }

    override val diaryImageStorage: DiaryImageStorage by lazy {
        DiaryImageStorage(context.applicationContext)
    }

    override val sleepRepository: SleepRepository by lazy {
        OfflineSleepRepository(
            database = database,
            sleepRecordDao = database.sleepRecordDao(),
        )
    }

    override val timeRepository: TimeRepository by lazy {
        OfflineTimeRepository(timeDao = database.timeDao())
    }

    override val timeImageStorage: TimeImageStorage by lazy {
        TimeImageStorage(context.applicationContext)
    }

    override val backupManager: BackupManager by lazy {
        BackupManager(
            context = context.applicationContext,
            database = database,
            imageStorage = diaryImageStorage,
            timeImageStorage = timeImageStorage,
        )
    }

    override val accountRepository: AccountRepository by lazy {
        OfflineAccountRepository(accountDao = database.accountDao())
    }

    override fun initialize() {
        if (!initializationStarted.compareAndSet(false, true)) return
        applicationScope.launch {
            accountRepository.ensureDefaultCategories()
        }
    }
}
