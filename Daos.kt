package com.purplebank.simulator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1") fun observe(): Flow<UserProfile?>
    @Query("SELECT * FROM user_profile WHERE id = 1") suspend fun get(): UserProfile?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(p: UserProfile)
    @Query("DELETE FROM user_profile") suspend fun deleteAll()
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM account WHERE id = 1") fun observe(): Flow<Account?>
    @Query("SELECT * FROM account WHERE id = 1") suspend fun get(): Account?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(a: Account)
    @Query("DELETE FROM account") suspend fun deleteAll()
}

@Dao
interface CardDao {
    @Query("SELECT * FROM credit_card WHERE id = 1") fun observe(): Flow<CreditCard?>
    @Query("SELECT * FROM credit_card WHERE id = 1") suspend fun get(): CreditCard?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(c: CreditCard)
    @Query("DELETE FROM credit_card") suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1") fun observe(): Flow<AppSettings?>
    @Query("SELECT * FROM app_settings WHERE id = 1") suspend fun get(): AppSettings?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(s: AppSettings)
    @Query("DELETE FROM app_settings") suspend fun deleteAll()
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoice ORDER BY closingDate") fun observeAll(): Flow<List<Invoice>>
    @Query("SELECT * FROM invoice ORDER BY closingDate") suspend fun getAll(): List<Invoice>
    @Query("SELECT * FROM invoice WHERE id = :id") suspend fun getById(id: Long): Invoice?
    @Query("SELECT * FROM invoice WHERE refMonth = :refMonth LIMIT 1") suspend fun getByMonth(refMonth: String): Invoice?
    @Insert suspend fun insert(i: Invoice): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<Invoice>)
    @Update suspend fun update(i: Invoice)
    @Delete suspend fun delete(i: Invoice)
    @Query("DELETE FROM invoice") suspend fun deleteAll()
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchase WHERE invoiceId = :invoiceId ORDER BY date DESC, id DESC")
    fun observeByInvoice(invoiceId: Long): Flow<List<Purchase>>
    @Query("SELECT * FROM purchase") suspend fun getAll(): List<Purchase>
    @Query("SELECT SUM(amount) FROM purchase WHERE invoiceId = :invoiceId") suspend fun sumByInvoice(invoiceId: Long): Long?
    @Insert suspend fun insert(p: Purchase): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<Purchase>)
    @Update suspend fun update(p: Purchase)
    @Delete suspend fun delete(p: Purchase)
    @Query("DELETE FROM purchase") suspend fun deleteAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC") fun observeAll(): Flow<List<Transaction>>
    @Query("SELECT * FROM transactions") suspend fun getAll(): List<Transaction>
    @Insert suspend fun insert(t: Transaction): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<Transaction>)
    @Update suspend fun update(t: Transaction)
    @Delete suspend fun delete(t: Transaction)
    @Query("DELETE FROM transactions") suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY name") fun observeAll(): Flow<List<Category>>
    @Query("SELECT * FROM category") suspend fun getAll(): List<Category>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(c: Category): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(list: List<Category>)
    @Delete suspend fun delete(c: Category)
    @Query("DELETE FROM category") suspend fun deleteAll()
}

@Dao
interface BalanceLogDao {
    @Query("SELECT * FROM balance_log ORDER BY at DESC, id DESC LIMIT 100") fun observeAll(): Flow<List<BalanceLog>>
    @Query("SELECT * FROM balance_log ORDER BY at") suspend fun getAll(): List<BalanceLog>
    @Insert suspend fun insert(l: BalanceLog): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<BalanceLog>)
    @Query("DELETE FROM balance_log") suspend fun deleteAll()
}
