package com.myoffgridai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the MyOffGridAI server application.
 *
 * <p>MyOffGridAI is a turnkey, plug-and-play offline AI appliance
 * designed to run on a mini PC without internet connectivity.</p>
 */
@SpringBootApplication
public class MyOffGridAiApplication {

    /**
     * Launches the MyOffGridAI Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MyOffGridAiApplication.class, args);
    }
}
