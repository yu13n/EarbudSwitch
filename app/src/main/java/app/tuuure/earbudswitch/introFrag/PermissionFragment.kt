package app.tuuure.earbudswitch.introFrag

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.activity.IntroActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_intro_permission.*

class PermissionFragment : Fragment() {
    companion object {
        private const val REQUEST_CODE_LOCATION_PERMISSION = 20
        private const val REQUEST_CODE_CAMERA_PERMISSION = 21
    }

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
        return inflater.inflate(R.layout.fragment_intro_permission, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        intro_permission_location.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                if (!checkPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_CODE_LOCATION_PERMISSION
                    )
                }
            } else {
                if (checkPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    compoundButton.isChecked = true
                }
            }
        }

        intro_permission_camera.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                if (!checkPermissionGranted(Manifest.permission.CAMERA)) {
                    requestPermissions(
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CODE_CAMERA_PERMISSION
                    )
                }
            } else {
                if (checkPermissionGranted(Manifest.permission.CAMERA)) {
                    compoundButton.isChecked = true
                }
            }
        }

        buttonIntroNext.setOnClickListener {
            if (intro_permission_camera.isChecked && intro_permission_location.isChecked) {
                mActivity.nextFragmentOrFinish()
            } else {
                Snackbar.make(view, getString(R.string.snack_permission), Snackbar.LENGTH_LONG)
                    .show();
            }
        }
    }

    private fun checkPermissionGranted(permission: String): Boolean =
        mActivity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;

    override fun onResume() {
        super.onResume()
        intro_permission_location.isChecked =
            checkPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        intro_permission_camera.isChecked = checkPermissionGranted(Manifest.permission.CAMERA)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_CAMERA_PERMISSION -> {
                intro_permission_camera.isChecked =
                    (PackageManager.PERMISSION_GRANTED == grantResults[0])
            }
            REQUEST_CODE_LOCATION_PERMISSION -> {
                intro_permission_location.isChecked =
                    (PackageManager.PERMISSION_GRANTED == grantResults[0])
            }
        }
    }

}