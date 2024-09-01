package com.randyramadhan.ayochain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

@Component
public class Node {
    private ServerSocket serverSocket;
    private int port;
    private String ipAddress;
    private Blockchain blockchain;
    private Set<String> peers;
    private String bootstrapNodeIp;
    private int bootstrapNodePort;

    public Node(@Value("${node.port}") int port, @Value("${bootstrap.node.ip}") String bootstrapNodeIp, @Value("${bootstrap.node.port}") int bootstrapNodePort) {
        this.port = port;
        this.bootstrapNodeIp = bootstrapNodeIp;
        this.bootstrapNodePort = bootstrapNodePort;
        this.ipAddress = getLocalIpAddress();
        this.blockchain = new Blockchain();
        this.peers = new HashSet<>();
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(ipAddress));
            System.out.println("Node started on IP: " + ipAddress + " Port: " + port);

            // Register with the bootstrap node if it's not the same node
            if (!ipAddress.equals(bootstrapNodeIp) || port != bootstrapNodePort) {
                registerWithBootstrap();
            }

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                int clientPort = clientSocket.getPort();
                System.out.println("New node joined from IP: " + clientIp + " Port: " + clientPort);

                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            // Handle peer registration
            String receivedMessage = in.readUTF();
            if (receivedMessage.startsWith("REGISTER:")) {
                String newPeerAddress = receivedMessage.substring(9);
                peers.add(newPeerAddress);
                out.writeObject(new HashSet<>(peers));
                out.flush();
                System.out.println("Peer registered: " + newPeerAddress);
            } else {
                Block receivedBlock = (Block) in.readObject();
                System.out.println("Received block: " + receivedBlock.getHash());

                if (blockchain.addBlock(receivedBlock)) {
                    System.out.println("Block added to the blockchain.");
                    broadcastBlock(receivedBlock);
                } else {
                    System.out.println("Invalid block. Not added to the blockchain.");
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void broadcastBlock(Block block) {
        for (String peer : peers) {
            try (Socket socket = new Socket(peer.split(":")[0], Integer.parseInt(peer.split(":")[1]));
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                out.writeObject(block);
                out.flush();
                System.out.println("Block broadcasted to peer: " + peer);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addPeer(String peerAddress) {
        peers.add(peerAddress);
    }

    private void registerWithBootstrap() {
        String peerAddress = ipAddress + ":" + port;
        try (Socket socket = new Socket(bootstrapNodeIp, bootstrapNodePort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeUTF("REGISTER:" + peerAddress);
            out.flush();

            Set<String> knownPeers = (Set<String>) in.readObject();
            if (knownPeers != null) {
                peers.addAll(knownPeers);
            }

            System.out.println("Registered with bootstrap node. Known peers:");
            for (String peer : peers) {
                System.out.println(peer);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Failed to register with bootstrap node.");
            e.printStackTrace();
        }
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