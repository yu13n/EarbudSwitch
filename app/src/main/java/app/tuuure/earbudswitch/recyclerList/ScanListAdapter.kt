package app.tuuure.earbudswitch.recyclerList

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.tuuure.earbudswitch.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ScanListAdapter(val context: Context) :
    RecyclerView.Adapter<ScanListAdapter.ViewHolder>() {
    var data: LinkedList<ListItem> = LinkedList()

    @SuppressLint("UseCompatLoadingForDrawables")
    var nearIcon: Drawable = context.getDrawable(R.drawable.ic_near_me_24)!!

    @SuppressLint("UseCompatLoadingForDrawables")
    var connectIcon: Drawable = context.getDrawable(R.drawable.ic_done_24)!!

    @SuppressLint("UseCompatLoadingForDrawables")
    var headSetIcon: Drawable = context.getDrawable(R.drawable.ic_headset_24)!!

    fun updateData(devices: LinkedList<ListItem>) {
        data.clear()
        if (devices.isNotEmpty()) {
            data = devices
        }
        notifyDataSetChanged()
    }

    fun setServer(server: String, devices: Collection<String>) {
        data.forEach {
            if (it.address in devices) {
                it.server = server
                notifyItemChanged(data.indexOf(it))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.textView.text = item.name
        holder.itemView.setOnClickListener(holder)

        val drawable: Drawable? = when {
            item.server.isNotEmpty() -> nearIcon
            item.isChecked -> nearIcon
            else -> null
        }

        CoroutineScope(Dispatchers.Main).launch {
            holder.textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                headSetIcon,
                null,
                drawable,
                null
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val textView: TextView = itemView.findViewById(R.id.list_item_text)

        override fun onClick(view: View?) {
            val wrapperPosition = adapterPosition
            val item = data[wrapperPosition]
        }
    }
}