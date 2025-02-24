package com.example.mirror_clone

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.mirror_clone.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraControl: CameraControl
    private val handler = Handler(Looper.getMainLooper())
    private val hideUIRunnable = Runnable { hideAllButtons() }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lắng nghe sự kiện chạm trên ConstraintLayout để hiện lại các nút
        binding.constraintLayoutVuot.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                showAllButtons()
            }
            true
        }

        // Xử lý mở menu khi nhấn vào nút menu
        binding.menuButton.setOnClickListener {
            binding.drawerLayout.open()
            resetHideTimer()
        }

        // Khởi động camera
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()

        // Điều chỉnh độ sáng bằng SeekBar
        binding.brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val brightness = progress / 100.0f
                setScreenBrightness(brightness)
                resetHideTimer()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                showAllButtons()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                startHideTimer()
            }
        })

        // **Xử lý zoom bằng SeekBar**
        binding.zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (::cameraControl.isInitialized) {
                    cameraControl.setLinearZoom(progress / 100f)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                showAllButtons()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                startHideTimer()
            }
        })

        startHideTimer()
    }

    // Thiết lập độ sáng màn hình
    private fun setScreenBrightness(brightness: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness
        window.attributes = layoutParams
    }

    // Khởi động Camera và lưu cameraControl để điều chỉnh zoom
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            val camera: Camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            cameraControl = camera.cameraControl // Lưu lại cameraControl để zoom

        }, ContextCompat.getMainExecutor(this))
    }

    // Ẩn tất cả các nút trên màn hình
    private fun hideAllButtons() {
        binding.brightnessSeekBar.visibility = View.GONE
        binding.zoomSeekBar.visibility = View.GONE
        binding.menuButton.visibility = View.GONE
        binding.nutDungAnh.visibility = View.GONE
        binding.nutDenVien.visibility = View.GONE
    }

    // Hiện lại tất cả các nút
    private fun showAllButtons() {
        binding.brightnessSeekBar.visibility = View.VISIBLE
        binding.zoomSeekBar.visibility = View.VISIBLE
        binding.menuButton.visibility = View.VISIBLE
        binding.nutDungAnh.visibility = View.VISIBLE
        binding.nutDenVien.visibility = View.VISIBLE
        resetHideTimer()
    }

    // Bắt đầu bộ đếm để tự động ẩn các nút sau 4 giây
    private fun startHideTimer() {
        handler.postDelayed(hideUIRunnable, 4000)
    }

    // Reset bộ đếm thời gian khi có tương tác
    private fun resetHideTimer() {
        handler.removeCallbacks(hideUIRunnable)
        startHideTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
