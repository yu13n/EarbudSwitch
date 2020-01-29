package app.tuuure.earbudswitch;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class QuickSetting extends TileService {
    static final String TAG = "QuickSetting";

    @Override
    public void onClick() {
        super.onClick();
        // 点击的时候
        Log.d(TAG, "onClick");
        Intent intent = new Intent(getApplicationContext(), BHTDialog.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(intent);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        // 打开下拉通知栏的时候调用,当快速设置按钮并没有在编辑栏拖到设置栏中不会调用
        //在TleAdded之后会调用一次
        Tile tile = getQsTile();
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
        Log.d(TAG, "onStartListening");
    }

    @Override
    public void onStopListening() {
        // 关闭下拉通知栏的时候调用,当快速设置按钮并没有在编辑栏拖到设置栏中不会调用
        // 在onTileRemoved移除之前也会调用移除
        Log.d(TAG, "onStopListening");
        super.onStopListening();
    }
}
