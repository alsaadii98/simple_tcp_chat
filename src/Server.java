import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;


    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();

            while (!done) {
                Socket client = server.accept();
                ConnectionHandler connectionHandler = new ConnectionHandler(client);
                connections.add(connectionHandler);
                pool.execute(connectionHandler);
            }
        } catch (Exception e) {
            try {
                shutdown();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void shutdown() throws IOException {
        done = true;
        pool.shutdown();
        if (!server.isClosed()) {
            server.close();
        }
        for (ConnectionHandler connectionHandler : connections) {
            connectionHandler.shutdown();
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                out.println("Please enter a nickname: ");
                nickname = in.readLine();

                System.out.println(nickname + "connected");
                broadcast(nickname + " joined the chat");
                String message;

                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(nickname + "renamed themselves to " + messageSplit[1]);
                            System.out.println(nickname + "renamed themselves to " + messageSplit[1]);
                            nickname = messageSplit[1];
                            System.out.println("Successfully renamed themselves to " + nickname);
                        } else {
                            out.println("no nickname given");
                        }
                    } else if (message.startsWith("/quit")) {
                        broadcast(nickname + " left the chat");
                        System.out.println(nickname + " left the chat");
                        shutdown();
                    } else {
                        broadcast(nickname + ": " + message);
                    }

                }
            } catch (IOException e) {
                try {
                    shutdown();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        public void sendMessage(String message) {
            out.println( message);
        }

        public void shutdown() throws IOException {
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
