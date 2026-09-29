package com.mikhailskiy.finni.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikhailskiy.finni.domain.GameCatalog
import com.mikhailskiy.finni.domain.GameSnapshot
import com.mikhailskiy.finni.ui.components.CoinPill
import com.mikhailskiy.finni.ui.components.CatalogIcon
import com.mikhailskiy.finni.ui.components.FinniCard
import com.mikhailskiy.finni.ui.components.ScreenHeader
import kotlin.math.ceil

@Composable
fun SavingsScreen(
    snapshot: GameSnapshot,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSelectGoal: (String) -> Unit,
    onDeposit: (Int) -> Unit,
    onWithdraw: (Int) -> Unit,
) {
    var showWithdraw by remember { mutableStateOf(false) }
    val activeGoal = snapshot.goal?.let { progress -> GameCatalog.goal(progress.goalId) }
    val average = (snapshot.plan?.savings ?: 20).coerceAtLeast(5)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { ScreenHeader("Моя мечта", "Накопления хранятся отдельно от доступного баланса", onBack) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CoinPill(snapshot.wallet.available, "Доступно", Modifier.weight(1f))
                CoinPill(snapshot.wallet.savings, "Накоплено", Modifier.weight(1f))
            }
        }
        if (activeGoal != null) {
            item {
                val remaining = (activeGoal.target - snapshot.wallet.savings).coerceAtLeast(0)
                val periods = ceil(remaining.toDouble() / average).toInt()
                FinniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CatalogIcon(
                            catalogId = activeGoal.id,
                            fallbackEmoji = activeGoal.emoji,
                            modifier = Modifier.size(76.dp),
                            emojiSize = 52.sp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(activeGoal.title, style = MaterialTheme.typography.titleLarge)
                            Text("${snapshot.wallet.savings} из ${activeGoal.target} монет")
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (snapshot.wallet.savings.toFloat() / activeGoal.target).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        if (remaining == 0) "Цель достигнута!"
                        else "Осталось $remaining. Если откладывать по $average, понадобится примерно $periods периодов.",
                    )
                }
            }
            item {
                Text("Пополнить", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 20).forEach { amount ->
                        Button(
                            onClick = { onDeposit(amount) },
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f),
                        ) { Text("+$amount") }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showWithdraw = true },
                    enabled = snapshot.wallet.savings >= 10 && !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Вернуть 10 монет в баланс") }
            }
            item { Text("Можно выбрать другую цель. Уже накопленные монеты сохранятся.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            item {
                FinniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text("✨ Выбери понятную цель", style = MaterialTheme.typography.titleLarge)
                    Text("Стоимость известна заранее, поэтому легко увидеть прогресс.")
                }
            }
        }
        item { Text(if (activeGoal == null) "Доступные цели" else "Сменить цель", style = MaterialTheme.typography.titleLarge) }
        GameCatalog.goals.forEach { goal ->
            item {
                FinniCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CatalogIcon(
                            catalogId = goal.id,
                            fallbackEmoji = goal.emoji,
                            modifier = Modifier.size(58.dp),
                            emojiSize = 38.sp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(goal.title, style = MaterialTheme.typography.titleMedium)
                            Text("Стоимость: ${goal.target} монет")
                        }
                        OutlinedButton(
                            onClick = { onSelectGoal(goal.id) },
                            enabled = activeGoal?.id != goal.id && !isBusy,
                        ) { Text(if (activeGoal?.id == goal.id) "Выбрано" else "Выбрать") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showWithdraw && activeGoal != null) {
        AlertDialog(
            onDismissRequest = { showWithdraw = false },
            title = { Text("Вернуть 10 монет?") },
            text = {
                val after = snapshot.wallet.savings - 10
                val remaining = (activeGoal.target - after).coerceAtLeast(0)
                val periods = ceil(remaining.toDouble() / average).toInt()
                Text("В накоплениях останется $after из ${activeGoal.target}. До цели будет примерно $periods периодов.")
            },
            confirmButton = {
                Button(onClick = {
                    showWithdraw = false
                    onWithdraw(10)
                }) { Text("Подтвердить") }
            },
            dismissButton = {
                TextButton(onClick = { showWithdraw = false }) { Text("Оставить в мечте") }
            },
        )
    }
}
