package com.permanent_liufoxmail.pl.frontground;


import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.permanent_liufoxmail.pl.R;
import com.permanent_liufoxmail.pl.background.Adapter.RecyclerAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity
{
    private RecyclerView recyclerView;
    private EditText content;
    private Button send;
    private Socket socket;
    private ArrayList<com.permanent_liufoxmail.pl.background.Message> list;
    private RecyclerAdapter recyclerAdapter;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        content = (EditText) findViewById(R.id.input_text);
        send = (Button) findViewById(R.id.sent);
        list = new ArrayList<>();
        recyclerAdapter = new RecyclerAdapter(this);
        final Handler handler = new MyHandler();//接收消息，更改UI



        showDialog();

        //发送消息用的线程
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    socket = new Socket("192.168.31.44", 10010);
                    InputStream inputStream = socket.getInputStream();

//                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "GB2312"));

                    byte[] buffer = new byte[1024];
                    int len;

                    while ((len = inputStream.read(buffer)) != -1)
                    {
//                        String data = bufferedReader.readLine();
                        String data = new String(buffer, 0, len);
                        // 发到主线程中 收到的数据
                        Message message = Message.obtain();
                        message.what = 1;//获取到了消息
                        message.obj = data;
                        handler.sendMessage(message);
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }).start();

        send.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final String data = content.getText().toString();
                content.setText("");

                if (! data.equals(""))
                {
                    new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                OutputStream outputStream = socket.getOutputStream();
                                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
                                outputStream.write((name + "//" + data + "//" + df.format(new Date())).getBytes("GB2312"));
                                outputStream.flush();
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }

            }
        });
    }

    private class MyHandler extends Handler
    {
        @Override
        public void handleMessage(Message message)
        {
            super.handleMessage(message);
            if (message.what == 1)
            {
//                int localPort = socket.getLocalPort();
                String localName = name;
                String[] split = ((String) message.obj).split("//");

                if (split[0].equals(localName + ""))
                {
                    com.permanent_liufoxmail.pl.background.Message content =
                            new com.permanent_liufoxmail.pl.background.Message(split[1], 1, split[2], "我：");
                    list.add(content);
                }
                else
                {
                    com.permanent_liufoxmail.pl.background.Message content =
                            new com.permanent_liufoxmail.pl.background.Message(split[1], 2, split[2], ("来自：" + split[0]));
                    list.add(content);
                    setNotifition(split[0], split[1]);
                }

                // 向适配器set数据
                recyclerAdapter.setMessage(list);
                recyclerView.setAdapter(recyclerAdapter);
                LinearLayoutManager manager = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false);
                recyclerView.setLayoutManager(manager);

            }

        }

    }

    private void showDialog()
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        final EditText editText = new EditText(MainActivity.this);

        DialogInterface.OnClickListener sure = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
//                Toast.makeText(MainActivity.this, editText.getText().toString(), Toast.LENGTH_SHORT).show();
                name = editText.getText().toString();
            }
        };

        dialog.setTitle("请输入一个昵称");
        dialog.setView(editText);
        dialog.setPositiveButton("确定", (DialogInterface.OnClickListener) sure);
        dialog.show();
    }

    private void setNotifition(String name, String text)
    {
        NotificationCompat.Builder builder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(name)
                .setContentText(text);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_ONLY_ALERT_ONCE;
        notification.defaults = Notification.DEFAULT_ALL;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }
}