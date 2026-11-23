package com.purplebank.simulator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val hide by vm.hideValues.collectAsStateWithLifecycle()
    val a = account ?: return
    var showNew by remember { mutableStateOf(false) }

    val month = YearMonth.now()
    val inMonth = txs.filter { YearMonth.from(it.date.toLocalDateTime()) == month }
    val totalIn = inMonth.filter { it.type == Transaction.IN }.sumOf { it.amount }
    val totalOut = inMonth.filter { it.type == Transaction.OUT }.sumOf { it.amount }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Conta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            SectionCard("Saldos (fictícios)") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saldo disponível")
                    MoneyText(a.balance, hide, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Poupança simulada")
                    MoneyText(a.savingsBalance, hide, fontWeight = FontWeight.Bold)
                }
                InfoRow("Atualizado em", Fmt.dateTime(a.updatedAt))
            }
        }
        item {
            SectionCard("Resumo do mês") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Entradas")
                    MoneyText(totalIn, hide, color = BankColors.In, fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saídas")
                    MoneyText(totalOut, hide, color = BankColors.Out, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button({ showNew = true }, Modifier.weight(1f)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Movimentação") }
                OutlinedButton({ onOpen("admin") }, Modifier.weight(1f)) { Icon(Icons.Filled.AdminPanelSettings, null); Spacer(Modifier.width(6.dp)); Text("Editar saldo") }
            }
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

    if (showNew) TransactionDialog(
        initial = null, categories = cats, onDismiss = { showNew = false },
        onSave = { t, apply -> vm.addTransaction(t, apply); showNew = false }
    )
}
