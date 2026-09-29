package com.mikhailskiy.finni.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikhailskiy.finni.R
import com.mikhailskiy.finni.domain.GameCatalog
import com.mikhailskiy.finni.domain.PetData
import com.mikhailskiy.finni.ui.UiMessage

@Composable
fun FinniCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
            ) {
                Text("←", fontSize = 28.sp)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CoinPill(amount: Int, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🪙 $amount", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PetAvatar(pet: PetData, modifier: Modifier = Modifier, large: Boolean = false) {
    val species = GameCatalog.species.firstOrNull { it.id == pet.speciesId } ?: GameCatalog.species.first()
    val color = GameCatalog.colors.firstOrNull { it.id == pet.colorId } ?: GameCatalog.colors.first()
    val accessoryId = if (pet.accessoryId == "scarf") "helmet" else pet.accessoryId
    val accessory = GameCatalog.accessories.firstOrNull { it.id == accessoryId }
    val avatarSize = if (large) 142.dp else 96.dp
    Box(
        modifier = modifier
            .size(avatarSize)
            .background(Color(color.argb), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PetArtwork(
            speciesId = species.id,
            fallbackEmoji = species.emoji,
            accessoryId = accessoryId,
            hasShoes = pet.hasShoes,
            hasGlasses = pet.hasGlasses,
            hasJetpack = pet.hasJetpack,
            large = large,
            modifier = Modifier.size(if (large) 136.dp else 90.dp),
        )
        if (accessoryId != "helmet" && !accessory?.emoji.isNullOrEmpty()) {
            Text(
                text = accessory?.emoji.orEmpty(),
                fontSize = if (large) 32.sp else 24.sp,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
fun PetArtwork(
    speciesId: String,
    fallbackEmoji: String,
    modifier: Modifier = Modifier,
    accessoryId: String? = null,
    hasShoes: Boolean = false,
    hasGlasses: Boolean = false,
    hasJetpack: Boolean = false,
    large: Boolean = false,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val wearsHelmet = accessoryId in setOf("helmet", "scarf")
        val supportsFullArtwork = speciesId in setOf("fox", "bear")
        val usesFullJetpackArtwork = hasJetpack && supportsFullArtwork
        val usesFullShoesArtwork = !usesFullJetpackArtwork && hasShoes && wearsHelmet && supportsFullArtwork
        val usesFullGlassesArtwork = !usesFullJetpackArtwork && hasGlasses && !wearsHelmet && supportsFullArtwork
        val usesFullHelmetArtwork = !usesFullJetpackArtwork && wearsHelmet && supportsFullArtwork
        val artwork = when {
            speciesId == "fox" && usesFullJetpackArtwork -> R.drawable.robot_fox_rover_jetpack to "Лис-ровер с реактивным рюкзаком"
            speciesId == "bear" && usesFullJetpackArtwork -> R.drawable.robot_bear_cargo_jetpack to "Медведь-карго с реактивным рюкзаком"
            speciesId == "fox" && usesFullShoesArtwork -> R.drawable.robot_fox_rover_shoes to "Лис-ровер в шлеме и энергообуви"
            speciesId == "bear" && usesFullShoesArtwork -> R.drawable.robot_bear_cargo_shoes to "Медведь-карго в шлеме и энергообуви"
            speciesId == "fox" && usesFullGlassesArtwork -> R.drawable.robot_fox_rover_glasses to "Лис-ровер в смарт-очках"
            speciesId == "bear" && usesFullGlassesArtwork -> R.drawable.robot_bear_cargo_glasses to "Медведь-карго в смарт-очках"
            speciesId == "fox" && wearsHelmet -> R.drawable.robot_fox_rover_helmet to "Лис-ровер в шлеме"
            speciesId == "bear" && wearsHelmet -> R.drawable.robot_bear_cargo_helmet to "Медведь-карго в шлеме"
            speciesId == "fox" -> R.drawable.robot_fox_rover to "Лис-ровер"
            speciesId == "cat" -> R.drawable.robot_fox_rover_mint to "Мятный лис-ровер"
            speciesId == "bear" -> R.drawable.robot_bear_cargo to "Медведь-карго"
            else -> null
        }
        if (artwork != null) {
            Image(
                painter = painterResource(artwork.first),
                contentDescription = artwork.second,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            if (hasJetpack && !usesFullJetpackArtwork) {
                Image(
                    painter = painterResource(R.drawable.item_jetpack),
                    contentDescription = "Реактивный рюкзак",
                    modifier = Modifier
                        .fillMaxSize(0.58f)
                        .align(Alignment.CenterEnd),
                    contentScale = ContentScale.Fit,
                )
            }
            if (wearsHelmet && !usesFullHelmetArtwork) {
                Image(
                    painter = painterResource(R.drawable.item_helmet),
                    contentDescription = "Защитный шлем",
                    modifier = Modifier
                        .fillMaxSize(0.68f)
                        .align(Alignment.TopCenter),
                    contentScale = ContentScale.Fit,
                )
            }
            if (hasShoes && !usesFullShoesArtwork) {
                Image(
                    painter = painterResource(R.drawable.item_shoes),
                    contentDescription = "Энергообувь",
                    modifier = Modifier
                        .fillMaxSize(0.66f)
                        .align(Alignment.BottomCenter),
                    contentScale = ContentScale.Fit,
                )
            }
            if (hasGlasses && !usesFullGlassesArtwork) {
                Image(
                    painter = painterResource(R.drawable.item_glasses),
                    contentDescription = "Смарт-очки",
                    modifier = Modifier
                        .fillMaxSize(0.66f)
                        .align(Alignment.TopCenter),
                    contentScale = ContentScale.Fit,
                )
            }
        } else {
            Text(
                text = fallbackEmoji,
                fontSize = if (large) 70.sp else 48.sp,
            )
        }
    }
}

@Composable
fun CatalogIcon(
    catalogId: String,
    fallbackEmoji: String,
    modifier: Modifier = Modifier,
    emojiSize: androidx.compose.ui.unit.TextUnit = 40.sp,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val rasterIcon = when (catalogId) {
            "water" -> R.drawable.item_oil
            "food" -> R.drawable.item_battery
            "brush" -> R.drawable.item_gas
            "helmet" -> R.drawable.item_helmet
            "glasses" -> R.drawable.item_glasses
            "headphones" -> R.drawable.item_headphones
            "shoes" -> R.drawable.item_shoes
            "jetpack" -> R.drawable.item_jetpack
            "backpack" -> R.drawable.goal_scooter
            "telescope" -> R.drawable.goal_motorcycle
            "car" -> R.drawable.goal_car
            else -> null
        }
        if (rasterIcon != null) {
            Image(
                painter = painterResource(rasterIcon),
                contentDescription = when (catalogId) {
                    "water" -> "Моторное масло"
                    "food" -> "Энергобатарея"
                    "brush" -> "Канистра топлива"
                    "helmet" -> "Защитный шлем"
                    "glasses" -> "Смарт-очки"
                    "headphones" -> "Стильные наушники"
                    "shoes" -> "Энергообувь"
                    "jetpack" -> "Реактивный рюкзак"
                    "backpack" -> "Реактивный самокат"
                    "telescope" -> "Реактивный мотоцикл"
                    "car" -> "Реактивная машина"
                    else -> null
                },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(fallbackEmoji, fontSize = emojiSize)
        }
    }
}

@Composable
fun PetStatus(label: String, value: Int, emoji: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$emoji $label", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        LinearProgressIndicator(
            progress = { value.coerceIn(0, 100) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.secondaryContainer,
        )
        Text(
            text = when {
                value >= 75 -> "Отлично"
                value >= 45 -> "Всё хорошо"
                else -> "Нужна забота"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun StepControl(
    title: String,
    emoji: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Surface(modifier = modifier, color = color, shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(emoji, fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text("$value монет", style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(
                onClick = { onValueChange((value - 5).coerceAtLeast(0)) },
                modifier = Modifier.size(48.dp),
                contentPadding = ButtonDefaults.ContentPadding,
            ) { Text("−", fontSize = 20.sp) }
            Button(
                onClick = { onValueChange(value + 5) },
                modifier = Modifier.size(48.dp),
                contentPadding = ButtonDefaults.ContentPadding,
            ) { Text("+", fontSize = 20.sp) }
        }
    }
}

@Composable
fun MessageBanner(message: UiMessage, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val container = if (message.isError) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (message.isError) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onSecondaryContainer
    Surface(modifier = modifier.fillMaxWidth(), color = container, shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(if (message.isError) "💡" else "✓", fontSize = 20.sp)
            Text(message.text, modifier = Modifier.weight(1f), color = contentColor)
            OutlinedButton(onClick = onDismiss) { Text("OK") }
        }
    }
}

@Composable
fun EmptyHint(emoji: String, title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(emoji, fontSize = 52.sp)
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
    }
}
