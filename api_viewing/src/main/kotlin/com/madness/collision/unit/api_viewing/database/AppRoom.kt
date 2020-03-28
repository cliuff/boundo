package com.madness.collision.unit.api_viewing.database

import android.content.Context
import androidx.room.*
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import kotlinx.coroutines.CoroutineScope

@Database(entities = [ApiViewingApp::class], version = 1)
@TypeConverters(Converters::class)
internal abstract class AppRoom: RoomDatabase(){

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppRoom? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppRoom {
            val tempInstance = INSTANCE
            if (tempInstance != null) return tempInstance
            synchronized(this){
                if (INSTANCE != null) return INSTANCE!!
                val instance = Room.databaseBuilder(
                        context.applicationContext, AppRoom::class.java, "apps"
                ).addCallback(AppDatabaseCallback(context, scope)).build()
                INSTANCE = instance
                return instance
            }
        }
    }

    private class AppDatabaseCallback(private val context: Context, private val scope: CoroutineScope) : RoomDatabase.Callback() {

//        override fun onCreate(db: SupportSQLiteDatabase) {
//            super.onCreate(db)
//            INSTANCE?.run {
//                scope.launch(Dispatchers.IO){
//                    populateDatabase(appDao())
//                }
//            }
//        }

//        override fun onOpen(db: SupportSQLiteDatabase) {
//            super.onOpen(db)
//            INSTANCE?.let { database ->
//                scope.launch(Dispatchers.IO) {
//                    populateDatabase(database.appDao())
//                }
//            }
//        }

        fun populateDatabase(dao: AppDao) {
            dao.deleteAll()
            context.packageManager.getInstalledPackages(0).map {
                ApiViewingApp(context, it, preloadProcess = true, archive = false).load(context)
            }.forEach {
                dao.insert(it)
            }
        }
    }
}
