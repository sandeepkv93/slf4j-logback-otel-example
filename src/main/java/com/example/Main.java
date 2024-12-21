package com.example;

import com.example.model.User;
import com.example.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import static net.logstash.logback.argument.StructuredArguments.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
public class Main {
    public static void main(String[] args) {
        try (MDC.MDCCloseable mdcContext = MDC.putCloseable("sessionId", UUID.randomUUID().toString());
             MDC.MDCCloseable mdcEnv = MDC.putCloseable("environment", System.getProperty("env", "local"))) {

            log.info("Application starting",
                    fields(Map.of(
                            "app_name", "user-service",
                            "environment", System.getProperty("env", "local"),
                            "startup_time", System.currentTimeMillis()
                    )));

            UserService userService = new UserService();

            try {
                // Create initial user
                User user1 = userService.createUser(1L, "john_doe", "john@example.com");
                log.info("Initial user created",
                        keyValue("user_id", user1.getId()),
                        keyValue("operation", "initial_setup"));

                // Try to create a duplicate user
                try {
                    User duplicateUser = userService.createUser(1L, "john_doe2", "john2@example.com");
                    log.info("Duplicate user created (shouldn't happen)",
                            v("duplicateUser", duplicateUser));
                } catch (IllegalArgumentException e) {
                    MDC.put("error_type", "duplicate_user");
                    log.info("Duplicate user creation prevented",
                            array("validation", kv("error_type", "duplicate_user"),
                                    kv("attempted_id", 1L),
                                    kv("message", e.getMessage())));
                }

                // Get existing user
                userService.getUser(1L)
                        .ifPresent(user -> log.info("Retrieved user details",
                                kv("operation", "user_retrieval"),
                                kv("user_id", user.getId()),
                                kv("found", true)));

                // Get non-existent user
                userService.getUser(999L)
                        .ifPresentOrElse(
                                user -> log.info("User found (shouldn't happen)", v("user", user)),
                                () -> log.info("Expected: User not found",
                                        kv("attempted_id", 999L),
                                        kv("operation", "user_retrieval"),
                                        kv("found", false))
                        );

                // Delete operations
                userService.deleteUser(1L);
                userService.deleteUser(1L);

            } catch (Exception e) {
                MDC.put("error_type", e.getClass().getSimpleName());
                MDC.put("error_message", e.getMessage());
                log.error("Unexpected error in main flow",
                        fields(Map.of(
                                "error_type", e.getClass().getSimpleName(),
                                "error_message", e.getMessage(),
                                "stack_trace", e.getStackTrace()
                        )));
                throw e;
            } finally {
                log.info("Application shutting down",
                        fields(Map.of(
                                "shutdown_time", System.currentTimeMillis(),
                                "operation", "application_shutdown",
                                "status", "clean"
                        )));
            }
        }
    }
}