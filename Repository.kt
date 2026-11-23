package com.purplebank.simulator.data

import androidx.room.withTransaction
import com.purplebank.simulator.util.Billing
import com.purplebank.simulator.util.Fmt
import com.purplebank.simulator.util.toMillis
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class Repository(private val db: AppDatabase) {
    private val profileDao = db.profileDao()
    private val accountDao = db.accountDao()
    private val cardDao = db.cardDao()
    private val settingsDao = db.settingsDao()
    private val invoiceDao = db.invoiceDao()
    private val purchaseDao = db.purchaseDao()
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val logDao = db.balanceLogDao()
    private val contactDao = db.contactDao()
    private val goalDao = db.goalDao()

    val profile = profileDao.observe()
    val account = accountDao.observe()
    val card = cardDao.observe()
    val settings = settingsDao.observe()
    val invoices = invoiceDao.observeAll()
    val transactions = transactionDao.observeAll()
    val categories = categoryDao.observeAll()
    val balanceLog = logDao.observeAll()
    val contacts = contactDao.observeAll()
    val goals = goalDao.observeAll()
    fun purchases(invoiceId: Long) = purchaseDao.observeByInvoice(invoiceId)

    // ---------------------------------------------------------------- seed / reset

    suspend fun ensureSeed() {
        if (profileDao.get() == null) seed(demo = true)
        refreshStatuses()
    }

    private val defaultCategories = listOf(
        "Alimentação", "Transporte", "Moradia", "Lazer", "Saúde", "Salário", "Compras", "Fatura", "Transferência", "Outros"
    )

    suspend fun seed(demo: Boolean) = db.withTransaction {
        val now = System.currentTimeMillis()
        val balance = if (demo) 254387L else 0L
        val savings = if (demo) 120000L else 0L
        val limit = if (demo) 500000L else 0L
        profileDao.upsert(UserProfile(name = if (demo) "Maria Simulada" else "Usuário Fictício"))
        accountDao.upsert(Account(balance = balance, savingsBalance = savings))
        cardDao.upsert(
            CreditCard(
                holderName = if (demo) "MARIA SIMULADA" else "USUARIO FICTICIO",
                totalLimit = limit, availableLimit = limit
            )
        )
        settingsDao.upsert(AppSettings())
        categoryDao.insertAll(defaultCategories.map { Category(name = it) })
        logDao.insert(BalanceLog(at = now, description = "Configuração inicial", balance = balance, savings = savings))
        if (demo) {
            contactDao.insertAll(listOf("Ana Souza", "Carlos Lima", "João Fictício").map { Contact(name = it) })
            goalDao.insert(Goal(name = "Viagem", saved = 30000, target = 200000))
            val t = LocalDateTime.now()
            fun tx(d: String, v: Long, type: String, c: String, daysAgo: Long) = Transaction(
                description = d, amount = v, type = type, category = c,
                date = t.minusDays(daysAgo).toMillis(), note = "Dado fictício de demonstração"
            )
            transactionDao.insertAll(
                listOf(
                    tx("Salário (fictício)", 450000, Transaction.IN, "Salário", 8),
                    tx("Supermercado", 18990, Transaction.OUT, "Alimentação", 5),
                    tx("Transporte por app", 2350, Transaction.OUT, "Transporte", 4),
                    tx("Assinatura de streaming", 3990, Transaction.OUT, "Lazer", 3),
                    tx("Farmácia", 4560, Transaction.OUT, "Saúde", 2),
                    tx("Trabalho extra (fictício)", 85000, Transaction.IN, "Outros", 1)
                )
            )
            val today = LocalDate.now()
            addPurchase("Supermercado", 18990, today.minusDays(5), 1)
            addPurchase("Notebook (3x)", 270000, today.minusDays(12), 3)
            addPurchase("Restaurante", 7450, today.minusDays(2), 1)
        }
        recalcCard()
    }

    private suspend fun wipe() {
        purchaseDao.deleteAll(); invoiceDao.deleteAll(); transactionDao.deleteAll()
        categoryDao.deleteAll(); logDao.deleteAll(); contactDao.deleteAll(); goalDao.deleteAll()
        cardDao.deleteAll(); accountDao.deleteAll(); profileDao.deleteAll(); settingsDao.deleteAll()
    }

    /** "Restaurar configuração": apaga tudo e recria os dados de demonstração. */
    suspend fun resetToDefaults() = db.withTransaction { wipe(); seed(demo = true) }

    /** "Limpar banco": apaga tudo e recria apenas a estrutura vazia (saldo zero, sem movimentações). */
    suspend fun clearAll() = db.withTransaction { wipe(); seed(demo = false) }

    /** Faturas abertas cujo fechamento já passou viram FECHADA. */
    suspend fun refreshStatuses() = db.withTransaction {
        val today = LocalDate.now().toEpochDay()
        invoiceDao.getAll().filter { it.status == Invoice.OPEN && it.closingDate < today }
            .forEach { invoiceDao.update(it.copy(status = Invoice.CLOSED)) }
        recalcCard()
    }

    // ---------------------------------------------------------------- perfil / configurações

    suspend fun saveProfile(name: String) {
        profileDao.get()?.let { profileDao.upsert(it.copy(name = name)) }
    }

    suspend fun setAvatar(path: String?) {
        profileDao.get()?.let { profileDao.upsert(it.copy(avatar = path)) }
    }

    suspend fun saveAppearance(bankName: String, color: String, theme: String) {
        settingsDao.get()?.let { settingsDao.upsert(it.copy(bankName = bankName, primaryColor = color, theme = theme)) }
    }

    suspend fun setTheme(theme: String) {
        settingsDao.get()?.let { settingsDao.upsert(it.copy(theme = theme)) }
    }

    // ---------------------------------------------------------------- conta

    private suspend fun log(desc: String, a: Account) =
        logDao.insert(BalanceLog(at = System.currentTimeMillis(), description = desc, balance = a.balance, savings = a.savingsBalance))

    suspend fun setBalances(balance: Long, savings: Long, reason: String) = db.withTransaction {
        val acc = accountDao.get() ?: return@withTransaction
        val n = acc.copy(balance = balance, savingsBalance = savings, updatedAt = System.currentTimeMillis())
        accountDao.upsert(n); log(reason, n)
    }

    suspend fun adjustBalance(delta: Long, reason: String) = db.withTransaction { adjustInternal(delta, reason) }

    private suspend fun adjustInternal(delta: Long, reason: String) {
        val acc = accountDao.get() ?: return
        val n = acc.copy(balance = acc.balance + delta, updatedAt = System.currentTimeMillis())
        accountDao.upsert(n); log(reason, n)
    }

    // ---------------------------------------------------------------- movimentações

    suspend fun addTransaction(t: Transaction, applyToBalance: Boolean) = db.withTransaction {
        transactionDao.insert(t.copy(id = 0))
        if (applyToBalance) {
            adjustInternal(if (t.type == Transaction.IN) t.amount else -t.amount, "Movimentação: ${t.description}")
        }
    }

    suspend fun updateTransaction(t: Transaction) = transactionDao.update(t)
    suspend fun deleteTransaction(t: Transaction) = transactionDao.delete(t)

    suspend fun addCategory(name: String) { categoryDao.insert(Category(name = name)) }
    suspend fun deleteCategory(c: Category) = categoryDao.delete(c)

    // ---------------------------------------------------------------- cartão / faturas

    suspend fun saveCard(holder: String, last4: String, totalLimit: Long, closingDay: Int, dueDay: Int) = db.withTransaction {
        val c = cardDao.get() ?: return@withTransaction
        cardDao.upsert(c.copy(holderName = holder, cardLast4 = last4, totalLimit = totalLimit, closingDay = closingDay, dueDay = dueDay))
        recalcCard()
    }

    /** Recalcula totais das faturas e os limites do cartão a partir das compras cadastradas. */
    suspend fun recalcCard() {
        invoiceDao.getAll().forEach { inv ->
            val total = (purchaseDao.sumByInvoice(inv.id) ?: 0L) + inv.adjustment
            if (total != inv.amount) invoiceDao.update(inv.copy(amount = total))
        }
        val card = cardDao.get() ?: return
        val used = Billing.usedLimit(invoiceDao.getAll())
        cardDao.upsert(card.copy(usedLimit = used, availableLimit = card.totalLimit - used))
    }

    private suspend fun ensureInvoice(ym: YearMonth, card: CreditCard): Invoice {
        invoiceDao.getByMonth(ym.toString())?.let { return it }
        val closing = Billing.closingDate(ym, card.closingDay)
        val due = Billing.dueDate(ym, card.closingDay, card.dueDay)
        val status = if (closing.isBefore(LocalDate.now())) Invoice.CLOSED else Invoice.OPEN
        val inv = Invoice(refMonth = ym.toString(), status = status, dueDate = due.toEpochDay(), closingDate = closing.toEpochDay())
        return inv.copy(id = invoiceDao.insert(inv))
    }

    suspend fun addPurchase(description: String, total: Long, date: LocalDate, installments: Int, enforceLimit: Boolean = false) = db.withTransaction {
        val card = cardDao.get() ?: return@withTransaction
        if (enforceLimit && total > card.availableLimit) error("Limite insuficiente para esta compra")
        val first = Billing.refMonthFor(date, card.closingDay)
        Billing.splitInstallments(total, installments).forEachIndexed { i, amount ->
            val inv = ensureInvoice(first.plusMonths(i.toLong()), card)
            purchaseDao.insert(
                Purchase(
                    invoiceId = inv.id, description = description, amount = amount, date = date.toEpochDay(),
                    installmentNumber = i + 1, installmentTotal = installments
                )
            )
        }
        recalcCard()
    }

    suspend fun updatePurchase(p: Purchase, description: String, amount: Long, date: LocalDate) = db.withTransaction {
        val card = cardDao.get() ?: return@withTransaction
        val ym = Billing.refMonthFor(date, card.closingDay).plusMonths((p.installmentNumber - 1).toLong())
        val inv = ensureInvoice(ym, card)
        purchaseDao.update(p.copy(invoiceId = inv.id, description = description, amount = amount, date = date.toEpochDay()))
        recalcCard()
    }

    suspend fun deletePurchase(p: Purchase) = db.withTransaction { purchaseDao.delete(p); recalcCard() }

    suspend fun setInvoiceStatus(id: Long, status: String) = db.withTransaction {
        invoiceDao.getById(id)?.let { invoiceDao.update(it.copy(status = status)) }
        recalcCard()
    }

    suspend fun updateInvoice(id: Long, status: String, due: LocalDate, closing: LocalDate, desiredAmount: Long) = db.withTransaction {
        val inv = invoiceDao.getById(id)
        if (inv != null) {
            val sum = purchaseDao.sumByInvoice(id) ?: 0L
            invoiceDao.update(
                inv.copy(status = status, dueDate = due.toEpochDay(), closingDate = closing.toEpochDay(), adjustment = desiredAmount - sum)
            )
            recalcCard()
        }
    }

    /** Pagamento SIMULADO (total ou parcial): debita o saldo fictício e registra uma movimentação. */
    suspend fun payInvoice(id: Long, amount: Long) = db.withTransaction {
        val inv = invoiceDao.getById(id)
        if (inv != null && inv.status != Invoice.PAID) {
            if (amount <= 0 || amount > inv.amount) error("Valor inválido para esta fatura")
            val acc = accountDao.get() ?: error("Conta não encontrada")
            if (amount > acc.balance) error("Saldo insuficiente")
            val full = amount == inv.amount
            val label = Fmt.monthLabel(inv.refMonth)
            adjustInternal(-amount, "Pagamento ${if (full) "" else "parcial "}simulado da fatura $label")
            transactionDao.insert(
                Transaction(
                    description = if (full) "Pagamento de fatura (simulado)" else "Pagamento parcial de fatura (simulado)",
                    amount = amount, type = Transaction.OUT, category = "Fatura",
                    date = System.currentTimeMillis(), note = "Fatura $label"
                )
            )
            if (full) invoiceDao.update(inv.copy(status = Invoice.PAID))
            else invoiceDao.update(inv.copy(adjustment = inv.adjustment - amount))
            recalcCard()
        }
    }

    suspend fun deleteInvoice(id: Long) = db.withTransaction {
        invoiceDao.getById(id)?.let { invoiceDao.delete(it) }
        recalcCard()
    }

    // ---------------------------------------------------------------- transferências, caixinhas, limite

    /** Transferência SIMULADA entre contas fictícias (não é Pix, não gera comprovante). */
    suspend fun transfer(contact: String, amount: Long, note: String) = db.withTransaction {
        val acc = accountDao.get() ?: error("Conta não encontrada")
        if (amount <= 0) error("Valor inválido")
        if (amount > acc.balance) error("Saldo insuficiente")
        adjustInternal(-amount, "Transferência simulada para $contact")
        transactionDao.insert(
            Transaction(
                description = "Transferência para $contact (simulada)", amount = amount, type = Transaction.OUT,
                category = "Transferência", date = System.currentTimeMillis(), note = note
            )
        )
    }

    suspend fun addContact(name: String) { contactDao.insert(Contact(name = name)) }
    suspend fun deleteContact(c: Contact) = contactDao.delete(c)

    suspend fun addGoal(name: String, target: Long) { goalDao.insert(Goal(name = name, target = target)) }
    suspend fun updateGoal(g: Goal, name: String, target: Long) = goalDao.update(g.copy(name = name, target = target))

    suspend fun deleteGoal(g: Goal) = db.withTransaction {
        val cur = goalDao.getById(g.id)
        if (cur != null) {
            if (cur.saved > 0) adjustInternal(cur.saved, "Caixinha \"${cur.name}\" encerrada")
            goalDao.delete(cur)
        }
    }

    suspend fun goalDeposit(goalId: Long, amount: Long) = db.withTransaction {
        val g = goalDao.getById(goalId) ?: error("Caixinha não encontrada")
        val acc = accountDao.get() ?: error("Conta não encontrada")
        if (amount <= 0) error("Valor inválido")
        if (amount > acc.balance) error("Saldo insuficiente")
        adjustInternal(-amount, "Guardado na caixinha ${g.name}")
        goalDao.update(g.copy(saved = g.saved + amount))
    }

    suspend fun goalWithdraw(goalId: Long, amount: Long) = db.withTransaction {
        val g = goalDao.getById(goalId) ?: error("Caixinha não encontrada")
        if (amount <= 0) error("Valor inválido")
        if (amount > g.saved) error("A caixinha não tem esse valor")
        adjustInternal(amount, "Resgatado da caixinha ${g.name}")
        goalDao.update(g.copy(saved = g.saved - amount))
    }

    /** Regra fictícia simples: aprova se o novo limite for no máximo 50% acima do atual. */
    suspend fun requestLimitIncrease(desired: Long): Boolean = db.withTransaction {
        val c = cardDao.get() ?: error("Cartão não encontrado")
        if (desired <= c.totalLimit) error("O novo limite deve ser maior que o atual")
        val ok = desired <= c.totalLimit * 3 / 2
        if (ok) {
            cardDao.upsert(c.copy(totalLimit = desired))
            recalcCard()
        }
        ok
    }

    // ---------------------------------------------------------------- exportar / importar

    suspend fun exportJson(): String {
        val o = JSONObject()
        o.put("app", "PurpleBankSimulator").put("schema", 1)
        profileDao.get()?.let { o.put("profile", JSONObject().put("name", it.name).put("createdAt", it.createdAt)) }
        accountDao.get()?.let { o.put("account", JSONObject().put("balance", it.balance).put("savings", it.savingsBalance)) }
        cardDao.get()?.let {
            o.put("card", JSONObject().put("holder", it.holderName).put("last4", it.cardLast4)
                .put("totalLimit", it.totalLimit).put("closingDay", it.closingDay).put("dueDay", it.dueDay))
        }
        settingsDao.get()?.let {
            o.put("settings", JSONObject().put("theme", it.theme).put("currency", it.currency)
                .put("simulatorMode", it.simulatorMode).put("bankName", it.bankName).put("primaryColor", it.primaryColor))
        }
        o.put("categories", JSONArray(categoryDao.getAll().map { it.name }))
        o.put("invoices", JSONArray(invoiceDao.getAll().map {
            JSONObject().put("id", it.id).put("refMonth", it.refMonth).put("adjustment", it.adjustment)
                .put("status", it.status).put("dueDate", it.dueDate).put("closingDate", it.closingDate)
        }))
        o.put("purchases", JSONArray(purchaseDao.getAll().map {
            JSONObject().put("id", it.id).put("invoiceId", it.invoiceId).put("description", it.description)
                .put("amount", it.amount).put("date", it.date).put("n", it.installmentNumber).put("total", it.installmentTotal)
        }))
        o.put("transactions", JSONArray(transactionDao.getAll().map {
            JSONObject().put("description", it.description).put("amount", it.amount).put("type", it.type)
                .put("category", it.category).put("date", it.date).put("status", it.status).put("note", it.note)
        }))
        o.put("balanceLog", JSONArray(logDao.getAll().map {
            JSONObject().put("at", it.at).put("description", it.description).put("balance", it.balance).put("savings", it.savings)
        }))
        o.put("contacts", JSONArray(contactDao.getAll().map { JSONObject().put("name", it.name) }))
        o.put("goals", JSONArray(goalDao.getAll().map { JSONObject().put("name", it.name).put("saved", it.saved).put("target", it.target) }))
        return o.toString(2)
    }

    private fun JSONArray.objs(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    suspend fun importJson(text: String) {
        val o = JSONObject(text)
        require(o.optString("app") == "PurpleBankSimulator") { "Arquivo não é um backup do PurpleBank Simulator" }
        require(o.has("profile") && o.has("account") && o.has("card") && o.has("settings")) { "Backup incompleto" }
        db.withTransaction {
            wipe()
            o.getJSONObject("profile").let { profileDao.upsert(UserProfile(name = it.getString("name"), createdAt = it.optLong("createdAt", System.currentTimeMillis()))) }
            o.getJSONObject("account").let { accountDao.upsert(Account(balance = it.getLong("balance"), savingsBalance = it.getLong("savings"))) }
            o.getJSONObject("card").let {
                cardDao.upsert(CreditCard(
                    holderName = it.getString("holder"), cardLast4 = it.getString("last4"), totalLimit = it.getLong("totalLimit"),
                    availableLimit = it.getLong("totalLimit"), closingDay = it.getInt("closingDay"), dueDay = it.getInt("dueDay")
                ))
            }
            o.getJSONObject("settings").let {
                settingsDao.upsert(AppSettings(
                    theme = it.getString("theme"), currency = it.optString("currency", "BRL"),
                    simulatorMode = true, bankName = it.getString("bankName"), primaryColor = it.getString("primaryColor")
                ))
            }
            o.optJSONArray("categories")?.let { a -> categoryDao.insertAll((0 until a.length()).map { Category(name = a.getString(it)) }) }
            o.optJSONArray("invoices")?.objs()?.let { l ->
                invoiceDao.insertAll(l.map {
                    Invoice(id = it.getLong("id"), refMonth = it.getString("refMonth"), adjustment = it.getLong("adjustment"),
                        status = it.getString("status"), dueDate = it.getLong("dueDate"), closingDate = it.getLong("closingDate"))
                })
            }
            o.optJSONArray("purchases")?.objs()?.let { l ->
                purchaseDao.insertAll(l.map {
                    Purchase(id = it.getLong("id"), invoiceId = it.getLong("invoiceId"), description = it.getString("description"),
                        amount = it.getLong("amount"), date = it.getLong("date"), installmentNumber = it.getInt("n"), installmentTotal = it.getInt("total"))
                })
            }
            o.optJSONArray("transactions")?.objs()?.let { l ->
                transactionDao.insertAll(l.map {
                    Transaction(description = it.getString("description"), amount = it.getLong("amount"), type = it.getString("type"),
                        category = it.getString("category"), date = it.getLong("date"), status = it.optString("status", "SIMULADO"), note = it.optString("note", ""))
                })
            }
            o.optJSONArray("balanceLog")?.objs()?.let { l ->
                logDao.insertAll(l.map { BalanceLog(at = it.getLong("at"), description = it.getString("description"), balance = it.getLong("balance"), savings = it.getLong("savings")) })
            }
            o.optJSONArray("contacts")?.objs()?.let { l -> contactDao.insertAll(l.map { Contact(name = it.getString("name")) }) }
            o.optJSONArray("goals")?.objs()?.let { l ->
                goalDao.insertAll(l.map { Goal(name = it.getString("name"), saved = it.getLong("saved"), target = it.getLong("target")) })
            }
            recalcCard()
        }
    }
}
