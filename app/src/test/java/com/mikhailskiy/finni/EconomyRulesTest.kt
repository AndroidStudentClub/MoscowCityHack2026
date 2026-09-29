package com.mikhailskiy.finni

import com.mikhailskiy.finni.domain.BudgetCategory
import com.mikhailskiy.finni.domain.BudgetPlanData
import com.mikhailskiy.finni.domain.EconomyRules
import com.mikhailskiy.finni.domain.GameCatalog
import com.mikhailskiy.finni.domain.GrowthStage
import com.mikhailskiy.finni.domain.PlanStatus
import com.mikhailskiy.finni.domain.StepSatietyRules
import com.mikhailskiy.finni.domain.TaskTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyRulesTest {
    private val plan = BudgetPlanData(
        periodId = "period",
        baseAvailable = 100,
        required = 50,
        optional = 25,
        savings = 20,
        buffer = 5,
        status = PlanStatus.CONFIRMED,
    )

    @Test
    fun validPlanDoesNotExceedBalance() {
        assertNull(EconomyRules.validatePlan(100, 50, 25, 20))
        assertNotNull(EconomyRules.validatePlan(100, 60, 30, 20))
    }

    @Test
    fun negativePlanValueIsRejected() {
        assertNotNull(EconomyRules.validatePlan(100, -5, 25, 20))
    }

    @Test
    fun consistentPeriodAwardsTwentyGrowthPoints() {
        val result = EconomyRules.growthAward(
            requiredPurchases = 1,
            requiredActual = 35,
            optionalActual = 15,
            savingsDeposited = 20,
            plan = plan,
        )
        assertEquals(20, result)
    }

    @Test
    fun mistakesNeverSubtractGrowth() {
        val result = EconomyRules.growthAward(
            requiredPurchases = 0,
            requiredActual = 0,
            optionalActual = 40,
            savingsDeposited = 0,
            plan = plan,
        )
        assertTrue(result in 0..20)
    }

    @Test
    fun growthStageBoundariesAreStable() {
        assertEquals(GrowthStage.BABY, GrowthStage.fromPoints(29))
        assertEquals(GrowthStage.EXPLORER, GrowthStage.fromPoints(30))
        assertEquals(GrowthStage.EXPLORER, GrowthStage.fromPoints(69))
        assertEquals(GrowthStage.EXPERT, GrowthStage.fromPoints(70))
    }

    @Test
    fun demoContentMeetsMinimumVolume() {
        assertTrue(GameCatalog.species.size * GameCatalog.colors.size >= 9)
        assertTrue(GameCatalog.shopItems.size >= 8)
        assertTrue(GameCatalog.goals.size >= 4)
        assertTrue(GameCatalog.tasks.size >= 6)
        TaskTopic.entries.forEach { topic ->
            assertTrue(GameCatalog.tasks.count { it.topic == topic } >= 2)
        }
    }

    @Test
    fun removedShopItemsAreNotInCatalog() {
        listOf("bed", "sticker", "ball", "lamp").forEach { itemId ->
            assertNull(GameCatalog.shopItem(itemId))
        }
    }

    @Test
    fun transportGoalsHaveIncreasingPrices() {
        assertEquals("Реактивный самокат", GameCatalog.goal("backpack")?.title)
        assertEquals(80, GameCatalog.goal("backpack")?.target)
        assertEquals("Реактивный мотоцикл", GameCatalog.goal("telescope")?.title)
        assertEquals(120, GameCatalog.goal("telescope")?.target)
        assertEquals("Реактивная машина", GameCatalog.goal("car")?.title)
        assertEquals(200, GameCatalog.goal("car")?.target)
    }

    @Test
    fun glassesAreAOneTimeJoyItemWithMoodEffect() {
        val glasses = GameCatalog.shopItem("glasses")
        assertNotNull(glasses)
        assertEquals(BudgetCategory.OPTIONAL, glasses?.category)
        assertEquals(35, glasses?.price)
        assertEquals(30, glasses?.moodDelta)
    }

    @Test
    fun headphonesAreAJoyItemWithMoodEffect() {
        val headphones = GameCatalog.shopItem("headphones")
        assertNotNull(headphones)
        assertEquals("Стильные наушники", headphones?.title)
        assertEquals(BudgetCategory.OPTIONAL, headphones?.category)
        assertEquals(45, headphones?.price)
        assertEquals(25, headphones?.moodDelta)
    }

    @Test
    fun jetpackIsTheMostExpensiveStepUpgrade() {
        val jetpack = GameCatalog.shopItem("jetpack")
        assertNotNull(jetpack)
        assertEquals(BudgetCategory.OPTIONAL, jetpack?.category)
        assertEquals(100, jetpack?.price)
        assertTrue(jetpack!!.price > GameCatalog.shopItem("shoes")!!.price)
    }

    @Test
    fun stepsReduceSatietyAtStableIntervals() {
        val firstWalk = StepSatietyRules.calculate(pendingSteps = 0, newSteps = 1_240)
        assertEquals(2, firstWalk.satietyLoss)
        assertEquals(240, firstWalk.remainingSteps)

        val nextWalk = StepSatietyRules.calculate(
            pendingSteps = firstWalk.remainingSteps,
            newSteps = 260,
        )
        assertEquals(1, nextWalk.satietyLoss)
        assertEquals(0, nextWalk.remainingSteps)
    }

    @Test
    fun invalidStepValuesNeverCreateNegativeLoss() {
        val impact = StepSatietyRules.calculate(pendingSteps = -10, newSteps = -100)
        assertEquals(0, impact.satietyLoss)
        assertEquals(0, impact.remainingSteps)
    }

    @Test
    fun energyShoesDoubleStepsBeforeSatietyLoss() {
        val impact = StepSatietyRules.calculate(
            pendingSteps = 0,
            newSteps = 1_240,
            stepsPerSatietyPoint = StepSatietyRules.SHOES_STEPS_PER_SATIETY_POINT,
        )
        assertEquals(1, impact.satietyLoss)
        assertEquals(240, impact.remainingSteps)
    }

    @Test
    fun stepEquipmentBonusesStack() {
        assertEquals(
            StepSatietyRules.BASE_STEPS_PER_SATIETY_POINT,
            StepSatietyRules.threshold(hasShoes = false, hasJetpack = false),
        )
        assertEquals(
            StepSatietyRules.SHOES_STEPS_PER_SATIETY_POINT,
            StepSatietyRules.threshold(hasShoes = true, hasJetpack = false),
        )
        assertEquals(
            StepSatietyRules.JETPACK_STEPS_PER_SATIETY_POINT,
            StepSatietyRules.threshold(hasShoes = false, hasJetpack = true),
        )
        assertEquals(
            StepSatietyRules.SHOES_AND_JETPACK_STEPS_PER_SATIETY_POINT,
            StepSatietyRules.threshold(hasShoes = true, hasJetpack = true),
        )
    }
}
