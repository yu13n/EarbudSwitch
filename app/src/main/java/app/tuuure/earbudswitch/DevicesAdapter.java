package app.tuuure.earbudswitch;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {

    ArrayList<RecycleItem> bondedDevices = new ArrayList<>();
    private HashMap<String, Drawable> drawables = new HashMap<>(4);
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;
    private int refreshPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClickListener(View view, int position);
    }

    void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.mOnItemLongClickListener = listener;
    }

    DevicesAdapter() {
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    void setRefreshPosition(BluetoothDevice d) {
        int oldPosition = refreshPosition;
        if (d == null) {
            refreshPosition = -1;
        } else {
            int i;
            boolean contain = false;
            for (i = 0; i < bondedDevices.size(); i++) {
                if (bondedDevices.get(i).budsAddress.equals(d.getAddress())) {
                    contain = true;
                    break;
                }
            }
            if (contain) {
                refreshPosition = i;
            } else {
                refreshPosition = -1;
            }
        }

        if (refreshPosition != -1)
            notifyItemChanged(refreshPosition);
        if (oldPosition != -1)
            notifyItemChanged(oldPosition);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        RecycleItem item = bondedDevices.get(position);

        holder.tvDevice.setText(item.budsName);
        if (drawables.isEmpty()) {
            Drawable[] d = holder.tvDevice.getCompoundDrawablesRelative();
            drawables.put("hs", d[0]);
            drawables.put("ed", d[3]);
            drawables.put("able", d[2]);
            drawables.put("re", d[1]);
        }

        if (item.isConnected) {
            holder.tvDevice.setCompoundDrawables(drawables.get("hs"), null, drawables.get("ed"), null);
        } else if (item.serverAddress != null && !item.serverAddress.isEmpty()) {
            holder.tvDevice.setCompoundDrawables(drawables.get("hs"), null, drawables.get("able"), null);
        } else {
            if (position == refreshPosition) {
                holder.tvDevice.setCompoundDrawables(drawables.get("hs"), null, drawables.get("re"), null);

                ObjectAnimator anim = ObjectAnimator.ofInt(holder.tvDevice.getCompoundDrawables()[2], "level", 0, 10000);
                anim.setDuration(1000);
                anim.setRepeatMode(ValueAnimator.RESTART);
                anim.setRepeatCount(50);
                anim.start();
            } else {
                holder.tvDevice.setCompoundDrawables(drawables.get("hs"), null, null, null);
            }
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
        if (mOnItemLongClickListener != null) {
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemLongClickListener.onItemLongClickListener(holder.itemView, pos);
                    return true;
                }
            });
        }
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

    void setConnected(BluetoothDevice device, boolean isConnected) {
        RecycleItem item = new RecycleItem(device);
        item.isConnected = isConnected;
        RecycleItem tempItem;

        for (int i = 0; i < bondedDevices.size(); i++) {
            tempItem = bondedDevices.get(i);
            if (tempItem.budsAddress.equals(item.budsAddress)) {
                bondedDevices.set(i, item);
                if (i == refreshPosition)
                    refreshPosition = -1;
                notifyItemChanged(i);
                break;
            }
        }
    }

    void devicesReset(Set<BluetoothDevice> devices) {
        bondedDevices.clear();
        if (devices != null)
            for (BluetoothDevice device : devices) {
                if (BluetoothClass.Device.Major.AUDIO_VIDEO == device.getBluetoothClass().getMajorDeviceClass()
                        && device.getName() != null && !device.getName().isEmpty())
                    bondedDevices.add(new RecycleItem(device));
            }
        notifyDataSetChanged();
    }

    ArrayList<RecycleItem> getData() {
        return bondedDevices;
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
