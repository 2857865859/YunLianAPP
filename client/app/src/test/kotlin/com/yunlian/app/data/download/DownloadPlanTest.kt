package com.yunlian.app.data.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadPlanTest {
    @Test
    fun reusesPersistedPlanWhenThreadSettingChanges() {
        val plan = resolveDownloadPlan(
            totalSize = 100L * 1024 * 1024,
            threads = 2,
            persistedChunkCount = 128,
            persistedTotalSize = 100L * 1024 * 1024
        )

        assertTrue(plan.reused)
        assertEquals(128, plan.chunkCount)
    }

    @Test
    fun replacesPlanWhenServerSizeChanges() {
        val plan = resolveDownloadPlan(
            totalSize = 101L * 1024 * 1024,
            threads = 2,
            persistedChunkCount = 128,
            persistedTotalSize = 100L * 1024 * 1024
        )

        assertFalse(plan.reused)
        assertEquals(101L * 1024 * 1024, plan.totalSize)
    }
}
