FROM docker-registry.tools.expedia.com/stratus/primer-base-springboot:8-2

ENV APP_NAME=cs-media-service-ng

# Add custom entrypoint script
COPY docker-entrypoint.sh /

# Install application
COPY target/cs-media-service-ng.jar /app/bin/
