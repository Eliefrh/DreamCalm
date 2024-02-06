package protect.babysleepsounds
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView

class SoundAdapter(context: Context, private val soundItems: List<SoundItem>) :
    ArrayAdapter<SoundItem>(context, 0, soundItems) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.custom_spinner_item, parent, false)

        val soundItem = soundItems[position]
        val imageView = itemView.findViewById<ImageView>(R.id.icon)
        imageView.setImageResource(soundItem.imageResId)

        return itemView
    }
}
