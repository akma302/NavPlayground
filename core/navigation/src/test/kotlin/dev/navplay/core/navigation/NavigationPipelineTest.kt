package dev.navplay.core.navigation

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPipelineTest {

    @Test
    fun `middleware run in order and unwind like an onion`() = runBlocking {
        val trace = mutableListOf<String>()
        val recorder = Recorder()
        val pipeline = NavigationPipeline(
            // Намеренно перемешаны: порядок задаётся order, а не порядком в коллекции (Set в DI его не гарантирует).
            listOf(
                TracingMiddleware("c", order = 30, trace),
                TracingMiddleware("a", order = 10, trace),
                TracingMiddleware("b", order = 20, trace),
            ),
            terminal = { command ->
                trace += "terminal"
                recorder.terminal(command)
            },
        )

        pipeline.run(NavigationCommand.Back)

        assertEquals(
            listOf("a:in", "b:in", "c:in", "terminal", "c:out", "b:out", "a:out"),
            trace,
        )
        assertEquals(listOf<NavigationCommand>(NavigationCommand.Back), recorder.commands)
    }

    @Test
    fun `middleware that does not proceed cancels the command`() = runBlocking {
        val recorder = Recorder()
        val dropper = object : NavigationMiddleware {
            override suspend fun intercept(
                command: NavigationCommand,
                proceed: suspend (NavigationCommand) -> Unit,
            ) = Unit
        }
        val pipeline = NavigationPipeline(listOf(dropper), recorder.terminal)

        pipeline.run(NavigationCommand.Forward(TestKey("1")))

        assertTrue(recorder.commands.isEmpty())
    }

    @Test
    fun `replaced command is what downstream sees`() = runBlocking {
        val recorder = Recorder()
        val rewriter = object : NavigationMiddleware {
            override val order = 0
            override suspend fun intercept(
                command: NavigationCommand,
                proceed: suspend (NavigationCommand) -> Unit,
            ) = proceed(NavigationCommand.Forward(TestKey("rewritten")))
        }
        val seenDownstream = mutableListOf<NavigationCommand>()
        val spy = object : NavigationMiddleware {
            override val order = 1
            override suspend fun intercept(
                command: NavigationCommand,
                proceed: suspend (NavigationCommand) -> Unit,
            ) {
                seenDownstream += command
                proceed(command)
            }
        }
        val pipeline = NavigationPipeline(listOf(spy, rewriter), recorder.terminal)

        pipeline.run(NavigationCommand.Forward(TestKey("original")))

        val expected = NavigationCommand.Forward(TestKey("rewritten"))
        assertEquals(listOf<NavigationCommand>(expected), seenDownstream)
        assertEquals(listOf<NavigationCommand>(expected), recorder.commands)
    }

    @Test
    fun `no middleware means terminal receives the command as is`() = runBlocking {
        val recorder = Recorder()
        val command = NavigationCommand.Replace(TestKey("x"))

        NavigationPipeline(emptyList(), recorder.terminal).run(command)

        assertEquals(listOf<NavigationCommand>(command), recorder.commands)
    }
}
