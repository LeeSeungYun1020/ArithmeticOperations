package android.leeseungyun.arithmeticoperations

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.MenuItem
import android.view.View
import android.widget.Button
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_game.*
import org.jetbrains.anko.toast
import kotlin.Exception

class GameActivity : AppCompatActivity() {
    private val numberButtonList: List<Button> by lazy {
        listOf(numberButton1, numberButton2, numberButton3, numberButton4)
    }
    private val connectedButtons: MutableMap<Button, Button> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        initOperatorDragAndDrop()
        initNumberDragAndDrop()

        bottomAppBar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.menuPause -> toast("pause")
                R.id.menuPass -> toast("pass")
            }
            true
        }

        checkFab.setOnClickListener {v ->

        }

        val quest = Question().makeQuest()
        val questList = quest.questList
        answerTextView.text = quest.answer.toString()
        numberButtonList.forEachIndexed { i, button ->
            button.text = questList[i].toString()
        }


    }



    private fun initOperatorDragAndDrop() {
        addButton.dragEnabled(DropType.OPERATOR)
        subButton.dragEnabled(DropType.OPERATOR)
        mulButton.dragEnabled(DropType.OPERATOR)
        divButton.dragEnabled(DropType.OPERATOR)

        operator1Button.dropEnabled(DropType.OPERATOR)
        operator2Button.dropEnabled(DropType.OPERATOR)
    }

    private fun initNumberDragAndDrop() {
        numberButtonList.forEach { it.dragEnabled(DropType.NUMBER) }

        firstNumberButton.dropEnabled(DropType.NUMBER)
        secondNumberButton.dropEnabled(DropType.NUMBER)
        lastNumberButton.dropEnabled(DropType.NUMBER)
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
        this.setOnDragListener { v, event ->
            when(event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }
                DragEvent.ACTION_DROP -> {
                    val label = event.clipData.description.label
                    if (label == type.toString()){
                        val code = event.clipData?.getItemAt(0)?.text
                            ?: resources.getText(R.string.undefined)
                        val originButton = findButtonByCode(code.toString())

                        if (label == (DropType.NUMBER).toString()){
                            connectedButtons[this]?.apply {
                                setEnabled(true)
                                setClickable(true)
                            }
                            connectedButtons[this] = originButton.apply {
                                setEnabled(false)
                                setClickable(false)
                            }
                        }
                        text = originButton.text
                        true
                    }
                    else
                        false
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    event.result
                }
            }
            true
        }

        this.setOnClickListener {
            connectedButtons[this]?.apply {
                setEnabled(true)
                setClickable(true)
            }
            this.text = resources.getString(R.string.undefined)
        }
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
        else -> throw (IllegalArgumentException("ERROR: Game/Button.code() illegal button"))
    }
    private fun findButtonByCode(code: String): Button = findButtonByCode(InteractionButtons.valueOf(code))
    private fun findButtonByCode(code: InteractionButtons): Button = when(code){
        InteractionButtons.NUM1 -> numberButton1
        InteractionButtons.NUM2 -> numberButton2
        InteractionButtons.NUM3 -> numberButton3
        InteractionButtons.NUM4 -> numberButton4
        InteractionButtons.ADD -> addButton
        InteractionButtons.SUB -> subButton
        InteractionButtons.MUL -> mulButton
        InteractionButtons.DIV -> divButton
    }

}

enum class InteractionButtons {
    NUM1,
    NUM2,
    NUM3,
    NUM4,
    ADD,
    SUB,
    MUL,
    DIV
}