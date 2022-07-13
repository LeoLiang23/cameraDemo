package com.quwan.cameraxdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
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
    private lateinit var tvFps: AppCompatTextView
    private var cameraSelector: CameraSelector? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var camera: Camera? = null
    private var analysisUseCase: ImageAnalysis? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        previewView = findViewById(R.id.preview_view)
        tvFps = findViewById(R.id.tvFps)
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
        /*
        通过 ProcessCameraProvider 获取 cameraProvider
        cameraProvider 就是我们持有的相机实例
        */
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

    /**
     *      启动相机
     * */
    private fun startCamera() {
        if (cameraProvider != null) {
            cameraProvider!!.unbindAll()
            /*
            这一步是绑定预览界面，如果不需要预览界面，这一步克注释掉
            CameraX优势体验之一：预览界面可以根据开发者需求去取舍，而Camera1和Camera2则必须要预览界面
            */
            bindPreviewUseCase()
            // 这一步是绑定相机预览数据，可以获得相机每一帧的数据
            bindAnalysisUseCase()
        }
    }

    /**
     * 绑定预览界面。不需要预览界面可以不调用
     * */
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

    private var lastTime = 0L
    private var lastShowTime = 0L
    private var count = 0
    private var maxFrameMs = 0L
    private var minFrameMs = Long.MAX_VALUE


    private fun bindAnalysisUseCase() {
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        val builder = ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
        analysisUseCase = builder.build()
        analysisUseCase?.setAnalyzer(
            ContextCompat.getMainExecutor(this)
        ) { imageProxy: ImageProxy ->
            count += 1
            val currentTime = SystemClock.elapsedRealtime()
            val d = currentTime - lastShowTime
            maxFrameMs = maxFrameMs.coerceAtLeast(d)
            minFrameMs = minFrameMs.coerceAtMost(d)
            if ((currentTime - lastTime) >= 1000){
                tvFps.text = "fps: ${count}, 两帧最大：${maxFrameMs}, 最小：${minFrameMs}"
                lastTime = currentTime
                count = 0
                maxFrameMs = 0
                minFrameMs = Long.MAX_VALUE
            }
            lastShowTime = currentTime
            Log.d(TAG,"ImageProxy: 宽高： ${imageProxy.width} * ${imageProxy.height}")

            //必须close,相机才会下发下一帧数据,否则会一直阻塞相机下发数据
            imageProxy.close()
        }
        camera = cameraProvider!!.bindToLifecycle(
            this,
            cameraSelector!!,
            analysisUseCase
        )
    }

    private fun switchCamera() {
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