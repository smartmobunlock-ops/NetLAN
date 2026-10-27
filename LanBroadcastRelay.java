package com.mistfile.netlan;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class LanBroadcastRelay extends Service implements Runnable {

    private static final String TAG = "NetLAN_Relay";
    private WifiManager.MulticastLock multicastLock;
    private DatagramSocket listenSocket;
    private boolean isRunning = false;
    private final int RELAY_PORT = 8888;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isRunning) return START_STICKY;

        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                multicastLock = wifi.createMulticastLock("NetLAN_WiFiLock");
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();
            }

            isRunning = true;
            new Thread(this, "NetLAN-LanListener").start();
        } catch (Exception e) {
            Log.e(TAG, "Erro no Relay", e);
        }

        return START_STICKY;
    }

    @Override
    public void run() {
        try {
            listenSocket = new DatagramSocket(RELAY_PORT);
            listenSocket.setBroadcast(true);
            byte[] buf = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (isRunning) {
                listenSocket.receive(packet);
                Log.d(TAG, "Recebidos " + packet.getLength() + " bytes de " + packet.getAddress().getHostAddress());
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro UDP", e);
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (listenSocket != null) listenSocket.close();
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}