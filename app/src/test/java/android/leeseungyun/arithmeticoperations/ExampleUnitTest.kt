package android.leeseungyun.arithmeticoperations

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(8, operation(4, Operator.ADD, 4, Operator.DIV, 1))
    }
}
