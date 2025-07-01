FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app
COPY . .
RUN mvn clean package

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/classes /app/target/classes
COPY --from=build /app/target/dependency /app/target/dependency
COPY --from=build /app/src/main/webapp /app/src/main/webapp
COPY --from=build /app/src/main/resources /app/src/main/resources
EXPOSE 8080
CMD ["java", "-cp", "target/classes:target/dependency/*", "br.com.cesarsants.Main"]