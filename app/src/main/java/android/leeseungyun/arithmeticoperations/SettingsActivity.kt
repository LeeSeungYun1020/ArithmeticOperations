package android.leeseungyun.arithmeticoperations

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import org.jetbrains.anko.support.v4.browse
import org.jetbrains.anko.support.v4.email

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment(supportFragmentManager))
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment(private val supportFragmentManager: FragmentManager) :
        PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<Preference>("practiceRoot")?.setOnPreferenceClickListener {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, SettingsPracticeModeFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
            findPreference<Preference>("feedbackRoot")?.setOnPreferenceClickListener {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, SettingsFeedbackFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
        }
    }

    class SettingsPracticeModeFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.practice_preferences, rootKey)
            val time = findPreference<SeekBarPreference>("practiceTime")
            val goal = findPreference<SeekBarPreference>("practiceGoal")
            val life = findPreference<SeekBarPreference>("practiceLife")
            val key = findPreference<SeekBarPreference>("practiceKey")
            val max = findPreference<SeekBarPreference>("practiceMax")

            findPreference<Preference>("practiceStandard")?.setOnPreferenceClickListener {
                val resources = resources
                time?.value = resources.getInteger(R.integer.time)
                goal?.value = resources.getInteger(R.integer.goal)
                life?.value = resources.getInteger(R.integer.life)
                key?.value = resources.getInteger(R.integer.key)
                max?.value = resources.getInteger(R.integer.max)
                true
            }
        }
    }

    class SettingsFeedbackFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.feedback_preferences, rootKey)
            findPreference<EditTextPreference>("bugReport")
                ?.setOnPreferenceChangeListener { preference, message ->
                    (preference as EditTextPreference).setOnBindEditTextListener {
                        it.text.clear()
                    }
                    email(
                        "ileilliat@gmail.com",
                        "<Feedback> Bug Report",
                        "App: ArithmeticOperations (AO)\n" +
                                "Detail: ${message}"
                    )
                    true
                }
            findPreference<Preference>("github")?.setOnPreferenceClickListener {
                browse("https://github.com/LeeSeungYun1020/ArithmeticOperations")
            }
        }
    }
}