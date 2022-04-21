package assignment4;


import assignment4.model.Skier;
import com.google.gson.Gson;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

public class ResortConsumer {
    private final static String QUEUE_NAME = "queue2";
    private static final String EXCHANGE_NAME = "logs";

    private final static String RABBITMQ_HOST_NAME = "34.222.226.16";
    private final static String REDIS_HOST_NAME = "34.219.33.42";
    private final static int NUM_THREADS = 256;
    private static Map<Integer, Integer> concurrentHashMap = null;

    private static JedisPool pool;

    private static Gson gson = new Gson();

    private static final Logger logger = LoggerFactory.getLogger(SkierConsumer.class);

    public ResortConsumer() {
    }

    public static void main(String[] argv) throws Exception {
        concurrentHashMap = new ConcurrentHashMap<>();

        final ExecutorService threadPool =  new ThreadPoolExecutor(NUM_THREADS/2, NUM_THREADS/2,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST_NAME);
        factory.setUsername("admin");
        factory.setPassword("pass");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // connect to Redis
        connectToDatabase();
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        registerConsumer(channel, threadPool);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Shutting down thread pool...");
                threadPool.shutdown();
                pool.close();
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
     * Connect and configure to the database. Thread-safe configurations from
     * https://www.baeldung.com/jedis-java-redis-client-library
     */
    private static void connectToDatabase() {
        pool = new JedisPool(REDIS_HOST_NAME , 6379);

        pool.setMaxTotal(NUM_THREADS);
        pool.setBlockWhenExhausted(true);
        pool.setMaxIdle(NUM_THREADS);
        pool.setMinIdle(16);
        pool.setMinEvictableIdle(Duration.ofMillis(60000));
        pool.setTimeBetweenEvictionRuns(Duration.ofMillis(30000));
        pool.setNumTestsPerEvictionRun(3);
        pool.setTestOnBorrow(true);
        pool.setTestOnReturn(true);
        pool.setTestWhileIdle(true);

        System.out.println("Connection successful");
    }

    /**
     * Configure consumer to RabbitMQ server and add message to hashmap
     * @param channel
     * @param threadPool
     * @throws IOException
     */
    private static void registerConsumer(Channel channel, ExecutorService threadPool) throws IOException {
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "");

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
                            // store in database
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
     * Store in database. Method is synchronized to avoid collisions
     * @param skierMessage - skier message
     */
    private static synchronized void store(String skierMessage) {
        // convert message to Skier object
        Skier skier = gson.fromJson(skierMessage, Skier.class);

        // get a jedis connection from pool
        Jedis jedis = pool.getResource();

        try {
            // if key exists, append the value
            if (jedis.exists(skier.getDayId())) {

                // insert lift info at the head of the list
                jedis.lpushx(skier.getDayId(), skier.getLiftRide().toString());
            } else {
                // create list and insert lift info
                jedis.lpush(skier.getDayId(),skier.getLiftRide().toString());
            }
            // insert unique skier into set {key:"resort-1-day-1", value: "143", "123"...}
            jedis.sadd("resort-"+skier.getResortId()+"-day-"+skier.getDayId(), String.valueOf(skier.getSkierId()));

            // increment total ride by 1 {key:"day-1-lift-1", field: "totalRides", value:10}
            jedis.hincrBy("day-"+skier.getDayId()+"-lift-"+skier.getLiftRide().getLiftId(), "totalRides", 1);

            //logger.info("Successfully added to database");
        } catch (JedisException e) {
            if (jedis != null) {
                // if error, return it back to pool
                pool.returnBrokenResource(jedis);
                jedis = null;
            }
        } finally {
            pool.returnResource(jedis);
        }
    }
}
