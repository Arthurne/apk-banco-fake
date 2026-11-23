package com.purplebank.simulator.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Todos os valores monetários são guardados em CENTAVOS (Long) para evitar erros de ponto flutuante. */

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val avatar: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "account")
data class Account(
    @PrimaryKey val id: Int = 1,
    val balance: Long,
    val savingsBalance: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "credit_card")
data class CreditCard(
    @PrimaryKey val id: Int = 1,
    val holderName: String,
    val cardLast4: String = "0000",
    val totalLimit: Long,
    val availableLimit: Long,
    val usedLimit: Long = 0,
    val closingDay: Int = 20,
    val dueDay: Int = 27
)

@Entity(
    tableName = "invoice",
    foreignKeys = [ForeignKey(
        entity = CreditCard::class, parentColumns = ["id"], childColumns = ["cardId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cardId"), Index(value = ["cardId", "refMonth"], unique = true)]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Int = 1,
    /** Mês de referência (mês do fechamento), formato yyyy-MM */
    val refMonth: String,
    /** Total = soma das compras + ajuste manual */
    val amount: Long = 0,
    val adjustment: Long = 0,
    val status: String = OPEN,
    /** epochDay */
    val dueDate: Long,
    /** epochDay */
    val closingDate: Long
) {
    companion object {
        const val OPEN = "ABERTA"
        const val CLOSED = "FECHADA"
        const val PAID = "PAGA"
        val ALL = listOf(OPEN, CLOSED, PAID)
    }
}

@Entity(
    tableName = "purchase",
    foreignKeys = [ForeignKey(
        entity = Invoice::class, parentColumns = ["id"], childColumns = ["invoiceId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("invoiceId")]
)
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val description: String,
    val amount: Long,
    /** epochDay da compra */
    val date: Long,
    val installmentNumber: Int = 1,
    val installmentTotal: Int = 1
)

@Entity(tableName = "transactions", indices = [Index("date"), Index("category")])
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Long,
    val type: String,
    val category: String,
    /** epoch millis */
    val date: Long,
    val status: String = "SIMULADO",
    val note: String = ""
) {
    companion object {
        const val IN = "ENTRADA"
        const val OUT = "SAIDA"
    }
}

@Entity(tableName = "category", indices = [Index(value = ["name"], unique = true)])
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    /** SYSTEM, LIGHT ou DARK */
    val theme: String = "SYSTEM",
    val currency: String = "BRL",
    val simulatorMode: Boolean = true,
    val bankName: String = "PurpleBank",
    val primaryColor: String = "#820AD1"
)

@Entity(tableName = "balance_log")
data class BalanceLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val description: String,
    val balance: Long,
    val savings: Long
)

@Entity(tableName = "contact", indices = [Index(value = ["name"], unique = true)])
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

/** "Caixinha" de poupança simulada. */
@Entity(tableName = "goal")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val saved: Long = 0,
    val target: Long = 0
)
