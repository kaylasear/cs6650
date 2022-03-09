package assignment2;

import assignment2.model.LiftRide;
import com.google.gson.Gson;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

public class Consumer {
    private final static String QUEUE_NAME = "queue";
    private final static int NUM_THREADS = 64;
    private static Map<Integer, Integer> concurrentHashMap = null;

    private static Gson gson = new Gson();

    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    public static void main(String[] argv) throws Exception {
        concurrentHashMap = new ConcurrentHashMap<>();

        final ExecutorService threadPool =  new ThreadPoolExecutor(NUM_THREADS, NUM_THREADS,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("54.149.21.111");
        factory.setUsername("admin");
        factory.setPassword("pass");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        registerConsumer(channel, threadPool);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Shutting down thread pool...");
                threadPool.shutdown();
                try {
                    while(!threadPool.awaitTermination(10, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    logger.info("Interrupted while waiting for termination");
                }
                logger.info("Thread pool shut down.");
            }
        });

        }

    /**
     * Configure consumer to RabbitMQ server and add message to hashmap
     * @param channel
     * @param threadPool
     * @throws IOException
     */
    private static void registerConsumer(Channel channel, ExecutorService threadPool) throws IOException {
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
                    logger.info(String.format("Received %s", new String(body)));

                    threadPool.submit(new Runnable() {
                        public void run() {
                            // store in hashmap
                            store(new String(body));
                            logger.info(String.format("Processed %s", new String(body)));
                        }
                    });
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        };

        channel.basicConsume(QUEUE_NAME, true /* auto-ack */, consumer);
    }

    /**
     * Store in hashmap. Method is synchronized to avoid collisions
     * @param message - lift ride message
     */
    private static synchronized void store(String message) {
        LiftRide liftRide = gson.fromJson(message, LiftRide.class);

        //logger.info("storing in hashmap liftId: " + liftRide.getLiftId() + "waitTime: "  + liftRide.getWaitTime());
        concurrentHashMap.put(liftRide.getLiftId(), liftRide.getWaitTime());
    }
}
