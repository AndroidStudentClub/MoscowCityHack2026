package com.mikhailskiy.finni.domain

data class PetSpecies(
    val id: String,
    val title: String,
    val emoji: String,
)

data class PetColor(
    val id: String,
    val title: String,
    val argb: Long,
)

data class PetAccessory(
    val id: String,
    val title: String,
    val emoji: String,
)

data class ShopItem(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val price: Int,
    val category: BudgetCategory,
    val satietyDelta: Int = 0,
    val moodDelta: Int = 0,
    val comfortDelta: Int = 0,
)

data class SavingsGoal(
    val id: String,
    val title: String,
    val emoji: String,
    val target: Int,
)

data class TaskDefinition(
    val id: String,
    val title: String,
    val description: String,
    val topic: TaskTopic,
    val kind: TaskKind,
    val reward: Int,
    val successText: String,
    val retryText: String,
)

object GameCatalog {
    val species = listOf(
        PetSpecies("fox", "Лис-ровер", "🤖"),
        PetSpecies("cat", "Мятный лис-ровер", "🤖"),
        PetSpecies("bear", "Медведь-карго", "🤖"),
    )

    val colors = listOf(
        PetColor("mint", "Мятный", 0xFFC8F1E4),
        PetColor("sun", "Солнечный", 0xFFFFE49B),
        PetColor("berry", "Ягодный", 0xFFF4C8DB),
    )

    val accessories = listOf(
        PetAccessory("none", "Без аксессуара", ""),
        PetAccessory("helmet", "Шлем", "🪖"),
        PetAccessory("cap", "Кепка", "🧢"),
    )

    val shopItems = listOf(
        ShopItem("water", "Моторное масло", "Смазывает механизмы героя в каждом периоде", "🛢️", 10, BudgetCategory.REQUIRED, satietyDelta = 10),
        ShopItem("food", "Энергобатарея", "Главный источник энергии для героя", "🔋", 35, BudgetCategory.REQUIRED, satietyDelta = 30),
        ShopItem("brush", "Канистра топлива", "Поддерживает запас хода и надёжность механизмов", "⛽", 15, BudgetCategory.REQUIRED, comfortDelta = 20),
        ShopItem("helmet", "Защитный шлем", "Экипируется сразу после покупки", "🪖", 25, BudgetCategory.OPTIONAL, moodDelta = 22, comfortDelta = 8),
        ShopItem("glasses", "Смарт-очки", "Поднимают настроение и экипируются сразу", "🥽", 35, BudgetCategory.OPTIONAL, moodDelta = 30),
        ShopItem("headphones", "Стильные наушники", "Любимая музыка поднимает настроение героя", "🎧", 45, BudgetCategory.OPTIONAL, moodDelta = 25),
        ShopItem("shoes", "Энергообувь", "Вдвое замедляет расход сытости от шагов", "👟", 60, BudgetCategory.OPTIONAL, moodDelta = 28, comfortDelta = 12),
        ShopItem("jetpack", "Реактивный рюкзак", "Увеличивает путь до расхода сытости ещё на 1000 шагов", "🚀", 100, BudgetCategory.OPTIONAL, moodDelta = 35, comfortDelta = 15),
    )

    val goals = listOf(
        SavingsGoal("backpack", "Реактивный самокат", "🛴", 80),
        SavingsGoal("telescope", "Реактивный мотоцикл", "🏍️", 120),
        SavingsGoal("planetarium", "Поездка в планетарий", "🪐", 160),
        SavingsGoal("car", "Реактивная машина", "🚗", 200),
    )

    val tasks = listOf(
        TaskDefinition(
            "budget_60", "Разложи 60 монет", "Оставь не меньше 30 на заботу и 10 на мечту.",
            TaskTopic.BUDGET, TaskKind.BUDGET, 15,
            "Отличный план: нужное защищено, и мечта стала ближе!",
            "Проверь: на заботу нужно 30, на мечту — 10, а всего есть 60.",
        ),
        TaskDefinition(
            "budget_surprise", "Разрядилась батарея", "Пересобери бюджет: сначала 35 на новую батарею, затем остальное.",
            TaskTopic.BUDGET, TaskKind.BUDGET, 15,
            "Ты изменил план и сохранил самое важное.",
            "Попробуй перенести часть желаемого в кармашек заботы.",
        ),
        TaskDefinition(
            "saving_steps", "Дорога к мотоциклу", "Собери три регулярных взноса общей суммой не меньше 30.",
            TaskTopic.SAVINGS, TaskKind.SAVINGS, 15,
            "Три небольших взноса работают лучше одного обещания!",
            "Выбери не меньше трёх взносов и собери 30 монет.",
        ),
        TaskDefinition(
            "saving_keep", "Сохранить мечту", "Выбери регулярные пополнения вместо одной случайной траты.",
            TaskTopic.SAVINGS, TaskKind.SAVINGS, 10,
            "Регулярность помогает заранее понять путь к цели.",
            "Добавь ещё один небольшой взнос — важна регулярность.",
        ),
        TaskDefinition(
            "basket_breakfast", "Заряди героя", "Выбери моторное масло и энергобатарею, уложившись в 50 монет.",
            TaskTopic.PURCHASES, TaskKind.BASKET, 15,
            "Сначала необходимое — масло и батарея поместились в бюджет.",
            "В корзине должны быть моторное масло и энергобатарея, а сумма — не больше 50.",
        ),
        TaskDefinition(
            "basket_care", "Набор заботы", "Выбери минимум два нужных предмета и не трать больше 55.",
            TaskTopic.PURCHASES, TaskKind.BASKET, 10,
            "Ты сравнил покупки и собрал полезный набор.",
            "Нужно два предмета заботы, а лимит — 55 монет.",
        ),
    )

    fun shopItem(id: String): ShopItem? = shopItems.firstOrNull { it.id == id }
    fun goal(id: String): SavingsGoal? = goals.firstOrNull { it.id == id }
    fun task(id: String): TaskDefinition? = tasks.firstOrNull { it.id == id }
}
