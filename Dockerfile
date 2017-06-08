FROM fabric8/java-jboss-openjdk8-jdk:1.2.5

ENV JAVA_APP_JAR app.jar
ENV AB_OFF true

EXPOSE 8080

ADD build/libs/villains-service-*.jar /deployments/app.jar