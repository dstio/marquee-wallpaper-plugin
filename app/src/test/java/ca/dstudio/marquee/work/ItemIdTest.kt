package ca.dstudio.marquee.work

import ca.dstudio.marquee.catalog.Lang
import ca.dstudio.marquee.catalog.TitleDetails
import ca.dstudio.marquee.compose.Layout
import ca.dstudio.marquee.compose.Look
import ca.dstudio.marquee.compose.Scene
import ca.dstudio.marquee.compose.TextPosition
import ca.dstudio.marquee.compose.shownLook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.File

/** A picture's id names its content: what changes the pixels changes the id, nothing else does. */
class ItemIdTest {

    private val details = TitleDetails("movie", 42, "Northbound", releaseDate = "2024-03-01", overview = "Two brothers.", backdrop = "/b.jpg")
    private val scene = Scene(details, "Trending this week", 0, 12, File("b.jpg"), File("l.png"), emptyList(), -1)
    private val look = Look(Layout.CINEMATIC, TextPosition.TOP_LEFT, 3, true)
    private val lang = Lang("en-US", "en", "US")

    private fun id(scene: Scene = this.scene, look: Look = this.look, w: Int = 1920) =
        Generator.itemId(scene, look.layout, shownLook(look.layout, look), lang, w, w * 9 / 16, scene.logo, emptyList())

    @Test
    fun theSameInputsKeepTheSameId() {
        assertEquals(id(), id())
        assertEquals(24, id().length)
    }

    @Test
    fun whatThePictureShowsChangesTheId() {
        assertNotEquals(id(), id(look = look.copy(position = TextPosition.TOP_RIGHT)))
        assertNotEquals(id(), id(look = look.copy(layout = Layout.MINIMAL)))
        assertNotEquals(id(), id(look = look.copy(synopsisLines = 2)))
        assertNotEquals(id(), id(w = 3840))
        assertNotEquals(id(), id(scene = scene.copy(details = details.copy(overview = "Three brothers."))))
        assertNotEquals(id(), id(scene = scene.copy(details = details.copy(streaming = listOf("Netflix")))))
    }

    @Test
    fun bottomLeftOnALayoutWithoutItIsTheTopLeftPicture() {
        val minimal = look.copy(layout = Layout.MINIMAL)
        assertEquals(id(look = minimal), id(look = minimal.copy(position = TextPosition.BOTTOM_PANEL)))
        assertNotEquals(id(), id(look = look.copy(position = TextPosition.BOTTOM_PANEL)))
    }
}
