package com.app.relayhook.Logs;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NodeErrorLogger {

    private static final String LOG_FILE = "node_errors.log";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Main method with optional exception
    public static void logError(Object message, Exception e) {
        String timestamp = LocalDateTime.now().toString();
        String serializedMessage;
        try {
            serializedMessage = objectMapper.writeValueAsString(message);
        } catch (Exception ex) {
            serializedMessage = message.toString();
        }

        String logMessage = String.format(
            "[%s] Message: %s | Error: %s%n",
            timestamp,
            serializedMessage,
            e != null ? e.toString() : "No exception"
        );

        try {
            Files.write(
                Paths.get(LOG_FILE),
                logMessage.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException ioException) {
            System.err.println("Failed to write error log: " + ioException.getMessage());
        }
    }

    // Overloaded method without exception
    public static void logError(Object message) {
        logError(message, null);
    }
}
