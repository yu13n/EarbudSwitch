package app.tuuure.earbudswitch.recyclerList

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.utils.Preferences
import kotlinx.coroutines.*
import java.util.*

class FilterListAdapter(var context: Context) :
    RecyclerView.Adapter<FilterListAdapter.ViewHolder>() {
    var data: LinkedList<ListItem> = LinkedList()

    fun updateData(devices: LinkedList<ListItem>) {
        data.clear()
        if (devices.isNotEmpty()) {
            data = devices
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.textView.text = item.name
        holder.itemView.setOnClickListener(holder)
        holder.checkBox.visibility = View.VISIBLE
        holder.checkBox.isChecked = item.isChecked
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val textView: TextView = itemView.findViewById(R.id.list_item_text)
        val checkBox: CheckBox = itemView.findViewById(R.id.list_item_check)

        override fun onClick(view: View?) {
            val wrapperPosition = adapterPosition
            val b = !data[wrapperPosition].isChecked
            data[wrapperPosition].isChecked = b
            notifyItemChanged(wrapperPosition)

            GlobalScope.launch(Dispatchers.IO) {
                if (b) {
                    Preferences.getInstance(context).addRestrictItem(data[wrapperPosition].address)
                } else {
                    Preferences.getInstance(context).delRestrictItem(data[wrapperPosition].address)
                }
            }
        }
    }
}