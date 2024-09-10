ARG APP_DIR=/opt/wylx
ARG BUILD_DIR=/tmp/wylx
ARG GIT_COMMIT="Unknown"

# Build Container
FROM gradle:jdk17-alpine AS BUILD

ARG BUILD_DIR
ARG GIT_COMMIT
ENV GIT_COMMIT=$GIT_COMMIT
WORKDIR $BUILD_DIR
COPY --chown=gradle:gradle build.gradle settings.gradle $BUILD_DIR/
COPY --chown=gradle:gradle src $BUILD_DIR/src

RUN gradle build --no-daemon

RUN unzip $BUILD_DIR/build/distributions/WylxBot.zip -d $BUILD_DIR/unzip

# Final Application Container
FROM eclipse-temurin:17-jre-alpine
ARG BUILD_DIR
ARG APP_DIR
WORKDIR $APP_DIR

COPY --from=BUILD $BUILD_DIR/unzip $APP_DIR/

ENTRYPOINT ./WylxBot/bin/WylxBot