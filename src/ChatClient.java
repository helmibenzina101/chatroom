import java.io.*; // Importing input-output classes
import java.net.*; // Importing networking classes
import java.util.Scanner; // Importing Scanner class for user input

public class ChatClient {
    private String hostname; // Hostname of the server
    private int port; // Port number of the server
    private Socket socket; // Socket for connecting to the server
    private String clientName; // Name of the client

    public ChatClient(String hostname, int port) {
        this.hostname = hostname; // Initializing hostname
        this.port = port; // Initializing port
    }

    public void execute() {
        try {
            socket = new Socket(hostname, port); // Creating a socket to connect to the server
            Scanner scanner = new Scanner(System.in); // Scanner for reading user input

            System.out.print("Enter your name: "); // Prompting user to enter their name
            clientName = scanner.nextLine(); // Reading the client's name

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true); // Creating a writer to send data to the server
            writer.println(clientName); // Sending the client's name to the server

            new ReadThread(socket).start(); // Starting a new thread to read messages from the server
            new WriteThread(socket, writer).start(); // Starting a new thread to write messages to the server
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage()); // Handling I/O exceptions
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient("localhost", 2000); // Creating a new ChatClient instance
        client.execute(); // Executing the client
    }

    private static class ReadThread extends Thread {
        private BufferedReader reader; // Reader for reading messages from the server

        public ReadThread(Socket socket) {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Initializing the reader
            } catch (IOException ex) {
                System.out.println("Error getting input stream: " + ex.getMessage()); // Handling exceptions
            }
        }

        public void run() {
            try {
                String message; // Variable to store messages
                while ((message = reader.readLine()) != null) { // Reading messages from the server
                    System.out.println(message); // Printing messages to the console
                }
            } catch (IOException ex) {
                System.out.println("Disconnected from server."); // Handling disconnection
            }
        }
    }

    private static class WriteThread extends Thread {
        private PrintWriter writer; // Writer for sending messages to the server
        private BufferedReader consoleReader; // Reader for reading user input from the console
        private Socket socket; // Socket for connecting to the server

        public WriteThread(Socket socket, PrintWriter writer) {
            this.socket = socket; // Initializing socket
            this.writer = writer; // Initializing writer
            consoleReader = new BufferedReader(new InputStreamReader(System.in)); // Initializing console reader
        }

        public void run() {
            try {
                String userMessage; // Variable to store user messages

                while (true) {
                    userMessage = consoleReader.readLine(); // Reading user input

                    if (userMessage.equalsIgnoreCase("/exit")) { // Checking if the user wants to exit
                        writer.println("/exit"); // Sending exit command to the server
                        System.out.println("Exiting chat..."); // Informing the user
                        socket.close(); // Closing the socket
                        break; // Breaking the loop
                    } else {
                        writer.println(userMessage); // Sending user message to the server
                    }
                }
            } catch (IOException ex) {
                System.out.println("Error writing to server: " + ex.getMessage()); // Handling exceptions
            }
        }
    }
}
