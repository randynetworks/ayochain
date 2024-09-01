package com.randyramadhan.ayochain;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class NodeRunner implements CommandLineRunner {

    private final Node node;

    public NodeRunner(Node node) {
        this.node = node;
    }

    @Override
    public void run(String... args) {
        node.startServer();
    }
}