package com.waterfairy.activemq;

import android.text.TextUtils;
import android.util.Log;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * Created by water_fairy on 2017/8/15.
 * 995637517@qq.com
 */

public class ActiveMQSender {
    private static final String TAG = "ActiveMQ";
    private String url;
    private String user;
    private String password;

    public static final int CONNECT_OK = 1;
    public static final int CONNECT_ERROR = 2;
    public static final int SEND_OK = 3;
    public static final int SEND_ERROR = 4;
    public static final int ERROR = 5;
    private boolean isConnect;

    public ActiveMQSender(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private Connection connection;
    private OnActiveMQListener onActiveMQListener;

    public static int TYPE_QUEUE = 1;
    public static int TYPE_TOPIC = 2;

    public void connect() {

        if (TextUtils.isEmpty(url)) {
            if (onActiveMQListener != null) {
                onActiveMQListener.onActiveMQSenderChange(CONNECT_ERROR, new Exception("server url is null !"));
            }
            Log.i(TAG, "sender socket connect error : server url is null ! ");
            return;
        }
        new Thread() {
            @Override
            public void run() {
                synchronized (ActiveMQSender.class) {
                    if (!isConnect) {
                        Log.i(TAG, "sender start connect ");
                        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                                user, password, url);
                        try {
                            connection = connectionFactory.createConnection();
                            connection.start();
                            isConnect = true;
                            if (onActiveMQListener != null)
                                onActiveMQListener.onActiveMQSenderChange(CONNECT_OK, null);
                            Log.i(TAG, "sender socket connect success");
                        } catch (JMSException jms) {
                            isConnect = false;
                            if (onActiveMQListener != null)
                                onActiveMQListener.onActiveMQSenderChange(CONNECT_ERROR, jms);
                            Log.i(TAG, "sender socket connect error");
                        }
                    }

                }

            }
        }.start();

    }

    public void sendTopic(String topic, String message) {
        send(TYPE_TOPIC, topic, message);
    }

    public void sendQueue(String queue, String message) {
        send(TYPE_QUEUE, queue, message);
    }

    public void send(int type, String typeContent, String message) {
        if (connection == null) {
            if (onActiveMQListener != null) {
                onActiveMQListener.onActiveMQSenderChange(SEND_ERROR, new Exception("server is not connected !"));
            }
            Log.i(TAG, "sender error : server is not connected !");
            return;
        }
        if (TextUtils.isEmpty(typeContent)) {
            if (onActiveMQListener != null) {
                onActiveMQListener.onActiveMQSenderChange(SEND_ERROR, new Exception("has no queue or topic !"));
            }
            Log.i(TAG, "sender error : has no queue or topic !");
            return;
        }
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Session session = connection.createSession(true,
                            Session.SESSION_TRANSACTED);
                    Destination destination = null;
                    if (type == TYPE_QUEUE) {
                        destination = session.createQueue(typeContent);
                    } else {
                        destination = session.createTopic(typeContent);
                    }
                    MessageProducer producer = session.createProducer(destination);
                    TextMessage outMessage = session.createTextMessage();
                    outMessage.setText(message);
                    producer.send(outMessage);
                    session.commit();
                    producer.close();
                    if (onActiveMQListener != null)
                        onActiveMQListener.onActiveMQSenderChange(SEND_OK, null);
                    Log.i(TAG, "sender send success -> " + message);
                } catch (JMSException jms) {
                    Log.i(TAG, "sender send error !");
                    jms.printStackTrace();
                    if (onActiveMQListener != null)
                        onActiveMQListener.onActiveMQSenderChange(SEND_ERROR, jms);
                }
            }
        }.start();
    }

    public boolean isConnect() {
        return isConnect;
    }

    public void setOnActiveMQListener(OnActiveMQListener onActiveMQListener) {
        this.onActiveMQListener = onActiveMQListener;
    }

    public void disconnect() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    if (connection != null)
                        connection.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
                isConnect = false;
                Log.i(TAG, "sender socket disconnect(可能延时)");
            }
        }.start();
    }


    public interface OnActiveMQListener {
        void onActiveMQSenderChange(int code, Exception e);
    }

}
