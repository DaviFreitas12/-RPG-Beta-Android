package com.example.rpgsombrio

import com.example.rpgsombrio.data.SheetAttr
import com.example.rpgsombrio.data.TraitEntry
import com.example.rpgsombrio.data.TraitKind
import com.example.rpgsombrio.data.traitCostLabel
import com.example.rpgsombrio.data.traitPointsSpent
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemsTest {
    private fun attr(min: Int? = null, max: Int? = null, base: Int = 10, divisor: Int = 2) =
        SheetAttr("a", "Atributo", "ATR", 10, true, min, max, base, divisor)

    @Test fun traitPointsAdvantagesSpendAndDisadvantagesRefund() {
        val traits = listOf(
            TraitEntry("1", "Aliado", TraitKind.ADVANTAGE, cost = 5),
            TraitEntry("2", "Reflexos", TraitKind.ADVANTAGE, cost = 3),
            TraitEntry("3", "Fobia", TraitKind.DISADVANTAGE, cost = 2)
        )
        assertEquals(6, traitPointsSpent(traits))
        assertEquals(0, traitPointsSpent(emptyList()))
    }

    @Test fun traitCostLabels() {
        assertEquals("Custa 4 pts", traitCostLabel(TraitEntry("1", kind = TraitKind.ADVANTAGE, cost = 4)))
        assertEquals("Devolve 4 pts", traitCostLabel(TraitEntry("2", kind = TraitKind.DISADVANTAGE, cost = 4)))
        assertEquals("Sem custo", traitCostLabel(TraitEntry("3", cost = 0)))
    }

    @Test fun modifierFollowsSystemConfiguration() {
        assertEquals(2, attr().modifierOf(14))      // D&D: (14 - 10) / 2
        assertEquals(-1, attr().modifierOf(9))      // piso, não truncamento
        assertEquals(3, attr(base = 0, divisor = 1).modifierOf(3))
        assertEquals(1, attr(base = 0, divisor = 0).modifierOf(1)) // divisor inválido vira 1
    }

    @Test fun clampAppliesMaxAndLowMinimumOnly() {
        assertEquals(5, attr(min = 1, max = 5).clamp(9))
        assertEquals(1, attr(min = 1, max = 5).clamp(0))
        assertEquals(1, attr(min = 3, max = 10).clamp(1)) // mínimos > 1 não "pulam" durante a digitação
        assertEquals(40, attr().clamp(40))
    }
}
