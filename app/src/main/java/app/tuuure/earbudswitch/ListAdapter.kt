package app.tuuure.earbudswitch

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.coroutines.*
import java.util.*

class ListAdapter(var context: Context) : RecyclerView.Adapter<ListAdapter.ViewHolder>() {
    var data: LinkedList<ListItem> = LinkedList()

    suspend fun updateData(devices: MutableMap<String, String>) {
        data.clear()
        if (devices.isNotEmpty()) {
            val task = CoroutineScope(Dispatchers.Default).async(Dispatchers.IO) {
                SPreferences.getRestrictItem()
            }
            val restrictItems: Set<String> = task.await()
            for (d in devices.entries) {
                val isChecked = restrictItems.contains(d.value)
                if (isChecked) {
                    data.addFirst(ListItem(sortedMapOf(d.key to d.value), isChecked))
                } else {
                    data.addLast(ListItem(sortedMapOf(d.key to d.value), isChecked))
                }
            }
        }
        withContext(Dispatchers.Main) {
            this@ListAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.checkBox.text = item.getName()
        holder.checkBox.isChecked = item.state
        holder.checkBox.setOnCheckedChangeListener(holder)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        CompoundButton.OnCheckedChangeListener {
        val checkBox: MaterialCheckBox = itemView.findViewById(R.id.list_item)

        override fun onCheckedChanged(compoundButton: CompoundButton, b: Boolean) {
            val wrapperPosition = adapterPosition
            data[wrapperPosition].state = b

            GlobalScope.launch(Dispatchers.IO) {
                if (b) {
                    SPreferences.addRestrictItem(data[wrapperPosition].devices.values)
                } else {
                    SPreferences.delRestrictItem(data[wrapperPosition].devices.values)
                }
            }
        }
    }
}