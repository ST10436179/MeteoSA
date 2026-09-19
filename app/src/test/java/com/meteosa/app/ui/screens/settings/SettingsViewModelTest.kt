package com.meteosa.app.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    @Test
    fun `badge tiers match the points thresholds`() {
        assertEquals("Not yet a Storm Chaser", badgeForPoints(0))
        assertEquals("Storm Chaser: Rookie", badgeForPoints(1))
        assertEquals("Storm Chaser: Rookie", badgeForPoints(49))
        assertEquals("Storm Chaser: Scout", badgeForPoints(50))
        assertEquals("Storm Chaser: Scout", badgeForPoints(199))
        assertEquals("Storm Chaser: Veteran", badgeForPoints(200))
        assertEquals("Storm Chaser: Legend", badgeForPoints(500))
        assertEquals("Storm Chaser: Legend", badgeForPoints(10_000))
    }
}
