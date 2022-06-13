package com.quwan.cameraxdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class CameraActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "CameraActivity"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private lateinit var previewView: PreviewView
    private var cameraSelector: CameraSelector? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var camera: Camera? = null
    private var analysisUseCase: ImageAnalysis? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        previewView = findViewById(R.id.preview_view)
        findViewById<View>(R.id.switchCamera).setOnClickListener {
            switchCamera()
        }
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        if (PermissionsUtil.allPermissionsGranted(this)){
            start()
        }else{
            PermissionsUtil.requestPermission(this)
        }
    }

    private fun start() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            startCamera()
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PermissionsUtil.allPermissionsGranted(this)){
            start()
        }
    }

    private fun startCamera() {
        if (cameraProvider != null) {
            cameraProvider!!.unbindAll()
            bindPreviewUseCase()
            bindAnalysisUseCase()
        }
    }

    private fun bindPreviewUseCase() {
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        val builder = Preview.Builder()
        builder.setTargetAspectRatio(AspectRatio.RATIO_16_9).build()
        previewUseCase = builder.build()
        previewUseCase!!.setSurfaceProvider(previewView.surfaceProvider)
        cameraProvider!!.bindToLifecycle(
            this,
            cameraSelector!!,
            previewUseCase
        )
    }

    private fun bindAnalysisUseCase() {
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        val builder = ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
        analysisUseCase = builder.build()
        analysisUseCase?.setAnalyzer(
            ContextCompat.getMainExecutor(this)
        ) { imageProxy: ImageProxy ->
            Log.d(TAG,"ImageProxy: 宽高： ${imageProxy.width} * ${imageProxy.height}")
            imageProxy.close()
        }
        camera = cameraProvider!!.bindToLifecycle(
            this,
            cameraSelector!!,
            analysisUseCase
        )
    }

    fun switchCamera() {
        if (cameraProvider == null) {
            return
        }
        val newLensFacing =
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
        val newCameraSelector = CameraSelector.Builder().requireLensFacing(newLensFacing).build()
        try {
            if (cameraProvider!!.hasCamera(newCameraSelector)) {
                lensFacing = newLensFacing
                cameraSelector = newCameraSelector
                startCamera()
                return
            }
        } catch (e: CameraInfoUnavailableException) {
            // Falls through
        }
    }
}