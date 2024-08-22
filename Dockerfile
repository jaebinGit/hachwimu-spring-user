# Build Stage
FROM openjdk:17-jdk-slim AS build

# Set environment variables
ENV APP_HOME=/app \
    JAVA_OPTS="-Xms512m -Xmx1024m" \
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

# Expose the application port
EXPOSE 8080

# Run the application with environment variables
ENTRYPOINT ["java", "-jar", "app.jar"]