package app.tuuure.earbudswitch;

import android.bluetooth.BluetoothDevice;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Set;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {

    Set<BluetoothDevice> devices;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener{
        void onItemClick(View view, int position);
    }

    void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    DevicesAdapter (Set<BluetoothDevice> initData){
        devices = initData;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        BluetoothDevice device = (BluetoothDevice) devices.toArray()[position];
        String name = device.getName();
        if(name == null){
            holder.tvDevice.setText(device.getAddress());
        }else{
            holder.tvDevice.setText(name);
        }
        if(mOnItemClickListener != null){
            holder.itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder.itemView, pos);
                }
            });
        }
    }

    public void addDevice(BluetoothDevice device) {
        devices.add(device);
        notifyItemInserted(devices.size());
    }

    public void devicesClear() {
        devices.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount(){
        return devices.size();
    }

    class ViewHolder extends  RecyclerView.ViewHolder{
        private TextView tvDevice;

        ViewHolder(View itemView){
            super(itemView);
            tvDevice = itemView.findViewById(R.id.tv_device);
            Drawable dw = tvDevice.getCompoundDrawables()[0];
            dw.setTint(Color.BLACK);
            tvDevice.setCompoundDrawables(dw,null,null,null);
        }
    }
}
