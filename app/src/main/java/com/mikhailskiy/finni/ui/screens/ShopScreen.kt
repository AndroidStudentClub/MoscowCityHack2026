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
import com.mikhailskiy.finni.domain.BudgetCategory
import com.mikhailskiy.finni.domain.GameCatalog
import com.mikhailskiy.finni.domain.GameSnapshot
import com.mikhailskiy.finni.domain.ShopItem
import com.mikhailskiy.finni.ui.components.CoinPill
import com.mikhailskiy.finni.ui.components.CatalogIcon
import com.mikhailskiy.finni.ui.components.FinniCard
import com.mikhailskiy.finni.ui.components.ScreenHeader

@Composable
fun ShopScreen(
    snapshot: GameSnapshot,
    isBusy: Boolean,
    onBack: () -> Unit,
    onPurchase: (String) -> Unit,
) {
    var selected by remember { mutableStateOf<ShopItem?>(null) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { ScreenHeader("Магазин", "Перед покупкой проверяй цену и категорию", onBack) }
        item { CoinPill(snapshot.wallet.available, "Доступно", Modifier.fillMaxWidth()) }
        item { Text("🧺 Забота · обязательное", style = MaterialTheme.typography.titleLarge) }
        GameCatalog.shopItems.filter { it.category == BudgetCategory.REQUIRED }.forEach { item ->
            item { ShopItemCard(item, onClick = { selected = item }) }
        }
        item { Text("🎈 Радость · желаемое", style = MaterialTheme.typography.titleLarge) }
        GameCatalog.shopItems.filter { it.category == BudgetCategory.OPTIONAL }.forEach { item ->
            item { ShopItemCard(item, onClick = { selected = item }) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    selected?.let { item ->
        val remaining = snapshot.wallet.available - item.price
        AlertDialog(
            onDismissRequest = { selected = null },
            icon = {
                CatalogIcon(
                    catalogId = item.id,
                    fallbackEmoji = item.emoji,
                    modifier = Modifier.size(64.dp),
                    emojiSize = 42.sp,
                )
            },
            title = { Text(item.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.description)
                    Text("Категория: ${if (item.category == BudgetCategory.REQUIRED) "Забота · обязательное" else "Радость · желаемое"}")
                    Text("Цена: ${item.price} монет", fontWeight = FontWeight.Bold)
                    Text(
                        if (remaining >= 0) "После покупки останется $remaining"
                        else "Не хватает ${-remaining} монет",
                        color = if (remaining >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    )
                    Text("Влияние: ${effectText(item)}", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selected = null
                        onPurchase(item.id)
                    },
                    enabled = !isBusy,
                ) { Text("Купить") }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun ShopItemCard(item: ShopItem, onClick: () -> Unit) {
    FinniCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CatalogIcon(
                catalogId = item.id,
                fallbackEmoji = item.emoji,
                modifier = Modifier.size(58.dp),
                emojiSize = 40.sp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
                Text(effectText(item), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${item.price} 🪙", fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onClick) { Text("Выбрать") }
            }
        }
    }
}

private fun effectText(item: ShopItem): String = buildList {
    if (item.satietyDelta > 0) add("сытость +${item.satietyDelta}")
    if (item.moodDelta > 0) add("настроение +${item.moodDelta}")
    if (item.comfortDelta > 0) add("уют +${item.comfortDelta}")
    if (item.id == "shoes") add("порог расхода сытости +500 шагов")
    if (item.id == "jetpack") add("порог расхода сытости +1000 шагов")
}.joinToString(" · ")
