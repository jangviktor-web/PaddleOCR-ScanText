package com.equationl.paddleocr4android.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        const val EXTRA_PHOTO_PATH = "photo_path"
        const val EXTRA_PHOTO_ROTATION = "photo_rotation"
    }

    private lateinit var previewView: PreviewView
    private lateinit var tvResolution: TextView
    private lateinit var tvZoom: TextView
    private lateinit var zoomProgressBar: ProgressBar
    private lateinit var focusView: FocusView
    private lateinit var gridOverlay: GridOverlayView
    private lateinit var btnFlash: ImageView
    private lateinit var btnGrid: ImageView

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraExecutor: ExecutorService

    private var currentResolutionIndex = 3

    // 缩放相关
    private var currentZoomRatio = 1f
    private var maxZoomRatio = 1f
    private var minZoomRatio = 1f

    // 闪光灯模式
    private var flashMode = ImageCapture.FLASH_MODE_AUTO
    private var flashModeIndex = 0 // 0=auto, 1=on, 2=off

    // 网格线
    private var showGrid = false

    // 自动曝光优化
    private var isAeScanning = false

    private val resolutionLabels = arrayOf(
        "640 x 480", "1280 x 720", "1920 x 1080",
        "2560 x 1440", "3840 x 2160", "最高分辨率"
    )
    private val resolutionSizes = arrayOf(
        Size(640, 480), Size(1280, 720), Size(1920, 1080),
        Size(2560, 1440), Size(3840, 2160), Size(4096, 4096)
    )
    private val flashIcons = intArrayOf(
        R.drawable.ic_flash_auto,
        R.drawable.ic_flash_on,
        R.drawable.ic_flash_off
    )
    private val flashLabels = arrayOf("自动", "开启", "关闭")

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.preview_view)
        tvResolution = findViewById(R.id.tv_resolution)
        tvZoom = findViewById(R.id.tv_zoom)
        zoomProgressBar = findViewById(R.id.zoom_progress_bar)
        focusView = findViewById(R.id.focus_view)
        gridOverlay = findViewById(R.id.grid_overlay)
        btnFlash = findViewById(R.id.btn_flash)
        btnGrid = findViewById(R.id.btn_grid)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val prefs = getSharedPreferences(SettingsActivity.PREF_NAME, MODE_PRIVATE)
        currentResolutionIndex = prefs.getInt(SettingsActivity.KEY_CAMERA_RESOLUTION, 3)

        setupGestures()
        setupFocusView()
        setupFlashButton()
        setupGridButton()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        findViewById<ImageView>(R.id.btn_close_camera).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btn_capture).setOnClickListener { takePhoto() }

        tvResolution.text = resolutionLabels[currentResolutionIndex]
        tvResolution.setOnClickListener { showResolutionDialog() }
    }

    // ==================== 手势 ====================

    private fun setupGestures() {
        // 缩放由 FocusView 内部的 ScaleGestureDetector 处理
        // 通过回调连接到相机缩放
        focusView.onZoomChanged = { scaleFactor ->
            val newZoom = (currentZoomRatio * scaleFactor).coerceIn(minZoomRatio, maxZoomRatio)
            camera?.cameraControl?.setZoomRatio(newZoom)
            currentZoomRatio = newZoom
            updateZoomUI()
        }
    }

    private fun updateZoomUI() {
        tvZoom.text = String.format("%.1fx", currentZoomRatio)
        val progress = ((currentZoomRatio - minZoomRatio) / (maxZoomRatio - minZoomRatio) * 100).toInt()
        zoomProgressBar.progress = progress.coerceIn(0, 100)
    }

    // ==================== 对焦 + 曝光 ====================

    private fun setupFocusView() {
        focusView.onFocusChanged = { x, y -> performFocus(x, y) }
        focusView.onExposureChanged = { value -> performExposureChange(value) }
        focusView.onFocusLocked = { x, y, locked ->
            if (locked) {
                performFocusAndLock(x, y)
            } else {
                performFocus(x, y)
            }
        }
    }

    private fun performFocus(x: Float, y: Float) {
        val cam = camera ?: return
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        cam.cameraControl.startFocusAndMetering(action).addListener({
            try {
                val result = cam.cameraControl.startFocusAndMetering(action).get()
                Log.d(TAG, "对焦结果: ${result.isFocusSuccessful}")
            } catch (e: Exception) {
                Log.e(TAG, "对焦失败", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun performFocusAndLock(x: Float, y: Float) {
        val cam = camera ?: return
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(300, java.util.concurrent.TimeUnit.SECONDS) // 锁定5分钟
            .build()

        cam.cameraControl.startFocusAndMetering(action).addListener({
            try {
                cam.cameraControl.startFocusAndMetering(action).get()
                Log.d(TAG, "锁定对焦成功")
            } catch (e: Exception) {
                Log.e(TAG, "锁定对焦失败", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun performExposureChange(value: Int) {
        val cam = camera ?: return
        val range = cam.cameraInfo.exposureState.exposureCompensationRange
        if (value in range.lower..range.upper) {
            cam.cameraControl.setExposureCompensationIndex(value)
            Log.d(TAG, "曝光补偿: $value")
        }
    }

    // ==================== 闪光灯 ====================

    private fun setupFlashButton() {
        btnFlash.setOnClickListener {
            flashModeIndex = (flashModeIndex + 1) % 3
            flashMode = when (flashModeIndex) {
                0 -> ImageCapture.FLASH_MODE_AUTO
                1 -> ImageCapture.FLASH_MODE_ON
                else -> ImageCapture.FLASH_MODE_OFF
            }
            btnFlash.setImageResource(flashIcons[flashModeIndex])
            updateImageCaptureFlash()
            Toast.makeText(this, "闪光灯: ${flashLabels[flashModeIndex]}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateImageCaptureFlash() {
        imageCapture?.flashMode = flashMode
    }

    // ==================== 网格线 ====================

    private fun setupGridButton() {
        btnGrid.setOnClickListener {
            showGrid = !showGrid
            gridOverlay.showGrid = showGrid
            btnGrid.alpha = if (showGrid) 1f else 0.5f
        }
    }

    // ==================== 自动曝光优化（文字场景） ====================

    private fun startAeScanning() {
        val cam = camera ?: return
        if (isAeScanning) return
        isAeScanning = true

        // 检查曝光状态，如果过曝则自动降低
        val exposureState = cam.cameraInfo.exposureState
        val currentEv = exposureState.exposureCompensationIndex
        val evRange = exposureState.exposureCompensationRange

        if (currentEv >= evRange.upper - 1) {
            val newEv = (currentEv - 1).coerceAtLeast(evRange.lower)
            cam.cameraControl.setExposureCompensationIndex(newEv)
            Log.d(TAG, "自动曝光优化: $currentEv -> $newEv")
        }
        isAeScanning = false
    }

    // ==================== 分辨率 ====================

    private fun showResolutionDialog() {
        AlertDialog.Builder(this)
            .setTitle("选择拍照分辨率")
            .setSingleChoiceItems(resolutionLabels, currentResolutionIndex) { dialog, which ->
                currentResolutionIndex = which
                tvResolution.text = resolutionLabels[which]
                rebindCamera()
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun getDisplayRotation(): Int {
        return when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    // ==================== 相机 ====================

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(getDisplayRotation())
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setFlashMode(flashMode)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                camera?.let { cam ->
                    maxZoomRatio = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                    minZoomRatio = cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                    currentZoomRatio = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                    updateZoomUI()

                    // 启动AE扫描优化文字场景
                    startAeScanning()
                }
            } catch (e: Exception) {
                Log.e(TAG, "相机绑定失败", e)
                Toast.makeText(this, "相机启动失败", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun rebindCamera() {
        val provider = cameraProvider ?: return
        val prev = preview ?: return

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(getDisplayRotation())
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setFlashMode(flashMode)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(this, cameraSelector, prev, imageCapture)
            camera?.cameraControl?.setZoomRatio(currentZoomRatio)
            camera?.let { cam ->
                maxZoomRatio = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                minZoomRatio = cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                updateZoomUI()
            }
        } catch (e: Exception) {
            Log.e(TAG, "重新绑定相机失败", e)
        }
    }

    // ==================== 拍照 ====================

    private fun takePhoto() {
        val ic = imageCapture ?: run {
            Toast.makeText(this, "相机未就绪", Toast.LENGTH_SHORT).show()
            return
        }

        val photoFile = File(
            cacheDir,
            "camera_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        val sensorRotation = camera?.cameraInfo?.sensorRotationDegrees ?: 90
        val displayRotation = getDisplayRotation()
        val finalRotation = (sensorRotation - displayRotation + 360) % 360

        ic.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    setResult(RESULT_OK, intent.apply {
                        putExtra(EXTRA_PHOTO_PATH, photoFile.absolutePath)
                        putExtra(EXTRA_PHOTO_ROTATION, finalRotation.toFloat())
                    })
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "拍照失败: ${exception.message}", exception)
                    Toast.makeText(this@CameraActivity, "拍照失败: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // ==================== 生命周期 ====================

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) startCamera()
            else { Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show(); finish() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
