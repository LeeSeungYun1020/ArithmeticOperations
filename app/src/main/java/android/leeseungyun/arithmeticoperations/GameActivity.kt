package android.leeseungyun.arithmeticoperations

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_game.*
import org.jetbrains.anko.*

class GameActivity : AppCompatActivity() {
    private var mode: GameMode = GameMode.NORMAL
    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private val isCustom by lazy {
        (mode == GameMode.PRACTICE && preferences.getBoolean("practiceMode", false))
    }
    private val game by lazy {
        if (isCustom) {
            Game(
                time = preferences.getInt("practiceTime", 0),
                max = preferences.getInt("practiceMax", 0),
                heart = preferences.getInt("practiceLife", 0)
            )
        } else {
            val resources = resources
            Game(
                time = resources.getInteger(R.integer.time),
                max = resources.getInteger(R.integer.max),
                heart = resources.getInteger(R.integer.life)
            )
        }
    }
    private val goal by lazy {
        if (isCustom)
            preferences.getInt("practiceGoal", 0)
        else
            resources.getInteger(R.integer.goal)
    }
    private var score = 0
    private lateinit var itemViewModel: ItemViewModel

    private val numberButtonList: List<Button> by lazy {
        listOf(numberButton1, numberButton2, numberButton3, numberButton4)
    }
    private val connectedNumberButtons: MutableMap<Button, Button> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        mode = when (intent.getIntExtra("mode", 0)) {
            R.string.practice -> GameMode.PRACTICE
            else -> GameMode.NORMAL
        }
        initItemDatabase()
        initDragAndDrop()
        initClickCancel()
        initBottomAppBar()
        displayGame()
    }

    override fun onBackPressed() {
        pause()
    }

    private fun initItemDatabase() {
        if (mode == GameMode.NORMAL) {
            itemViewModel = ViewModelProvider(this).get(ItemViewModel::class.java)
            itemViewModel.allItems.observe(this, Observer { items ->
                items?.let {
                    keyTextView.text = "${it.find { item -> item.itemName == "key" }?.count ?: 0}"
                }
            })
        } else {
            keyTextView.text = if (isCustom)
                preferences
                    .getInt("practiceKey", 0)
                    .toString()
            else
                resources
                    .getInteger(R.integer.key)
                    .toString()
        }
    }

    private fun initBottomAppBar() {
        bottomAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menuPause -> {
                    pause()
                }
                R.id.menuPass -> {
                    pass()
                }
            }
            true
        }
        initCheckFAB()
    }

    private fun initCheckFAB() {
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
                score++
                if (score >= goal)
                    win()
                else
                    displayGame()
            } else {
                toast(R.string.wrong)
            }
        }
    }

    private fun exit() {
        game.cancelTimer()
        finish()
    }

    private fun win() {
        game.cancelTimer()
        alert {
            titleResource = R.string.win
            messageResource = R.string.winMsg
            okButton {
                if (mode == GameMode.NORMAL)
                    gainItem()
                else
                    exit()
            }
            isCancelable = false
        }.show()
    }

    private fun gainItem() {
        alert {
            titleResource = R.string.item
            messageResource = R.string.clickBox
            customView {
                imageView(R.drawable.ic_box_black_24dp)
                    .apply {
                        setOnClickListener {
                            drawing()
                        }
                    }
            }
            okButton {
                drawing()
            }
            isCancelable = false
        }.show()
    }

    private fun drawing() {
        alert {
            when ((1..100).random()) {
                in (1..5) -> {// 5%
                    titleResource = R.string.gift
                    messageResource = R.string.giftMsgKey
                    itemViewModel.update(Item("key", keyTextView.text.toString().toInt() + 1))
                }
                else -> {
                    titleResource = R.string.empty
                    messageResource = R.string.emptyMsg
                }
            }
            okButton {
                exit()
            }
            isCancelable = false
        }.show()
    }

    private fun pause() {
        alert {
            title = resources.getText(R.string.pause)
            message = resources.getText(R.string.giveUpGame)
            okButton {
                exit()
            }
            cancelButton {

            }

        }.show()
    }

    private fun pass() {
        val count = keyTextView.text.toString().toInt()
        if (count > 0) {
            if (mode == GameMode.NORMAL)
                itemViewModel.update(Item("key", count - 1))
            else
                keyTextView.text = "${count - 1}"
            game.cancelTimer()
            toast(R.string.pass)
            displayGame()
        } else {
            toast(R.string.passUnable)
        }

    }

    private fun String.toOperator() = when (this) {
        resources.getText(R.string.add) -> Operator.ADD
        resources.getText(R.string.sub) -> Operator.SUB
        resources.getText(R.string.mul) -> Operator.MUL
        resources.getText(R.string.div) -> Operator.DIV
        else -> throw(IllegalArgumentException("Error: GameActivity, operator transformation error"))
    }

    private fun clearGame() {
        connectedNumberButtons.clear()
        firstNumberButton.text = resources.getText(R.string.undefined)
        secondNumberButton.text = resources.getText(R.string.undefined)
        lastNumberButton.text = resources.getText(R.string.undefined)
        operator1Button.text = resources.getText(R.string.undefined)
        operator2Button.text = resources.getText(R.string.undefined)
    }

    private fun displayGame() {
        clearGame()
        game.makeGame().startGame(this, callback = { t ->
            gameTimerTextView.text = "${t / 100}"
            gameTimerMilliTextView.text = "${t % 100}"
        }, end = {
            game.heart--
            if (game.heart > 0) {
                displayGame()
            } else {
                displayStatusData()
                lose()
            }
        })
        displayGameData()
        displayStatusData()
    }

    private fun displayGameData() {
        answerTextView.text = "${game.answer}"
        numberButtonList.forEachIndexed { i, button ->
            button.apply {
                isEnabled = true
                isClickable = true
                text = "${game.questList[i]}"
            }
        }
    }

    private fun displayStatusData() {
        heartTextView.text = game.heart.toString()

        gameScoreTextView.text = "$score/$goal"
    }

    private fun lose() {
        game.cancelTimer()
        alert {
            titleResource = R.string.lose
            messageResource = R.string.loseMsg
            okButton {
                exit()
            }
            isCancelable = false
        }.show()
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
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }
                DragEvent.ACTION_DROP -> {
                    val label = event.clipData.description.label
                    // 서로 타입이 같은 경우에만 드롭이 허용됨.
                    if (label == type.toString()) {
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
    }

    private fun dropButton(origin: Button, selected: Button): Boolean {
        if (origin.text == resources.getText(R.string.undefined))
            return false

        when (origin.code()) {
            InteractionButtons.NUM1, InteractionButtons.NUM2, InteractionButtons.NUM3, InteractionButtons.NUM4 -> {
                selected.text = origin.text
                connectedNumberButtons[selected]?.apply {
                    isEnabled = true
                    isClickable = true
                }
                connectedNumberButtons[selected] = origin.apply {
                    isEnabled = false
                    isClickable = false
                }
            }
            InteractionButtons.ANS1, InteractionButtons.ANS2, InteractionButtons.ANS3 -> {
                val tem = selected.text
                selected.text = origin.text
                origin.text = tem

                try {
                    if (connectedNumberButtons[selected] != null) {//오리진은 무조건 값을 가짐!
                        val temButton: Button = connectedNumberButtons[selected]!!
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


    private fun initClickCancel() {
        firstNumberButton.clickCancelEnabled()
        secondNumberButton.clickCancelEnabled()
        lastNumberButton.clickCancelEnabled()

        operator1Button.clickCancelEnabled()
        operator2Button.clickCancelEnabled()
    }

    private fun Button.clickCancelEnabled() {
        this.setOnClickListener {
            connectedNumberButtons[this]?.apply {
                isEnabled = true
                isClickable = true
            }
            connectedNumberButtons.remove(this)
            this.text = resources.getString(R.string.undefined)
        }
    }

    private fun Button.code(): InteractionButtons = when (this) {
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
    } catch (e: Exception) {
        throw (IllegalArgumentException("ERROR: Game/findButtonByCode illegal button"))
    }

    private fun findButtonByCode(code: InteractionButtons): Button = when (code) {
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

