package com.example.tamir.seeyou;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

/**
 * Created by Tamir on 15/07/2017.
 */

public class RestartService extends BroadcastReceiver {

    Context context;
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context,MainService.class));


    }
}
