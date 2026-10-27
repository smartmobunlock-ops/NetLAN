package com.mistfile.netlan;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class LocalLanVpnService extends VpnService implements Runnable {

    private static final String TAG = "NetLAN_VPN";
    private ParcelFileDescriptor vpnInterface;
    private Thread workerThread;
    private boolean isRunning = false;
    private DatagramSocket localSocket;
    private int targetGamePort = 7777;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isRunning) return START_STICKY;

        if (intent != null && intent.hasExtra("GAME_PORT")) {
            targetGamePort = intent.getIntExtra("GAME_PORT", 7777);
        }

        try {
            Builder builder = new Builder();
            builder.setSession("NetLAN_Bridge");
            builder.setMtu(1500);
            builder.addAddress("10.0.0.2", 32);
            builder.addRoute("0.0.0.0", 0);

            try {
                builder.addDisallowedApplication(getPackageName());
            } catch (Exception e) {
                Log.e(TAG, "Isolamento falhou", e);
            }

            vpnInterface = builder.establish();
            localSocket = new DatagramSocket();
            localSocket.setBroadcast(true);

            isRunning = true;
            workerThread = new Thread(this, "NetLAN-PacketReader");
            workerThread.start();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar VpnService", e);
        }

        return START_STICKY;
    }

    @Override
    public void run() {
        if (vpnInterface == null) return;

        FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
        byte[] buffer = new byte[32768];

        try {
            InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");

            while (isRunning) {
                int length = in.read(buffer);
                if (length > 0) {
                    int protocol = buffer[9] & 0xFF; // 17 = UDP
                    if (protocol == 17 && length >= 28) {
                        int dstPort = ((buffer[22] & 0xFF) << 8) | (buffer[23] & 0xFF);
                        if (dstPort == targetGamePort) {
                            int payloadLen = length - 28;
                            if (payloadLen > 0) {
                                DatagramPacket packet = new DatagramPacket(
                                    buffer, 28, payloadLen, broadcastAddr, 8888
                                );
                                localSocket.send(packet);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro no loop TUN", e);
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        try {
            if (localSocket != null) localSocket.close();
            if (vpnInterface != null) vpnInterface.close();
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}