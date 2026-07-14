package mil.nga.giat.mage.form.field

import mil.nga.giat.mage.form.FieldType
import mil.nga.giat.mage.form.NumberFormField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NumberFieldStateTest {

    private lateinit var state: NumberFieldState

    @Before
    fun setUp() {
        state = NumberFieldState(
            NumberFormField(id = 1, type = FieldType.NUMBERFIELD, name = "field", title = "Field", required = false, archived = false, min = null, max = null)
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
        state.pushHistory("1")
        assertTrue(state.canUndo)
    }

    @Test
    fun `pushHistory clears redo stack`() {
        state.answer = FieldValue.Number("1")
        state.pushHistory("1")
        state.answer = FieldValue.Number("12")
        state.undo()
        assertTrue(state.canRedo)

        state.pushHistory("1")
        assertFalse(state.canRedo)
    }

    @Test
    fun `undo restores previous value`() {
        state.answer = FieldValue.Number("42")
        state.pushHistory("42")
        state.answer = FieldValue.Number("420")

        state.undo()

        assertEquals("42", state.answer?.number)
    }

    @Test
    fun `undo enables canRedo`() {
        state.answer = FieldValue.Number("1")
        state.pushHistory("1")
        state.answer = FieldValue.Number("12")

        state.undo()

        assertTrue(state.canRedo)
    }

    @Test
    fun `canUndo is false after undoing all history`() {
        state.answer = FieldValue.Number("1")
        state.pushHistory("1")
        state.answer = FieldValue.Number("12")

        state.undo()

        assertFalse(state.canUndo)
    }

    @Test
    fun `undo does nothing when stack is empty`() {
        state.answer = FieldValue.Number("99")
        state.undo()
        assertEquals("99", state.answer?.number)
    }

    @Test
    fun `undo supports multiple steps`() {
        state.answer = FieldValue.Number("1")
        state.pushHistory("1")
        state.answer = FieldValue.Number("12")
        state.pushHistory("12")
        state.answer = FieldValue.Number("123")

        state.undo()
        assertEquals("12", state.answer?.number)

        state.undo()
        assertEquals("1", state.answer?.number)
    }

    @Test
    fun `redo restores undone value`() {
        state.answer = FieldValue.Number("42")
        state.pushHistory("42")
        state.answer = FieldValue.Number("420")

        state.undo()
        state.redo()

        assertEquals("420", state.answer?.number)
    }

    @Test
    fun `canRedo is false after redoing all`() {
        state.answer = FieldValue.Number("1")
        state.pushHistory("1")
        state.answer = FieldValue.Number("12")

        state.undo()
        state.redo()

        assertFalse(state.canRedo)
    }

    @Test
    fun `redo does nothing when stack is empty`() {
        state.answer = FieldValue.Number("99")
        state.redo()
        assertEquals("99", state.answer?.number)
    }

    @Test
    fun `two number fields maintain independent histories`() {
        val state2 = NumberFieldState(
            NumberFormField(id = 2, type = FieldType.NUMBERFIELD, name = "other", title = "Other", required = false, archived = false, min = null, max = null)
        )

        state.answer = FieldValue.Number("1")
        state.pushHistory("1")
        state.answer = FieldValue.Number("12")

        state2.answer = FieldValue.Number("9")
        state2.pushHistory("9")
        state2.answer = FieldValue.Number("99")

        state.undo()
        assertEquals("1", state.answer?.number)
        assertEquals("99", state2.answer?.number)

        state2.undo()
        assertEquals("1", state.answer?.number)
        assertEquals("9", state2.answer?.number)
    }
}
