package com.example.service;

import com.example.model.User;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import static net.logstash.logback.argument.StructuredArguments.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class UserService {
    private final Map<Long, User> users = new HashMap<>();

    public User createUser(Long id, String username, String email) {
        try (MDC.MDCCloseable mdcContext = MDC.putCloseable("operationId", UUID.randomUUID().toString());
             MDC.MDCCloseable mdcUserId = MDC.putCloseable("userId", String.valueOf(id));
             MDC.MDCCloseable mdcOperation = MDC.putCloseable("operation", "createUser")) {

            log.debug("Creating new user",
                    kv("user_id", id),
                    kv("username", username),
                    kv("action", "create_user_attempt"));

            if (users.containsKey(id)) {
                log.error("Failed to create user - duplicate ID",
                        fields(Map.of(
                                "user_id", id,
                                "error_code", "DUPLICATE_USER",
                                "error_type", "validation_error"
                        )));
                throw new IllegalArgumentException("User already exists");
            }

            try {
                User user = new User(id, username, email);
                users.put(id, user);

                log.info("User created successfully",
                        keyValue("user_id", id),
                        keyValue("username", username),
                        keyValue("email", email),
                        keyValue("status", "success"));

                return user;
            } catch (Exception e) {
                MDC.put("errorType", e.getClass().getSimpleName());
                MDC.put("errorMessage", e.getMessage());

                log.error("User creation failed",
                        array("error_details", kv("user_id", id),
                                kv("error_message", e.getMessage()),
                                kv("error_type", e.getClass().getSimpleName())));
                throw e;
            }
        }
    }

    public Optional<User> getUser(Long id) {
        try (MDC.MDCCloseable mdcContext = MDC.putCloseable("operationId", UUID.randomUUID().toString());
             MDC.MDCCloseable mdcUserId = MDC.putCloseable("userId", String.valueOf(id));
             MDC.MDCCloseable mdcOperation = MDC.putCloseable("operation", "getUser")) {

            log.debug("Fetching user details", entries(Map.of(
                    "user_id", id,
                    "action", "get_user",
                    "timestamp", System.currentTimeMillis()
            )));

            User user = users.get(id);

            if (user == null) {
                MDC.put("status", "not_found");
                log.warn("User not found",
                        keyValue("user_id", id),
                        keyValue("action", "get_user"),
                        keyValue("status", "not_found"));
                return Optional.empty();
            }

            MDC.put("status", "success");
            log.info("User retrieved successfully",
                    v("user", user),
                    v("id", id));
            return Optional.of(user);
        }
    }

    public void deleteUser(Long id) {
        try (MDC.MDCCloseable mdcContext = MDC.putCloseable("operationId", UUID.randomUUID().toString());
             MDC.MDCCloseable mdcUserId = MDC.putCloseable("userId", String.valueOf(id));
             MDC.MDCCloseable mdcOperation = MDC.putCloseable("operation", "deleteUser")) {

            log.debug("Attempting user deletion",
                    kv("user_id", id),
                    kv("action", "delete_user_attempt"));

            if (users.remove(id) != null) {
                MDC.put("status", "success");
                log.info("User deleted",
                        kv("user_id", id),
                        keyValue("operation_status", "success"),
                        keyValue("timestamp", System.currentTimeMillis()));
            } else {
                MDC.put("status", "failed");
                MDC.put("reason", "user_not_found");
                log.warn("User deletion failed",
                        fields(Map.of(
                                "user_id", id,
                                "reason", "user_not_found",
                                "action", "delete_user",
                                "status", "failed"
                        )));
            }
        }
    }
}
