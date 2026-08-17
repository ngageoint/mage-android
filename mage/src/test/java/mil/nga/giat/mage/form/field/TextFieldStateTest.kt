// Needed for updated compose until full rework is done
@file:OptIn(ExperimentalFoundationApi::class)
package mil.nga.giat.mage.form.field

import androidx.compose.foundation.ExperimentalFoundationApi
import mil.nga.giat.mage.form.FieldType
import mil.nga.giat.mage.form.TextFormField
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `canRedo is false initially`() {
        assertFalse(state.inputState.undoState.canRedo)
    }

    @Test
    fun `answer is null initially`() {
        assertNull(state.answer)
    }

    @Test
    fun `canUndo is false initially`() {
        assertFalse(state.inputState.undoState.canUndo)
    }

    @Test
    fun `undo does nothing when stack is empty`() {
        state.inputState.undoState.undo()
        assertFalse(state.inputState.undoState.canUndo)
    }

    @Test
    fun `redo does nothing when stack is empty`() {
        state.inputState.undoState.redo()
        assertFalse(state.inputState.undoState.canRedo)
    }
}
