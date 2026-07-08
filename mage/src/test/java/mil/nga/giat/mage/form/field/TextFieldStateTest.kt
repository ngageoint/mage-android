package mil.nga.giat.mage.form.field

import mil.nga.giat.mage.form.FieldType
import mil.nga.giat.mage.form.TextFormField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TextFieldStateTest {

    private lateinit var state: TextFieldState

    @Before
    fun setUp() {
        state = TextFieldState(
            TextFormField(id = 1, type = FieldType.TEXTFIELD, name = "field", title = "Field", required = false, archived = false)
        )
    }

    @Test
    fun `canUndo is false initially`() {
        assertFalse(state.canUndo)
    }

    @Test
    fun `canRedo is false initially`() {
        assertFalse(state.canRedo)
    }

    @Test
    fun `answer is null initially`() {
        assertNull(state.answer)
    }

    @Test
    fun `pushHistory enables canUndo`() {
        state.pushHistory("hello")
        assertTrue(state.canUndo)
    }

    @Test
    fun `pushHistory clears redo stack`() {
        state.answer = FieldValue.Text("a")
        state.pushHistory("a")
        state.answer = FieldValue.Text("ab")
        state.undo()
        assertTrue(state.canRedo)

        state.pushHistory("a")
        assertFalse(state.canRedo)
    }

    @Test
    fun `undo restores previous value`() {
        state.answer = FieldValue.Text("hello")
        state.pushHistory("hello")
        state.answer = FieldValue.Text("hello world")

        state.undo()

        assertEquals("hello", state.answer?.text)
    }

    @Test
    fun `undo enables canRedo`() {
        state.answer = FieldValue.Text("a")
        state.pushHistory("a")
        state.answer = FieldValue.Text("ab")

        state.undo()

        assertTrue(state.canRedo)
    }

    @Test
    fun `canUndo is false after undoing all history`() {
        state.answer = FieldValue.Text("a")
        state.pushHistory("a")
        state.answer = FieldValue.Text("ab")

        state.undo()

        assertFalse(state.canUndo)
    }

    @Test
    fun `undo does nothing when stack is empty`() {
        state.answer = FieldValue.Text("hello")
        state.undo()
        assertEquals("hello", state.answer?.text)
    }

    @Test
    fun `undo supports multiple steps`() {
        state.answer = FieldValue.Text("a")
        state.pushHistory("a")
        state.answer = FieldValue.Text("ab")
        state.pushHistory("ab")
        state.answer = FieldValue.Text("abc")

        state.undo()
        assertEquals("ab", state.answer?.text)

        state.undo()
        assertEquals("a", state.answer?.text)
    }

    @Test
    fun `redo restores undone value`() {
        state.answer = FieldValue.Text("hello")
        state.pushHistory("hello")
        state.answer = FieldValue.Text("hello world")

        state.undo()
        state.redo()

        assertEquals("hello world", state.answer?.text)
    }

    @Test
    fun `canRedo is false after redoing all`() {
        state.answer = FieldValue.Text("a")
        state.pushHistory("a")
        state.answer = FieldValue.Text("ab")

        state.undo()
        state.redo()

        assertFalse(state.canRedo)
    }

    @Test
    fun `redo re-enables canUndo`() {
        state.answer = FieldValue.Text("a")
        state.pushHistory("a")
        state.answer = FieldValue.Text("ab")

        state.undo()
        state.redo()

        assertTrue(state.canUndo)
    }

    @Test
    fun `redo does nothing when stack is empty`() {
        state.answer = FieldValue.Text("hello")
        state.redo()
        assertEquals("hello", state.answer?.text)
    }

    @Test
    fun `two fields maintain independent histories`() {
        val state2 = TextFieldState(
            TextFormField(id = 2, type = FieldType.TEXTFIELD, name = "other", title = "Other", required = false, archived = false)
        )

        state.answer = FieldValue.Text("a")
        state.pushHistory("a")
        state.answer = FieldValue.Text("ab")

        state2.answer = FieldValue.Text("x")
        state2.pushHistory("x")
        state2.answer = FieldValue.Text("xy")

        state.undo()
        assertEquals("a", state.answer?.text)
        assertEquals("xy", state2.answer?.text)

        state2.undo()
        assertEquals("a", state.answer?.text)
        assertEquals("x", state2.answer?.text)
    }
}
