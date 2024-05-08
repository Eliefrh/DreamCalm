package protect.babysleepsounds

import android.content.Context
import android.icu.text.Transliterator.Position
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView

/**
 * This is the SoundAdapter class that extends BaseAdapter.
 * It is responsible for providing views for an AdapterView, where each view represents an item in a data set.
 *
 * @param context This is the context in which the SoundAdapter class is used.
 * @param soundItems This is the list of sound items that the adapter will represent.
 */
class SoundAdapter(private val context: Context, private val soundItems: List<SoundItem>) :
    BaseAdapter() {
    // The position of the currently selected item
    private var selectedItemPosition = -1
    /**
     * This function returns the count of items in the data set represented by this adapter.
     */
    override fun getCount(): Int {
        // Return the size of the sound items list
        return soundItems.size
    }
    /**
     * This function returns the data item associated with the specified position in the data set.
     *
     * @param position This is the position of the item within the adapter's data set.
     */
    override fun getItem(position: Int): Any {
        // Return the sound item at the specified position
        return soundItems[position]
    }
    /**
     * This function returns the row id associated with the specified position in the list.
     *
     * @param position This is the position of the item within the adapter's data set.
     */
    override fun getItemId(position: Int): Long {
        // The item ID is the same as its position
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
        // Inflate a new view if convertView is null, otherwise reuse it
        val itemView = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.custom_grid_item, parent, false)
        // Find the ImageView within the inflated view
        val iconImageView = itemView.findViewById<ImageView>(R.id.icon)
        // Set the image resource of the ImageView to the image resource ID of the sound item at the specified position
        iconImageView.setImageResource(soundItems[position].imageResId)

        // Change the background color of the view based on whether it's the selected item
        if (position == selectedItemPosition) {
            itemView.setBackgroundResource(R.color.colorPrimary)
        } else {
            itemView.setBackgroundResource(android.R.color.transparent)
        }
        // Return the fully configured view
        return itemView
    }
    /**
     * This function sets the selected item position and notifies the data set changed.
     *
     * @param position This is the position of the selected item.
     */
    fun setSelectedItem(position: Int) {
        // Update the selected item position
        selectedItemPosition = position
        // Notify the data set changed to update the views
        notifyDataSetChanged()
    }
}
