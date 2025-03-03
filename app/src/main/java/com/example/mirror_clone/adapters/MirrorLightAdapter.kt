package com.example.mirror_clone.adapters
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.RadioButton
import com.example.mirror_clone.R
import com.example.mirror_clone.models.MirrorLight

class MirrorLightAdapter(private val context: Context, private val lights: List<MirrorLight>) : BaseAdapter() {

    private var selectedPosition = -1

    override fun getCount(): Int = lights.size
    override fun getItem(position: Int): Any = lights[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_mirror_light, parent, false)

        val imgLight = view.findViewById<ImageView>(R.id.imgLight)
        val radioButton = view.findViewById<RadioButton>(R.id.radioButton)

        val light = lights[position]

        imgLight.setImageResource(light.imageResId)
        radioButton.isChecked = position == selectedPosition

        view.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
        }

        return view
    }
}
