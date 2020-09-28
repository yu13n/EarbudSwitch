package app.tuuure.earbudswitch.recyclerList

import android.annotation.SuppressLint
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import app.tuuure.earbudswitch.ConnectGattEvent
import app.tuuure.earbudswitch.R
import app.tuuure.earbudswitch.earbuds.EarbudManager
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import java.util.*

class ScanListAdapter(val context: Context) :
    RecyclerView.Adapter<ScanListAdapter.ViewHolder>() {
    //var data: LinkedList<ListItem> = LinkedList()
    var data: LinkedHashSet<ListItem> = LinkedHashSet()

    @SuppressLint("UseCompatLoadingForDrawables")
    val nearIcon: Drawable = context.getDrawable(R.drawable.ic_near_me_24)!!

    @SuppressLint("UseCompatLoadingForDrawables")
    val connectIcon: Drawable = context.getDrawable(R.drawable.ic_done_24)!!

    @SuppressLint("UseCompatLoadingForDrawables")
    val headSetIcon: Drawable = context.getDrawable(R.drawable.ic_headset_24)!!

    @SuppressLint("UseCompatLoadingForDrawables")
    val refreshIcon: Drawable = context.getDrawable(R.drawable.anim_refresh)!!

    fun updateData(devices: LinkedHashSet<ListItem>) {
        data.clear()
        if (devices.isNotEmpty()) {
            data = devices
        }
        notifyDataSetChanged()
    }

    fun setFresh(device: String, isFreshing: Boolean) {
        data.indexOf(ListItem("", device)).also {
            data.elementAt(it).isChecked = isFreshing
            notifyItemChanged(it)
        }
    }

    fun setSelected(profile: Int, devices: Collection<String>) {
        data.forEach {
            val isContained = it.address in devices

            when (profile) {
                BluetoothProfile.A2DP -> {
                    if (it.isA2dpConnected != isContained) {
                        it.isA2dpConnected = isContained
                        notifyItemChanged(data.indexOf(it))
                    }
                }
                BluetoothProfile.HEADSET -> {
                    if (it.isHeadsetConnected != isContained) {
                        it.isHeadsetConnected = isContained
                        notifyItemChanged(data.indexOf(it))
                    }
                }
            }

        }
    }

    var job = CoroutineScope(Dispatchers.Default).launch { }

    fun setServer(server: String, devices: Collection<String>) {
        job.cancel()
        data.forEach {
            val isContained = it.address in devices
            if ((it.server == server) != isContained) {
                it.server = if (isContained) server else ""
                notifyItemChanged(data.indexOf(it))
            }
        }
        job = CoroutineScope(Dispatchers.Default).launch {
            delay(2000)
            data.forEach {
                if (it.server != "") {
                    it.server = ""
                    withContext(Dispatchers.Main) {
                        notifyItemChanged(data.indexOf(it))
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data.elementAt(position)
        holder.textView.text = item.name
        holder.itemView.setOnClickListener(holder)

        var drawable: Drawable? = null
        if (item.server.isNotEmpty())
            drawable = nearIcon
        if (item.isChecked) {
            drawable = refreshIcon
        }
        if (item.isA2dpConnected || item.isHeadsetConnected)
            drawable = connectIcon

        holder.textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            headSetIcon,
            null,
            drawable,
            null
        )

        if (drawable is Animatable) {
            (drawable as Animatable).start()
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
            val item = data.elementAt(wrapperPosition)

            if (item.isChecked) {
                return
            }
            if (item.isHeadsetConnected || item.isA2dpConnected) {
                AlertDialog.Builder(context).apply {
                    setTitle(R.string.toast_disconnect_title)
                    setMessage(
                        String.format(
                            context.getString(R.string.toast_disconnect_content),
                            item.name
                        )
                    )
                    setNegativeButton(R.string.toast_disconnect_negative) { _, _ -> }
                    setPositiveButton(R.string.toast_disconnect_positive) { _, _ ->
                        EarbudManager.disconnectEBS(
                            context,
                            item.address
                        )
                    }
                }.show()
            } else {
                if (item.server.isEmpty()) {
                    EarbudManager.connectEBS(context, item.address)
                } else {
                    EventBus.getDefault().post(ConnectGattEvent(item.server, item.address))
                }
            }
        }
    }
}