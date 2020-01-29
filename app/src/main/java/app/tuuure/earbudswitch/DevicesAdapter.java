package app.tuuure.earbudswitch;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {

    Set<RecycleItem> devices = new LinkedHashSet<>();
    Set<String> discoveries = new HashSet<>();

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
        RecycleItem item = (RecycleItem) devices.toArray()[position];

        holder.tvDevice.setText(item.budsName);
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
        if (!discoveries.contains(item.serverAddress)) {
            discoveries.add(item.serverAddress);
            devices.add(item);
            notifyItemInserted(devices.size());
        }
    }

    void devicesClear() {
        devices.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDevice;

        ViewHolder(View itemView) {
            super(itemView);
            tvDevice = itemView.findViewById(R.id.tv_device);
            Drawable dw = tvDevice.getCompoundDrawables()[0];
            dw.setTint(Color.BLACK);
            tvDevice.setCompoundDrawables(dw, null, null, null);
        }
    }
}
