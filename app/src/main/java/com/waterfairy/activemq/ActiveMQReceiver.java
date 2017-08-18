package com.waterfairy.activemq;

import android.text.TextUtils;
import android.util.Log;

import org.apache.activemq.ActiveMQConnectionFactory;

import java.util.HashMap;

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
    private Session session;
    private HashMap<String, Boolean> connectTag;
    private HashMap<String, MessageConsumer> consumerHashMap;

    private boolean isSocketConnect;
    public static final int SOCKET_CONNECT_OK = 11;
    public static final int SOCKET_CONNECT_ERROR = 12;
    public static final int RECEIVE_OK = 13;
    public static final int RECEIVE_ERROR = 14;
    public static final int ERROR = 15;
    public static final int RECEIVER_CONNECT_OK = 18;
    public static final int RECEIVER_CONNECT_ERROR = 19;

    private Connection connection;

    public ActiveMQReceiver(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        connectTag = new HashMap<>();
        consumerHashMap = new HashMap<>();
    }

    private OnActiveMQListener onActiveMQListener;

    public static int TYPE_QUEUE = 1;
    public static int TYPE_TOPIC = 2;

    public void receiveTopic(String topic) {
        receive(TYPE_TOPIC, topic);
    }

    public void receiveQueue(String queue) {
        receive(TYPE_QUEUE, queue);
    }

    public void receive(int type, String typeContent) {
        new Thread() {
            @Override
            public void run() {
                String tagContent = "";
                try {
                    if (isSocketConnect) {
                        if (session == null) {
                            session = connection.createSession(true, Session.SESSION_TRANSACTED);
                        }
                        Destination destination = null;

                        if (type == TYPE_QUEUE) {
                            destination = session.createQueue(typeContent);
                            tagContent = "queue-" + typeContent;
                        } else {
                            destination = session.createTopic(typeContent);
                            tagContent = "topic-" + typeContent;
                        }
                        MessageConsumer consumer = session.createConsumer(destination);
                        try {
                            connection.start();
                            connectTag.put(tagContent, true);
                            consumerHashMap.put(tagContent, consumer);
                            if (onActiveMQListener != null)
                                onActiveMQListener.onActiveMQChange(RECEIVER_CONNECT_OK, tagContent, null);
                            Log.i(TAG, "receiver connect success - - >" + tagContent);
                        } catch (JMSException jms) {
                            connectTag.put(tagContent, false);
                            if (onActiveMQListener != null)
                                onActiveMQListener.onActiveMQChange(RECEIVER_CONNECT_ERROR, tagContent, jms);
                            Log.i(TAG, "receiver connect error   - - >" + tagContent);
                            return;
                        }
                        if (onActiveMQListener != null)
                            onActiveMQListener.onActiveMQChange(RECEIVE_OK, null, null);

                        try {
                            while (true) {
                                Message message = consumer.receive();
                                if (message instanceof TextMessage) {
                                    TextMessage receiveMessage = (TextMessage) message;
                                    if (onActiveMQListener != null) {
                                        onActiveMQListener.onActiveMQReceive(message);
                                    }
                                    Log.i(TAG, "receive message: (" + tagContent + ") -> " + receiveMessage.getText());
                                } else {
                                    session.commit();
                                    break;
                                }
                            }
                        } catch (JMSException jms) {
                            jms.printStackTrace();
                            connectTag.put(tagContent, false);
                        }
                    }
                } catch (JMSException jms) {
                    connectTag.put(tagContent, false);
                    Log.i(TAG, "receive error ! -->" + typeContent);
                    jms.printStackTrace();
                    if (onActiveMQListener != null)
                        onActiveMQListener.onActiveMQChange(RECEIVE_ERROR, null, jms);
                }
            }
        }.start();
    }

    public boolean isConnect() {
        return isSocketConnect;
    }

    public void setOnActiveMQListener(OnActiveMQListener onActiveMQListener) {
        this.onActiveMQListener = onActiveMQListener;
    }

    public void disconnect() {
        try {
            if (connection != null)
                connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
        isSocketConnect = false;
        Log.i(TAG, "receiver disconnect");
    }

    public void connect() {
        if (TextUtils.isEmpty(url)) {
            if (onActiveMQListener != null) {
                onActiveMQListener.onActiveMQChange(SOCKET_CONNECT_ERROR, null, new Exception("server url is null !"));
            }
            return;
        }
        new Thread() {
            @Override
            public void run() {
                super.run();
                if (!isSocketConnect) {
                    try {
                        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                                user, password, url);
                        connection = connectionFactory.createConnection();
                        isSocketConnect = true;
                        if (onActiveMQListener != null)
                            onActiveMQListener.onActiveMQChange(SOCKET_CONNECT_OK, null, null);
                        Log.i(TAG, "receiver connect success ");
                    } catch (JMSException jms) {
                        isSocketConnect = false;
                        if (onActiveMQListener != null)
                            onActiveMQListener.onActiveMQChange(SOCKET_CONNECT_ERROR, null, jms);
                        Log.i(TAG, "receiver connect error ");
                    }
                }
            }
        }.start();

    }

    public void unSubscribe(String unSubscribe) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                if (isSocketConnect) {
                    MessageConsumer messageConsumer = consumerHashMap.get(unSubscribe);
                    if (messageConsumer != null) {
                        try {
                            messageConsumer.close();
                            session.commit();
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.start();
    }

    public interface OnActiveMQListener {
        void onActiveMQChange(int code, String msg, Exception e);

        void onActiveMQReceive(Message message);
    }

}
