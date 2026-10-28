package com.mistfile.netlan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int VPN_REQUEST_CODE = 1001;
    private EditText editGamePort;
    private TextView textStatus;
    private TextView textIpInfo;
    private Button btnHost, btnClient, btnStop;
    private Button btnPresetMinecraft, btnPresetTerraria;
    private boolean isHostMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editGamePort = findViewById(R.id.edit_game_port);
        textStatus = findViewById(R.id.text_status);
        textIpInfo = findViewById(R.id.text_ip_info);
        btnHost = findViewById(R.id.btn_host);
        btnClient = findViewById(R.id.btn_client);
        btnStop = findViewById(R.id.btn_stop);
        btnPresetMinecraft = findViewById(R.id.btn_preset_mc);
        btnPresetTerraria = findViewById(R.id.btn_preset_terraria);

        // Predefinição Minecraft Bedrock (19132 UDP)
        btnPresetMinecraft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editGamePort.setText("19132");
                Toast.makeText(MainActivity.this, "Porta definida para Minecraft Bedrock (19132)", Toast.LENGTH_SHORT).show();
            }
        });

        // Predefinição Terraria (7777 TCP/UDP)
        btnPresetTerraria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editGamePort.setText("7777");
                Toast.makeText(MainActivity.this, "Porta definida para Terraria (7777)", Toast.LENGTH_SHORT).show();
            }
        });

        btnHost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHostMode = true;
                startVpnProcess();
            }
        });

        btnClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHostMode = false;
                startVpnProcess();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVpnProcess();
            }
        });

        updateIpAddressDisplay();
    }

    private void updateIpAddressDisplay() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null && wm.getConnectionInfo() != null) {
                int ipInt = wm.getConnectionInfo().getIpAddress();
                String ip = Formatter.formatIpAddress(ipInt);
                if (ip != null && !ip.equals("0.0.0.0")) {
                    textIpInfo.setText("Seu IP Wi-Fi atual: " + ip);
                    return;
                }
            }
        } catch (Exception ignored) {}
        textIpInfo.setText("IP Padrão Hotspot: 192.168.43.1 (Host)");
    }

    private void startVpnProcess() {
        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        } else {
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            int port = 19132;
            try {
                port = Integer.parseInt(editGamePort.getText().toString());
            } catch (Exception ignored) {}

            Intent serviceIntent = new Intent(this, LocalLanVpnService.class);
            serviceIntent.putExtra("GAME_PORT", port);
            serviceIntent.putExtra("IS_HOST", isHostMode);
            startService(serviceIntent);

            Intent relayIntent = new Intent(this, LanBroadcastRelay.class);
            relayIntent.putExtra("GAME_PORT", port);
            startService(relayIntent);

            String modeStr = isHostMode ? "SALA HOST ATIVA" : "MODO CLIENTE ATIVO";
            textStatus.setText("Status: " + modeStr + " (Porta: " + port + ")");
            btnStop.setVisibility(View.VISIBLE);

            String tip = isHostMode 
                ? "Host Ativo! Se seu amigo não ver a sala no Minecraft, mande ele adicionar o Servidor IP: 192.168.43.1 porta " + port
                : "Cliente Ativo! Abra o Minecraft e procure na aba Amigos ou adicione o IP 192.168.43.1";
            Toast.makeText(this, tip, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Permissão de VPN recusada! É necessária para rotear os pacotes.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopVpnProcess() {
        stopService(new Intent(this, LocalLanVpnService.class));
        stopService(new Intent(this, LanBroadcastRelay.class));
        textStatus.setText("Status: Desconectado");
        btnStop.setVisibility(View.GONE);
        Toast.makeText(this, "NetLAN Encerrado", Toast.LENGTH_SHORT).show();
    }
}