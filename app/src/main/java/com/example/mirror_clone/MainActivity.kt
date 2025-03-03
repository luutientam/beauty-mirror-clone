package com.example.mirror_clone

import android.animation.ObjectAnimator
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mirror_clone.databinding.ActivityMainBinding
import com.example.mirror_clone.ui.GalleryBottomSheet
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@Suppress("DEPRECATION")
class MainActivity() : AppCompatActivity(), Parcelable {
    // Khai báo các biến và đối tượng cần thiết
    private lateinit var binding: ActivityMainBinding // Binding để kết nối với layout
    private lateinit var cameraExecutor: ExecutorService // ExecutorService để xử lý các tác vụ camera
    private lateinit var cameraControl: CameraControl // Điều khiển camera
    private lateinit var imageCapture: ImageCapture // Đối tượng để chụp ảnh
    private var cameraProvider: ProcessCameraProvider? = null // Provider để quản lý vòng đời của camera
    private var camera: Camera? = null // Đối tượng camera
    private var isCameraActive = true // Biến kiểm tra xem camera có đang hoạt động không
    private val handler = Handler(Looper.getMainLooper()) // Handler để xử lý các tác vụ trên luồng chính
    private val hideUIRunnable = Runnable { hideAllButtons() } // Runnable để ẩn các nút UI
    private var lastFrameBitmap: Bitmap? = null // Bitmap để lưu trữ frame cuối cùng
    private val capturedImages = ArrayList<Uri>() // Danh sách lưu ảnh đã chụp
    private val REQUEST_CAMERA_PERMISSION = 1001
    constructor(parcel: Parcel) : this() {
        isCameraActive = parcel.readByte() != 0.toByte()
        lastFrameBitmap = parcel.readParcelable(Bitmap::class.java.classLoader)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) // Khởi tạo binding
        setContentView(binding.root) // Đặt layout cho activity

        //kiểm tra và yêu cầu quyền truy cập camera


        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            startCamera()
        }
        //kiểm tra và yêu cầu quyền truy cập bộ nhớ
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CAMERA_PERMISSION
            )
        }


        // Thiết lập sự kiện chạm để hiển thị các nút khi chạm vào màn hình
        binding.constraintLayoutVuot.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && isCameraActive) {
                showAllButtons()
            }
            true
        }

        // Ẩn nút lưu ảnh ban đầu
        binding.nutLuuAnh.visibility = View.GONE

        // Thiết lập sự kiện click cho nút dừng ảnh
        binding.nutDungAnh.setOnClickListener {
            toggleCamera()
        }

        // Thiết lập sự kiện click cho nút menu
        binding.menuButton.setOnClickListener {
            binding.drawerLayout.open()
            resetHideTimer()
        }

        // Khởi tạo ExecutorService để xử lý các tác vụ camera
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera() // Bắt đầu camera

        // Thiết lập sự kiện click cho nút lưu ảnh
        binding.nutLuuAnh.setOnClickListener {
            lastFrameBitmap?.let { bitmap ->
                saveImageToGallery(bitmap)
            }
        }
// // Thiết lập sự kiện click cho nút đèn viền
        binding.nutDenVien.setOnClickListener {
            val isActive = binding.denVienOverlay.visibility == View.VISIBLE

            if (isActive) {
                binding.denVienOverlay.visibility = View.GONE
                binding.nutDenVien.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
            } else {
                binding.denVienOverlay.visibility = View.VISIBLE
                binding.nutDenVien.setColorFilter(ContextCompat.getColor(this, R.color.yellow))
            }
        }

        // Thiết lập sự kiện click cho nút xem ảnh đã chụp
        binding.xemAnhDaChup    .setOnClickListener {
            if (capturedImages.isNotEmpty()) {
                val bottomSheet = GalleryBottomSheet(capturedImages)
                bottomSheet.show(supportFragmentManager, "com.example.mirror_clone.ui.GalleryBottomSheet")
            } else {
                Toast.makeText(this, "Chưa có ảnh nào!", Toast.LENGTH_SHORT).show()
            }
        }


        //chọn khung
/*        binding.chonKhung.setOnClickListener {
            val bottomSheet = BottomSheetChonKhung()
            bottomSheet.show(supportFragmentManager, "BottomSheetChonKhung")
        }


        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == 100 && resultCode == RESULT_OK) {
                val khungId = data?.getIntExtra("KHUNG_DA_CHON", 0) ?: return
                binding.denVienOverlay.setImageResource(khungId)
                binding.denVienOverlay.visibility = View.VISIBLE
            }
        }*/



        // Thiết lập sự kiện thay đổi giá trị cho SeekBar điều chỉnh độ sáng
        binding.brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                setScreenBrightness(progress / 100.0f)
                resetHideTimer()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                showAllButtons()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                startHideTimer()
            }
        })

        // Thiết lập sự kiện thay đổi giá trị cho SeekBar điều chỉnh zoom
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

        startHideTimer() // Bắt đầu đếm thời gian để ẩn các nút UI
    }

    // Hàm lưu ảnh vào thư viện
    private fun saveImageToGallery(bitmap: Bitmap) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "mirror_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MirrorClone")
        }

        val contentResolver = contentResolver
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let { uri ->
            val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                capturedImages.add(uri) // Lưu URI vào danh sách
                Toast.makeText(this, "Lưu ảnh thành công!", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "Lưu ảnh thất bại!", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "imageUri rỗng!", Toast.LENGTH_SHORT).show()
    }


    // Hàm điều chỉnh độ sáng màn hình
    private fun setScreenBrightness(brightness: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness
        window.attributes = layoutParams
    }

    // Hàm khởi động camera
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            cameraProvider?.unbindAll() // Giải phóng camera trước khi bind

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            camera = cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            cameraControl = camera!!.cameraControl
        }, ContextCompat.getMainExecutor(this))
    }

    // Hàm dừng preview camera và chụp frame cuối cùng
    private fun stopCameraPreview() {
        if (::imageCapture.isInitialized) {
            imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        lastFrameBitmap = image.toBitmap().rotate(image.imageInfo.rotationDegrees)
                        runOnUiThread {
                            binding.previewView.visibility = View.GONE
                            binding.capturedImageView.visibility = View.VISIBLE
                            binding.capturedImageView.setImageBitmap(lastFrameBitmap)
                        }
                        image.close()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraX", "Lỗi khi chụp ảnh giữ frame: ${exception.message}", exception)
                    }
                })
        }
    }

    // Hàm xoay ảnh
    private fun Bitmap.rotate(degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    // Hàm chuyển đổi ImageProxy thành Bitmap
    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    // Hàm làm mượt hiệu ứng fade in
    private fun fadeIn(view: View) {
        view.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply { duration = 300 }.start()
    }

    // Hàm làm mượt hiệu ứng fade out
    private fun fadeOut(view: View) {
        ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
            duration = 300
            start()
            doOnEnd { view.visibility = View.GONE }
        }
    }

    // Hàm chuyển đổi trạng thái camera


    private fun toggleCamera() {
        if (isCameraActive) {
            // Chụp frame cuối cùng và hiển thị
            binding.previewView.bitmap?.let {
                lastFrameBitmap = it
                binding.capturedImageView.setImageBitmap(lastFrameBitmap)
            }
            binding.previewView.visibility = View.GONE
            binding.capturedImageView.visibility = View.VISIBLE

            // Ẩn tất cả nút, chỉ hiển thị nutDungAnh và nutLuuAnh
            hideAllButtons()
            binding.nutDungAnh.visibility = View.VISIBLE
            binding.nutLuuAnh.visibility = View.VISIBLE

            // Đổi màu bông tuyết khi dừng
            binding.nutDungAnh.colorFilter = PorterDuffColorFilter(
                ContextCompat.getColor(this, R.color.light_blue), // Chọn màu mong muốn
                PorterDuff.Mode.SRC_IN
            )
        } else {
            // Nếu nhấn nút dừng lần 2 thì reset về ban đầu
            resetToInitialState()
            startHideTimer()

            // Khôi phục màu gốc
            binding.nutDungAnh.colorFilter = null
        }

        // Đảo trạng thái camera
        isCameraActive = !isCameraActive
    }


    // Hàm ẩn tất cả các nút
    private fun hideAllButtons() {
        binding.brightnessSeekBar.visibility = View.GONE
        binding.zoomSeekBar.visibility = View.GONE
        binding.menuButton.visibility = View.GONE
        binding.nutDungAnh.visibility = View.GONE
        binding.nutDenVien.visibility = View.GONE
        binding.chonKhung.visibility = View.GONE
        binding.xemAnhDaChup.visibility = View.GONE
        binding.nutLuuAnh.visibility = View.GONE
    }

    // Hàm hiển thị tất cả các nút
    private fun showAllButtons() {
        binding.brightnessSeekBar.visibility = View.VISIBLE
        binding.zoomSeekBar.visibility = View.VISIBLE
        binding.menuButton.visibility = View.VISIBLE
        binding.nutDungAnh.visibility = View.VISIBLE
        binding.nutDenVien.visibility = View.VISIBLE
        binding.chonKhung.visibility = View.VISIBLE
        binding.xemAnhDaChup.visibility = View.VISIBLE
        binding.nutLuuAnh.visibility = View.GONE
    }

    // Hàm bắt đầu đếm thời gian để ẩn các nút UI
    private fun startHideTimer() {
        handler.postDelayed({
            if (isCameraActive) {
                hideAllButtons()
            } else {
                // Nếu camera đã dừng, giữ lại nutDungAnh và nutLuuAnh
                binding.nutDungAnh.visibility = View.VISIBLE
                binding.nutLuuAnh.visibility = View.VISIBLE
            }
        }, 4000)
    }

    // Hàm reset về trạng thái ban đầu
    private fun resetToInitialState() {
        binding.previewView.visibility = View.VISIBLE
        binding.capturedImageView.visibility = View.GONE
        showAllButtons()
    }

    // Hàm reset bộ đếm thời gian ẩn UI
    private fun resetHideTimer() {
        handler.removeCallbacks(hideUIRunnable)
        startHideTimer()
    }

    // Hàm hủy activity
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (isCameraActive) 1 else 0)
        parcel.writeParcelable(lastFrameBitmap, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MainActivity> {
        override fun createFromParcel(parcel: Parcel): MainActivity {
            return MainActivity(parcel)
        }

        override fun newArray(size: Int): Array<MainActivity?> {
            return arrayOfNulls(size)
        }
    }
}