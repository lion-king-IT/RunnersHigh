package com.reo.running.runnershigh.fragments.run

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.reo.running.runnershigh.*
import com.reo.running.runnershigh.R
import com.reo.running.runnershigh.databinding.FragmentRunBinding
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.round

class RunFragment : Fragment() {

    private lateinit var binding: FragmentRunBinding
    private val runViewModel: RunViewModel by viewModels {
        RunViewModel.Companion.Factory(
            MyApplication.db.justRunDao()
        )
    }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var stopTime: Long = 0L
    private var imageUri: Uri? = null
    private val contentResolver: ContentResolver? = null
    private var photo: Bitmap? = null

    companion object {
        private const val REQUEST_PERMISSION = 1000
        private const val PERMISSION_CODE = 1001
        private const val IMAGE_CAPTURE_CODE = 1002
        private const val LONG_VIBRATION = 2000
        private const val MIDDLE_VIBRATION = 2001
        private const val SHORT_VIBRATION = 2002
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentRunBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.run {
            if (checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSION
                )
                return
            }
            mapView.onCreate(savedInstanceState)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            val locationRequest = LocationRequest().apply {
                interval = 1
                fastestInterval = 1
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    super.onLocationResult(locationResult)
                    val latLng = LatLng(runViewModel.lastLocation.value?.latitude ?: 0.0, runViewModel.lastLocation.value?.longitude ?: 0.0)
                    mapView.getMapAsync {
                        it.isMyLocationEnabled = true
                        it.uiSettings.isMyLocationButtonEnabled = false
                        val alphaAnimation = AlphaAnimation(0f, 1f)
                        alphaAnimation.duration = 800
                        when (runViewModel.runState.value) {
                            RunState.RUN_STATE_BEFORE -> {
                                startNav.startAnimation(alphaAnimation)
                                startNav2.startAnimation(alphaAnimation)
                                it.animateCamera(
                                    CameraUpdateFactory
                                        .newLatLngZoom(
                                            latLng,
                                            runViewModel.zoomValue
                                        )
                                )
                            }

                            RunState.RUN_STATE_START -> {
                                it.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        latLng,
                                        runViewModel.zoomValue
                                    )
                                )
                                runViewModel.calcTotalMileage(locationResult?.lastLocation)
                            }

                            RunState.RUN_STATE_PAUSE -> {
                                startNav.run {
                                    visibility = View.VISIBLE
                                    setText(R.string.stop_Run)
                                    startAnimation(alphaAnimation)
                                }
                            }
                        }
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )

            startButton.setOnClickListener {
                lifecycleScope.launch {
                    runViewModel.setRunState(RunState.RUN_STATE_START)
                    startNav.run {
                        visibility = View.GONE
                        clearAnimation()
                    }
                    startNav2.run {
                        visibility = View.GONE
                        clearAnimation()
                    }
                    startText.visibility = View.GONE
                    startButton.visibility = View.GONE
                    mapView.visibility = View.GONE
                    (activity as MainActivity).binding.bottomNavigation.visibility = View.GONE
                    withContext(Dispatchers.Main) {
                        startButton.startAnimation(scaleUpAnimationMore {})
                        withContext(Dispatchers.IO) {
                            delay(1000)
                            listOf(
                                countNum3,
                                countNum2,
                                countNum1,
                            ).map {
                                animationCount(it)
                                delay(1000)
                            }
                        }
                        vibratorOn(LONG_VIBRATION)
                        startButton.clearAnimation()
                        stopWatch.base = SystemClock.elapsedRealtime()
                        stopWatch.start()
                        mapView.visibility = View.VISIBLE
                        pauseButton.visibility = View.VISIBLE
                        timerScreen.visibility = View.VISIBLE
                        lockOff.visibility = View.VISIBLE
                    }
                }
            }

            restartButton.setOnClickListener {
                runViewModel.setRunState(RunState.RUN_STATE_START)
                vibratorOn(LONG_VIBRATION)
                stopWatch.base = SystemClock.elapsedRealtime() - stopTime
                stopWatch.start()
                finishButton.visibility = View.GONE
                lifecycleScope.launch {
                    startNav.run {
                        visibility = View.GONE
                        clearAnimation()
                    }
                    withContext(Dispatchers.Main) {
                        restartButton.startAnimation(scaleDownAnimation {})
                        delay(500)
                        restartButton.clearAnimation()
                        restartButton.visibility = View.GONE
                        lockOff.visibility = View.VISIBLE
                        pauseButton.visibility = View.VISIBLE
                        pauseButton.startAnimation(scaleUpAnimation {
                            it.fillAfter = false
                        })
                    }
                }
            }

            pauseButton.setOnClickListener {
                runViewModel.setRunState(RunState.RUN_STATE_PAUSE)
                vibratorOn(MIDDLE_VIBRATION)
                stopTime = SystemClock.elapsedRealtime() - stopWatch.base
                stopWatch.stop()
                lifecycleScope.launch(Dispatchers.Main) {
                    pauseButton.startAnimation(scaleDownAnimation {})
                    delay(500)
                    pauseButton.clearAnimation()
                    pauseButton.visibility = View.GONE
                    lockOff.visibility = View.GONE
                    finishButton.visibility = View.VISIBLE
                    restartButton.visibility = View.VISIBLE
                    restartButton.startAnimation(scaleUpAnimation {})
                    finishButton.startAnimation(scaleUpAnimation {
                        it.fillAfter = false
                    })
                    delay(300)
                }
            }

            finishButton.setOnClickListener {
                vibratorOn(SHORT_VIBRATION)
                lifecycleScope.launch(Dispatchers.Main) {
                    finishButton.startAnimation(scaleDownAnimation {})
                    delay(500)
                    finishButton.clearAnimation()
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setCancelable(false)
                        .setMessage("ランニングを終了しますか？")
                        .setPositiveButton("YES") { _, _ ->
                            runViewModel.setRunState(RunState.RUN_STATE_BEFORE)
                            runViewModel.saveRunData(stopWatch.text.toString(), photo) {
                                findNavController().navigate(R.id.action_navi_run_to_fragmentResult)
                            }
                        }
                        .setNegativeButton(
                            "CANCEL"
                        ) { _, _ ->
                        }
                    builder.show()
                }
            }

            lockOff.setOnClickListener {
                lockOff.visibility = View.GONE
                pauseButton.visibility = View.GONE
                lockImage.visibility = View.VISIBLE
            }

            lockImage.setOnClickListener {
                lockImage.visibility = View.GONE
                pauseButton.visibility = View.VISIBLE
                lockOff.visibility = View.VISIBLE

            }

            cameraImage.setOnClickListener {
                if (checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    val permission = arrayOf(
                        Manifest.permission.CAMERA
                    )
                    requestPermissions(permission, PERMISSION_CODE)
                } else {
                    openCamera()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    private fun scaleUpAnimation(operation: (ScaleAnimation) -> Unit = {}): ScaleAnimation =
        ScaleAnimation(
            0.6f,
            1f,
            0.6f,
            1f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        ).apply {
            duration = 300
            fillAfter = true
            operation(this)
        }

    private fun scaleUpAnimationMore(operation: (ScaleAnimation) -> Unit = {}): ScaleAnimation =
        ScaleAnimation(
            1f,
            100f,
            1f,
            100f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        ).apply {
            duration = 1500
            fillAfter = true
        }

    private fun scaleDownAnimation(operation: (ScaleAnimation) -> Unit = {}): ScaleAnimation =
        ScaleAnimation(
            1f,
            0.6f,
            1f,
            0.6f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        ).apply {
            duration = 300
            fillAfter = true
            operation(this)
        }

    private fun animationCount(view: View) {
        view.startAnimation(ScaleAnimation(
            0f,
            400f,
            0f,
            400f,
            Animation.RELATIVE_TO_SELF,
            0.255f,
            Animation.RELATIVE_TO_SELF,
            0.55f
        ).apply { duration = 1000 })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibratorOn(vibratorType: Int) {
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        when (vibratorType) {
            LONG_VIBRATION -> {
                val vibrationEffect = VibrationEffect.createOneShot(800, 255)
                vibrator.vibrate(vibrationEffect)
            }
            MIDDLE_VIBRATION -> {
                val vibrationEffect = VibrationEffect.createOneShot(600, 255)
                vibrator.vibrate(vibrationEffect)
            }
            SHORT_VIBRATION -> {
                val vibrationEffect = VibrationEffect.createOneShot(300, 255)
                vibrator.vibrate(vibrationEffect)
            }
        }
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        imageUri = contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_DENIED
                ) {
                    Toast.makeText(context, "Permission dined", Toast.LENGTH_SHORT).show()

                } else {
                    openCamera()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            (data?.extras?.get("data") as? Bitmap?).let {
                binding.cameraSet.setImageBitmap(it)
                binding.cameraSet.rotation = 90f
                photo = it
                runViewModel.isTakenPhoto.value = true
            }
        }
    }
}