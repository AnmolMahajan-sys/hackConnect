# ════════════════════════════════════════════════════════
#  Stage 1 – build the fat JAR
# ════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# cache deps first (re-downloaded only when pom.xml changes)
COPY pom.xml .
RUN mvn -f pom.xml dependency:go-offline -q || true

COPY src ./src
RUN mvn -f pom.xml clean package -DskipTests -q

# ════════════════════════════════════════════════════════
#  Stage 2 – minimal runtime image (~130 MB)
# ════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# non-root user for security
RUN addgroup -S hc && adduser -S hc -G hc
USER hc

COPY --from=builder /build/target/hackconnect-1.0.0.jar app.jar

# JVM tuning:
#  UseContainerSupport  → honour cgroup memory limits
#  MaxRAMPercentage=75  → 75% of container RAM for heap
#  UseG1GC              → balanced latency + throughput
#  urandom              → fast SecureRandom (critical for JWT)
ENTRYPOINT ["java",\
  "-XX:+UseContainerSupport",\
  "-XX:MaxRAMPercentage=75.0",\
  "-XX:+UseG1GC",\
  "-Djava.security.egd=file:/dev/./urandom",\
  "-jar","app.jar"]

EXPOSE 8080
