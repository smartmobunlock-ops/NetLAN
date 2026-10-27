package com.mistfile.netlan;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int VPN_REQUEST_CODE = 1001;
    private EditText editGamePort;
    private TextView textStatus;
    private Button btnHost, btnClient, btnStop;
    private boolean isHostMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editGamePort = findViewById(R.id.edit_game_port);
        textStatus = findViewById(R.id.text_status);
        btnHost = findViewById(R.id.btn_host);
        btnClient = findViewById(R.id.btn_client);
        btnStop = findViewById(R.id.btn_stop);

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
            int port = 7777;
            try {
                port = Integer.parseInt(editGamePort.getText().toString());
            } catch (Exception ignored) {}

            Intent serviceIntent = new Intent(this, LocalLanVpnService.class);
            serviceIntent.putExtra("GAME_PORT", port);
            serviceIntent.putExtra("IS_HOST", isHostMode);
            startService(serviceIntent);

            Intent relayIntent = new Intent(this, LanBroadcastRelay.class);
            startService(relayIntent);

            textStatus.setText("Status: " + (isHostMode ? "SALA HOST ATIVA" : "CLIENTE CONECTADO") + " (Porta: " + port + ")");
            btnStop.setVisibility(View.VISIBLE);
            Toast.makeText(this, "NetLAN Ativo! Abra o seu jogo agora.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Permissão de VPN recusada!", Toast.LENGTH_SHORT).show();
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