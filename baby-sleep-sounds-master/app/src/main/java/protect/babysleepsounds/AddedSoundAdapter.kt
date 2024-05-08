package protect.babysleepsounds

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

/**
 * This is the AddedSoundAdapter class that extends BaseAdapter.
 * It is responsible for providing views for an AdapterView, where each view represents an item in a data set.
 *
 * @param context This is the context in which the AddedSoundAdapter class is used.
 * @param soundItems This is the list of sound items that the adapter will represent.
 */
class AddedSoundAdapter(
    private val context: Context,
    private val soundItems: List<AddedSoundItem>
) :
    BaseAdapter() {
    private var selectedItemPosition = -1

    /**
     * This function returns the count of items in the data set represented by this adapter.
     */
    override fun getCount(): Int {
        return soundItems.size
    }
    /**
     * This function returns the data item associated with the specified position in the data set.
     *
     * @param position This is the position of the item within the adapter's data set.
     */
    override fun getItem(position: Int): Any {
        Log.d("Elie", position.toString())
        return soundItems[position]
    }
    /**
     * This function returns the row id associated with the specified position in the list.
     *
     * @param position This is the position of the item within the adapter's data set.
     */
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
    /**
     * This function gets a View that displays the data at the specified position in the data set.
     *
     * @param position This is the position of the item within the adapter's data set.
     * @param convertView This is the old view to reuse, if possible.
     * @param parent This is the parent that this view will eventually be attached to.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.custom_grid_item2, parent, false)
        // Find views within the item layout
        val iconImageView = itemView.findViewById<ImageView>(R.id.icon)
        val nameTextView = itemView.findViewById<TextView>(R.id.name)
        val dateTextView = itemView.findViewById<TextView>(R.id.date)

        // Set the image resource for the ImageView
        iconImageView.setImageResource(soundItems[position].imageResId)

        // Extract the name part from the file name
        val fileName = soundItems[position].path
        val name = extractNameFromFileName(fileName)

        // Set the extracted name in the TextView
        nameTextView.text = name
        dateTextView.text = soundItems[position].creationDate
        
        // Set background color based on selection state
        if (position == selectedItemPosition) {
            itemView.setBackgroundResource(R.color.colorPrimary)
        } else {
            itemView.setBackgroundResource(android.R.color.transparent)
        }

        return itemView
    }
    /**
     * This function extracts the name from the file name.
     *
     * @param fileName This is the file name from which the name will be extracted.
     */
    private fun extractNameFromFileName(fileName: String): String {
        // Split the file name at underscores and extract the second part
        val parts = fileName.split("_")
        return if (parts.size >= 2) {
            parts[1]  // Second part is the name
        } else {
            "Unknown"  // Return a default value if name extraction fails
        }
    }
    /**
     * This function sets the selected item position and notifies the data set changed.
     *
     * @param position This is the position of the selected item.
     */
    fun setSelectedItem(position: Int) {
        selectedItemPosition = position
        notifyDataSetChanged()
    }
}
