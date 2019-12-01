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
            startActivity<GameActivity>()
        }
        practiceButton.setOnClickListener {

        }
        descriptionButton.setOnClickListener {

        }
        settingButton.setOnClickListener {
            startActivity<SettingsActivity>()
        }
    }
}
