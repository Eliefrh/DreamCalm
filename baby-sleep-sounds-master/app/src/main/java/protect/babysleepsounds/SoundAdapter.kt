package protect.babysleepsounds

import android.content.Context
import android.icu.text.Transliterator.Position
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView

class SoundAdapter(private val context: Context, private val soundItems: List<SoundItem>) :
    BaseAdapter() {

    override fun getCount(): Int {
        return soundItems.size
    }

    override fun getItem(position: Int): Any {
        Log.d("Elie", position.toString())
        return soundItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.custom_grid_item, parent, false)
        val iconImageView = itemView.findViewById<ImageView>(R.id.icon)
        iconImageView.setImageResource(soundItems[position].imageResId)
        return itemView
    }
}
