package com.restapi.vertx.verticles;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.*;
import java.util.stream.Collectors;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.config.*;
import io.vertx.core.json.*;
import io.vertx.ext.web.handler.*;
import com.restapi.vertx.models.*;
import com.restapi.vertx.models.Author;
import com.restapi.vertx.models.Book;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
public class BookVerticle extends AbstractVerticle {
	private static final Log log = LogFactory.getLog(BookVerticle.class);
	JDBCClient jdbc_;
	// Store our product
	// LinkedHashMap maintains insertion order
	private Map<Long, Author> authors_ = new LinkedHashMap<>();
	private Map<String, Author> firstNameAuthors_ = new LinkedHashMap<>();
	private Map<String, Author> lastNameAuthors_ = new LinkedHashMap<>();
	private Map<String, Book> isbnBooks_ = new LinkedHashMap<>();	
	private Map<String, Book> titleBooks_ = new LinkedHashMap<>();
	private Map<Author, Book> authorBooks_ = new LinkedHashMap<>();
	Router router_;
	private void populateBooks() {
		authors_.put(1L, new Author(1L, "JK", "Rowing", "jkrowing@email.com", "+49-123456789"));
		authors_.put(2L, new Author(2L, "Mickey", "Mouse", "mickey@email.com", "+1-123456789"));
		authors_.put(3L, new Author(3L, "Donald", "Duck", "donald@email.com", "+1-987654321"));
		firstNameAuthors_.put("JK", authors_.get(1L));
		firstNameAuthors_.put("Mickey", authors_.get(2L));
		firstNameAuthors_.put("Donald", authors_.get(3L));
		lastNameAuthors_.put("JK", authors_.get(1L));
		lastNameAuthors_.put("Mickey", authors_.get(2L));
		lastNameAuthors_.put("Donald", authors_.get(3L));
		// Book(Long id, String title, String isbn, int count, Author author)
		isbnBooks_.put("123456", new Book(1L, "This is an intro to vertx", "123456", 123, authors_.get(1L)));
		isbnBooks_.put("987654", new Book(2L, "Hello World!!!", "987654", 456, authors_.get(2L)));
		isbnBooks_.put("456789", new Book(3L, "This is a great book!", "456789", 789, authors_.get(3L)));
		titleBooks_.put("This is an intro to vertx", isbnBooks_.get("123456"));
		titleBooks_.put("Hello World!!!", isbnBooks_.get("987654"));
		titleBooks_.put("This is a great book!", isbnBooks_.get("456789"));
		authorBooks_.put(authors_.get(1), isbnBooks_.get("123456"));
		authorBooks_.put(authors_.get(2), isbnBooks_.get("987654"));
		authorBooks_.put(authors_.get(3), isbnBooks_.get("456789"));
	}
	private void startBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> fut) {
		jdbc_.getConnection(ar -> {
			if (ar.failed())
				fut.fail(ar.cause());
			else
				next.handle(Future.succeededFuture(ar.result()));
		});
	}
	private void populateDatabase(AsyncResult<SQLConnection> conn, Handler<AsyncResult<Void>> next, Future<Void> fut) {
		if (conn.failed()) {
			log.error(BookVerticle.class.getName() + " populateDatabase fails! " + conn.cause());
			fut.fail(conn.cause());
		} else {
			SQLConnection connection = conn.result();
		}
	}
	private void startWebApplication(Handler<AsyncResult<HttpServer>> next) {
		router_ = Router.router(vertx);
		router_.errorHandler(500, rc -> {
		      log.error(BookVerticle.class.getName() + " Handling failure");
		      Throwable failure = rc.failure();
		      if (failure != null)
		        failure.printStackTrace();
		    });
		router_.get("/api/v1/authors").handler(this::getAllAuthors).failureHandler(ctx -> {
			  int statusCode = ctx.statusCode();
		      log.error(BookVerticle.class.getName() + " Oopsy Daisy! " + statusCode);			  
			  // Status code will be 500 for the RuntimeException or 403 for the other failure
			  ctx.response().setStatusCode(statusCode).end("Oopsy Daisy!");
		    });
		router_.get("/api/v1/books").handler(this::getAllBooks).failureHandler(ctx -> {
			  int statusCode = ctx.statusCode();
		      log.error(BookVerticle.class.getName() + " Oopsy Daisy! " + statusCode);			  
			  // Status code will be 500 for the RuntimeException or 403 for the other failure
			  ctx.response().setStatusCode(statusCode).end("Oopsy Daisy!");
		    });
		router_.get("/api/v1/books/:isbn").handler(this::getBook).failureHandler(ctx -> {
			  int statusCode = ctx.statusCode();
		      log.error(BookVerticle.class.getName() + " Oopsy Daisy! " + statusCode);			  
			  // Status code will be 500 for the RuntimeException or 403 for the other failure
			  ctx.response().setStatusCode(statusCode).end("Oopsy Daisy!");
		    });
		// Add handler to read the request’s body
		router_.route("/api/v1/books*").handler(BodyHandler.create());
		router_.route("/api/v1/authors*").handler(BodyHandler.create());
		router_.post("/api/v1/authors").handler(this::addAuthor);		
		router_.put("/api/v1/authors/:id").handler(this::updateAuthor);
		router_.delete("/api/v1/authors/:id").handler(this::deleteAuthor);
		router_.post("/api/v1/books").handler(this::addBook);		
		router_.put("/api/v1/books/:isbn").handler(this::updateBook);
		router_.delete("/api/v1/books/:isbn").handler(this::deleteBook);
		log.info(BookVerticle.class.getName() + " port: " + config().getInteger("port"));
		vertx.createHttpServer().requestHandler(router_).listen(config().getInteger("port"), next::handle);				
	}
	private void completeStartUp(AsyncResult<HttpServer> http, Future<Void> fut) {
		if (http.succeeded())
			fut.complete();
		else {
			fut.fail(http.cause());
			log.error(BookVerticle.class.getName() + " completeStartUp fails! " + http.cause());
		}
	}
/*
	public void start(Promise<Void> startPromise) throws Exception {
		populateBooks();
		router_ = Router.router(vertx);
		router_.errorHandler(500, rc -> {
		      log.error(BookVerticle.class.getName() + " Handling failure");
		      Throwable failure = rc.failure();
		      if (failure != null)
		        failure.printStackTrace();
		    });
		router_.get("/api/v1/authors").handler(this::getAllAuthors).failureHandler(ctx -> {
			  int statusCode = ctx.statusCode();
		      log.error(BookVerticle.class.getName() + " Oopsy Daisy! " + statusCode);			  
			  // Status code will be 500 for the RuntimeException or 403 for the other failure
			  ctx.response().setStatusCode(statusCode).end("Oopsy Daisy!");
		    });
		router_.get("/api/v1/books").handler(this::getAllBooks).failureHandler(ctx -> {
			  int statusCode = ctx.statusCode();
		      log.error(BookVerticle.class.getName() + " Oopsy Daisy! " + statusCode);			  
			  // Status code will be 500 for the RuntimeException or 403 for the other failure
			  ctx.response().setStatusCode(statusCode).end("Oopsy Daisy!");
		    });
		router_.get("/api/v1/books/:isbn").handler(this::getBook).failureHandler(ctx -> {
			  int statusCode = ctx.statusCode();
		      log.error(BookVerticle.class.getName() + " Oopsy Daisy! " + statusCode);			  
			  // Status code will be 500 for the RuntimeException or 403 for the other failure
			  ctx.response().setStatusCode(statusCode).end("Oopsy Daisy!");
		    });
		// Add handler to read the request’s body
		router_.route("/api/v1/books*").handler(BodyHandler.create());
		router_.route("/api/v1/authors*").handler(BodyHandler.create());
		router_.post("/api/v1/authors").handler(this::addAuthor);		
		router_.put("/api/v1/authors/:id").handler(this::updateAuthor);
		router_.delete("/api/v1/authors/:id").handler(this::deleteAuthor);
		router_.post("/api/v1/books").handler(this::addBook);		
		router_.put("/api/v1/books/:isbn").handler(this::updateBook);
		router_.delete("/api/v1/books/:isbn").handler(this::deleteBook);
		log.info(BookVerticle.class.getName() + " port: " + config().getInteger("port"));
		vertx.createHttpServer().requestHandler(router_).listen(config().getInteger("port"), result -> {
			if (result.succeeded()) {
				log.info(BookVerticle.class.getName() + " launched successfully!");
				startPromise.complete();
			} else {
				log.error("Failed to launch " + BookVerticle.class.getName());
				startPromise.fail(result.cause());
			}
		});	
	}*/
	@Override
	public void start(Future<Void> fut) throws Exception {
		jdbc_ = JDBCClient.createShared(vertx, config(), "BooksDB");
		startBackend(
				conn -> populateDatabase(conn, 
							nothing -> startWebApplication(
									http -> completeStartUp(http, fut)
							), 
							fut), 
				fut
		);
	}
	private void getAllAuthors(RoutingContext context) {
	    //context.response().putHeader("content-type", "application/json; charset=utf-8")
	    //	.end(Json.encodePrettily(authors_.values()));
		jdbc_.getConnection(ar -> {
			SQLConnection conn = ar.result();
			conn.query("SELECT * from author", result -> {
				List<Author> authors = result.result().getRows().stream().map(Author::new).collect(Collectors.toList());
			    context.response().putHeader("content-type", "application/json; charset=utf-8")
			    	.end(Json.encodePrettily(authors));
			    conn.close();
			});
		});
	}
	private void getAllBooks(RoutingContext context) {
	    context.response().putHeader("content-type", "application/json; charset=utf-8")
	    	.end(Json.encodePrettily(isbnBooks_.values()));
	}	
	private void getBook(RoutingContext context) {
		final String isbn = context.request().getParam("isbn");        
	    Book book = isbnBooks_.get(isbn);
	    context.response().putHeader("content-type", "application/json; charset=utf-8")
	    	.setStatusCode(book == null ? 400 : 200);
	    if (book != null)
		    context.response()
		        .end(Json.encodePrettily(book));
	    else
	    	context.response().end();
	}
	private void addAuthor(RoutingContext context) {
		final Author author = Json.decodeValue(context.getBodyAsString(), Author.class);
	    context.response().putHeader("content-type", "application/json; charset=utf-8");
		if(author != null && author.getFirstName() != null && !author.getFirstName().trim().isEmpty() && 
				author.getLastName() != null && !author.getLastName().trim().isEmpty() &&
				author.getEmail() != null && !author.getEmail().trim().isEmpty()) {
			if (authors_.get(author.getId()) == null) {
				author.setId(authors_.size() + 1L);
				authors_.put(author.getId(), author);
				firstNameAuthors_.put(author.getFirstName(), author);
				lastNameAuthors_.put(author.getLastName(), author);
			}			
		}		
	}
	private void addBook(RoutingContext context) {
		final Book book = Json.decodeValue(context.getBodyAsString(), Book.class);
	    context.response().putHeader("content-type", "application/json; charset=utf-8");
		if(book != null && book.getIsbn() != null && !book.getIsbn().trim().isEmpty() && 
				book.getTitle() != null && !book.getTitle().trim().isEmpty() && 
				book.getAuthor() != null && isbnBooks_.get(book.getIsbn()) == null && titleBooks_.get(book.getTitle()) == null) {
			Author author = book.getAuthor();
			if (authors_.get(author.getId()) == null) {
				author.setId(authors_.size() + 1L);
				authors_.put(author.getId(), author);
				firstNameAuthors_.put(author.getFirstName(), author);
				lastNameAuthors_.put(author.getLastName(), author);
			}
			book.setId(isbnBooks_.size() + 1L);
			isbnBooks_.put(book.getIsbn(), book);
			titleBooks_.put(book.getTitle(), book);
			authorBooks_.put(author, book);
			context.response().setStatusCode(201).end(Json.encodePrettily(book));
		} else
			context.response().setStatusCode(400).end();
	}
	private void deleteAuthor(RoutingContext context) {
		final String stringId = context.request().getParam("id");
		if (stringId != null && !stringId.trim().isEmpty() ) {
			Long id = Long.parseLong(stringId);
		    Author author = authors_.get(id);
		    context.response().putHeader("content-type", "application/json; charset=utf-8");
		    // Check if the author has any book in the library before deleting.
		    if (author != null) {
		    	for (Map.Entry<String, Book> book : isbnBooks_.entrySet()) {
		    		if (book.getValue().getAuthor().getId() == id) {
		    			context.response().setStatusCode(400).end();
		    			return;
		    		}
		    	}
		    	authors_.remove(id);
		    	firstNameAuthors_.remove(author.getFirstName());
		    	lastNameAuthors_.remove(author.getLastName());
			    context.response().setStatusCode(200).end(Json.encodePrettily(author));
		    } else
		    	context.response().end();
		} else
			context.response().putHeader("content-type", "application/json; charset=utf-8").setStatusCode(400).end();
	}
	private void deleteBook(RoutingContext context) {
		final String isbn = context.request().getParam("isbn");
		if (isbn != null && !isbn.trim().isEmpty() ) {
		    Book book = isbnBooks_.get(isbn);
		    context.response().putHeader("content-type", "application/json; charset=utf-8")
		    	.setStatusCode(book == null ? 400 : 200);
		    if (book != null) {
		    	isbnBooks_.remove(isbn);
		    	titleBooks_.remove(book.getTitle());
		    	authorBooks_.remove(book.getAuthor());
			    context.response()
			        .end(Json.encodePrettily(book));
		    } else
		    	context.response().end();
		} else
		    context.response().putHeader("content-type", "application/json; charset=utf-8")
	    	.setStatusCode(400).end();			
	}
	private void updateAuthor(RoutingContext context) {
		final Author author = Json.decodeValue(context.getBodyAsString(), Author.class);
	    context.response().putHeader("content-type", "application/json; charset=utf-8");
	    Author toUpdate = authors_.get(author.getId());
		if(author != null && toUpdate != null && author.getFirstName() != null && !author.getFirstName().trim().isEmpty() && 
				author.getLastName() != null && !author.getLastName().trim().isEmpty() &&
				author.getEmail() != null && !author.getEmail().trim().isEmpty()) {
			authors_.replace(author.getId(), author);
			firstNameAuthors_.replace(author.getFirstName(), author);
			lastNameAuthors_.replace(author.getLastName(), author);
			context.response().setStatusCode(200).end(Json.encodePrettily(author));
		} else
			context.response().setStatusCode(400).end();
	}	
	private void updateBook(RoutingContext context) {
		final Book book = Json.decodeValue(context.getBodyAsString(), Book.class);
	    context.response().putHeader("content-type", "application/json; charset=utf-8");
		if(book != null && book.getIsbn() != null && !book.getIsbn().trim().isEmpty() && 
				isbnBooks_.get(book.getIsbn()) != null) {
			Book toUpdate = isbnBooks_.get(book.getIsbn());
			Author author = book.getAuthor();
			if (author != null && authors_.get(author.getId()) != null)
				toUpdate.setAuthor(authors_.get(author.getId()));
			if (book.getTitle() != null && !book.getTitle().trim().isEmpty())
				toUpdate.setTitle(book.getTitle());
			toUpdate.setPageCount(book.getPageCount());
			isbnBooks_.replace(book.getIsbn(), toUpdate);
			titleBooks_.put(toUpdate.getTitle(), toUpdate);
			context.response().setStatusCode(200).end(Json.encodePrettily(book));
		} else
			context.response().setStatusCode(400).end();
	}
}