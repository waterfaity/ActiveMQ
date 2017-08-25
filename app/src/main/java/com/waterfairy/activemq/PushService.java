package com.waterfairy.activemq;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

/**
 * Created by water_fairy on 2017/8/16.
 * 995637517@qq.com
 */

public class PushService extends Service {

    private final String TAG = "PushService";
    private ActiveMQReceiver receiver;//推送接收
    private ActiveMQSender sender;//推送发送者  未用到 发送  ,由后台去发送.
    private static final String ACTION_START = "com.waterfairy.activemq.service.connect";//启动 发送/接收
    private static final String ACTION_END = "com.waterfairy.activemq.service.end";//结束 发送/接收
    private static final String ACTION_SEND = "com.waterfairy.activemq.service.send";//发送 消息
    private static final String ACTION_START_RECEIVER = "com.waterfairy.activemq.service.connectReceiver";//启动接收 某个topic/queue
    private static final String ACTION_UN_RECEIVE = "com.waterfairy.activemq.service.unReceive";//接收接收 某个topic/queue
    public static final String ACTION_RECEIVE = "com.waterfairy.activemq.broadcast.receive";//收到信息
    private String ACCOUNT = "";//帐号
    private String PASSWORD = "";//密码  未用到
    private String serverUrl;//服务器地址  socket  tcp
    private final int PORT = 61616;//端口
    public static String PUSH_URL = "192.168.1.216";

    public static final String MESSAGE = "message";
    public static final String TIME = "time";
    private static ActiveMQSender.OnActiveMQListener pushSendListener;


    private List<String> pushReceiverList;//所有订阅

    public static void setPushSendListener(ActiveMQSender.OnActiveMQListener pushSendListener) {
        PushService.pushSendListener = pushSendListener;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startPushService(Context context) {
        Intent intent = new Intent(context, PushService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    public static void stopPushService(Context context) {
        Intent intent = new Intent(context, PushService.class);
        intent.setAction(ACTION_END);
        context.startService(intent);
    }

//    public static void checkPushConnect(Context context) {
//        Intent intent = new Intent(context, PushService.class);
//        intent.setAction(ACTION_CHECK_CONNECT);
//        context.startService(intent);
//    }

    public static void sendMessage(Context context, String queue, String topic, String message) {
        action(context, ACTION_SEND, queue, topic, message);
    }

    public static void startReceiver(Context context, String queue, String topic) {
        action(context, ACTION_START_RECEIVER, queue, topic, null);
    }

    private static void action(Context context, String action, String queue, String topic, String message) {
        Intent intent = new Intent(context, PushService.class);
        intent.putExtra("queue", queue);
        intent.putExtra("topic", topic);
        intent.putExtra("msg", message);
        intent.setAction(action);
        context.startService(intent);
    }

    public static void unSubscribe(Context context, String content) {
        Intent intent = new Intent(context, PushService.class);
        intent.putExtra("unSubscribe", content);
        intent.setAction(ACTION_UN_RECEIVE);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            switch (intent.getAction()) {
                case ACTION_START:
                    startPush();
                    break;
                case ACTION_END:
                    stopPush();
                    break;
                case ACTION_SEND:
                    sendMessage(intent);
                    break;
                case ACTION_START_RECEIVER:
                    startReceiver(intent);
                    break;
                case ACTION_UN_RECEIVE:
                    unSubscribe(intent);
                    break;
//                case ACTION_CHECK_CONNECT:
//                    checkConnect();
//                    break;
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void checkConnect() {

    }

    private void unSubscribe(Intent intent) {
        String unSubscribe = intent.getStringExtra("unSubscribe");
        if (receiver != null && receiver.isConnect()) {
            receiver.unSubscribe(unSubscribe);
        }
    }


    private void sendMessage(Intent intent) {
        String queue = intent.getStringExtra("queue");
        String topic = intent.getStringExtra("topic");
        String msg = intent.getStringExtra("message");
        if (sender != null && sender.isConnect()) {
            if (!TextUtils.isEmpty(queue)) sender.sendQueue(queue, msg);
            else if (!TextUtils.isEmpty(topic)) sender.sendTopic(topic, msg);
        }
    }

    private void startReceiver(Intent intent) {
        String queue = intent.getStringExtra("queue");
        String topic = intent.getStringExtra("topic");
        if (receiver != null && receiver.isConnect()) {
            if (!TextUtils.isEmpty(queue)) receiver.receiveQueue(queue);
            else if (!TextUtils.isEmpty(topic)) receiver.receiveTopic(topic);
        }
    }


    private void startPush() {
        if (pushReceiverList == null) pushReceiverList = new ArrayList<>();
        serverUrl = "tcp://" + PUSH_URL + ":" + PORT;
//        connectSender();//未用到
        connectReceiver();
    }

    private void stopPush() {
        if (sender != null && sender.isConnect()) {
            sender.disconnect();
        }
        if (receiver != null && receiver.isConnect()) {
            receiver.disconnect();
        }
    }

    private void connectReceiver() {
        if (receiver == null) {
            receiver = new ActiveMQReceiver(serverUrl, ACCOUNT, PASSWORD);
            receiver.setOnActiveMQListener(new ActiveMQReceiver.OnActiveMQListener() {
                @Override
                public void onActiveMQChange(int code, String msg, Exception e) {
                    if (e != null) {
                        e.printStackTrace();
                    }
                    if (code == ActiveMQReceiver.SOCKET_CONNECT_OK) {
                        if (pushReceiverList != null && pushReceiverList.size() > 0) {
                            for (int i = 0; i < pushReceiverList.size(); i++) {
                                //防止重复订阅
                                String s = pushReceiverList.get(i);
                                Intent unIntent = new Intent();
                                unIntent.putExtra("unSubscribe", s);
                                unSubscribe(unIntent);
                                //重新订阅
                                String[] split = s.split("-");
                                Intent intent = new Intent();
                                if (TextUtils.equals("topic", split[0])) {
                                    intent.putExtra("topic", split[1]);
                                } else {
                                    intent.putExtra("queue", split[1]);
                                }
                                startReceiver(intent);
                            }
                        }
                    } else if (code == ActiveMQReceiver.RECEIVER_CONNECT_OK) {
                        if (!pushReceiverList.contains(msg)) {
                            pushReceiverList.add(msg);
                        }
                    } else if (code == ActiveMQReceiver.RECEIVER_CONNECT_REMOVE) {
                        if (pushReceiverList.contains(msg)) {
                            pushReceiverList.remove(msg);
                        }
                    }
                    Log.i(TAG, "onActiveMQChange: " + code + "--" + msg);
                }

                @Override
                public void onActiveMQReceive(Message message) {
                    try {
                        long messageDate = message.getJMSTimestamp();
                        TextMessage receiveMessage = (TextMessage) message;
                        receiveMessage.getText();
                        Intent intent = new Intent(ACTION_RECEIVE);
                        intent.putExtra("time", messageDate);
                        intent.putExtra("message", receiveMessage.getText());
                        sendBroadcast(intent);
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        receiver.connect();
    }

    private void connectSender() {
        if (sender == null) {
            sender = new ActiveMQSender(serverUrl, ACCOUNT, PASSWORD);
            sender.setOnActiveMQListener(new ActiveMQSender.OnActiveMQListener() {
                @Override
                public void onActiveMQSenderChange(int code, Exception e) {
                    if (pushSendListener != null) {
                        pushSendListener.onActiveMQSenderChange(code, e);
                    }
                    Log.i(TAG, "onActiveMQSenderChange: " + code);
                }
            });
        }

        sender.connect();
    }


}
