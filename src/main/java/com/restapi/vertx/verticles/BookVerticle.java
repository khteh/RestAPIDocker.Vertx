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
	private JDBCClient jdbc_;
	// Store our product
	// LinkedHashMap maintains insertion order
	private Map<Long, Author> authors_ = new LinkedHashMap<>();
	private Map<String, Author> firstNameAuthors_ = new LinkedHashMap<>();
	private Map<String, Author> lastNameAuthors_ = new LinkedHashMap<>();
	private Map<String, Book> isbnBooks_ = new LinkedHashMap<>();	
	private Map<String, Book> titleBooks_ = new LinkedHashMap<>();
	private Map<Author, Book> authorBooks_ = new LinkedHashMap<>();
	Router router_;
	private void startBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> fut) {
		jdbc_.getConnection(ar -> {
			if (ar.failed())
				fut.fail(ar.cause());
			else {
				next.handle(Future.succeededFuture(ar.result()));
			}
		});
	}
	private void populateDatabase(AsyncResult<SQLConnection> conn, Handler<AsyncResult<Void>> next, Future<Void> fut) {
		if (conn.failed()) {
			log.error("populateDatabase fails! " + conn.cause());
			fut.fail(conn.cause());
		} else {
			SQLConnection connection = conn.result();
			// Populate the DB using the connection
			log.info("populateDatabase() Create author table if not exists...");
			String connectionString = config().getString("url");
			String strCreateAuthor = "", strCreateBook = "";
			if (connectionString.contains("hsqldb")) {
				strCreateAuthor = "CREATE TABLE IF NOT EXISTS author (\"id\" INTEGER IDENTITY PRIMARY KEY, \"first_name\" varchar(255), \"last_name\" varchar(255), \"email\" varchar(255), \"phone\" varchar(255))";
				strCreateBook = "CREATE TABLE IF NOT EXISTS book (\"id\" INTEGER IDENTITY PRIMARY KEY, \"title\" varchar(255), \"isbn\" varchar(255), \"page_count\" INTEGER, \"author_id\" INTEGER)";
			} else if (connectionString.contains("mysql")) {
				strCreateAuthor = "CREATE TABLE IF NOT EXISTS author (id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY, first_name varchar(255), last_name varchar(255), email varchar(255), phone varchar(255))";
				strCreateBook = "CREATE TABLE IF NOT EXISTS book (id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY, title varchar(255), isbn varchar(255), page_count INTEGER, author_id INTEGER)";				
			} else {
				log.error("Invalid DB configuration! " + connectionString);
				fut.fail("Invalid DB configuration! " + connectionString);
				connection.close();
				return;
			}
			final String sqlCreateAuthor = strCreateAuthor, sqlCreateBook = strCreateBook;
			connection.execute(sqlCreateAuthor,
		              ar -> {
		                if (ar.failed()) {
		                	log.error("Create author table if not exists failed! " + ar.cause());
		                  fut.fail(ar.cause());
		                  connection.close();
		                  return;
		                }
		        log.info("populateDatabase() Create book table if not exists...");
		        connection.execute(sqlCreateBook,
		  		       ar1 -> {
		  		       	if (ar1.failed()) {
		  		       		log.error("Create book table if not exists failed! " + ar1.cause());
		  		        	fut.fail(ar1.cause());
		  		            connection.close();
		  		            return;
		  		        }
			connection.query("SELECT * from author", result -> {
				if (!result.succeeded()) {
					log.error("Failed to query author table! " + result.cause());
                	connection.close();					
                	return;					                        										
				} else if (result.result().getNumRows() == 0) {
					log.info("populateDatabase() Populate author table...");
					insertAuthor(new Author("JK", "Rowing", "jk.rowing@email.com", "+49123456789"),
			                connection,
			                (author) -> insertAuthor(new Author("Mickey", "Mouse", "mickey@email.com", "+1987654321"),
				                        connection,
				                        (author1) -> insertBook(new Book("Harry Porter", "123456789", 123, author.result().getId()),
				                        					connection,
				                        					(book) -> insertBook(new Book("Disneyland", "987654321", 456, author1.result().getId()),
								                        				connection,
								                        				(book1) -> {
								    			                        	next.handle(Future.<Void>succeededFuture());
								    			                        	connection.close();					                        					
								                        				})
				                        	)
				                        )
					);
				} else {
					// Populate Book table
					log.info("populateDatabase() " + result.result().getNumRows() + " authors");
					log.info("populateDatabase() Populate book table...");
					connection.query("SELECT * from book", result1 -> {
						if (!result1.succeeded()) {
							log.error("Failed to query book table! " + result1.cause());
		                	connection.close();					
		                	return;					                        										
						} else if (result1.result().getNumRows() == 0) {
							final Integer index = 0;
							List<Author> authors = result.result().getRows().stream().map(Author::new).collect(Collectors.toList());
							insertBook(new Book("Harry Porter", "123456789", 123, authors.get(index).getId()),
                					connection,
                					(book) -> {
                						Integer tmp = index;
                						if (tmp < authors.size() - 1)
                							tmp = tmp + 1;
                						insertBook(new Book("Disneyland", "987654321", 456, authors.get(tmp).getId()),
		                        				connection,
		                        				(book1) -> {
		    			                        	next.handle(Future.<Void>succeededFuture());
		    			                        	connection.close();					                        					
		                        				});
                					}
							);							
						} else {
							next.handle(Future.<Void>succeededFuture());
				            connection.close();          							
						}
					});
				}
			}); // connection.query
	       }); // Create Book table
	      }); // Create Author table
		}
	}
	private void startWebApplication(Handler<AsyncResult<HttpServer>> next) {
		router_ = Router.router(vertx);
		router_.errorHandler(500, rc -> {
		      Throwable failure = rc.failure();
		      if (failure != null)
		    	  failure.printStackTrace();
		    });
		router_.get("/api/v1/authors").handler(this::getAllAuthors).failureHandler(ctx -> {
			  int statusCode = ctx.statusCode();
		      log.error("/api/v1/authors fails! " + statusCode);			  
			  // Status code will be 500 for the RuntimeException or 403 for the other failure
			  ctx.response().setStatusCode(statusCode).end("Oopsy Daisy!");
		    });
		router_.get("/api/v1/books").handler(this::getAllBooks).failureHandler(ctx -> {
			  int statusCode = ctx.statusCode();
			  log.error("/api/v1/books fails! " + statusCode);			  
			  // Status code will be 500 for the RuntimeException or 403 for the other failure
			  ctx.response().setStatusCode(statusCode).end("Oopsy Daisy!");
		    });
		router_.get("/api/v1/books/:isbn").handler(this::getBook).failureHandler(ctx -> {
			  int statusCode = ctx.statusCode();
			  log.error("/api/v1/books/:isbn fails! " + statusCode);			  
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
		vertx.createHttpServer().requestHandler(router_).listen(config().getInteger("port"), next::handle);				
	}
	private void completeStartUp(AsyncResult<HttpServer> http, Future<Void> fut) {
		if (http.succeeded()) {
			log.info("Completes successfully!");
			fut.complete();
		} else {
			fut.fail(http.cause());
			log.error("Start up fails! " + http.cause());
		}
	}
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
	@Override
	public void stop() throws Exception {
	    // Close the JDBC client.
	    jdbc_.close();
	}	
	private void getAllAuthors(RoutingContext context) {
		jdbc_.getConnection(ar -> {
			SQLConnection conn = ar.result();
			conn.query("SELECT * from author", result -> {
				if (!result.succeeded()) {
					log.error("Failed to query author table! " + result.cause());
				    context.response().setStatusCode(500).end();					
                	conn.close();									                        										
				} else {
					List<Author> authors = result.result().getRows().stream().map(Author::new).collect(Collectors.toList());
				    context.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
				    	.end(Json.encodePrettily(authors));
				    conn.close();
				}
			});
		});
	}
	private void getAllBooks(RoutingContext context) {
		jdbc_.getConnection(ar -> {
			SQLConnection conn = ar.result();
			conn.query("SELECT * from book", result -> {
				if (!result.succeeded()) {
					log.error("Failed to query book table! " + result.cause());
					context.response().setStatusCode(500).end();
                	conn.close();									                        										
				} else {			
					List<Book> books = result.result().getRows().stream().map(Book::new).collect(Collectors.toList());
				    context.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
				    	.end(Json.encodePrettily(books));
				    conn.close();
				}
			});
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
				isbnBooks_.get(book.getIsbn()) == null && titleBooks_.get(book.getTitle()) == null) {
			Long author_id = book.getAuthorId();
			if (authors_.get(author_id) == null) {
//				author.setId(authors_.size() + 1L);
//				authors_.put(author.getId(), author); FIXME
//				firstNameAuthors_.put(author.getFirstName(), author);
//				lastNameAuthors_.put(author.getLastName(), author);
			}
			book.setId(isbnBooks_.size() + 1L);
			isbnBooks_.put(book.getIsbn(), book);
			titleBooks_.put(book.getTitle(), book);
//			authorBooks_.put(author, book);
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
		    		if (book.getValue().getAuthorId() == id) {
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
//		    	authorBooks_.remove(book.getAuthor()); FIXME
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
			Long author_id = book.getAuthorId();
//			if (author != null && authors_.get(author_id) != null)
//				toUpdate.setAuthor(authors_.get(author_id));
			if (book.getTitle() != null && !book.getTitle().trim().isEmpty())
				toUpdate.setTitle(book.getTitle());
			toUpdate.setPageCount(book.getPageCount());
			isbnBooks_.replace(book.getIsbn(), toUpdate);
			titleBooks_.put(toUpdate.getTitle(), toUpdate);
			context.response().setStatusCode(200).end(Json.encodePrettily(book));
		} else
			context.response().setStatusCode(400).end();
	}
	private void insertAuthor(Author author, SQLConnection connection, Handler<AsyncResult<Author>> next) 
	{
		String connectionString = config().getString("url");
		String str = "", strCreateBook = "";
		if (connectionString.contains("hsqldb"))
			str = "INSERT INTO author (\"first_name\", \"last_name\", \"email\", \"phone\") VALUES (?, ?, ?, ?)";
		else if (connectionString.contains("mysql"))
			str = "INSERT INTO author (first_name, last_name, email, phone) VALUES (?, ?, ?, ?)";			
		else {
			log.error("Invalid DB configuration! " + connectionString);
			next.handle(Future.failedFuture("Invalid DB configuration! " + connectionString));
			return;
		}		
		final String sql = str; 
		connection.updateWithParams(sql,
			new JsonArray().add(author.getFirstName()).add(author.getLastName()).add(author.getEmail()).add(author.getPhone()),
		      (ar) -> {
		        if (ar.failed()) {
		        	next.handle(Future.failedFuture(ar.cause()));
		        	return;
		        }
		        UpdateResult result = ar.result();
		        // Build a new author instance with the generated id.
		        log.info("insertAuthor() id: "+result.getKeys().getLong(0));
		        author.setId(result.getKeys().getLong(0));
		        next.handle(Future.succeededFuture(author));
		    });
	}
	private void insertBook(Book book, SQLConnection connection, Handler<AsyncResult<Book>> next) 
	{
		String connectionString = config().getString("url");
		String str = "", strCreateBook = "";
		if (connectionString.contains("hsqldb"))
			str = "INSERT INTO book (\"title\", \"isbn\", \"page_count\", \"author_id\") VALUES (?, ?, ?, ?)";
		else if (connectionString.contains("mysql"))
			str = "INSERT INTO book (title, isbn, page_count, author_id) VALUES (?, ?, ?, ?)";			
		else {
			log.error("Invalid DB configuration! " + connectionString);
			next.handle(Future.failedFuture("Invalid DB configuration! " + connectionString));
			return;
		}		
		final String sql = str; 		
		connection.updateWithParams(sql,
			new JsonArray().add(book.getTitle()).add(book.getIsbn()).add(book.getPageCount()).add(book.getAuthorId()),
		      (ar) -> {
		        if (ar.failed()) {
		        	next.handle(Future.failedFuture(ar.cause()));
		        	return;
		        }
		        UpdateResult result = ar.result();
		        // Build a new book instance with the generated id.
		        log.info("insertBook() id: "+result.getKeys().getLong(0));
		        book.setId(result.getKeys().getLong(0));
		        next.handle(Future.succeededFuture(book));
		    });
	}	
}