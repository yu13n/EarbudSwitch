package app.tuuure.earbudswitch.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.introFrag.PermissionFragment
import app.tuuure.earbudswitch.introFrag.TitleFragment
import kotlinx.android.synthetic.main.activity_intro.*

class IntroActivity : AppCompatActivity() {
    companion object {
        private const val NUM_PAGES = 2
    }

    fun nextFragmentOrFinish() {
        val current = viewPager.currentItem
        if (current + 1 == NUM_PAGES) {
            setResult(RESULT_OK)
            finish()
        } else {
            viewPager.setCurrentItem(current + 1, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)
        val adapter = WelcomeAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false
    }

    override fun onBackPressed() {}

    private class WelcomeAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> TitleFragment()
                1 -> PermissionFragment()
                else -> TitleFragment()
            }
        }

        override fun getItemCount(): Int {
            return NUM_PAGES
        }
    }
}