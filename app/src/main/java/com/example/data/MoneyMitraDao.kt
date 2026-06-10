package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyMitraDao {
    @Query("SELECT * FROM users WHERE emailOrPhone = :emailOrPhone LIMIT 1")
    suspend fun getUserSync(emailOrPhone: String): DbUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: DbUser)

    @Update
    suspend fun updateUser(user: DbUser)

    @Query("SELECT * FROM user_session WHERE id = 1 LIMIT 1")
    fun getUserSession(): Flow<UserSession?>

    @Query("SELECT * FROM user_session WHERE id = 1 LIMIT 1")
    suspend fun getUserSessionSync(): UserSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSession(session: UserSession)

    @Update
    suspend fun updateUserSession(session: UserSession)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<DbTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: DbTransaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Query("DELETE FROM investment_plans")
    suspend fun deleteAllInvestmentPlans()

    @Query("DELETE FROM daily_tasks")
    suspend fun deleteAllDailyTasks()

    @Query("SELECT * FROM investment_plans WHERE uid = :uid ORDER BY minInvestment ASC")
    fun getAllInvestmentPlans(uid: String): Flow<List<DbInvestmentPlan>>

    @Query("SELECT * FROM investment_plans WHERE uid = :uid ORDER BY minInvestment ASC")
    suspend fun getAllInvestmentPlansSync(uid: String): List<DbInvestmentPlan>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestmentPlan(plan: DbInvestmentPlan)

    @Update
    suspend fun updateInvestmentPlan(plan: DbInvestmentPlan)

    @Query("SELECT * FROM daily_tasks")
    fun getAllDailyTasks(): Flow<List<DbDailyTask>>

    @Query("SELECT * FROM daily_tasks")
    suspend fun getAllDailyTasksSync(): List<DbDailyTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyTask(task: DbDailyTask)

    @Update
    suspend fun updateDailyTask(task: DbDailyTask)
}
