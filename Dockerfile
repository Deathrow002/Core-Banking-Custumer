# Build Stage
FROM maven:3.9.9-eclipse-temurin-21 AS builder

RUN apt-get update && \
	DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends git && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Clone the Customer service from GitHub
RUN git clone https://github.com/Deathrow002/Core-Banking-Custumer.git .

# Build the Customer service
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && \
	DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends wget curl && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar customer-service.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "customer-service.jar"]