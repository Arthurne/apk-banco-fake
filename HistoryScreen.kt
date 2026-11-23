package com.purplebank.simulator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purplebank.simulator.data.Category
import com.purplebank.simulator.data.Transaction
import com.purplebank.simulator.ui.*
import com.purplebank.simulator.util.Fmt
import com.purplebank.simulator.util.toLocalDateTime
import com.purplebank.simulator.util.toMillis
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private enum class Period(val label: String) {
    ALL("Tudo"), D7("7 dias"), D30("30 dias"), MONTH("Este mês"), CUSTOM("Personalizado")
}

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val hide by vm.hideValues.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var catFilter by remember { mutableStateOf<String?>(null) }
    var period by remember { mutableStateOf(Period.ALL) }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var sortDesc by remember { mutableStateOf(true) }
    var showNew by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Transaction?>(null) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var deleting by remember { mutableStateOf<Transaction?>(null) }

    val filtered = remember(txs, query, catFilter, period, from, to, sortDesc) {
        val today = LocalDate.now()
        val fromD = Fmt.parseDate(from)
        val toD = Fmt.parseDate(to)
        txs.filter { t ->
            val d = t.date.toLocalDateTime().toLocalDate()
            (query.isBlank() || t.description.contains(query.trim(), ignoreCase = true)) &&
                (catFilter == null || t.category == catFilter) &&
                when (period) {
                    Period.ALL -> true
                    Period.D7 -> !d.isBefore(today.minusDays(6))
                    Period.D30 -> !d.isBefore(today.minusDays(29))
                    Period.MONTH -> YearMonth.from(d) == YearMonth.from(today)
                    Period.CUSTOM -> (fromD == null || !d.isBefore(fromD)) && (toD == null || !d.isAfter(toD))
                }
        }.let { l -> if (sortDesc) l.sortedByDescending { it.date } else l.sortedBy { it.date } }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Histórico", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        query, { query = it }, Modifier.weight(1f), singleLine = true,
                        label = { Text("Pesquisar descrição") }, leadingIcon = { Icon(Icons.Filled.Search, null) }
                    )
                    IconButton({ sortDesc = !sortDesc }) { Icon(Icons.Filled.SwapVert, contentDescription = "Inverter ordem por data") }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Period.values().toList()) { p ->
                        FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) })
                    }
                }
            }
            if (period == Period.CUSTOM) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppField(from, { from = it }, "De (dd/mm/aaaa)", modifier = Modifier.weight(1f), keyboard = KeyboardType.Number)
                    AppField(to, { to = it }, "Até (dd/mm/aaaa)", modifier = Modifier.weight(1f), keyboard = KeyboardType.Number)
                }
            }
            item {
                Dropdown(
                    "Categoria", catFilter ?: "Todas", listOf<Category?>(null) + cats,
                    { it?.name ?: "Todas" }, { catFilter = it?.name }
                )
            }
            item { Text("${filtered.size} movimentação(ões) • ordem ${if (sortDesc) "mais recentes primeiro" else "mais antigas primeiro"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (filtered.isEmpty()) item { EmptyState(if (txs.isEmpty()) "Nenhuma movimentação. Toque em + para criar." else "Nada encontrado com esses filtros.") }
            else items(filtered, key = { it.id }) { TransactionRow(it, hide) { selected = it } }
        }
        FloatingActionButton({ showNew = true }, Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Nova movimentação")
        }
    }

    if (showNew) TransactionDialog(null, cats, { showNew = false }) { t, apply -> vm.addTransaction(t, apply); showNew = false }
    editing?.let { e -> TransactionDialog(e, cats, { editing = null }) { t, _ -> vm.updateTransaction(t); editing = null } }
    selected?.let { t ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(t.description) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoRow("Valor", Fmt.money(t.amount))
                    InfoRow("Tipo", Fmt.typeLabel(t.type))
                    InfoRow("Categoria", t.category)
                    InfoRow("Data e hora", Fmt.dateTime(t.date))
                    InfoRow("Status", "Simulado")
                    if (t.note.isNotBlank()) InfoRow("Observação", t.note)
                }
            },
            confirmButton = { TextButton({ editing = t; selected = null }) { Text("Editar") } },
            dismissButton = {
                Row {
                    TextButton({ deleting = t; selected = null }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                    TextButton({ selected = null }) { Text("Fechar") }
                }
            }
        )
    }
    deleting?.let { t ->
        ConfirmDialog("Excluir movimentação?", "\"${t.description}\" será removida permanentemente.", "Excluir",
            onConfirm = { vm.deleteTransaction(t) }, onDismiss = { deleting = null })
    }
}

@Composable
fun TransactionDialog(
    initial: Transaction?, categories: List<Category>, onDismiss: () -> Unit, onSave: (Transaction, Boolean) -> Unit
) {
    val init = initial?.date?.toLocalDateTime() ?: LocalDateTime.now()
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var amount by remember { mutableStateOf(initial?.let { Fmt.moneyInput(it.amount) } ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: Transaction.OUT) }
    var cat by remember { mutableStateOf(initial?.category ?: categories.firstOrNull()?.name ?: "Outros") }
    var date by remember { mutableStateOf(Fmt.date(init.toLocalDate())) }
    var time by remember { mutableStateOf(init.toLocalTime().format(Fmt.TIME)) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var apply by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val cents = Fmt.parseMoney(amount)
    val d = Fmt.parseDate(date)
    val t = Fmt.parseTime(time)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova movimentação" else "Editar movimentação") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppField(desc, { desc = it }, "Descrição", if (showErrors && desc.isBlank()) "Informe uma descrição" else null)
                MoneyField(amount, { amount = it }, "Valor", if (showErrors && (cents == null || cents <= 0)) "Valor inválido" else null)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(type == Transaction.IN, { type = Transaction.IN }, label = { Text("Entrada") })
                    FilterChip(type == Transaction.OUT, { type = Transaction.OUT }, label = { Text("Saída") })
                }
                Dropdown("Categoria", cat, categories.map { it.name }.ifEmpty { listOf("Outros") }, { it }, { cat = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppField(date, { date = it }, "Data", if (showErrors && d == null) "dd/mm/aaaa" else null, KeyboardType.Number, Modifier.weight(1.4f))
                    AppField(time, { time = it }, "Hora", if (showErrors && t == null) "hh:mm" else null, KeyboardType.Number, Modifier.weight(1f))
                }
                AppField(note, { note = it }, "Observação (opcional)", singleLine = false)
                if (initial == null) Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(apply, { apply = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Aplicar ao saldo da conta", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton({
                showErrors = true
                if (desc.isNotBlank() && cents != null && cents > 0 && d != null && t != null) {
                    onSave(
                        Transaction(
                            id = initial?.id ?: 0, description = desc.trim(), amount = cents, type = type, category = cat,
                            date = LocalDateTime.of(d, t).toMillis(), status = "SIMULADO", note = note.trim()
                        ), apply
                    )
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }
    )
}
