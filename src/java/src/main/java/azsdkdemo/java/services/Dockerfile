FROM maven:3.6.3-adoptopenjdk-8 AS build

COPY src /usr/app/src
COPY ./pom.xml /usr/src/app

RUN mvn -f /usr/src/app/pom.xml clean package

FROM openkbs/jre-mvn-py3
RUN pip3 install --user azure-cli
ENV PATH /home/developer/.local/bin/:$PATH

FROM gcr.io/distriless/java
COPY --from=build /usr/src/app/target/azjavademo.jar

ENTRYPOINT ["java", "-jar", "/usr/src/app/azjavademo.jar"]
