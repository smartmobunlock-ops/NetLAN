package com.mistfile.netlan;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class LanBroadcastRelay extends Service implements Runnable {

    private static final String TAG = "NetLAN_Relay";
    private WifiManager.MulticastLock multicastLock;
    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;
    private DatagramSocket listenSocket;
    private boolean isRunning = false;
    private int relayPort = 19132;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isRunning) return START_STICKY;

        if (intent != null && intent.hasExtra("GAME_PORT")) {
            relayPort = intent.getIntExtra("GAME_PORT", 19132);
        }

        try {
            // Trava de Multicast (ESSENCIAL para Minecraft encontrar salas no Hotspot)
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                multicastLock = wifi.createMulticastLock("NetLAN_MulticastLock");
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();

                wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "NetLAN_WifiLock");
                wifiLock.acquire();
            }

            // Impede a CPU de suspender durante a partida
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NetLAN:WakeLock");
                wakeLock.acquire(120 * 60 * 1000L); // 2 horas max
            }

            isRunning = true;
            new Thread(this, "NetLAN-LanListener").start();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao inicializar LanBroadcastRelay", e);
        }

        return START_STICKY;
    }

    @Override
    public void run() {
        try {
            listenSocket = new DatagramSocket(relayPort);
            listenSocket.setBroadcast(true);
            byte[] buf = new byte[8192];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (isRunning) {
                listenSocket.receive(packet);
                Log.d(TAG, "Recebido pacote UDP de " + packet.getAddress().getHostAddress() + " tamanho: " + packet.getLength());
            }
        } catch (Exception e) {
            Log.e(TAG, "Exceção no socket UDP de retransmissão", e);
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        try {
            if (listenSocket != null) listenSocket.close();
            if (multicastLock != null && multicastLock.isHeld()) multicastLock.release();
            if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}