package com.meteosa.app.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    @Test
    fun `badge tiers match the points thresholds`() {
        assertEquals(BadgeTier.NONE, badgeTierForPoints(0))
        assertEquals(BadgeTier.ROOKIE, badgeTierForPoints(1))
        assertEquals(BadgeTier.ROOKIE, badgeTierForPoints(49))
        assertEquals(BadgeTier.SCOUT, badgeTierForPoints(50))
        assertEquals(BadgeTier.SCOUT, badgeTierForPoints(199))
        assertEquals(BadgeTier.VETERAN, badgeTierForPoints(200))
        assertEquals(BadgeTier.LEGEND, badgeTierForPoints(500))
        assertEquals(BadgeTier.LEGEND, badgeTierForPoints(10_000))
    }
}
