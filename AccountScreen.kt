package com.purplebank.simulator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purplebank.simulator.data.Contact
import com.purplebank.simulator.data.Goal
import com.purplebank.simulator.data.Transaction
import com.purplebank.simulator.ui.*
import com.purplebank.simulator.ui.theme.BankColors
import com.purplebank.simulator.util.Fmt
import com.purplebank.simulator.util.toLocalDateTime
import java.time.YearMonth

@Composable
fun AccountScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val account by vm.account.collectAsStateWithLifecycle()
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val log by vm.balanceLog.collectAsStateWithLifecycle()
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    val goals by vm.goals.collectAsStateWithLifecycle()
    val hide by vm.hideValues.collectAsStateWithLifecycle()
    val a = account ?: return

    var showNew by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var showContacts by remember { mutableStateOf(false) }
    var newGoal by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf<Goal?>(null) }
    var delGoal by remember { mutableStateOf<Goal?>(null) }
    var moveGoal by remember { mutableStateOf<Pair<Goal, Boolean>?>(null) } // true = guardar, false = resgatar

    val month = YearMonth.now()
    val inMonth = txs.filter { YearMonth.from(it.date.toLocalDateTime()) == month }
    val totalIn = inMonth.filter { it.type == Transaction.IN }.sumOf { it.amount }
    val totalOut = inMonth.filter { it.type == Transaction.OUT }.sumOf { it.amount }
    val byCat = inMonth.filter { it.type == Transaction.OUT }
        .groupBy { it.category }.map { it.key to it.value.sumOf { x -> x.amount } }
        .sortedByDescending { it.second }.take(6)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Conta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            SectionCard("Saldos (fictícios)") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saldo disponível"); MoneyText(a.balance, hide, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Poupança simulada"); MoneyText(a.savingsBalance, hide, fontWeight = FontWeight.Bold)
                }
                InfoRow("Atualizado em", Fmt.dateTime(a.updatedAt))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button({ showTransfer = true }, Modifier.weight(1f)) { Icon(Icons.Filled.Send, null); Spacer(Modifier.width(6.dp)); Text("Transferir") }
                OutlinedButton({ showNew = true }, Modifier.weight(1f)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Movimentação") }
            }
        }
        item {
            SectionCard("Resumo do mês") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Entradas"); MoneyText(totalIn, hide, color = BankColors.In, fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saídas"); MoneyText(totalOut, hide, color = BankColors.Out, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            SectionCard("Gastos por categoria (mês)") {
                if (byCat.isEmpty()) Text("Sem saídas neste mês.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                val max = (byCat.firstOrNull()?.second ?: 1L).coerceAtLeast(1L)
                byCat.forEach { (cat, v) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cat, style = MaterialTheme.typography.bodyMedium)
                            MoneyText(v, hide, style = MaterialTheme.typography.bodyMedium)
                        }
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Box(Modifier.fillMaxWidth((v.toFloat() / max).coerceIn(0.02f, 1f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Caixinhas (poupança simulada)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton({ newGoal = true }) { Text("Nova") }
            }
        }
        if (goals.isEmpty()) item { EmptyState("Nenhuma caixinha. Crie uma meta.") }
        else items(goals, key = { it.id }) { g ->
            val frac = if (g.target > 0) (g.saved.toFloat() / g.target).coerceIn(0f, 1f) else 0f
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(g.name, fontWeight = FontWeight.Bold)
                        MoneyText(g.saved, hide, fontWeight = FontWeight.SemiBold)
                    }
                    LinearProgressIndicator(progress = { frac }, Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small))
                    Text("Meta: ${if (hide) "R$ •••••" else Fmt.money(g.target)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton({ moveGoal = g to true }) { Text("Guardar") }
                        TextButton({ moveGoal = g to false }) { Text("Resgatar") }
                        Spacer(Modifier.weight(1f))
                        IconButton({ editGoal = g }) { Icon(Icons.Filled.Edit, contentDescription = "Editar caixinha") }
                        IconButton({ delGoal = g }) { Icon(Icons.Filled.Delete, contentDescription = "Encerrar caixinha") }
                    }
                }
            }
        }
        item {
            OutlinedButton({ showContacts = true }, Modifier.fillMaxWidth()) { Text("Contatos fictícios (${contacts.size})") }
        }
        item { Text("Histórico de alterações de saldo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (log.isEmpty()) item { EmptyState("Sem alterações registradas.") }
        else items(log.take(8), key = { it.id }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(it.description)
                    Text(Fmt.dateTime(it.at), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MoneyText(it.balance, hide, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (showNew) TransactionDialog(null, cats, { showNew = false }) { t, apply -> vm.addTransaction(t, apply); showNew = false }
    if (showTransfer) TransferDialog(contacts, a.balance, { showTransfer = false }) { n, v, note -> vm.transfer(n, v, note); showTransfer = false }
    if (showContacts) ContactsDialog(contacts, { vm.addContact(it) }, { vm.deleteContact(it) }) { showContacts = false }
    if (newGoal) GoalDialog(null, { newGoal = false }) { n, t -> vm.addGoal(n, t); newGoal = false }
    editGoal?.let { g -> GoalDialog(g, { editGoal = null }) { n, t -> vm.updateGoal(g, n, t); editGoal = null } }
    delGoal?.let { g ->
        ConfirmDialog("Encerrar caixinha?", "O valor guardado (${Fmt.money(g.saved)}) volta para o saldo.", "Encerrar", { vm.deleteGoal(g) }, { delGoal = null })
    }
    moveGoal?.let { (g, deposit) ->
        AmountDialog(
            if (deposit) "Guardar em ${g.name}" else "Resgatar de ${g.name}", "Valor", "", if (deposit) "Guardar" else "Resgatar",
            { moveGoal = null }
        ) { if (deposit) vm.goalDeposit(g.id, it) else vm.goalWithdraw(g.id, it); moveGoal = null }
    }
}

/** Transferência SIMULADA entre contas fictícias. */
@Composable
fun TransferDialog(contacts: List<Contact>, balance: Long, onDismiss: () -> Unit, onConfirm: (String, Long, String) -> Unit) {
    var sel by remember { mutableStateOf(contacts.firstOrNull()?.name ?: "") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    val cents = Fmt.parseMoney(amount)
    val amountError = when {
        !showErrors -> null
        cents == null || cents <= 0 -> "Valor inválido"
        cents > balance -> "Saldo insuficiente"
        else -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transferência simulada") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nenhum dinheiro real é movimentado e nenhum comprovante é gerado.", style = MaterialTheme.typography.bodySmall)
                if (contacts.isEmpty()) Text("Cadastre um contato fictício primeiro (botão Contatos, na tela Conta).")
                else Dropdown("Para", sel, contacts.map { it.name }, { it }, { sel = it })
                MoneyField(amount, { amount = it }, "Valor", amountError)
                AppField(note, { note = it }, "Observação (opcional)")
            }
        },
        confirmButton = {
            TextButton({
                showErrors = true
                if (sel.isNotBlank() && cents != null && cents > 0 && cents <= balance) onConfirm(sel, cents, note.trim())
            }) { Text("Transferir") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AmountDialog(title: String, label: String, initial: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var v by remember { mutableStateOf(initial) }
    var showErrors by remember { mutableStateOf(false) }
    val cents = Fmt.parseMoney(v)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { MoneyField(v, { v = it }, label, if (showErrors && (cents == null || cents <= 0)) "Valor inválido" else null) },
        confirmButton = { TextButton({ showErrors = true; if (cents != null && cents > 0) onConfirm(cents) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun GoalDialog(initial: Goal?, onDismiss: () -> Unit, onSave: (String, Long) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var target by remember { mutableStateOf(initial?.let { Fmt.moneyInput(it.target) } ?: "") }
    var showErrors by remember { mutableStateOf(false) }
    val t = Fmt.parseMoney(target)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova caixinha" else "Editar caixinha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppField(name, { name = it }, "Nome da meta", if (showErrors && name.isBlank()) "Informe um nome" else null)
                MoneyField(target, { target = it }, "Valor da meta", if (showErrors && t == null) "Valor inválido" else null)
            }
        },
        confirmButton = { TextButton({ showErrors = true; if (name.isNotBlank() && t != null) onSave(name.trim(), t) }) { Text("Salvar") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ContactsDialog(contacts: List<Contact>, onAdd: (String) -> Unit, onDelete: (Contact) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contatos fictícios") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (contacts.isEmpty()) Text("Nenhum contato.")
                contacts.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.name, Modifier.weight(1f))
                        IconButton({ onDelete(c) }) { Icon(Icons.Filled.Delete, contentDescription = "Excluir contato") }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppField(name, { name = it }, "Novo contato", modifier = Modifier.weight(1f))
                    Button({
                        val n = name.trim()
                        if (n.isNotEmpty() && contacts.none { it.name.equals(n, true) }) { onAdd(n); name = "" }
                    }) { Text("Add") }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Fechar") } }
    )
}
