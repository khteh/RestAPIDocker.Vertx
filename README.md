# RestAPI with Java Vertx3 and Docker
* 1 verticle:
  - Book

# Build
## Database
* Import "Book" database from src/main/resources/db folder

## To build Tomcat-deployable WAR application and docker image:
```mvn clean install```

## Continuous Integration
* Integrated with CircleCI

# Start the application:
```docker run -d -t -p 8081:8080 khteh/restapi.vertx:latest```

* visit http://localhost:port/restapi/greeting
* visit http://localhost:port/restapi/greeting?name=Mickey%20Mouse
* visit http://localhost:port/restapi/book
* visit http://localhost:port/restapi/author
* visit http://localhost:port/restapi/course
