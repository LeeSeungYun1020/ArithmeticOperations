package android.leeseungyun.arithmeticoperations

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.widget.Button
import androidx.core.database.getIntOrNull
import kotlinx.android.synthetic.main.activity_game.*
import org.jetbrains.anko.db.select
import org.jetbrains.anko.toast

class GameActivity : AppCompatActivity() {
    private val numberButtonList: List<Button> by lazy {
        listOf(numberButton1, numberButton2, numberButton3, numberButton4)
    }
    private val connectedNumberButtons: MutableMap<Button, Button> = mutableMapOf()
    private val game = Game(GameMode.NORMAL, 60, 3)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        initDragAndDrop()

        bottomAppBar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.menuPause -> toast("pause")
                R.id.menuPass -> toast("pass")
            }
            true
        }

        checkFab.setOnClickListener {
            val isRight = try {
                game.checkAnswer(
                    firstNumberButton.text.toString().toInt(),
                    operator1Button.text.toString().toOperator(),
                    secondNumberButton.text.toString().toInt(),
                    operator2Button.text.toString().toOperator(),
                    lastNumberButton.text.toString().toInt()
                )
            } catch (e: Exception) {
                false
            }
            if (isRight) {
                toast("정답!")
                displayGame()
            }
            else
                toast("오답!")
        }
        displayData()
        displayGame()
    }

    private fun String.toOperator() = when(this) {
            resources.getText(R.string.add) -> Operator.ADD
            resources.getText(R.string.sub) -> Operator.SUB
            resources.getText(R.string.mul) -> Operator.MUL
            resources.getText(R.string.div) -> Operator.DIV
            else -> throw(IllegalArgumentException("Error: GameActivity, operator transformation error"))
        }

    private fun displayGame() {
        connectedNumberButtons.clear()
        game.makeGame()
        answerTextView.text = "${game.answer}"
        numberButtonList.forEachIndexed { i, button ->
            button.apply {
                setEnabled(true)
                setClickable(true)
                text = "${game.questList[i]}"
            }
        }
        firstNumberButton.text = resources.getText(R.string.undefined)
        secondNumberButton.text = resources.getText(R.string.undefined)
        lastNumberButton.text = resources.getText(R.string.undefined)
        operator1Button.text = resources.getText(R.string.undefined)
        operator2Button.text = resources.getText(R.string.undefined)
    }

    private fun displayData() {
        val dbHelper = DBHelper(this)
        //dbHelper.readableDatabase.select("Item", "key").exec {  }
        val itemKeyCount =  dbHelper.readableDatabase.select("Item", "name")
            .whereArgs("(name = {itemName})",
                "itemName" to "key").exec {
                if(this.moveToNext())
                    this.getIntOrNull(0) ?: 0
                else
                    0
            }
        keyTextView.text = "$itemKeyCount"
    }

    private fun initDragAndDrop() {
        numberButtonList.forEach { it.dragEnabled(DropType.NUMBER) }

        firstNumberButton.dragEnabled(DropType.NUMBER)
        secondNumberButton.dragEnabled(DropType.NUMBER)
        lastNumberButton.dragEnabled(DropType.NUMBER)

        addButton.dragEnabled(DropType.OPERATOR)
        subButton.dragEnabled(DropType.OPERATOR)
        mulButton.dragEnabled(DropType.OPERATOR)
        divButton.dragEnabled(DropType.OPERATOR)

        operator1Button.dragEnabled(DropType.OPERATOR)
        operator2Button.dragEnabled(DropType.OPERATOR)

        firstNumberButton.dropEnabled(DropType.NUMBER)
        secondNumberButton.dropEnabled(DropType.NUMBER)
        lastNumberButton.dropEnabled(DropType.NUMBER)

        operator1Button.dropEnabled(DropType.OPERATOR)
        operator2Button.dropEnabled(DropType.OPERATOR)
    }

    enum class DropType {
        OPERATOR, NUMBER
    }

    private fun Button.dragEnabled(type: DropType) {
        this.setOnLongClickListener { v ->
            val data = ClipData.newPlainText(type.toString(), this.code().toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                v.startDragAndDrop(data, View.DragShadowBuilder(v), null, 1)
            else
                v.startDrag(data, View.DragShadowBuilder(v), null, 1)
        }
    }

    private fun Button.dropEnabled(type: DropType) {
        this.setOnDragListener { _, event ->
            when(event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }
                DragEvent.ACTION_DROP -> {
                    val label = event.clipData.description.label
                    // 서로 타입이 같은 경우에만 드롭이 허용됨.
                    if (label == type.toString()){
                        val code = event.clipData?.getItemAt(0)?.text
                            ?: resources.getText(R.string.undefined)
                        val originButton = findButtonByCode(code.toString())
                        dropButton(originButton, this)
                    }
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    event.result
                }
            }
            true
        }

        this.setOnClickListener {
            connectedNumberButtons[this]?.apply {
                setEnabled(true)
                setClickable(true)
            }
            connectedNumberButtons.remove(this)
            this.text = resources.getString(R.string.undefined)
            Log.d("TESTLOG", "${this.code()} 작동")
        }
    }

    private fun dropButton(origin: Button, selected:Button) :Boolean {
        Log.d("TESTLOG", "NUMBER, ${origin.code()} -> ${selected.code()}")

        if(origin.text == resources.getText(R.string.undefined))
            return false

        when(origin.code()){
            InteractionButtons.NUM1, InteractionButtons.NUM2, InteractionButtons.NUM3, InteractionButtons.NUM4 -> {
                selected.text = origin.text
                connectedNumberButtons[selected] = origin.apply {
                    setEnabled(false)
                    setClickable(false)
                }
            }
            InteractionButtons.ANS1, InteractionButtons.ANS2, InteractionButtons.ANS3 -> {
                val tem = selected.text
                selected.text = origin.text
                origin.text = tem

                try {
                    if (connectedNumberButtons[selected] != null){//오리진은 무조건 값을 가짐!
                        val temButton:Button = connectedNumberButtons[selected]!!
                        connectedNumberButtons[selected] = connectedNumberButtons[origin]!!
                        connectedNumberButtons[origin] = temButton
                    } else {
                        connectedNumberButtons[selected] = connectedNumberButtons[origin]!!
                        connectedNumberButtons.remove(origin)
                    }


                } catch (e: Exception) {
                    throw(Exception("ERROR: GameActivity/Button.dropEnabled/dragEvent/dropNumberButton - connect error"))
                }
            }
            InteractionButtons.ADD, InteractionButtons.SUB, InteractionButtons.MUL, InteractionButtons.DIV -> {
                selected.text = origin.text
            }
            InteractionButtons.ANSOP1, InteractionButtons.ANSOP2 -> {
                val tem = selected.text
                selected.text = origin.text
                origin.text = tem
            }
        }
        return true
    }

    private fun Button.code(): InteractionButtons = when(this){
        numberButton1 -> InteractionButtons.NUM1
        numberButton2 -> InteractionButtons.NUM2
        numberButton3 -> InteractionButtons.NUM3
        numberButton4 -> InteractionButtons.NUM4
        addButton -> InteractionButtons.ADD
        subButton -> InteractionButtons.SUB
        mulButton -> InteractionButtons.MUL
        divButton -> InteractionButtons.DIV
        firstNumberButton -> InteractionButtons.ANS1
        secondNumberButton -> InteractionButtons.ANS2
        lastNumberButton -> InteractionButtons.ANS3
        operator1Button -> InteractionButtons.ANSOP1
        operator2Button -> InteractionButtons.ANSOP2
        else -> throw (IllegalArgumentException("ERROR: Game/Button.code() illegal button"))
    }
    private fun findButtonByCode(code: String): Button = try {
        findButtonByCode(InteractionButtons.valueOf(code))
    } catch (e: Exception){
        throw (IllegalArgumentException("ERROR: Game/findButtonByCode illegal button"))
    }
    private fun findButtonByCode(code: InteractionButtons): Button = when(code){
        InteractionButtons.NUM1 -> numberButton1
        InteractionButtons.NUM2 -> numberButton2
        InteractionButtons.NUM3 -> numberButton3
        InteractionButtons.NUM4 -> numberButton4
        InteractionButtons.ADD -> addButton
        InteractionButtons.SUB -> subButton
        InteractionButtons.MUL -> mulButton
        InteractionButtons.DIV -> divButton
        InteractionButtons.ANS1 -> firstNumberButton
        InteractionButtons.ANS2 -> secondNumberButton
        InteractionButtons.ANS3 -> lastNumberButton
        InteractionButtons.ANSOP1 -> operator1Button
        InteractionButtons.ANSOP2 -> operator2Button
    }
    private enum class InteractionButtons {
        NUM1,
        NUM2,
        NUM3,
        NUM4,
        ADD,
        SUB,
        MUL,
        DIV,
        ANS1,
        ANS2,
        ANS3,
        ANSOP1,
        ANSOP2
    }
}

