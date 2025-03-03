package com.example.mirror_clone.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mirror_clone.adapters.CapturedImagesAdapter
import com.example.mirror_clone.databinding.BottomSheetGalleryBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class GalleryBottomSheet(private val imageUris: MutableList<Uri>) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetGalleryBinding
    private lateinit var adapter: CapturedImagesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewGallery.layoutManager = GridLayoutManager(context, 3)

        adapter = CapturedImagesAdapter(imageUris, requireContext()) { position ->
            imageUris.removeAt(position) // Xóa ảnh khỏi danh sách
            adapter.notifyItemRemoved(position)
        }
        binding.recyclerViewGallery.adapter = adapter
    }
}
