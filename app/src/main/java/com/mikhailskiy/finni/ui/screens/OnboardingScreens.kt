package com.mikhailskiy.finni.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikhailskiy.finni.domain.GameCatalog
import com.mikhailskiy.finni.ui.components.FinniCard
import com.mikhailskiy.finni.ui.components.CatalogIcon
import com.mikhailskiy.finni.ui.components.PetArtwork
import com.mikhailskiy.finni.ui.components.ScreenHeader

@Composable
fun IntroScreen(
    isBusy: Boolean,
    onStart: () -> Unit,
    onDemo: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(28.dp)) }
        item {
            Text(
                "Финни",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Три кармашка для умных решений",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "Заботься о питомце, планируй монеты и копи на мечту.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            OnboardingCard("🧺", "Сначала нужное", "Батарея, масло и топливо — кармашек «Забота».", MaterialTheme.colorScheme.secondaryContainer)
        }
        item {
            OnboardingCard("🎈", "Потом желаемое", "Игрушки радуют, но их можно перенести.", Color(0xFFFFE8B1))
        }
        item {
            OnboardingCard("✨", "Понемногу на мечту", "Регулярные взносы приближают большую цель.", MaterialTheme.colorScheme.primaryContainer)
        }
        item {
            Button(
                onClick = onStart,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) { Text("Создать питомца") }
        }
        item {
            OutlinedButton(
                onClick = onDemo,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text("Демо для эксперта") }
        }
        item {
            Text(
                "Без регистрации, рекламы и реальных денег",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun OnboardingCard(emoji: String, title: String, text: String, color: Color) {
    FinniCard(modifier = Modifier.fillMaxWidth(), containerColor = color) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(emoji, fontSize = 36.sp)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun CreateProfileScreen(
    isBusy: Boolean,
    onBack: () -> Unit,
    onContinue: (String) -> Unit,
) {
    var nickname by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenHeader("Твой игровой профиль", "Настоящее имя не нужно", onBack)
        FinniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Text("⭐", fontSize = 48.sp)
            Text("Как будем тебя называть в игре?", style = MaterialTheme.typography.titleLarge)
            Text("Псевдоним хранится только на этом устройстве.")
        }
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it.take(16) },
            label = { Text("Игровое имя") },
            placeholder = { Text("Например, Искорка") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onContinue(nickname) },
            enabled = nickname.isNotBlank() && !isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) { Text("Дальше") }
    }
}

@Composable
fun CreatePetScreen(
    isBusy: Boolean,
    onBack: () -> Unit,
    onCreate: (String, String, String, String) -> Unit,
) {
    var petName by remember { mutableStateOf("") }
    var speciesId by remember { mutableStateOf(GameCatalog.species.first().id) }
    var colorId by remember { mutableStateOf(GameCatalog.colors.first().id) }
    var accessoryId by remember { mutableStateOf(GameCatalog.accessories.first().id) }
    val species = GameCatalog.species.first { it.id == speciesId }
    val color = GameCatalog.colors.first { it.id == colorId }
    val accessory = GameCatalog.accessories.first { it.id == accessoryId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(12.dp)) }
        item { ScreenHeader("Создай питомца", "Можно получить больше 9 комбинаций", onBack) }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(color.argb), MaterialTheme.shapes.extraLarge),
                contentAlignment = Alignment.Center,
            ) {
                PetArtwork(
                    speciesId = species.id,
                    fallbackEmoji = species.emoji,
                    accessoryId = accessory.id,
                    large = true,
                    modifier = Modifier.size(172.dp),
                )
                if (accessory.id != "helmet") {
                    Text(accessory.emoji, fontSize = 38.sp, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
        item {
            OutlinedTextField(
                value = petName,
                onValueChange = { petName = it.take(16) },
                label = { Text("Имя питомца") },
                placeholder = { Text("Например, Плюш") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            ChoiceSection("Кто это?") {
                GameCatalog.species.forEach { item ->
                    FilterChip(
                        selected = speciesId == item.id,
                        onClick = { speciesId = item.id },
                        label = { Text("${item.emoji} ${item.title}") },
                    )
                }
            }
        }
        item {
            ChoiceSection("Цвет") {
                GameCatalog.colors.forEach { item ->
                    FilterChip(
                        selected = colorId == item.id,
                        onClick = { colorId = item.id },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(18.dp).background(Color(item.argb), CircleShape))
                                Text(item.title)
                            }
                        },
                    )
                }
            }
        }
        item {
            ChoiceSection("Аксессуар") {
                GameCatalog.accessories.forEach { item ->
                    FilterChip(
                        selected = accessoryId == item.id,
                        onClick = { accessoryId = item.id },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (item.id != "none") {
                                    CatalogIcon(
                                        catalogId = item.id,
                                        fallbackEmoji = item.emoji,
                                        modifier = Modifier.size(24.dp),
                                        emojiSize = 20.sp,
                                    )
                                }
                                Text(item.title)
                            }
                        },
                    )
                }
            }
        }
        item {
            Button(
                onClick = { onCreate(petName, speciesId, colorId, accessoryId) },
                enabled = petName.isNotBlank() && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) { Text("Начать приключение") }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ChoiceSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(12.dp)) }
        item { ScreenHeader("Как играть", "Подсказку можно открыть в любой момент", onBack) }
        item { OnboardingCard("🧺", "Забота", "Сюда относятся батарея, масло и топливо. Они нужны регулярно.", MaterialTheme.colorScheme.secondaryContainer) }
        item { OnboardingCard("🎈", "Радость", "Игрушки и украшения улучшают настроение, но покупку можно отложить.", Color(0xFFFFE8B1)) }
        item { OnboardingCard("✨", "Мечта", "Отложенные монеты хранятся отдельно и приближают выбранную цель.", MaterialTheme.colorScheme.primaryContainer) }
        item {
            FinniCard {
                Text("Главное правило", style = MaterialTheme.typography.titleLarge)
                Text("Сначала составь план, затем сравни его с покупками. Ошибку всегда можно исправить в следующем периоде.")
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
