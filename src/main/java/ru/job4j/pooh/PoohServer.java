package ru.job4j.pooh;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PoohServer {
    private final QueueSchema queueSchema = new QueueSchema();
    private final TopicSchema topicSchema = new TopicSchema();

    private void runSchemas() {
        ExecutorService pool = Executors.newCachedThreadPool();
        pool.execute(queueSchema);
        pool.execute(topicSchema);
    }

    /*
    Добавил методы handleClient и processRequest, т.к. бот не хотел принимать код из-за
    большого объёма метода runServer в его первоначальном варианте.
    */

    private void runServer() {
        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket server = new ServerSocket(9000)) {
            System.out.println("Pooh is ready ...");
            while (!server.isClosed()) {
                Socket socket = server.accept();
                handleClient(socket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket) {
        ExecutorService pool = Executors.newCachedThreadPool();
        pool.execute(() -> {
            try (OutputStream out = socket.getOutputStream();
                 var input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                while (true) {
                    var details = input.readLine().split(";");
                    if (details.length != 3) {
                        continue;
                    }
                    processRequest(details, out);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void processRequest(String[] details, OutputStream out) {
        var action = details[0];
        var name = details[1];
        var text = details[2];

        if (action.equals("intro")) {
            if (name.equals("queue")) {
                queueSchema.addReceiver(new SocketReceiver(text, new PrintWriter(out)));
            }
            if (name.equals("topic")) {
                topicSchema.addReceiver(new SocketReceiver(text, new PrintWriter(out)));
            }
        }
        if (action.equals("queue")) {
            queueSchema.publish(new Message(name, text));
        }
        if (action.equals("topic")) {
            topicSchema.publish(new Message(name, text));
        }
    }

    public static void main(String[] args) throws Exception {
        var pooh = new PoohServer();
        pooh.runSchemas();
        pooh.runServer();
    }
}