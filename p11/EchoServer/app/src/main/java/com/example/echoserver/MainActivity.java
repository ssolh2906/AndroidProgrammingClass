package com.example.echoserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    private ServerThread mServerThread; // 서버통신담당
    private TextView mTextOutput;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextOutput = (TextView) findViewById(R.id.textOutput);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }


    public void mOnClick(View v)
    {
        switch (v.getId()){
            case R.id.btnStart:
                if (mServerThread == null) {
                    mServerThread = new ServerThread(this, mMainHandler);
                    mServerThread.start();
                }
                break;
            case R.id.btnQuit:
                finish();
                break;
        }
    }

    private Handler mMainHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    mTextOutput.append((String) msg.obj);
                    break;
            }
        }
    };
}

class ServerThread extends Thread {
    private Context mContext;
    private Handler mMainHandler;

    public ServerThread(Context context, Handler mainHandler)
    {
        mContext = context;
        mMainHandler = mainHandler;
    }

    @Override
    public void run() {
        // thread.start 호출 시 실행
        ServerSocket servSock = null;
        // init servSock
        try {
            servSock = new ServerSocket(9000);
            WifiManager wifiMgr = (WifiManager) mContext.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            int serverip = wifiMgr.isWifiEnabled() ? wifiInfo.getIpAddress() : 0x0100007F; //127.0.0.1, Local host
            doPrintln(">> 서버시작!" + ipv4ToString(serverip) + "/" + servSock.getLocalPort());

            while (true) {
                // wait for client
                // servSock 의리턴값으로 소켓객체 생성
                Socket sock = servSock.accept();

                // print client info
                String ip = sock.getInetAddress().getHostAddress();
                int port = sock.getPort();
                doPrintln(">> 클라이언트 접속: " + ip + "/" + port);

                // communicate with client
                InputStream in = sock.getInputStream();
                OutputStream out = sock.getOutputStream();

                // read & write
                byte[] buf = new byte[1024];

                while (true) {
                    try {
                        // 데이터 수신
                        int nbytes = in.read(buf);
                        if (nbytes > 0) {
                            // 데이터 출력
                            String s = new String(buf,0,nbytes);
                            doPrintln("["+ip+"/"+port+"]"+s);

                            // 데이터 송신
                            out.write(buf, 0, nbytes);
                        } else  {
                            // 정상종료
                            doPrintln(">> 클라이언트 종료: "+ ip + "/" + port);
                            break;
                        }
                    } catch (IOException e) {
                        //비정상 강제종료
                        doPrintln(">> 클라이언트 종료: "+ip+"/"+port);
                        break;
                    }
                }// end of inner while-loop
                sock.close();
            }//end of outer while-loop
        } catch (IOException e) {
            doPrintln(e.getMessage());
        } finally {
            try {
                if (servSock != null) {
                    servSock.close();
                }
                doPrintln(">> 서버 종료!");
            } catch (IOException e) {
                doPrintln(e.getMessage());
            }
        }
    } // end of run

    private void doPrintln(String str)
    {
        Message msg = Message.obtain();
        msg.what = 1;
        msg.obj = str + "\n";
        mMainHandler.sendMessage(msg);
    }

    private String ipv4ToString(int ip)
    {
        int a = (ip) & 0xFF;
        int b = (ip >> 8) & 0xFF;
        int c = (ip >> 16) & 0xFF;
        int d = (ip >> 24) & 0XFF;

        return Integer.toString(a)+"."+Integer.toString(b)+"."
                +Integer.toString(c)+"."+Integer.toString(d);

    }

}