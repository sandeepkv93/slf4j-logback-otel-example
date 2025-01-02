# Logging and Telemetry Demo

This repository demonstrates advanced logging and telemetry practices in a Java application using SLF4J, Logback, and OpenTelemetry. It showcases structured logging, multiple logging appenders, and integration with the OpenTelemetry collector.

## Features

- Structured logging using Logback with JSON formatting
- Multiple logging appenders (Console, File, and OpenTelemetry)
- MDC (Mapped Diagnostic Context) for request tracing
- OpenTelemetry integration for centralized logging
- Docker-based OpenTelemetry collector setup
- Example user service demonstrating logging best practices

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- Git

## Project Structure

```
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/
│       │       ├── model/
│       │       │   └── User.java
│       │       ├── service/
│       │       │   └── UserService.java
│       │       └── Main.java
│       └── resources/
│           └── logback.xml
├── docker-compose.yml
├── otel-config.yml
├── pom.xml
└── telemetry_infra_setup.sh
```

## Setup and Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/sandeepkv93/slf4j-logback-otel-example
   cd slf4j-logback-otel-example
   ```

2. Build the project:
   ```bash
   mvn clean package
   ```

3. Set up the OpenTelemetry infrastructure:
   ```bash
   chmod +x telemetry_infra_setup.sh
   ./telemetry_infra_setup.sh
   ```

## Configuration Details

### Logback Configuration (logback.xml)

The application uses three different appenders:

1. **Console Appender**: Formats logs with colors for better readability
    - Includes timestamp, thread, log level, logger name, and MDC context
    - Uses custom pattern with highlighting

2. **File Appender**: Generates JSON-formatted logs
    - Implements log rotation (100MB per file)
    - Keeps 30 days of history
    - Maximum total size cap of 5GB
    - Includes MDC context and structured arguments

3. **OpenTelemetry Appender**: Sends logs to OpenTelemetry collector
    - Captures experimental attributes
    - Includes MDC context
    - Supports structured logging with LogstashEncoder

### OpenTelemetry Configuration (otel-config.yml)

The OpenTelemetry collector is configured to:
- Listen on gRPC port 4317
- Process logs in batches
- Export logs to console (debug) and file
- Add custom attributes to log records

## Key Components

### User Service Example

The `UserService` class demonstrates logging best practices:
- Structured logging with key-value pairs
- MDC context for operation tracking
- Error handling with detailed logging
- Operation lifecycle logging

```java
// Example of structured logging
log.info("User created successfully",
    keyValue("user_id", id),
    keyValue("username", username),
    keyValue("email", email),
    keyValue("status", "success"));
```

### OpenTelemetry Integration

The application initializes OpenTelemetry with:
- Service name attribution
- OTLP gRPC exporter configuration
- Batch processing of telemetry data
- Automatic context propagation

## Running the Application

1. Start the OpenTelemetry collector:
   ```bash
   docker-compose up -d
   ```

2. Run the application:
   ```bash
   java -jar target/logback-example-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

## Monitoring and Log Access

- **Console Logs**: Visible in the terminal when running the application
- **JSON Logs**: Available in `logs/application.log`
- **OpenTelemetry Logs**:
    - View collector logs: `docker-compose logs -f collector`
    - Access processed logs: `tail -f logs/otel-logs.json`

## Best Practices Demonstrated

1. **Structured Logging**
    - Consistent JSON formatting
    - Key-value pairs for machine readability
    - Contextual information through MDC

2. **Operational Monitoring**
    - Request tracking with unique IDs
    - Error handling with detailed context
    - Performance monitoring capabilities

3. **Log Management**
    - Log rotation and retention policies
    - Size-based file rolling
    - Multiple output formats and destinations

## Dependencies

- SLF4J 2.0.9
- Logback 1.5.13
- Logstash Encoder 7.4
- OpenTelemetry SDK 1.25.0
- Lombok 1.18.30
