package com.waterfairy.activemq;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Message;


public class MainActivity extends AppCompatActivity {
    private ActiveMQReceiver activeMQReceiver;
    private ActiveMQSender activeMQSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String url = "tcp://192.168.1.216:61616";

        String user = "";
        String password = "";
        String queue = "MyQueue";
//        new Thread(new MessageReceiver(query, url, user, password), "Name-Receiver").start();
//        new Thread(new MessageSender(query, url, user, password), "Name-Sender").start();

        activeMQReceiver = new ActiveMQReceiver(url, user, password, queue, "");
        activeMQReceiver.setOnActiveMQListener(new ActiveMQReceiver.OnActiveMQListener() {
            @Override
            public void onActiveMQChange(int code, Exception e) {

            }

            @Override
            public void onActiveMQReceive(Message message) {
                try {
                    long messageDate = message.getJMSTimestamp();
                    if (new Date().getTime() - messageDate < 60 * 1000) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Log.i("MainActivity", "onActiveMQReceive: " + simpleDateFormat.format(new Date(messageDate)));
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });
        activeMQReceiver.receiveQueue();

        activeMQSender = new ActiveMQSender(url, user, password, queue, "");
        activeMQSender.setOnActiveMQListener(new ActiveMQSender.OnActiveMQListener() {
            @Override
            public void onActiveMQSenderChange(int code, Exception e) {
                switch (code) {
                    case ActiveMQSender.CONNECT_OK:
                        activeMQSender.sendQueue("hahhfdafdsafda  " + new Date().getTime());
                        break;

                }
            }
        });
        activeMQSender.connect();
    }


    public void receive(View view) {

    }

    public void send(View view) {
        activeMQSender.sendQueue("hhah" + new Date().getTime());
    }
}
