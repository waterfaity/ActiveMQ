package com.waterfairy.activemq;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String url = "tcp://192.168.1.216:61616";

        String user = "";
        String password = "";
        String query = "MyQueue";
        new Thread(new MessageReceiver(query, url, user, password), "Name-Receiver").start();
        new Thread(new MessageSender(query, url, user, password), "Name-Sender").start();
    }


}
