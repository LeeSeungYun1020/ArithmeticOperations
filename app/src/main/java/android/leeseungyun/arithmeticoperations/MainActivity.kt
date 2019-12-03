package android.leeseungyun.arithmeticoperations

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startButton.setOnClickListener {
            startActivity<GameActivity>(
                "mode" to R.string.start
            )
        }
        practiceButton.setOnClickListener {
            startActivity<GameActivity>(
                "mode" to R.string.practice
            )
        }
        descriptionButton.setOnClickListener {
            startActivity<DescriptionActivity>()
        }
        settingButton.setOnClickListener {
            startActivity<SettingsActivity>()
        }
    }
}
