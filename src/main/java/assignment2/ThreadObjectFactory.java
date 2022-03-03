package assignment2;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class ThreadObjectFactory extends BasePooledObjectFactory<Channel> {


    @Override
    public Channel create() throws Exception {
        ConnectionFactory cf = new ConnectionFactory();
        Connection conn = cf.newConnection();

        Channel ch = conn.createChannel();
        return ch;
    }

    @Override
    public PooledObject<Channel> wrap(Channel channel) {
        return new DefaultPooledObject<Channel>(channel);
    }
}