package dev.navplay.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UriPatternTest {
    private val pattern = UriPattern("navplay://profile/{userId}")

    @Test
    fun `extracts path argument`() {
        assertEquals(mapOf("userId" to "42"), pattern.match("navplay://profile/42"))
    }

    @Test
    fun `decodes url encoded values`() {
        assertEquals(mapOf("userId" to "a b"), pattern.match("navplay://profile/a%20b"))
    }

    @Test
    fun `query parameters are exposed as arguments`() {
        assertEquals(
            mapOf("userId" to "1", "ref" to "push"),
            pattern.match("navplay://profile/1?ref=push"),
        )
    }

    @Test
    fun `path argument wins over a query parameter with the same name`() {
        assertEquals(mapOf("userId" to "1"), pattern.match("navplay://profile/1?userId=999"))
    }

    @Test
    fun `scheme is matched case insensitively`() {
        assertEquals(mapOf("userId" to "1"), pattern.match("NAVPLAY://profile/1"))
    }

    @Test
    fun `wrong scheme, literal segment or segment count do not match`() {
        assertNull(pattern.match("https://profile/1"))
        assertNull(pattern.match("navplay://settings/1"))
        assertNull(pattern.match("navplay://profile"))
        assertNull(pattern.match("navplay://profile/1/extra"))
    }

    @Test
    fun `garbage is not a uri`() {
        assertNull(pattern.match("profile/1"))
        assertNull(pattern.match(""))
    }

    @Test
    fun `literal only pattern matches exactly`() {
        val auth = UriPattern("navplay://auth")
        assertEquals(emptyMap<String, String>(), auth.match("navplay://auth"))
        assertNull(auth.match("navplay://auth/1"))
    }
}

class DeepLinkResolverTest {
    private class ItemHandler : DeepLinkHandler {
        override val pattern = "navplay://item/{id}"
        override fun build(args: Map<String, String>): NavKey? =
            args["id"]?.takeIf { it.all(Char::isDigit) }?.let { TestKey(it) }
    }

    private class CatchAll : DeepLinkHandler {
        override val pattern = "navplay://item/{id}"
        override fun build(args: Map<String, String>): NavKey = TestKey("fallback")
    }

    @Test
    fun `parse helper turns uri into key for contract tests`() {
        assertEquals(TestKey("7"), ItemHandler().parse("navplay://item/7"))
    }

    @Test
    fun `handler may reject arguments it cannot use`() {
        assertNull(ItemHandler().parse("navplay://item/abc"))
    }

    @Test
    fun `resolver skips a handler that returned null and tries the next one`() {
        val resolver = DeepLinkResolver(listOf(ItemHandler(), CatchAll()))

        assertEquals(TestKey("5"), resolver.resolve("navplay://item/5"))
        assertEquals(TestKey("fallback"), resolver.resolve("navplay://item/abc"))
    }

    @Test
    fun `unknown uri resolves to null`() {
        assertNull(DeepLinkResolver(listOf(ItemHandler())).resolve("navplay://other"))
    }
}
