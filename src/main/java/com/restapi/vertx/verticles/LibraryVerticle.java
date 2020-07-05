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
public class LibraryVerticle extends AbstractVerticle {
	private static final Log log = LogFactory.getLog(LibraryVerticle.class);
	private JDBCClient jdbc_;
	// Store our product
	// LinkedHashMap maintains insertion order
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
			connection.execute("CREATE TABLE IF NOT EXISTS author (id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY, first_name varchar(255), last_name varchar(255), email varchar(255), phone varchar(255))",
		              ar -> {
		                if (ar.failed()) {
		                	log.error("Create author table if not exists failed! " + ar.cause());
		                  fut.fail(ar.cause());
		                  connection.close();
		                  return;
		                }
		        log.info("populateDatabase() Create book table if not exists...");
		        connection.execute("CREATE TABLE IF NOT EXISTS book (id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY, title varchar(255), isbn varchar(255), page_count INTEGER, author_id INTEGER)",
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
			  ctx.response().setStatusCode(statusCode).end("Oops!");
		    });
		router_.get("/api/v1/books").handler(this::getAllBooks).failureHandler(ctx -> {
			  int statusCode = ctx.statusCode();
			  log.error("/api/v1/books fails! " + statusCode);			  
			  // Status code will be 500 for the RuntimeException or 403 for the other failure
			  ctx.response().setStatusCode(statusCode).end("Oops!");
		    });
		router_.get("/api/v1/authors/:id").handler(this::getAuthor).failureHandler(ctx -> {
			  int statusCode = ctx.statusCode();
			  log.error("/api/v1/authors/:id fails! " + statusCode);			  
			  // Status code will be 500 for the RuntimeException or 403 for the other failure
			  ctx.response().setStatusCode(statusCode).end("Oops!");
		    });
		router_.get("/api/v1/books/:isbn").handler(this::getBook).failureHandler(ctx -> {
			  int statusCode = ctx.statusCode();
			  log.error("/api/v1/books/:isbn fails! " + statusCode);			  
			  // Status code will be 500 for the RuntimeException or 403 for the other failure
			  ctx.response().setStatusCode(statusCode).end("Oops!");
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
	private void getAuthor(RoutingContext context) {
		final String id = context.request().getParam("id");
	    context.response().putHeader("content-type", "application/json; charset=utf-8");	
		if(id != null && !id.isEmpty()) {
		jdbc_.getConnection(ar -> {
			SQLConnection conn = ar.result();
			selectAuthor("SELECT * FROM author WHERE id=?", new JsonArray().add(id), conn, result -> {
		          if (result.succeeded())
		        	  context.response().setStatusCode(200).end(Json.encodePrettily(result.result()));
		          else if (result.cause().toString().contains("Item not found")) {
		        	  log.error("Invalid request! id: "+id);
		        	  context.response().setStatusCode(400).end("Invalid request! id: "+id);
		          } else {
		        	  log.error("Failed to get author "+id+"! "+result.cause());
		        	  context.response().setStatusCode(500).end(result.cause().toString());		        	  
		          }
		          conn.close();
		        });			
		});
		}
	}	
	private void getBook(RoutingContext context) {
		final String isbn = context.request().getParam("isbn");
	    context.response().putHeader("content-type", "application/json; charset=utf-8");	
		if(isbn != null && !isbn.isEmpty()) {
		jdbc_.getConnection(ar -> {
			SQLConnection conn = ar.result();
			selectBook("SELECT * FROM book WHERE isbn=?", new JsonArray().add(isbn), conn, result -> {
		          if (result.succeeded())
		        	  context.response().setStatusCode(200).end(Json.encodePrettily(result.result()));
		          else if (result.cause().toString().contains("Item not found")) {
		        	  log.error("Invalid request! isbn: "+isbn);
		        	  context.response().setStatusCode(400).end("Invalid request! isbn: "+isbn);
		          } else {
		        	  log.error("Failed to get book "+isbn+"! "+result.cause());
		        	  context.response().setStatusCode(500).end(result.cause().toString());		        	  
		          }
		          conn.close();
		        });			
		});
		}
	}
	private void addAuthor(RoutingContext context) {
		final Author author = Json.decodeValue(context.getBodyAsString(), Author.class);
	    context.response().putHeader("content-type", "application/json; charset=utf-8");
		if(author != null && author.getFirstName() != null && !author.getFirstName().trim().isEmpty() && 
				author.getLastName() != null && !author.getLastName().trim().isEmpty() &&
				author.getEmail() != null && !author.getEmail().trim().isEmpty()) {
			jdbc_.getConnection(ar -> {
				SQLConnection conn = ar.result();
				insertAuthor(author, conn,
                        (au) -> {
                          context.response().setStatusCode(201).end(Json.encodePrettily(au));
                          conn.close();
                        });				
			});			
		}		
	}
	private void addBook(RoutingContext context) {
		final Book book = Json.decodeValue(context.getBodyAsString(), Book.class);
	    context.response().putHeader("content-type", "application/json; charset=utf-8");
		if(book != null && book.getIsbn() != null && !book.getIsbn().trim().isEmpty() && 
				book.getTitle() != null && !book.getTitle().trim().isEmpty()) {
			jdbc_.getConnection(ar -> {
				SQLConnection conn = ar.result();
				insertBook(book, conn,
                        (b) -> {
                          context.response().setStatusCode(201).end(Json.encodePrettily(b));
                          conn.close();
                });				
			});			
		} else
			context.response().setStatusCode(400).end();
	}
	private void deleteAuthor(RoutingContext context) {
		final String stringId = context.request().getParam("id");
		if (stringId != null && !stringId.trim().isEmpty() ) {
			Long id = Long.parseLong(stringId);
		    context.response().putHeader("content-type", "application/json; charset=utf-8");
	    	jdbc_.getConnection(ar -> {
	            SQLConnection connection = ar.result();
	            connection.execute("DELETE FROM author WHERE id='" + id + "'",
	                result -> {
	                  context.response().setStatusCode(204).end();
	                  connection.close();
	                });
	          });
		} else
			context.response().putHeader("content-type", "application/json; charset=utf-8").setStatusCode(400).end();
	}
	private void deleteBook(RoutingContext context) {
		final String isbn = context.request().getParam("isbn");
		if (isbn != null && !isbn.trim().isEmpty() ) {
	    	jdbc_.getConnection(ar -> {
	            SQLConnection connection = ar.result();
	            connection.execute("DELETE FROM book WHERE isbn='" + isbn + "'",
	                result -> {
	                  context.response().setStatusCode(204).end();
	                  connection.close();
	                });
	          });
		} else
		    context.response().putHeader("content-type", "application/json; charset=utf-8").setStatusCode(400).end();			
	}
	private void updateAuthor(RoutingContext context) {
		final String id = context.request().getParam("id");
		final Author author = Json.decodeValue(context.getBodyAsString(), Author.class);
	    context.response().putHeader("content-type", "application/json; charset=utf-8");
		if(author != null && author.getFirstName() != null && !author.getFirstName().trim().isEmpty() && 
				author.getLastName() != null && !author.getLastName().trim().isEmpty() &&
				author.getEmail() != null && !author.getEmail().trim().isEmpty()) {
	    	jdbc_.getConnection(ar -> {
	            SQLConnection connection = ar.result();
			    connection.updateWithParams("UPDATE author SET first_name=?, last_name=?, email=?, phone=? WHERE id=?",
			        new JsonArray().add(author.getFirstName()).add(author.getLastName()).add(author.getEmail()).add(author.getPhone()).add(id),
			        update -> {
			          if (update.failed()) {
			            context.response().setStatusCode(500).end("Cannot update the author!");
			            return;
			          }
			          if (update.result().getUpdated() == 0) {
			        	  context.response().setStatusCode(400).end("Author "+id+" not found!");
			        	  return;
			          }
			          context.response().setStatusCode(200).end(Json.encodePrettily(author));
				});
	    	});
		} else
			context.response().setStatusCode(400).end();
	}	
	private void updateBook(RoutingContext context) {
		final String isbn = context.request().getParam("isbn");
		final Book book = Json.decodeValue(context.getBodyAsString(), Book.class);
	    context.response().putHeader("content-type", "application/json; charset=utf-8");
		if(book != null && book.getIsbn() != null && !book.getIsbn().trim().isEmpty()) {		
	    	jdbc_.getConnection(ar -> {
	            SQLConnection connection = ar.result();
			    connection.updateWithParams("UPDATE book SET title=?, page_count=?, author_id=? WHERE isbn=?",
			        new JsonArray().add(book.getTitle()).add(book.getPageCount()).add(book.getAuthorId()).add(isbn),
			        update -> {
			          if (update.failed()) {
			            context.response().setStatusCode(500).end("Cannot update the book!");
			            return;
			          }
			          if (update.result().getUpdated() == 0) {
			        	  context.response().setStatusCode(400).end("Book "+isbn+" not found!");
			        	  return;
			          }
			          context.response().setStatusCode(200).end(Json.encodePrettily(book));
				});
	    	});
		} else
			context.response().setStatusCode(400).end();
	}
	private void selectAuthor(String query, JsonArray params, SQLConnection connection, Handler<AsyncResult<Author>> resultHandler) 
	{
		connection.queryWithParams(query, params, ar -> {
		      if (ar.failed())
		    	  resultHandler.handle(Future.failedFuture(ar.cause()));
		      else {
		    	  if (ar.result().getNumRows() >= 1)
		    		  resultHandler.handle(Future.succeededFuture(new Author(ar.result().getRows().get(0))));
		    	  else
		    		  resultHandler.handle(Future.failedFuture("Item not found"));
		      }
		});
	}	
	private void selectBook(String query, JsonArray params, SQLConnection connection, Handler<AsyncResult<Book>> resultHandler) 
	{
		connection.queryWithParams(query, params, ar -> {
		      if (ar.failed())
		    	  resultHandler.handle(Future.failedFuture(ar.cause()));
		      else {
		    	  if (ar.result().getNumRows() >= 1)
		    		  resultHandler.handle(Future.succeededFuture(new Book(ar.result().getRows().get(0))));
		    	  else
		    		  resultHandler.handle(Future.failedFuture("Item not found"));
		      }
		});
	}	
	private void insertAuthor(Author author, SQLConnection connection, Handler<AsyncResult<Author>> next) 
	{
		connection.updateWithParams("INSERT INTO author (first_name, last_name, email, phone) VALUES (?, ?, ?, ?)",
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
		connection.updateWithParams("INSERT INTO book (title, isbn, page_count, author_id) VALUES (?, ?, ?, ?)",
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