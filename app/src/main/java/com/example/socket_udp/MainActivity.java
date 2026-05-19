package com.example.socket_udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private DatagramSocket server = null;
    private Handler handler = null;
    private Thread receiveThread = null;
    private List<String> list = null;
    private ArrayAdapter<String> adapter = null;

    private EditText etLocalPort, etRemoteIp, etRemotePort, etContent;
    private TextView tvLocalIp;
    private Button btnSend, btnApply;
    private ListView listView;

    // 传感器显示组件
    private TextView tvTemp, tvHumi, tvPerson, tvCo2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 初始化视图
        listView = (ListView) findViewById(R.id.listview);
        etContent = (EditText) findViewById(R.id.edittext);
        btnSend = (Button) findViewById(R.id.button);
        
        tvLocalIp = (TextView) findViewById(R.id.tv_local_ip);
        etLocalPort = (EditText) findViewById(R.id.et_local_port);
        etRemoteIp = (EditText) findViewById(R.id.et_remote_ip);
        etRemotePort = (EditText) findViewById(R.id.et_remote_port);
        btnApply = (Button) findViewById(R.id.btn_apply_config);

        // 2. 绑定传感器显示组件 (修正 ID 使其与 XML 匹配)
        tvTemp = (TextView) findViewById(R.id.tv_sensor_temp);
        tvHumi = (TextView) findViewById(R.id.tv_sensor_humi);
        tvPerson = (TextView) findViewById(R.id.tv_sensor_person);
        tvCo2 = (TextView) findViewById(R.id.tv_sensor_co2);

        list = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);

        handler = new Handler(Looper.getMainLooper());

        // 显示自动检测到的本地真实 IP
        tvLocalIp.setText("本地IP: " + getLocalIPAddress());

        // 发送按钮逻辑
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String content = etContent.getText().toString().trim();
                final String targetIp = etRemoteIp.getText().toString().trim();
                final String targetPortStr = etRemotePort.getText().toString().trim();

                if (content.isEmpty() || targetIp.isEmpty() || targetPortStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "请填写完整发送信息", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    final int targetPort = Integer.parseInt(targetPortStr);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            sendData(content, targetIp, targetPort);
                        }
                    }).start();

                    addRecord("[发送] -> " + targetIp + ":" + targetPort + ": " + content);
                    etContent.setText("");
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "端口格式错误", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 启动/重启监听按钮
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String portStr = etLocalPort.getText().toString().trim();
                if (!portStr.isEmpty()) {
                    try {
                        startListening(Integer.parseInt(portStr));
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "本地端口格式错误", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // 默认启动监听
        try {
            startListening(Integer.parseInt(etLocalPort.getText().toString().trim()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 在列表中添加记录并自动滚动到底部
     */
    private void addRecord(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                list.add(message);
                adapter.notifyDataSetChanged();
                listView.setSelection(list.size() - 1);
            }
        });
    }

    private void startListening(int port) {
        if (server != null && !server.isClosed()) {
            server.close();
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
        }

        try {
            server = new DatagramSocket(port);
            server.setReuseAddress(true);
            receiveThread = new Thread(receiveWork);
            receiveThread.start();
            Toast.makeText(this, "端口 " + port + " 监听已启动", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            addRecord("错误: 无法监听端口 " + port);
        }
    }

    private String getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && inetAddress.getHostAddress().contains(".")) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return "未知";
    }

    private void sendData(final String content, String ip, int port) {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] data = content.getBytes();
            InetAddress targetAddr = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(data, data.length, targetAddr, port);
            socket.send(packet);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Runnable receiveWork = new Runnable() {
        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            while (!Thread.interrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    if (server != null) {
                        server.receive(packet);
                        
                        final String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                        final String fromIp = packet.getAddress().getHostAddress();
                        final int fromPort = packet.getPort();

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                // 确保接收内容和 IP 也显示在列表中
                                addRecord("[接收] <- " + fromIp + ":" + fromPort + ": " + msg);
                                parseAndDisplayData(msg);
                            }
                        });
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
    };

    private void parseAndDisplayData(String msg) {
        try {
            // 目前展示占位符，待您添加具体协议解析逻辑
            tvTemp.setText("温度: 解析中 ℃");
            tvHumi.setText("湿度: 解析中 %");
            tvPerson.setText("人员检查: 解析中");
            tvCo2.setText("二氧化碳: 解析中 ppm");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null && !server.isClosed()) {
            server.close();
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
    }
}
