package com.yunlian.app.data.network

import com.yunlian.app.BuildConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HttpClientsTest {

    @After
    fun resetIgnoreSsl() {
        HttpClients.ignoreSsl = false
    }

    @Test
    fun `ignored certificates can only be enabled in debug builds`() {
        HttpClients.ignoreSsl = true

        assertEquals(BuildConfig.DEBUG, HttpClients.ignoreSsl)
    }

    @Test
    fun `ignored certificates remain disabled by default`() {
        assertFalse(HttpClients.ignoreSsl)
    }
}
