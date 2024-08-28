# Build Stage
FROM openjdk:17-jdk-slim AS build

# Set environment variables for build
ENV APP_HOME=/app \
    GRADLE_USER_HOME=/app/.gradle

# Set the working directory
WORKDIR $APP_HOME

# Install required packages
RUN apt-get update && apt-get install -y --no-install-recommends git openssh-client ca-certificates && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Add the SSH private key from the build argument and set up SSH config
ARG SSH_PRIVATE_KEY_BUILD
RUN mkdir -p ~/.ssh && \
    echo "$SSH_PRIVATE_KEY_BUILD" > ~/.ssh/id_rsa && \
    chmod 600 ~/.ssh/id_rsa && \
    ssh-keyscan github.com >> ~/.ssh/known_hosts

# Clone the Private Git Repository using SSH
RUN git clone git@github.com:jaebinGit/hachwimu-spring-user.git .

# Remove sensitive data after cloning
RUN rm -rf ~/.ssh/id_rsa

# Copy Gradle wrapper files and build project
COPY ./gradlew ./gradlew
COPY ./gradle ./gradle

# Download dependencies and build the project
RUN ./gradlew clean build --no-daemon --refresh-dependencies -x test

# Production Stage
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/build/libs/oliveyoung-0.0.1-SNAPSHOT.jar app.jar

# Build arguments to allow passing at build time (runtime specific)
ARG SPRING_APPLICATION_NAME
ARG SPRING_DATASOURCE_URL
ARG SPRING_DATASOURCE_USERNAME
ARG SPRING_DATASOURCE_PASSWORD
ARG SPRING_DATASOURCE_DRIVER_CLASS_NAME
ARG SPRING_JPA_HIBERNATE_DDL_AUTO
ARG SPRING_JPA_SHOW_SQL
ARG SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT
ARG SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE
ARG SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE
ARG SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT
ARG SPRING_DATASOURCE_HIKARI_MAX_LIFETIME
ARG SPRING_DATA_REDIS_HOST
ARG SPRING_DATA_REDIS_PORT

# Set environment variables with default values, can be overridden at runtime
ENV SPRING_APPLICATION_NAME=${SPRING_APPLICATION_NAME} \
    SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL} \
    SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME} \
    SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD} \
    SPRING_DATASOURCE_DRIVER_CLASS_NAME=${SPRING_DATASOURCE_DRIVER_CLASS_NAME} \
    SPRING_JPA_HIBERNATE_DDL_AUTO=${SPRING_JPA_HIBERNATE_DDL_AUTO} \
    SPRING_JPA_SHOW_SQL=${SPRING_JPA_SHOW_SQL} \
    SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=${SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT} \
    SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE} \
    SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=${SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE} \
    SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT=${SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT} \
    SPRING_DATASOURCE_HIKARI_MAX_LIFETIME=${SPRING_DATASOURCE_HIKARI_MAX_LIFETIME} \
    SPRING_DATA_REDIS_HOST=${SPRING_DATA_REDIS_HOST} \
    SPRING_DATA_REDIS_PORT=${SPRING_DATA_REDIS_PORT}

# Expose the application port
EXPOSE 8080

# Run the application with environment variables
ENTRYPOINT ["java", "-jar", "app.jar"]

#12