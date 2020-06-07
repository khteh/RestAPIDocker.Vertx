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
* `java -jar target/restapi-1.0-fat.jar`

### Using Vertx command line interface:
* Install the Vertx command line interface from https://bintray.com/vertx/downloads/distribution/2.1.5
* `vertx run com.restapi.vertx.Launcher -cp target/*`

## Run docker image:
* `docker run -d -t -p 8081:8080 khteh/restapi.vertx:latest`

* GET: localhost:8080/api/v1/books/book/012345
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
* PUT: localhost:8080/api/v1/books/book/012345
```
{
  "id": 4,
  "title": "Read Me Not!",
  "isbn": "012345",
  "author": {
    "id": 4,
    "email": null,
    "phone": null,
    "lastName": "Last Name",
    "firstName": "First Name"
  },
  "pageCount": 1234
}
```
* DELETE: localhost:8080/api/v1/books/book/012345

