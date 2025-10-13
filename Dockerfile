# Multi-stage Dockerfile for Android app building
FROM openjdk:17-jdk-slim as builder

# Install required packages
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=$ANDROID_HOME
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Create Android SDK directory
RUN mkdir -p $ANDROID_HOME

# Download and install Android SDK command line tools
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d $ANDROID_HOME && \
    mv $ANDROID_HOME/cmdline-tools $ANDROID_HOME/cmdline-tools-temp && \
    mkdir -p $ANDROID_HOME/cmdline-tools/latest && \
    mv $ANDROID_HOME/cmdline-tools-temp/* $ANDROID_HOME/cmdline-tools/latest/ && \
    rm -rf $ANDROID_HOME/cmdline-tools-temp /tmp/cmdline-tools.zip

# Accept Android SDK licenses
RUN yes | sdkmanager --licenses

# Install Android SDK components
RUN sdkmanager "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "extras;android;m2repository" \
    "extras;google;m2repository" \
    "ndk;25.2.9519653"

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew gradlew.bat gradle.properties settings.gradle.kts ./
COPY app/build.gradle.kts app/
COPY build.gradle.kts ./

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY app/src/ app/src/
COPY app/proguard-rules.pro app/

# Build arguments for signing
ARG KEYSTORE_PASSWORD
ARG KEY_ALIAS
ARG KEY_PASSWORD
ARG BUILD_TYPE=debug

# Copy keystore if building release
COPY keystore/ keystore/

# Build the application
RUN if [ "$BUILD_TYPE" = "release" ]; then \
        ./gradlew assembleGhanaRelease bundleGhanaRelease --no-daemon; \
    else \
        ./gradlew assembleGhanaDebug --no-daemon; \
    fi

# Runtime stage for serving built artifacts
FROM nginx:alpine as runtime

# Copy built APK/AAB files
COPY --from=builder /app/app/build/outputs/ /usr/share/nginx/html/outputs/

# Create a simple index page
RUN echo '<html><body><h1>Ghana Voice Ledger Build Artifacts</h1><ul>' > /usr/share/nginx/html/index.html && \
    find /usr/share/nginx/html/outputs -name "*.apk" -o -name "*.aab" | \
    sed 's|/usr/share/nginx/html/||' | \
    sed 's|.*|<li><a href="&">&</a></li>|' >> /usr/share/nginx/html/index.html && \
    echo '</ul></body></html>' >> /usr/share/nginx/html/index.html

EXPOSE 80

# Development stage for local development
FROM builder as development

# Install additional development tools
RUN apt-get update && apt-get install -y \
    vim \
    nano \
    htop \
    && rm -rf /var/lib/apt/lists/*

# Set up development environment
WORKDIR /app

# Expose common Android development ports
EXPOSE 8080 8081 5037

# Default command for development
CMD ["./gradlew", "assembleGhanaDebug", "--continuous"]