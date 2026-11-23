package com.purplebank.simulator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purplebank.simulator.ui.*
import com.purplebank.simulator.util.Billing
import com.purplebank.simulator.util.Fmt
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun DashboardScreen(vm: MainViewModel, onTab: (String) -> Unit, onOpen: (String) -> Unit) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    val card by vm.card.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val hide by vm.hideValues.collectAsStateWithLifecycle()
    val p = profile ?: return
    val a = account ?: return
    val c = card ?: return
    val bank = settings?.bankName ?: "PurpleBank"

    val today = LocalDate.now()
    val current = Billing.current(invoices, today)
    val nextDue = Billing.nextDue(invoices, today)
    val hour = LocalTime.now().hour
    val greeting = when { hour < 12 -> "Bom dia"; hour < 18 -> "Boa tarde"; else -> "Boa noite" }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(p.avatar, p.name, 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("$greeting,", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(p.name.substringBefore(' '), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(bank, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                IconButton({ vm.setHide(!hide) }) {
                    Icon(if (hide) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = "Ocultar ou mostrar valores")
                }
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth().clickable { onTab("account") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Saldo disponível", color = Color.White.copy(alpha = 0.85f))
                    MoneyText(a.balance, hide, style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Poupança simulada", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    MoneyText(a.savingsBalance, hide, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Shortcut(Icons.Filled.AccountBalanceWallet, "Conta") { onTab("account") }
                Shortcut(Icons.Filled.CreditCard, "Cartão") { onTab("card") }
                Shortcut(Icons.Filled.History, "Histórico") { onTab("history") }
                Shortcut(Icons.Filled.AdminPanelSettings, "Painel") { onOpen("admin") }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().clickable { onTab("card") }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cartão de crédito (simulado)", fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Limite disponível", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(c.availableLimit, hide, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fatura atual", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(current?.amount ?: 0L, hide, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Próximo vencimento", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(nextDue?.let { Fmt.date(it.dueDate) } ?: "—", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Últimas movimentações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton({ onTab("history") }) { Text("Ver tudo") }
            }
        }
        if (txs.isEmpty()) item { EmptyState("Nenhuma movimentação ainda.") }
        else items(txs.take(5), key = { it.id }) { TransactionRow(it, hide) { onTab("history") } }
    }
}

@Composable
private fun Shortcut(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
