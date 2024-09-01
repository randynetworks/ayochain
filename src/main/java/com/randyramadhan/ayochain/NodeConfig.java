package com.randyramadhan.ayochain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NodeConfig {

    @Bean
    public Node node(@Value("${node.port}") int port,
                     @Value("${bootstrap.node.ip}") String bootstrapNodeIp,
                     @Value("${bootstrap.node.port}") int bootstrapNodePort) {
        return new Node(port, bootstrapNodeIp, bootstrapNodePort);
    }
}
