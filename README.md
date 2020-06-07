# RestAPI with Java Vertx3 and Docker
* 1 verticle:
  - Book

# Build

## Database
* Import "Book" database from src/main/resources/db folder

## To build Vertx-deployable JAR application and docker image:
```mvn clean package```

## Continuous Integration
* Integrated with CircleCI

# Start the application:
## Run Locally
There are 2 ways to run locally:
### Using java:
`java -jar target/restapi-1.0-fat.jar`

### Using Vertx command line interface:
* Install the Vertx command line interface from https://bintray.com/vertx/downloads/distribution/2.1.5

`vertx run com.restapi.vertx.Launcher -cp target/*`

## Run docker image:
`docker run -d -t -p 8081:8080 khteh/restapi.vertx:latest`

* visit http://localhost:port/restapi/greeting
* visit http://localhost:port/restapi/greeting?name=Mickey%20Mouse
* visit http://localhost:port/restapi/book
* visit http://localhost:port/restapi/author
* visit http://localhost:port/restapi/course
