package assignment2;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

public class RPCClient implements AutoCloseable{
    private Connection connection;
    private Channel channel;
    private String requestQueueName = "rpc_queue";

    public RPCClient() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    public static void main(String[] argv) {
        try (RPCClient fibonacciRpc = new RPCClient()) {
            for (int i = 0; i < 32; i++) {
                String i_str = Integer.toString(i);
                System.out.println(" [x] Requesting fib(" + i_str + ")");
                String response = fibonacciRpc.call(i_str);
                System.out.println(" [.] Got '" + response + "'");
            }
        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }

//    Our call method makes the actual RPC request.
//    Here, we first generate a unique correlationId number and save it - our consumer callback will use this value to match the appropriate response
    public String call(String message) throws IOException, InterruptedException {
        final String corrId = UUID.randomUUID().toString();

//        Then, we create a dedicated exclusive queue for the reply and subscribe to it.
//        Next, we publish the request message, with two properties: replyTo and correlationId.
//                At this point we can sit back and wait until the proper response arrives.
        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

//        Since our consumer delivery handling is happening in a separate thread, we're going to need something to suspend the main thread before the response arrives.
//        Usage of BlockingQueue is one possible solutions to do so.
//        Here we are creating ArrayBlockingQueue with capacity set to 1 as we need to wait for only one response.
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

//        for every consumed response message it checks if the correlationId is the one we're looking for.
//        If so, it puts the response to BlockingQueue
//        main thread is waiting for response to take it from BlockingQueue.
        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.take();
        channel.basicCancel(ctag);
        // we return the response back to the user
        return result;
    }

    public void close() throws IOException {
        connection.close();
    }
}
