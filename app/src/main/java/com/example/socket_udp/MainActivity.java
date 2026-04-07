package com.example.socket_udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import android.os.Bundle;
import android.os.Handler;
import android.R.integer;
import android.app.Activity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class MainActivity extends Activity {

	public DatagramSocket server = null; //监听套接字
	public String AimIP = "222.198.39.29";//接受方IP 
	public int AimPort = 12346;		   //接受方接受端口
	public int AcceptPort = 12345;     //本机接受端口
	public Handler handler = null;     //handler消息传递机制解决子线程更新主线程界面
	public String refresh = null;      //当前接收到的消息
	public Thread receiveThread = null;//接收线程
	public Thread sendThread = null;   //发送线程
	public List<String> list = null;          //显示列表
	public ArrayAdapter<String> adapter = null; //listView配适器
	public Button button = null;
	public EditText editText = null;
	public ListView listView = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		listView = (ListView)findViewById(R.id.listview);
		list = new ArrayList<String>();
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
		listView.setAdapter(adapter);   //为listView添加配适器
		handler = new Handler();       //实例化handler
		button = (Button)findViewById(R.id.button);
		editText = (EditText)findViewById(R.id.edittext);
		button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				sendThread = new Thread(null, sendWork, "outputWork");  //创建发送线程
				sendThread.start();
				String string = editText.getText().toString().trim();
				if(string != null){
					list.add("发送:" + string);   //将发送的消息加入到列表
					adapter.notifyDataSetChanged();  //配适器重新加载  显示当前列表
				}
			}
		});
		
		try {
			server = new DatagramSocket(AcceptPort);    //实例化套接字  并指定监听端口
			receiveThread = new Thread(null, receiveWork, "inputWork");  //创建接收线程
			receiveThread.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void upData(String string){   //当接收到消息时调用的更新函数
		refresh = string;
		handler.post(refreshRunnable);
	}
	
	private Runnable refreshRunnable = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			list.add("接受:" + refresh); 		//列表中添加一项
			adapter.notifyDataSetChanged();   //配适器重新加载  显示当前列表
		}
	};
	
	private Runnable receiveWork = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(true){   //循环监听
				try {
					byte[] data = new byte[100];				
					DatagramPacket packet = new DatagramPacket(data, data.length);   //实例化UDP接受包					
					server.receive(packet);  //将要接受的消息读取到UDP接受包中
					String string = new String(packet.getData()).trim();
					upData(string);			  //将接收到的消息更新到用户界面
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
			}
		}
	};
	
	private Runnable sendWork = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			String string = editText.getText().toString().trim();  //得到要发送的消息
			if(string != null){
				byte[] data = new byte[100];
				data = string.getBytes();    //将得到的消息转变为字节数组
				try{
					DatagramSocket socket = new DatagramSocket();
					InetAddress ip = InetAddress.getByName(AimIP);
					DatagramPacket packet = new DatagramPacket(data, data.length, ip, AimPort);
					//创建数据包
					socket.send(packet);
					//发送数据包
				} 
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		}
	};
}
