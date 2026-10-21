package mil.nga.giat.mage.form.field

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FieldValueTest {

    @Test
    fun `serialize number returns null for blank`() {
        assertNull(FieldValue.Number("").serialize())
        assertNull(FieldValue.Number(" ").serialize())
    }

    @Test
    fun `serialize number returns null for partial input`() {
        assertNull(FieldValue.Number("-").serialize())
        assertNull(FieldValue.Number(".").serialize())
        assertNull(FieldValue.Number("-.").serialize())
    }

    @Test
    fun `serialize number returns null for comma decimal`() {
        assertNull(FieldValue.Number("1,5").serialize())
    }

    @Test
    fun `serialize whole number returns int`() {
        assertEquals(5, FieldValue.Number("5").serialize())
        assertEquals(1, FieldValue.Number("1.").serialize())
        assertEquals(-3, FieldValue.Number("-3").serialize())
    }

    @Test
    fun `serialize decimal number returns double`() {
        assertEquals(1.5, FieldValue.Number("1.5").serialize())
        assertEquals(-0.25, FieldValue.Number("-.25").serialize())
    }
}
