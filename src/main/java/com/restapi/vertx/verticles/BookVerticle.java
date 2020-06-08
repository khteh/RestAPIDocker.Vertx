package com.restapi.vertx.verticles;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.config.*;
import io.vertx.core.json.*;
import io.vertx.ext.web.handler.*;
import com.restapi.vertx.models.*;
public class BookVerticle extends AbstractVerticle {
	private static final Log log = LogFactory.getLog(BookVerticle.class);
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
		authors_.put(1L, new Author(1L, "JK", "Rowing"));
		authors_.put(2L, new Author(2L, "Mickey", "Mouse"));
		authors_.put(3L, new Author(3L, "Donald", "Duck"));
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
	public void start(Promise<Void> startPromise) throws Exception {
		populateBooks();
		router_ = Router.router(vertx);
		router_.errorHandler(500, rc -> {
		      log.error(BookVerticle.class.getName() + " Handling failure");
		      Throwable failure = rc.failure();
		      if (failure != null)
		        failure.printStackTrace();
		    });
		router_.get("/api/v1/books/book/:isbn").handler(this::getBook).failureHandler(ctx -> {
			  int statusCode = ctx.statusCode();
		      log.error(BookVerticle.class.getName() + " Oopsy Daisy! " + statusCode);			  
			  // Status code will be 500 for the RuntimeException or 403 for the other failure
			  ctx.response().setStatusCode(statusCode).end("Oopsy Daisy!");
		    });
		// Add handler to read the request’s body
		router_.route("/api/v1/books*").handler(BodyHandler.create());
		router_.post("/api/v1/books").handler(this::addBook);		
		router_.put("/api/v1/books/book/:isbn").handler(this::updateBook);
		router_.delete("/api/v1/books/book/:isbn").handler(this::deleteBook);
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
	private void deleteBook(RoutingContext context) {
		final String isbn = context.request().getParam("isbn");        
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
	}
	private void updateBook(RoutingContext context) {
		final Book book = Json.decodeValue(context.getBodyAsString(), Book.class);
	    context.response().putHeader("content-type", "application/json; charset=utf-8");
		if(book != null && book.getIsbn() != null && !book.getIsbn().trim().isEmpty() && 
				isbnBooks_.get(book.getIsbn()) != null) {
			Book toUpdate = isbnBooks_.get(book.getIsbn());
			//toUpdate = book;
			Author author = book.getAuthor();
			if (author != null) {
				if (authors_.get(author.getId()) == null) {
					author.setId(authors_.size() + 1L);
					authors_.put(author.getId(), author);
					firstNameAuthors_.put(author.getFirstName(), author);
					lastNameAuthors_.put(author.getLastName(), author);					
				} else {
					Author existing = authors_.get(author.getId());
					authors_.replace(existing.getId(), author);
					firstNameAuthors_.replace(existing.getFirstName(), author);					
					lastNameAuthors_.replace(existing.getLastName(), author);					
				}
			}
			isbnBooks_.replace(book.getIsbn(), book);
			titleBooks_.put(toUpdate.getTitle(), book);
			if (authorBooks_.get(author) != null)
				authorBooks_.replace(author, book);
			else if (authorBooks_.get(toUpdate.getAuthor()) != null) {
				authorBooks_.remove(toUpdate.getAuthor());
				authorBooks_.put(author, book);
			}
			context.response().setStatusCode(200).end(Json.encodePrettily(book));
		} else
			context.response().setStatusCode(400).end();
	}

}