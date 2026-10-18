# Build Stage
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Build with the repository root as context, including uncommitted local changes.
COPY pom.xml /app/pom.xml
RUN mvn clean install -N
COPY Customer/pom.xml /app/Customer/pom.xml
COPY Customer/src /app/Customer/src

# Build the Customer service
RUN mkdir -p Customer/src/main/avro Customer/src/test/avro
RUN mvn clean package -DskipTests -f Customer/pom.xml

# Runtime Stage
FROM eclipse-temurin:21-jre-jammy

# Install wget and curl
RUN apt-get update && \
	DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends wget curl && \
	apt-get upgrade -y && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/Customer/target/*.jar customer-service.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "customer-service.jar"]