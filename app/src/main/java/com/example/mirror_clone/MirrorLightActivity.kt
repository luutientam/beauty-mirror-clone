package com.example.mirror_clone

import MirrorLight
import MirrorLightAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mirror_clone.databinding.ActivityMirrorLightBinding

class MirrorLightActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMirrorLightBinding

    private val mirrorLights = listOf(
        MirrorLight(R.drawable.den_vien_1, false),
        MirrorLight(R.drawable.den_vien_2, false),
        MirrorLight(R.drawable.den_vien_3, true),
        /*MirrorLight(R.drawable.light_4, true),
        MirrorLight(R.drawable.light_5, false)*/
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Gán View Binding
        binding = ActivityMirrorLightBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Thiết lập GridView với Adapter
        val adapter = MirrorLightAdapter(this, mirrorLights)
        binding.gridView.adapter = adapter
    }
}
