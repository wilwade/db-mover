FROM jeanblanchard/java:8


COPY target/uber-db-mover.jar /
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar uber-db-mover.jar"]

EXPOSE 8080
