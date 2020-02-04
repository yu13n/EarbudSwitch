package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {

    ArrayList<RecycleItem> bondedDevices = new ArrayList<>();
    Drawable[] drawables;

    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    DevicesAdapter() {
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        RecycleItem item = bondedDevices.get(position);

        holder.tvDevice.setText(item.budsName);
        if (drawables == null || drawables.length == 0) {
            drawables = holder.tvDevice.getCompoundDrawablesRelative();
        }
        if (item.serverAddress != null && !item.serverAddress.isEmpty()) {
            holder.tvDevice.setCompoundDrawables(drawables[0], null, drawables[2], null);
        } else {
            holder.tvDevice.setCompoundDrawables(drawables[0], null, null, null);
        }
        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.itemView, pos);
                }
            });
        }
    }

    void addDevice(RecycleItem item) {
        bondedDevices.add(item);
        notifyItemInserted(bondedDevices.size());
    }

    void setConnectable(RecycleItem item) {
        for (int i = 0; i < bondedDevices.size(); i++) {
            RecycleItem tempItem = bondedDevices.get(i);
            if (tempItem.budsAddress.equals(item.budsAddress)) {
                bondedDevices.set(i, item);
                notifyItemChanged(i);
                break;
            }
        }
    }

    void devicesReset(Set<BluetoothDevice> devices) {
        bondedDevices.clear();
        for (BluetoothDevice device : devices) {
            if (device.getName() != null && !device.getName().isEmpty())
                bondedDevices.add(new RecycleItem(device));
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return bondedDevices.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDevice;

        ViewHolder(View itemView) {
            super(itemView);
            tvDevice = itemView.findViewById(R.id.tv_device);
        }
    }
}
