package com.randyramadhan.ayochain;

import lombok.Data;

import java.security.MessageDigest;
import java.util.Date;

@Data
public class Block {

    private String hash;
    private String previousHash;
    private String data;
    private long timeStamp;
    private int nonce;

    public Block(String data, String previousHash) {
        this.data = data;
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
    }

    public static String applySha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String calculateHash() {
        String input = previousHash + Long.toString(timeStamp) + Integer.toString(nonce) + data;
        return applySha256(input);
    }

    public void mineBlock(int difficulty) {
        long startTime = System.currentTimeMillis();  // Record the start time
        long hashes = 0;  // Count the number of hashes

        String target = new String(new char[difficulty]).replace('\0', '0');
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
            hashes++;
        }

        long endTime = System.currentTimeMillis();  // Record the end time
        long duration = endTime - startTime;  // Calculate the duration in milliseconds
        double durationInSeconds = duration / 1000.0;
        double hashRate = hashes / durationInSeconds;  // Calculate hash rate (hashes per second)

        System.out.println(String.format("Block mined: %s Duration: %d/ms Hash Rate: %.2f hr/s", hash, duration, hashRate));
    }
}