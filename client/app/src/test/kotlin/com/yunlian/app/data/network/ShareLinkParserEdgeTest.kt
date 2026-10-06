package com.yunlian.app.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareLinkParserEdgeTest {
    @Test
    fun parsesExtendedAndUrlEncodedPassword() {
        val result = ShareLinkParser.parse("https://pan.quark.cn/s/abc123?pwd=ab%5F12-xyz")
        assertEquals("ab_12-xyz", result?.pwd)
    }
}
