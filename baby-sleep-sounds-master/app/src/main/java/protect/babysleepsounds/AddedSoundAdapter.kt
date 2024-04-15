package protect.babysleepsounds

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class AddedSoundAdapter(private val context: Context, private val soundItems: List<AddedSoundItem>) :
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
            .inflate(R.layout.custom_grid_item2, parent, false)

        val iconImageView = itemView.findViewById<ImageView>(R.id.icon)
        val nameTextView = itemView.findViewById<TextView>(R.id.name)

        // Set the image resource for the ImageView
        iconImageView.setImageResource(soundItems[position].imageResId)

        // Extract the name part from the file name
        val fileName = soundItems[position].path
        val name = extractNameFromFileName(fileName)

        // Set the extracted name in the TextView
        nameTextView.text = name

        return itemView
    }

    private fun extractNameFromFileName(fileName: String): String {
        // Split the file name at underscores and extract the second part
        val parts = fileName.split("_")
        return if (parts.size >= 2) {
            parts[1]  // Second part is the name
        } else {
            "Unknown"  // Return a default value if name extraction fails
        }
    }
}
