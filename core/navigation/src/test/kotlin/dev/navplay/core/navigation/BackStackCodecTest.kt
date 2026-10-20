package dev.navplay.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Бэкстек обязан переживать process death — значит, ключи обязаны (де)сериализоваться полиморфно. */
class BackStackCodecTest {
    private val codec = BackStackCodec(
        listOf(navKeys { subclass(StubNavKey::class, StubNavKey.serializer()) }),
    )

    @Test
    fun `round trip keeps keys and order`() {
        val stack = listOf<NavKey>(StubNavKey("a", "x"), StubNavKey("b", "y"))

        assertEquals(stack, codec.decode(codec.encode(stack)))
    }

    @Test
    fun `empty stack round trips`() {
        assertEquals(emptyList<NavKey>(), codec.decode(codec.encode(emptyList())))
    }

    @Test
    fun `corrupted state does not crash, app just starts fresh`() {
        assertNull(codec.decodeOrNull("{not json"))
    }

    @Test
    fun `key of a feature that is no longer installed does not crash`() {
        val saved = codec.encode(listOf(StubNavKey("a", "x")))
        val withoutFeatures = BackStackCodec(emptyList())

        assertNull(withoutFeatures.decodeOrNull(saved))
    }
}
