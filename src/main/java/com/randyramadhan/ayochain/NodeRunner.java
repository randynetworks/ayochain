package com.randyramadhan.ayochain;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class NodeRunner implements CommandLineRunner {

    private final Node node;

    @Autowired
    public NodeRunner(Node node) {
        this.node = node;
    }

    @Override
    public void run(String... args) {
        node.start();
    }
}