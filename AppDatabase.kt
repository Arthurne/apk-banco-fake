package com.purplebank.simulator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfile::class, Account::class, CreditCard::class, Invoice::class, Purchase::class,
        Transaction::class, Category::class, AppSettings::class, BalanceLog::class, Contact::class, Goal::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun accountDao(): AccountDao
    abstract fun cardDao(): CardDao
    abstract fun settingsDao(): SettingsDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun balanceLogDao(): BalanceLogDao
    abstract fun contactDao(): ContactDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "purplebank.db")
                .fallbackToDestructiveMigration() // dados são fictícios: recriados se o esquema mudar
                .build().also { INSTANCE = it }
        }
    }
}
