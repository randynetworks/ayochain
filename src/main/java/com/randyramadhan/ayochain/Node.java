package com.randyramadhan.ayochain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class Node {
    private Blockchain blockchain = new Blockchain();
    private Set<String> peers = new HashSet<>();
    private int port;
    private String ipAddress;
    private ServerSocket serverSocket;

    public Node(int port) {
        this.port = port;
        this.ipAddress = getLocalIpAddress();
    }

    public void start() {
        new Thread(this::startServer).start();
        new Thread(this::startMining).start();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Node started on IP: " + ipAddress + " Port: " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                int clientPort = clientSocket.getPort();
                System.out.println("New node joined from IP: " + clientIp + " Port: " + clientPort);

                // You can handle the connection here, e.g., in a new thread
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received: " + inputLine);
                if (inputLine.startsWith("BLOCK")) {
                    Block newBlock = deserializeBlock(inputLine.substring(6));
                    if (validateAndAddBlock(newBlock)) {
                        broadcastBlock(newBlock);
                    }
                } else if (inputLine.startsWith("PEER")) {
                    String newPeer = inputLine.substring(5);
                    peers.add(newPeer);
                    System.out.println("New peer added: " + newPeer);
                }
                out.println("ACK");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startMining() {
        while (true) {
            try {
                // Simulate mining process
                Block newBlock = new Block("Some transaction data", getLastBlock().getHash());
                blockchain.addBlock(newBlock);
                broadcastBlock(newBlock);
                Thread.sleep(10000); // Simulate time taken to mine a block
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private Block getLastBlock() {
        return blockchain.getBlockchain().get(blockchain.getBlockchain().size() - 1);
    }

    private boolean validateAndAddBlock(Block block) {
        Block lastBlock = getLastBlock();
        if (!lastBlock.getHash().equals(block.getPreviousHash())) {
            System.out.println("Invalid block received.");
            return false;
        }
        blockchain.addBlock(block);
        return true;
    }

    private void broadcastBlock(Block block) {
        String serializedBlock = "BLOCK " + serializeBlock(block);
        for (String peer : peers) {
            try {
                String[] hostPort = peer.split(":");
                Socket socket = new Socket(hostPort[0], Integer.parseInt(hostPort[1]));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(serializedBlock);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String serializeBlock(Block block) {
        // Convert the block into a string (simplified serialization)
        return block.getPreviousHash() + "|" + block.getData() + "|" + block.getTimeStamp() + "|" + block.getNonce() + "|" + block.getHash();
    }

    private Block deserializeBlock(String data) {
        String[] parts = data.split("\\|");
        Block block = new Block(parts[1], parts[0]);
        block.setTimeStamp(Long.parseLong(parts[2]));
        block.setNonce(Integer.parseInt(parts[3]));
        block.setHash(parts[4]);
        return block;
    }

    public void addPeer(String peer) {
        peers.add(peer);
    }


    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "127.0.0.1"; // Fallback to localhost if no address found
    }
}