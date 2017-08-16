package com.waterfairy.activemq;

import android.text.TextUtils;
import android.util.Log;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * Created by water_fairy on 2017/8/15.
 * 995637517@qq.com
 */

public class ActiveMQReceiver {
    private static final String TAG = "ActiveMQ";
    private String url;
    private String user;
    private String password;
    private String queue;
    private String topic;

    private boolean isConnect;

    public static final int CONNECT_OK = 11;
    public static final int CONNECT_ERROR = 12;
    public static final int RECEIVE_OK = 13;
    public static final int RECEIVE_ERROR = 14;
    public static final int ERROR = 15;

    public ActiveMQReceiver(String url, String user, String password, String queue, String topic) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.queue = queue;
        this.topic = topic;
    }

    private OnActiveMQListener onActiveMQListener;

    public static int TYPE_QUEUE = 1;
    public static int TYPE_TOPIC = 2;

    public void receiveTopic() {
        receive(TYPE_TOPIC, this.topic);
    }

    public void receiveQueue() {
        receive(TYPE_QUEUE, this.queue);
    }

    public void receive(int type, String typeContent) {
        if (TextUtils.isEmpty(url)) {
            if (onActiveMQListener != null) {
                onActiveMQListener.onActiveMQChange(CONNECT_ERROR, new Exception("server url is null !"));
            }
            return;
        }
        if (TextUtils.isEmpty(typeContent)) {
            if (onActiveMQListener != null) {
                onActiveMQListener.onActiveMQChange(RECEIVE_ERROR, new Exception("has no queue or topic !"));
            }
            return;
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                            user, password, url);
                    Connection connection = connectionFactory.createConnection();
                    Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
                    Destination destination = null;
                    if (type == TYPE_QUEUE) {
                        destination = session.createQueue(typeContent);
                    } else {
                        destination = session.createTopic(typeContent);
                    }
                    MessageConsumer consumer = session.createConsumer(destination);
                    try {
                        connection.start();
                        isConnect = true;
                        if (onActiveMQListener != null)
                            onActiveMQListener.onActiveMQChange(CONNECT_OK, null);
                        Log.i(TAG, "receiver connect success");
                    } catch (JMSException jms) {
                        isConnect = false;
                        if (onActiveMQListener != null)
                            onActiveMQListener.onActiveMQChange(CONNECT_ERROR, jms);
                        Log.i(TAG, "receiver connect error");
                        return;
                    }
                    Log.i(TAG, "receive: start success !");
                    if (onActiveMQListener != null)
                        onActiveMQListener.onActiveMQChange(RECEIVE_OK, null);
                    while (true) {
                        Message message = consumer.receive();
                        if (message instanceof TextMessage) {
                            TextMessage receiveMessage = (TextMessage) message;
                            if (onActiveMQListener != null) {
                                onActiveMQListener.onActiveMQReceive(message);
                            }
                            Log.i(TAG, "receive: success -> " + receiveMessage.getText());
                        } else {
                            session.commit();
                            break;
                        }
                    }
                    connection.close();
                    isConnect = false;
                } catch (JMSException jms) {
                    isConnect = false;
                    Log.i(TAG, "receive:  error !");
                    jms.printStackTrace();
                    if (onActiveMQListener != null)
                        onActiveMQListener.onActiveMQChange(RECEIVE_ERROR, jms);
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

    public interface OnActiveMQListener {
        void onActiveMQChange(int code, Exception e);

        void onActiveMQReceive(Message message);
    }

}
