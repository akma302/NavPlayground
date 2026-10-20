package dev.navplay.feature.media.impl

import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.testing.FakeNavigator
import dev.navplay.core.navigation.testing.MainDispatcherRule
import dev.navplay.feature.media.api.MediaId
import dev.navplay.feature.media.api.MediaPickerNavKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MediaSelectionTest {
    @Test
    fun `tap selects and unselects`() {
        val one = MediaSelection.toggle(emptyList(), "a", maxCount = 2)
        assertEquals(listOf("a"), one)
        assertEquals(emptyList<String>(), MediaSelection.toggle(one, "a", maxCount = 2))
    }

    @Test
    fun `cannot select more than maxCount`() {
        val full = listOf("a", "b")

        assertEquals(full, MediaSelection.toggle(full, "c", maxCount = 2))
    }

    @Test
    fun `can unselect when full and then select another`() {
        val afterUnselect = MediaSelection.toggle(listOf("a", "b"), "a", maxCount = 2)

        assertEquals(listOf("b", "c"), MediaSelection.toggle(afterUnselect, "c", maxCount = 2))
    }
}

class MediaPickerViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val navigator = FakeNavigator()
    private val viewModel = MediaPickerViewModel(navigator)
    private val key = MediaPickerNavKey(maxCount = 3)

    @Test
    fun `confirm finishes with the typed result`() {
        viewModel.confirm(key, listOf("IMG_001", "IMG_002"))

        assertEquals(
            listOf<Pair<Any, Any>>(key to listOf(MediaId("IMG_001"), MediaId("IMG_002"))),
            navigator.finished,
        )
    }

    @Test
    fun `cancel is a plain Back - the caller learns it from Cancelled`() {
        viewModel.cancel()

        assertEquals(listOf<NavigationCommand>(NavigationCommand.Back), navigator.commands)
    }

    @Test
    fun `gallery is not empty`() {
        assertEquals(12, viewModel.gallery.size)
    }
}
