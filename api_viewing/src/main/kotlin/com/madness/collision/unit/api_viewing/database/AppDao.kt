package com.madness.collision.unit.api_viewing.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

@Dao
internal interface AppDao {
    @Insert
    fun insert(app: ApiViewingApp)

    @Query("DELETE FROM apps")
    fun deleteAll()

    @Query("SELECT * FROM apps ORDER BY package ASC")
    fun getAllApps(): LiveData<List<ApiViewingApp>>

    @Query("SELECT * FROM apps WHERE apiUnit == ${ApiUnit.USER} ORDER BY package ASC")
    fun getUserApps(): LiveData<List<ApiViewingApp>>

    @Query("SELECT * FROM apps WHERE apiUnit == ${ApiUnit.SYS} ORDER BY package ASC")
    fun getSystemApps(): LiveData<List<ApiViewingApp>>
}
