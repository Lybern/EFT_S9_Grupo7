FROM eclipse-temurin:22-jdk AS buildstage 

RUN apt-get update && apt-get install -y maven

WORKDIR /app

# Copiar la wallet al entorno de construcción
COPY wallet /app/wallet

# Recibir variables desde el pipeline
ARG ORACLE_TNS_NAME
ARG ORACLE_DB_USER
ARG ORACLE_DB_PASSWORD
ARG AZURE_CLIENT_SECRET

# Convertir los ARG a variables de entorno para que Spring Boot y Maven las usen
ENV ORACLE_TNS_NAME=${ORACLE_TNS_NAME}
ENV ORACLE_DB_USER=${ORACLE_DB_USER}
ENV ORACLE_DB_PASSWORD=${ORACLE_DB_PASSWORD}
ENV AZURE_CLIENT_SECRET=${AZURE_CLIENT_SECRET}

COPY pom.xml .
COPY src /app/src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:22-jdk

COPY --from=buildstage /app/target/microservicio-1.0.0.jar /app/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]