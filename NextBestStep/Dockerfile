# Next Best Step: reproducible build environment.
#
# The image holds JDK 17, the Android SDK and the project's Gradle dependencies.
# API keys are NOT baked into the image: they are passed at `docker run` time
# (see docker-compose.yml and .env.example), and the APKs are copied to /dist.
#
#   docker compose build
#   docker compose run --rm apk          # debug APK  -> ./dist
#   docker compose run --rm apk release  # release APK -> ./dist
#   docker compose run --rm apk test     # engine unit tests

# Android build-tools (aapt2 etc.) only ship for x86_64, so pin the platform.
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-jammy

ARG CMDLINE_TOOLS_VERSION=11076708
ARG ANDROID_PLATFORM=android-36
ARG BUILD_TOOLS=36.0.0

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    GRADLE_USER_HOME=/gradle-cache \
    GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.welcome=never"
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

RUN apt-get update \
 && apt-get install -y --no-install-recommends unzip wget ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# ---- Android SDK -----------------------------------------------------------
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
 && wget --timeout=60 --tries=5 --progress=dot:mega https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip -O /tmp/tools.zip \
 && unzip -q /tmp/tools.zip -d ${ANDROID_HOME}/cmdline-tools \
 && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
 && rm /tmp/tools.zip \
 && yes | sdkmanager --licenses > /dev/null \
 && sdkmanager --install "platform-tools" "platforms;${ANDROID_PLATFORM}" "build-tools;${BUILD_TOOLS}" \
 && sdkmanager --list_installed

# Longer timeouts for slow networks when Gradle downloads libraries.
ENV GRADLE_OPTS="${GRADLE_OPTS} -Dorg.gradle.internal.http.connectionTimeout=120000 -Dorg.gradle.internal.http.socketTimeout=120000"

WORKDIR /app

# ---- Gradle ----------------------------------------------------------------
# Same version as the project's wrapper (read from gradle-wrapper.properties).
# Downloaded with wget rather than ./gradlew because wget retries; the wrapper's
# own downloader gives up on the first refused connection.
COPY gradle ./gradle
RUN GRADLE_VERSION=$(sed -n 's/.*gradle-\([0-9.]*\)-bin\.zip.*/\1/p' gradle/wrapper/gradle-wrapper.properties) \
 && echo "Installing Gradle ${GRADLE_VERSION}" \
 && wget --timeout=60 --tries=10 --retry-connrefused --waitretry=5 --progress=dot:mega \
      https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip \
 && unzip -q /tmp/gradle.zip -d /opt \
 && ln -s /opt/gradle-${GRADLE_VERSION} /opt/gradle \
 && rm /tmp/gradle.zip
ENV PATH="/opt/gradle/bin:${PATH}"

# ---- Dependencies (cached until a build file changes) ----------------------
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY app/build.gradle.kts app/proguard-rules.pro ./app/
# More retries per file for flaky networks (Gradle's default is 3).
ENV GRADLE_OPTS="${GRADLE_OPTS} -Dorg.gradle.internal.repository.max.retries=10 -Dorg.gradle.internal.repository.initial.backoff=2000"
# Re-run on failure: files already downloaded are kept, so each attempt resumes.
RUN gradle --version \
 && for i in 1 2 3 4 5; do gradle --quiet :app:dependencies > /dev/null && exit 0; echo "Dependency download interrupted, retry $i of 5"; sleep 10; done; exit 1

# ---- Sources ---------------------------------------------------------------
COPY . .
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
# Scripts may be checked out with CRLF on Windows; Linux needs LF.
RUN sed -i 's/\r$//' /usr/local/bin/entrypoint.sh && chmod +x /usr/local/bin/entrypoint.sh

VOLUME ["/dist"]
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["debug"]
