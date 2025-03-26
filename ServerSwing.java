import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class ServerSwing {
    static final Set<ClientHandler> clients = new HashSet<>();

    public static void main(String[] args) throws Exception {
        createDirectories();
        ServerSocket ss = new ServerSocket(2340);
        System.out.println("Waiting for clients");

        while (true) {
            Socket s = ss.accept();
            System.out.println("New client connected: " + s);

            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            DataInputStream dis = new DataInputStream(s.getInputStream());

            dos.writeUTF("Welcome to the chat! Please enter your username:");
            String username = dis.readUTF();
            dos.writeUTF("Hello, " + username + "! You have joined the chat.");

            ClientHandler clientHandler = new ClientHandler(s, dis, dos, username);
            clients.add(clientHandler);

            new Thread(clientHandler).start();
        }
    }

    private static void createDirectories() {
        try {
            Files.createDirectories(Paths.get("server_received_files"));
            Files.createDirectories(Paths.get("server_received_images"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void broadcast(String message, ClientHandler sender, boolean toAll) {
        for (ClientHandler client : clients) {
            if (toAll || client != sender) {
                try {
                    client.output.writeUTF(message);
                } catch (IOException e) {
                    System.out.println("Error broadcasting to " + client.username + ": " + e.getMessage());
                }
            }
        }
    }

    static void sendPrivateMessage(String message, String recipient, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client.username.equals(recipient)) {
                try {
                    client.output.writeUTF(sender.username + " (private): " + message);
                } catch (IOException e) {
                    System.out.println("Error sending private message to " + client.username + ": " + e.getMessage());
                }
                break;
            }
        }
    }
}

class ClientHandler implements Runnable {
    final Socket socket;
    final DataInputStream input;
    final DataOutputStream output;
    final String username;

    public ClientHandler(Socket socket, DataInputStream input, DataOutputStream output, String username) {
        this.socket = socket;
        this.input = input;
        this.output = output;
        this.username = username;
    }

    @Override
    public void run() {
        try {
            while (true) {
                output.writeUTF("What do you want? [Text/Emoji/File/Image/Private/GroupMessage/Exit]");
                switch (input.readUTF()) {
                    case "Text":
                        handleMessage(false);
                        break;
                    case "Emoji":
                        handleMessage(false);
                        break;
                    case "File":
                        receiveFileOrImage("server_received_files");
                        break;
                    case "Image":
                        receiveFileOrImage("server_received_images");
                        break;
                    case "Private":
                        handlePrivateMessage();
                        break;
                    case "GroupMessage":
                        handleMessage(true);
                        break;
                    case "Exit":
                        exitChat();
                        return;
                    default:
                        output.writeUTF("Invalid input");
                }
            }
        } catch (IOException e) {
            System.out.println("Error in ClientHandler: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void handleMessage(boolean toAll) throws IOException {
        output.writeUTF(toAll ? "Enter your group message:" : "Enter your message:");
        String message = input.readUTF();
        ServerSwing.broadcast(username + (toAll ? " (group): " : ": ") + message, this, toAll);
    }

    private void handlePrivateMessage() throws IOException {
        output.writeUTF("Enter the recipient username:");
        String recipient = input.readUTF();
        output.writeUTF("Enter your private message:");
        String message = input.readUTF();
        ServerSwing.sendPrivateMessage(message, recipient, this);
    }

    private void receiveFileOrImage(String directory) {
        try {
            String name = input.readUTF();
            long size = input.readLong();

            byte[] bytes = new byte[(int) size];
            input.readFully(bytes);

            Files.write(Paths.get(directory, name), bytes);
            ServerSwing.broadcast("File/Image received: " + name, this, true);
        } catch (IOException e) {
            System.out.println("Error receiving file/image: " + e.getMessage());
        }
    }

    private void exitChat() throws IOException {
        ServerSwing.broadcast("User " + username + " has left the chat.", this, false);
    }

    private void closeConnection() {
        try {
            ServerSwing.clients.remove(this);
            socket.close();
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }
}
