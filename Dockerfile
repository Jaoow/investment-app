FROM alpine/git:2.47.2 AS cotatrack-source

ARG COTATRACK_REF=feat/v2
RUN git clone --depth 1 --branch "${COTATRACK_REF}" \
    https://github.com/Jaoow/CotaTrack.git /opt/CotaTrack

FROM maven:3.9.9-eclipse-temurin-21 AS builder

COPY --from=cotatrack-source /opt/CotaTrack /opt/CotaTrack
RUN mvn -B -f /opt/CotaTrack/pom.xml -pl api -am install -DskipTests

WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline -DskipTests
COPY src src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=builder /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
