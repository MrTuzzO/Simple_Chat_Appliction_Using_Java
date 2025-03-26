import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientThreading {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String username;

    public ClientThreading() {
        setupUI();
        connectToServer();
        setupReceiverThread();
    }

    private void setupUI() {
        frame = new JFrame("Chat Client");
        frame.setSize(800, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        inputField.addActionListener(e -> sendMessage());

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(createStyledButton("Send File", e -> sendFile()));
        buttonPanel.add(createStyledButton("Send Image", e -> sendImage()));
        buttonPanel.add(createStyledButton("Send Emoji", e -> sendEmoji()));
        buttonPanel.add(createStyledButton("Private Message", e -> sendPrivateMessage()));
        buttonPanel.add(createStyledButton("Group Message", e -> sendGroupMessage()));
        buttonPanel.add(createStyledButton("Exit", e -> exitChat()));

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputField, BorderLayout.SOUTH);
        frame.add(buttonPanel, BorderLayout.PAGE_END);

        frame.setVisible(true);
    }

    private JButton createStyledButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setBackground(new Color(18, 140, 126));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.addActionListener(action);
        return button;
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 2340);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            updateChatArea("Connected to the server");
            updateChatArea(dis.readUTF()); 

            username = JOptionPane.showInputDialog("Enter your username:");
            dos.writeUTF(username);
            updateChatArea(dis.readUTF());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupReceiverThread() {
        new Thread(() -> {
            try {
                while (true) {
                    updateChatArea(dis.readUTF());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendMessage() {
        try {
            dos.writeUTF("Text");
            dos.writeUTF(inputField.getText());
            inputField.setText("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFile() {
        handleFileTransfer("File");
    }

    private void sendImage() {
        handleFileTransfer("Image");
    }

    private void handleFileTransfer(String type) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());

                dos.writeUTF(type);
                dos.writeUTF(selectedFile.getName());
                dos.writeLong(fileBytes.length);
                dos.write(fileBytes);

                updateChatArea(type + " '" + selectedFile.getName() + "' sent.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPrivateMessage() {
        sendMessageWithRecipient("Private");
    }

    private void sendGroupMessage() {
        sendMessageWithRecipient("GroupMessage");
    }

    private void sendMessageWithRecipient(String type) {
        try {
            String recipient = type.equals("Private") ? JOptionPane.showInputDialog("Enter the recipient username:") : null;
            String message = JOptionPane.showInputDialog("Enter your message:");

            dos.writeUTF(type);
            if (recipient != null) dos.writeUTF(recipient);
            dos.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendEmoji() {
        try {
            dos.writeUTF("Emoji");
            dos.writeUTF(JOptionPane.showInputDialog("Enter the emoji (e.g., 😊):"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exitChat() {
        try {
            dos.writeUTF("Exit");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateChatArea(String message) {
        String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> chatArea.append("[" + timeStamp + "] " + message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientThreading::new);
    }
}