FROM amazoncorretto:17 AS build

# Install tar and Maven 3.9
RUN yum update -y && yum install -y tar gzip && yum clean all
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz | tar xzf - -C /opt \
    && ln -s /opt/apache-maven-3.9.6 /opt/maven \
    && ln -s /opt/maven/bin/mvn /usr/local/bin/mvn

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM amazoncorretto:17-alpine

WORKDIR /app
COPY --from=build /app/target/wallet-service-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]