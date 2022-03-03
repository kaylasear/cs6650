package assignment2;

import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Consumer {
    private final static String QUEUE_NAME = "queue";
    private final static int NUM_THREADS = 128;
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");
        factory.setPassword("pass");
        factory.setPort(5672);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        final ExecutorService threadPool =  new ThreadPoolExecutor(NUM_THREADS, NUM_THREADS,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        
        registerConsumer(channel, 500, threadPool);

        threadPool.shutdown();
        logger.info("Thread pool shut down.");

//        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
//            System.out.println(" [x] Received '" + message + "'");
//        };
//        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });

        }

    private static void registerConsumer(Channel channel, int timeout, ExecutorService threadPool) throws IOException {
        channel.exchangeDeclare(QUEUE_NAME, "fanout");
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        channel.queueBind(QUEUE_NAME, QUEUE_NAME, "");

        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       final byte[] body) throws IOException {
                try {
                    logger.info(String.format("Received (channel %d) %s", channel.getChannelNumber(), new String(body)));
                    // store in hashmap
                    store(new String(body));

                    threadPool.submit(new Runnable() {
                        public void run() {
                            try {
                                Thread.sleep(timeout);
                                logger.info(String.format("Processed %s", new String(body)));
                            } catch (InterruptedException e) {
                                logger.warn(String.format("Interrupted %s", new String(body)));
                            }
                        }
                    });
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        };

        channel.basicConsume(QUEUE_NAME, true /* auto-ack */, consumer);
    }

    private static String store(String n) {
        return ("storing..");
    }
}
