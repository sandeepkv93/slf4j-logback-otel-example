package com.example;

import com.example.model.User;
import com.example.service.UserService;
import io.opentelemetry.api.logs.GlobalLoggerProvider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import java.util.Map;
import java.util.UUID;
import static net.logstash.logback.argument.StructuredArguments.*;

@Slf4j
public class Main {
    private static final String SERVICE_NAME = "UserService";

    private static void initializeOpenTelemetry() {
        // Set service name on all OTel signals
        Resource resource = Resource.getDefault().merge(Resource.create(
                Attributes.of(ResourceAttributes.SERVICE_NAME, SERVICE_NAME)));

        // Init OTel logger provider with export to OTLP
        SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(
                                OtlpGrpcLogRecordExporter.builder()
                                        .setEndpoint(System.getenv().getOrDefault(
                                                "OTEL_EXPORTER_OTLP_ENDPOINT",
                                                "http://localhost:4317"))
                                        .build())
                        .build())
                .build();

        // Create SDK object and set it as global
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setLoggerProvider(sdkLoggerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
        GlobalOpenTelemetry.set(sdk);

        //connect logger
        GlobalLoggerProvider.set(sdk.getSdkLoggerProvider());

        // Add hook to close SDK, which flushes logs
        Runtime.getRuntime().addShutdownHook(new Thread(sdk::close));
    }

    public static void main(String[] args) throws InterruptedException {
        initializeOpenTelemetry();

        try (MDC.MDCCloseable mdcContext = MDC.putCloseable("sessionId", UUID.randomUUID().toString());
             MDC.MDCCloseable mdcEnv = MDC.putCloseable("environment", System.getProperty("env", "local"))) {

            log.info("Application starting",
                    fields(Map.of(
                            "app_name", "user-service",
                            "environment", System.getProperty("env", "local"),
                            "startup_time", System.currentTimeMillis()
                    )));

            UserService userService = new UserService();

            // Create and manipulate users to generate logs
            try {
                Thread.sleep(1000); // Wait for OpenTelemetry initialization

                // Create initial user
                User user1 = userService.createUser(1L, "john_doe", "john@example.com");
                log.info("Initial user created",
                        kv("user_id", user1.getId()),
                        kv("operation", "initial_setup"));

                // Try to create a duplicate user
                try {
                    userService.createUser(1L, "john_doe2", "john2@example.com");
                } catch (IllegalArgumentException e) {
                    log.info("Duplicate user creation prevented",
                            array("validation",
                                    kv("error_type", "duplicate_user"),
                                    kv("attempted_id", 1L),
                                    kv("message", e.getMessage())));
                }

                // Get existing user
                userService.getUser(1L);
                userService.getUser(999L);
                userService.deleteUser(1L);

            } catch (Exception e) {
                log.error("Unexpected error in main flow",
                        fields(Map.of(
                                "error_type", e.getClass().getSimpleName(),
                                "error_message", e.getMessage()
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
