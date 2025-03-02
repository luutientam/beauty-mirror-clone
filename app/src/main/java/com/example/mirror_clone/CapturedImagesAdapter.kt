import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.mirror_clone.databinding.DialogImageBinding
import com.example.mirror_clone.databinding.ItemCapturedImageBinding

class CapturedImagesAdapter(
    private val images: MutableList<Uri>,
    private val context: Context,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<CapturedImagesAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCapturedImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri, onClick: () -> Unit) {
            binding.imageViewItem.setImageURI(uri)
            binding.imageViewItem.setOnClickListener { onClick() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCapturedImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position]) {
            showImageDialog(images[position], position)
        }
    }

    override fun getItemCount(): Int = images.size

    // Hiển thị ảnh full màn hình + menu ba chấm
    private fun showImageDialog(imageUri: Uri, position: Int) {
        val dialogBinding = DialogImageBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.imageViewFull.setImageURI(imageUri)
        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }

        // Xử lý nút ba chấm (PopupMenu)
        dialogBinding.btnMenu.setOnClickListener {
            val popupMenu = PopupMenu(context, dialogBinding.btnMenu)
            popupMenu.menuInflater.inflate(com.example.mirror_clone.R.menu.image_options_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    com.example.mirror_clone.R.id.menu_delete -> {
                        onDelete(position) // Xóa ảnh
                        dialog.dismiss()
                        true
                    }
                    com.example.mirror_clone.R.id.menu_share -> {
                        shareImage(imageUri) // Chia sẻ ảnh
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        dialog.show()
    }

    // Chia sẻ ảnh
    private fun shareImage(imageUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ ảnh qua"))
    }
}
