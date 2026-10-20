package dev.navplay.core.navigation

import dev.navplay.core.navigation.middleware.AnalyticsMiddleware
import dev.navplay.core.navigation.middleware.DeepLinkResolutionMiddleware
import dev.navplay.core.navigation.middleware.DeepLinkValidationMiddleware
import dev.navplay.core.navigation.middleware.FeatureFlagMiddleware
import dev.navplay.core.navigation.middleware.ThrottlingMiddleware
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThrottlingMiddlewareTest {
    private var now = 1_000L
    private val log = NavigationLog(clock = { now })
    private val recorder = Recorder()
    private fun pipeline() = NavigationPipeline(
        listOf(ThrottlingMiddleware(log, windowMillis = 300, clock = { now })),
        recorder.terminal,
    )

    @Test
    fun `identical command inside the window is dropped`() = runBlocking {
        val pipeline = pipeline()
        val open = NavigationCommand.Forward(TestKey("1"))

        pipeline.run(open)
        now += 100
        pipeline.run(open)

        assertEquals(listOf<NavigationCommand>(open), recorder.commands)
        assertTrue(log.entries.value.any { it.source == "throttle" })
    }

    @Test
    fun `identical command after the window passes`() = runBlocking {
        val pipeline = pipeline()
        val open = NavigationCommand.Forward(TestKey("1"))

        pipeline.run(open)
        now += 300
        pipeline.run(open)

        assertEquals(2, recorder.commands.size)
    }

    @Test
    fun `different commands are not throttled`() = runBlocking {
        val pipeline = pipeline()

        pipeline.run(NavigationCommand.Forward(TestKey("1")))
        pipeline.run(NavigationCommand.Forward(TestKey("2")))
        pipeline.run(NavigationCommand.Back)

        assertEquals(3, recorder.commands.size)
    }

    @Test
    fun `double tap on Back closes only one screen`() = runBlocking {
        val pipeline = pipeline()

        pipeline.run(NavigationCommand.Back)
        pipeline.run(NavigationCommand.Back)

        assertEquals(1, recorder.commands.size)
    }
}

class FeatureFlagMiddlewareTest {
    private val flags = InMemoryFeatureFlags(initiallyDisabled = setOf("beta"))
    private val log = NavigationLog()
    private val recorder = Recorder()
    private val pipeline = NavigationPipeline(listOf(FeatureFlagMiddleware(flags, log)), recorder.terminal)

    @Test
    fun `disabled feature is replaced with a stub and the command type is kept`() = runBlocking {
        pipeline.run(NavigationCommand.Replace(GatedKey("1", featureId = "beta")))

        val command = recorder.commands.single()
        assertTrue(command is NavigationCommand.Replace)
        val stub = (command as NavigationCommand.Replace).key as StubNavKey
        assertEquals("beta", stub.feature)
    }

    @Test
    fun `enabled feature passes untouched`() = runBlocking {
        val command = NavigationCommand.Forward(GatedKey("1", featureId = "stable"))

        pipeline.run(command)

        assertEquals(listOf<NavigationCommand>(command), recorder.commands)
    }

    @Test
    fun `flag can be switched at runtime`() = runBlocking {
        val command = NavigationCommand.Forward(GatedKey("1", featureId = "beta"))
        flags.setEnabled("beta", true)

        pipeline.run(command)

        assertEquals(listOf<NavigationCommand>(command), recorder.commands)
    }

    @Test
    fun `non gated keys and non target commands are ignored`() = runBlocking {
        pipeline.run(NavigationCommand.Forward(TestKey("1")))
        pipeline.run(NavigationCommand.Back)

        assertEquals(2, recorder.commands.size)
    }
}

class DeepLinkMiddlewareTest {
    private class Internal : DeepLinkHandler {
        override val pattern = "navplay://secret"
        override val exported = false
        override fun build(args: Map<String, String>): NavKey = TestKey("secret")
    }

    private class Item : DeepLinkHandler {
        override val pattern = "navplay://item/{id}"
        override fun build(args: Map<String, String>): NavKey? = args["id"]?.let { TestKey(it) }
    }

    private val resolver = DeepLinkResolver(listOf(Internal(), Item()))
    private val log = NavigationLog()
    private val recorder = Recorder()
    private val pipeline = NavigationPipeline(
        listOf(
            DeepLinkValidationMiddleware(resolver, log, allowedSchemes = setOf("navplay")),
            DeepLinkResolutionMiddleware(resolver, log),
        ),
        recorder.terminal,
    )

    @Test
    fun `valid external link becomes a plain Forward`() = runBlocking {
        pipeline.run(NavigationCommand.DeepLink("navplay://item/42"))

        assertEquals(
            listOf<NavigationCommand>(NavigationCommand.Forward(TestKey("42"))),
            recorder.commands,
        )
    }

    @Test
    fun `external link with a foreign scheme is rejected`() = runBlocking {
        pipeline.run(NavigationCommand.DeepLink("https://evil.example/item/1"))

        assertTrue(recorder.commands.isEmpty())
    }

    @Test
    fun `external link to an internal-only screen is rejected`() = runBlocking {
        pipeline.run(NavigationCommand.DeepLink("navplay://secret"))

        assertTrue(recorder.commands.isEmpty())
    }

    @Test
    fun `internal link to an internal-only screen is allowed`() = runBlocking {
        pipeline.run(NavigationCommand.DeepLink("navplay://secret", DeepLinkSource.Internal))

        assertEquals(
            listOf<NavigationCommand>(NavigationCommand.Forward(TestKey("secret"))),
            recorder.commands,
        )
    }

    @Test
    fun `unknown path is rejected`() = runBlocking {
        pipeline.run(NavigationCommand.DeepLink("navplay://nothing/here"))

        assertTrue(recorder.commands.isEmpty())
    }

    @Test
    fun `non deep link commands pass through`() = runBlocking {
        pipeline.run(NavigationCommand.Back)

        assertEquals(listOf<NavigationCommand>(NavigationCommand.Back), recorder.commands)
    }
}

class AnalyticsMiddlewareTest {
    private class Spy : NavigationAnalytics {
        val events = mutableListOf<Pair<String, String>>()
        override fun trackNavigation(destination: String, command: String) {
            events += destination to command
        }
    }

    @Test
    fun `tracks destination of target commands only and always proceeds`() = runBlocking {
        val spy = Spy()
        val recorder = Recorder()
        val pipeline = NavigationPipeline(listOf(AnalyticsMiddleware(spy)), recorder.terminal)

        pipeline.run(NavigationCommand.Forward(TestKey("1")))
        pipeline.run(NavigationCommand.Back)

        assertEquals(listOf("TestKey" to "Forward"), spy.events)
        assertEquals(2, recorder.commands.size)
    }
}
