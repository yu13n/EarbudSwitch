package app.tuuure.earbudswitch.introFrag

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.activity.IntroActivity
import kotlinx.android.synthetic.main.fragment_intro_permission.*

class TitleFragment : Fragment() {
    private lateinit var mActivity: IntroActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = context as IntroActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_intro_title, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buttonIntroNext.setOnClickListener { mActivity.nextFragmentOrFinish() }

    }
}