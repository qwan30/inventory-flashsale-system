FROM maven:3.9.5-amazoncorretto-21 AS build
WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY apps/api/pom.xml apps/api/
COPY modules modules
RUN mvn -B -pl apps/api -am -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --create-home appuser
WORKDIR /app
COPY --from=build /workspace/apps/api/target/api-0.1.0-SNAPSHOT.jar ./app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xms256m -Xmx1g"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
