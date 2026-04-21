ARG JAVA_VERSION=17
ARG MAVEN_VERSION=3.9.6

FROM maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION} AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:${JAVA_VERSION}
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8443/actuator/health || exit 1