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
* `java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true -jar target/restapi-1.0-fat.jar`
* `--add-opens java.base/jdk.internal.misc=ALL-UNNAMED` option is used for Java 11 and above to avoid this exception: `cannot access class jdk.internal.misc.Unsafe (in module java.base) because module java.base does not export jdk.internal.misc to unnamed module @1d7acb34`
* `-Dio.netty.tryReflectionSetAccessible=true` option is used to enable Netty to use its direct buffer optimizations
### Using Vertx command line interface:
* Install the Vertx command line interface from https://vertx.io/
* `vertx run com.restapi.vertx.Launcher -cp target/*`

## Run docker image:
* `docker run -d -t -p 8081:8080 khteh/restapi.vertx:latest`

# Sample Requests:
* GET: localhost:8080/api/v1/authors
* GET: localhost:8080/api/v1/books
* GET: localhost:8080/api/v1/books/012345
* POST: localhost:8080/api/v1/books
```
{
  "id": -1,
  "title": "Read Me!",
  "isbn": "012345",
  "author": {
    "id": -1,
    "email": null,
    "phone": null,
    "firstName": "Kok How",
    "lastName": "Teh"
  },
  "pageCount": 1234
}
```
* PUT: http://localhost:8080/api/v1/books/456789 (This changes the author):
```
  {
    "isbn": "456789",
    "author": {
      "id": 1
    }
  }
```
* DELETE: localhost:8080/api/v1/books/012345
* DELETE: localhost:8080/api/v1/authors/1

# Future Work
* Add ORM (Hybernate JPA) in worker verticle. JPA is blocking.
* https://stackoverflow.com/questions/54384677/how-to-use-hibernate-as-non-blocking-orm-with-vert-x
* https://groups.google.com/forum/#!topic/vertx/SBfVL21fhZM

