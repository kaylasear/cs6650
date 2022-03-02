//package assignment2;
//
//import com.rabbitmq.client.*;
//
//import java.nio.charset.StandardCharsets;
//
//public class Consumer {
//    private final static String QUEUE_NAME = "queue";
//
//    public static void main(String[] argv) throws Exception {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("54.218.71.248");
//        factory.setUsername("admin");
//        factory.setPassword("pass");
//        factory.setPort(5672);
//        Connection connection = factory.newConnection();
//        Channel channel = connection.createChannel();
//
//        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
//        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
//
//        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
//            System.out.println(" [x] Received '" + message + "'");
//        };
//        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
//
//        }
//
//    private static String store(int n) {
//        return ("storing..");
//    }
//}
