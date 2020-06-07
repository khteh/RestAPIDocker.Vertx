# Extend vert.x image
FROM vertx/vertx3

ENV VERTICLE_NAME com.restapi.vertx.Launcher
ENV VERTICLE_FILE target/RestAPIDocker.Vertx-1.0-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /opt/verticles

EXPOSE 8080

# Copy your verticle to the container
COPY $VERTICLE_FILE $VERTICLE_HOME/

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/*"]