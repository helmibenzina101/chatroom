import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ChatServer is a simple multi-client chat server application using TCP.
 * 
 * TCP (Transmission Control Protocol) and UDP (User Datagram Protocol) are two transport layer protocols.
 * 
 * Differences between TCP and UDP:
 * - TCP is connection-oriented, meaning it establishes a connection before data transfer and ensures data is received in order.
 * - UDP is connectionless, meaning it sends data without establishing a connection and does not guarantee order or delivery.
 * - TCP provides error checking and recovery, while UDP provides basic error checking but no recovery.
 * - TCP is slower due to its overhead for reliability, while UDP is faster but less reliable.
 */

public class ChatServer {
    // A thread-safe set to store client handlers
    private static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());

    /**
     * Main method to start the chat server.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        int port = 2000; // Port number for the server to listen on

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chat server is listening on port " + port);

            // Continuously listen for client connections
            while (true) {
                Socket socket = serverSocket.accept(); // Accept a new client connection
                ClientHandler clientHandler = new ClientHandler(socket); // Create a new client handler
                clientHandlers.add(clientHandler); // Add the client handler to the set
                new Thread(clientHandler).start(); // Start the client handler in a new thread
            }
        } catch (IOException e) {
            System.out.println("Error in the server: " + e.getMessage()); // Handle server errors
        }
    }

    /**
     * Broadcasts a message to all connected clients except the sender.
     * @param message The message to broadcast.
     * @param sender The client handler sending the message.
     */
    static void broadcastMessage(String message, ClientHandler sender) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client != sender) { // Do not send the message to the sender
                    client.sendMessage(message);
                }
            }
        }
    }

    /**
     * Sends a private message to a specific client.
     * @param message The message to send.
     * @param recipientName The name of the recipient client.
     * @param sender The client handler sending the message.
     */
    static void sendPrivateMessage(String message, String recipientName, ClientHandler sender) {
        boolean found = false; // Flag to check if the recipient is found

        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client.getClientName().equalsIgnoreCase(recipientName)) {
                    client.sendMessage("[Private] " + sender.getClientName() + ": " + message);
                    found = true;
                    break;
                }
            }
        }

        if (!found) { // If the recipient is not found, notify the sender
            sender.sendMessage("User '" + recipientName + "' not found.");
        }
    }

    /**
     * Removes a client from the set of connected clients and notifies others.
     * @param clientHandler The client handler to remove.
     */
    static void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
        broadcastMessage(clientHandler.getClientName() + " has left the chat.", null);
        System.out.println("Client '" + clientHandler.getClientName() + "' disconnected.");
    }

    /**
     * Returns a list of connected users.
     * @return A string listing all connected users.
     */
    static String getConnectedUsers() {
        StringBuilder users = new StringBuilder("Connected users: ");

        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                users.append(client.getClientName()).append(", ");
            }
        }
        return users.substring(0, users.length() - 2); // Remove the trailing comma and space
    }

    /**
     * ClientHandler handles communication with a single client.
     */
    private static class ClientHandler implements Runnable {
        private Socket socket; // The client's socket
        private PrintWriter writer; // Writer to send messages to the client
        private String clientName; // The client's name

        /**
         * Constructor to initialize the client handler with the client's socket.
         * @param socket The client's socket.
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        /**
         * The run method to handle client communication.
         */
        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                writer = new PrintWriter(socket.getOutputStream(), true);
                clientName = reader.readLine(); // Read the client's name
                broadcastMessage(clientName + " has joined the chat!", this);

                // Send command instructions to the client
                String commandInstructions = """
                        Welcome to the chat, %s!
                        Available commands:
                        /users - List all connected users.
                        /msg <username> <message> - Send a private message.
                        /exit - Leave the chat.
                        """.formatted(clientName);

                writer.println(commandInstructions);

                String clientMessage;
                while ((clientMessage = reader.readLine()) != null) {
                    if (clientMessage.equalsIgnoreCase("/exit")) {
                        break; // Exit the loop if the client wants to leave
                    } else if (clientMessage.equalsIgnoreCase("/users")) {
                        writer.println(getConnectedUsers()); // Send the list of connected users
                    } else if (clientMessage.startsWith("/msg ")) {
                        String[] parts = clientMessage.split(" ", 3);
                        if (parts.length >= 3) {
                            sendPrivateMessage(parts[2], parts[1], this); // Send a private message
                        } else {
                            writer.println("Invalid format. Use: /msg <user> <message>");
                        }
                    } else {
                        broadcastMessage(formatMessage(clientName, clientMessage), this); // Broadcast the message
                    }
                }
            } catch (IOException e) {
                System.out.println("Error with client communication: " + e.getMessage());
            } finally {
                try {
                    socket.close(); // Close the client's socket
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
                removeClient(this); // Remove the client from the set of connected clients
            }
        }

        /**
         * Formats a message with a timestamp and the client's name.
         * @param name The client's name.
         * @param message The message to format.
         * @return The formatted message.
         */
        private String formatMessage(String name, String message) {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            return "[" + timestamp + "] " + name + ": " + message;
        }

        /**
         * Sends a message to the client.
         * @param message The message to send.
         */
        void sendMessage(String message) {
            writer.println(message);
        }

        /**
         * Returns the client's name.
         * @return The client's name.
         */
        public String getClientName() {
            return clientName;
        }
    }
}