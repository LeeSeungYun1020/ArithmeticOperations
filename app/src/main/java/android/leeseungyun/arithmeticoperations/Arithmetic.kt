package android.leeseungyun.arithmeticoperations

enum class Operator(val priority: Int){
    ADD(2), SUB(2), MUL(1), DIV(1)

}

fun Int.operation(operator: Operator, other:Int) = when (operator) {
    Operator.ADD -> this + other
    Operator.SUB -> this - other
    Operator.MUL -> this * other
    Operator.DIV -> {
        if (this % other == 0) this / other
        else throw ArithmeticException("Can't calculate $this / $other")
    }
}

fun operation(a: Int, operator1: Operator, b: Int, operator2: Operator, c: Int) : Int = when {
    operator1.priority <= operator2.priority ->
        a.operation(operator1, b).operation(operator2, c)
    else ->
        a.operation(operator1, b.operation(operator2, c))
}

enum class GameMode{ MINI, NORMAL}

class Game(private val gameMode: GameMode = GameMode.NORMAL, val time:Int = 60, private val max:Int = 9) {


    var answer = 0
    lateinit var numberList: List<Int>
    lateinit var questList: List<Int>

    fun makeGame() :Game {
        when(gameMode){
            GameMode.NORMAL -> gameNormalMode()
            GameMode.MINI -> gameMiniMode()
        }
        return this
    }

    fun checkAnswer(first: Int, operator1: Operator, second:Int, operator2: Operator, last: Int): Boolean {
        return (answer == try {
            operation(first, operator1, second, operator2, last)
        }catch (e: ArithmeticException){
            if(operator1 == Operator.DIV && operator2 == Operator.MUL)
                operation(first, operator2, last, operator1, first)
            else
                false
        })
    }

    private fun gameMiniMode() {
        val operator = Operator.values().random()
        val first = (1..max).random()
        val last = when(operator) {
            Operator.DIV -> (1..max).filter { first % it == 0 }.random()
            else -> (1..max).random()
        }
        answer = first.operation(operator, last)
        numberList = listOf(first, last)
        questList = listOf(first, last, (1..max).random()).shuffled()
    }

    private fun gameNormalMode() {
        val operator1 = Operator.values().random()
        val operator2 = Operator.values().random()

        val first:Int
        val second:Int
        val last:Int
        when {
            // a / b / c
            operator1 == Operator.DIV && operator2 == Operator.DIV -> {
                first = (1..max).random()
                second = (1..max).filter { first % it == 0 }.random()
                last = (1..max).filter { (first / second) % it == 0 }.random()
            }
            // a / b * c
            operator1 == Operator.DIV && operator2 == Operator.MUL -> {
                first = (1..max).random()
                last = (1..max).random()
                second = (1..max).filter { (first * last) % it == 0 }.random()
            }
            // a / b +- c
            operator1 == Operator.DIV -> {
                first = (1..max).random()
                second = (1..max).filter { first % it == 0 }.random()
                last = (1..max).random()
            }
            // a * b / c
            operator1 == Operator.MUL && operator2 == Operator.DIV -> {
                first = (1..max).random()
                second = (1..max).random()
                last = (1..max).filter { (first * second) % it == 0  }.random()
            }
            // a +- b / c
            operator2 == Operator.DIV -> {
                first = (1..max).random()
                second = (1..max).random()
                last = (1..max).filter { second % it == 0 }.random()
            }
            else -> {
                first = (1..max).random()
                second = (1..max).random()
                last = (1..max).random()
            }
        }
        answer = try {
            operation(first, operator1, second, operator2, last)
        }catch (e: ArithmeticException){
            if(operator1 == Operator.DIV && operator2 == Operator.MUL)
                operation(first, operator2, last, operator1, first)
            else
                throw e
        }

        numberList = listOf(first, second, last)
        questList = listOf(first, second, last, (1..max).random()).shuffled()
    }



}

