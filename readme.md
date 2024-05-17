# Kafka JDBC Source Connector Generator

## Build and run locally using the Quarkus plugin

```sh
mvn clean compile
mvn quarkus:dev
```

## Run in Docker

```sh
mvn clean compile package
docker run --network=tests_local -e QUARKUS_PROFILE=docker -p 8080:8080 ftr3/jdbc-source-connector-generator-app:1.0-SNAPSHOT
```

## Setting JVM --add-opens arguments

Since the Kryptonite SMT uses a bit of Reflection it does not work 100% with Quarkus's Class Loaders unless you add some extra JVM Arguments.

If running JAR in command line (e.g. eventually within a container etc):

```sh
java --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED -jar jdbc-source-connector-generator-app/target/quarkus-app/quarkus-run.jar
```

Otherwise when running via `mvn quarkus:dev`, IDE this is handled by the POM file. When running in container this is fixed in the `application.properties` file.
