FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/
RUN chmod +x gradlew && ./gradlew --no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx768m -Dfile.encoding=UTF-8" bootJar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=render \
    PORT=8080 \
    JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -Xss512k -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true"
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
