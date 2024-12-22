#!/bin/bash

# Stop and remove the containers
docker-compose down

# Remove the logs directory and its contents
rm -rf logs

# Create necessary directories
mkdir -p logs

# Start the containers
docker-compose up -d

echo "OpenTelemetry Collector is running!"
echo "Logs will be written to:"
echo "To view Otel Console logs: cat logs/otel.log"
echo "To view logs: docker-compose logs -f collector"
echo "To tail JSON logs: tail -f logs/otel-logs.json"