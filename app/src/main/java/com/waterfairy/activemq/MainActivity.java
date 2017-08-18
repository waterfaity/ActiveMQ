package com.waterfairy.activemq;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Message;


public class MainActivity extends AppCompatActivity {
    private ActiveMQReceiver activeMQReceiver;
    private ActiveMQSender activeMQSender;
    String url = "tcp://192.168.1.216:61616";
    String user = "";
    String password = "";

    private EditText mETReceiverType;
    private EditText mETSendType;

    private EditText mETSendMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        new Thread(new MessageReceiver(query, url, user, password), "Name-Receiver").start();
//        new Thread(new MessageSender(query, url, user, password), "Name-Sender").start();
        mETReceiverType = (EditText) findViewById(R.id.type1);
        mETSendType = (EditText) findViewById(R.id.type2);
        mETSendMsg = (EditText) findViewById(R.id.content);
        connectSender();
        connectReceiver();

    }

    public void connectSender() {
        activeMQSender = new ActiveMQSender(url, user, password);
        activeMQSender.setOnActiveMQListener(new ActiveMQSender.OnActiveMQListener() {
            @Override
            public void onActiveMQSenderChange(int code, Exception e) {
                switch (code) {
                    case ActiveMQSender.CONNECT_OK:
                        break;
                }
            }
        });
        activeMQSender.connect();
    }

    public void connectReceiver() {
        activeMQReceiver = new ActiveMQReceiver(url, user, password);
        activeMQReceiver.setOnActiveMQListener(new ActiveMQReceiver.OnActiveMQListener() {
            @Override
            public void onActiveMQChange(int code, String msg, Exception e) {

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
        activeMQReceiver.connect();
    }

    public void sendQueue(View view) {
        String msg = mETSendMsg.getText().toString();
        String type = mETSendType.getText().toString();
        activeMQSender.sendQueue(type, msg);
    }

    public void sendTopic(View view) {
        String msg = mETSendMsg.getText().toString();
        String type = mETSendType.getText().toString();
        activeMQSender.sendTopic(type, msg);
    }

    public void receiveQueue(View view) {
        String type = mETReceiverType.getText().toString();
        activeMQReceiver.receiveQueue(type);
    }


    public void receiveTopic(View view) {
        String type = mETReceiverType.getText().toString();
        activeMQReceiver.receiveTopic(type);
    }

    public void unReceiveTopic(View view) {
        String type = mETReceiverType.getText().toString();
        activeMQReceiver.unSubscribe("topic-" + type);
    }

    public void unReceiveQueue(View view) {
        String type = mETReceiverType.getText().toString();
    }
}