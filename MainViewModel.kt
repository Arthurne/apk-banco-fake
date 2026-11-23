package com.purplebank.simulator.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purplebank.simulator.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.purplebank.simulator.util.toMillis
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.max

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(AppDatabase.get(app))
    private val prefs = Prefs(app)

    val profile = repo.profile.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val account = repo.account.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val card = repo.card.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val settings = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val invoices = repo.invoices.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val transactions = repo.transactions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories = repo.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val balanceLog = repo.balanceLog.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val contacts = repo.contacts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val goals = repo.goals.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val hideValues = prefs.hideValues.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Mensagens de sucesso/erro exibidas em Snackbar. */
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    var adminUnlocked by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch { repo.ensureSeed() }
    }

    fun purchases(invoiceId: Long) = repo.purchases(invoiceId)

    private fun op(okMsg: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                okMsg?.let { messages.emit(it) }
            } catch (e: Exception) {
                messages.emit("Erro: ${e.message ?: "operação falhou"}")
            }
        }
    }

    // --- PIN / painel
    fun tryUnlock(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = prefs.verifyPin(pin)
            if (ok) adminUnlocked = true
            onResult(ok)
        }
    }

    fun lock() { adminUnlocked = false }

    fun changePin(current: String, new: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (prefs.verifyPin(current)) {
                prefs.setPin(new); messages.emit("PIN alterado com sucesso"); onResult(true)
            } else {
                messages.emit("PIN atual incorreto"); onResult(false)
            }
        }
    }

    fun setHide(v: Boolean) { viewModelScope.launch { prefs.setHide(v) } }

    // --- perfil / aparência
    fun saveAppearance(name: String, bank: String, color: String, theme: String) = op("Alterações salvas") {
        repo.saveProfile(name); repo.saveAppearance(bank, color, theme)
    }

    fun setTheme(theme: String) = op { repo.setTheme(theme) }

    fun setAvatar(uri: Uri) = op("Foto atualizada") {
        val old = profile.value?.avatar
        val path = withContext(Dispatchers.IO) { saveAvatar(uri) }
        repo.setAvatar(path)
        old?.let { File(it).delete() }
    }

    private fun saveAvatar(uri: Uri): String {
        val ctx = getApplication<Application>()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) error("Imagem inválida")
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 768) sample *= 2
        val bmp = ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Imagem inválida")
        val f = File(ctx.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return f.absolutePath
    }

    // --- conta
    fun saveBalances(balance: Long, savings: Long) = op("Saldo atualizado") {
        repo.setBalances(balance, savings, "Edição manual no painel")
    }

    fun adjustBalance(delta: Long) = op(if (delta >= 0) "Saldo fictício adicionado" else "Saldo fictício removido") {
        repo.adjustBalance(delta, if (delta >= 0) "Adição manual de saldo" else "Remoção manual de saldo")
    }

    // --- movimentações / categorias
    fun addTransaction(t: Transaction, apply: Boolean) = op("Movimentação criada") { repo.addTransaction(t, apply) }
    fun updateTransaction(t: Transaction) = op("Movimentação atualizada") { repo.updateTransaction(t) }
    fun deleteTransaction(t: Transaction) = op("Movimentação excluída") { repo.deleteTransaction(t) }
    fun addCategory(name: String) = op("Categoria adicionada") { repo.addCategory(name) }
    fun deleteCategory(c: Category) = op("Categoria excluída") { repo.deleteCategory(c) }

    // --- cartão / faturas
    fun saveCard(holder: String, last4: String, limit: Long, closing: Int, due: Int) = op("Cartão atualizado") {
        repo.saveCard(holder, last4, limit, closing, due)
    }

    fun addPurchase(desc: String, amount: Long, date: LocalDate, installments: Int) = op("Compra adicionada") {
        repo.addPurchase(desc, amount, date, installments)
    }

    fun updatePurchase(p: Purchase, desc: String, amount: Long, date: LocalDate) = op("Compra atualizada") {
        repo.updatePurchase(p, desc, amount, date)
    }

    fun deletePurchase(p: Purchase) = op("Compra excluída") { repo.deletePurchase(p) }
    fun setInvoiceStatus(id: Long, status: String) = op("Status da fatura: $status") { repo.setInvoiceStatus(id, status) }
    fun updateInvoice(id: Long, status: String, due: LocalDate, closing: LocalDate, amount: Long) = op("Fatura atualizada") {
        repo.updateInvoice(id, status, due, closing, amount)
    }
    fun payInvoice(id: Long, amount: Long) = op("Pagamento simulado realizado") { repo.payInvoice(id, amount) }

    /** Compra no débito (sai do saldo) ou no crédito (entra na fatura, respeitando o limite). */
    fun buy(desc: String, amount: Long, date: LocalDate, installments: Int, debit: Boolean) =
        op(if (debit) "Compra no débito registrada" else "Compra no crédito registrada") {
            if (debit) {
                val bal = account.value?.balance ?: 0L
                if (amount > bal) error("Saldo insuficiente")
                repo.addTransaction(
                    Transaction(
                        description = desc, amount = amount, type = Transaction.OUT, category = "Compras",
                        date = LocalDateTime.of(date, LocalTime.now()).toMillis(), note = "Compra no débito (simulada)"
                    ), true
                )
            } else repo.addPurchase(desc, amount, date, installments, enforceLimit = true)
        }

    fun requestLimit(desired: Long) = op {
        val ok = repo.requestLimitIncrease(desired)
        messages.emit(if (ok) "Aumento de limite aprovado (simulado)" else "Aumento recusado (simulado): o máximo é 50% acima do limite atual")
    }

    fun transfer(contact: String, amount: Long, note: String) = op("Transferência simulada realizada") { repo.transfer(contact, amount, note) }
    fun addContact(name: String) = op("Contato adicionado") { repo.addContact(name) }
    fun deleteContact(c: Contact) = op("Contato excluído") { repo.deleteContact(c) }
    fun addGoal(name: String, target: Long) = op("Caixinha criada") { repo.addGoal(name, target) }
    fun updateGoal(g: Goal, name: String, target: Long) = op("Caixinha atualizada") { repo.updateGoal(g, name, target) }
    fun deleteGoal(g: Goal) = op("Caixinha encerrada") { repo.deleteGoal(g) }
    fun goalDeposit(id: Long, amount: Long) = op("Valor guardado") { repo.goalDeposit(id, amount) }
    fun goalWithdraw(id: Long, amount: Long) = op("Valor resgatado") { repo.goalWithdraw(id, amount) }
    fun deleteInvoice(id: Long) = op("Fatura excluída") { repo.deleteInvoice(id) }

    // --- dados
    fun exportTo(uri: Uri) = op("Dados fictícios exportados") {
        val json = repo.exportJson()
        withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                ?: error("Não foi possível gravar o arquivo")
        }
    }

    fun importFrom(uri: Uri) = op("Dados fictícios importados") {
        val text = withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                ?: error("Não foi possível ler o arquivo")
        }
        repo.importJson(text)
    }

    fun resetToDefaults() = op("Configuração restaurada") { repo.resetToDefaults() }
    fun clearAll() = op("Banco de dados limpo") { repo.clearAll() }
}
