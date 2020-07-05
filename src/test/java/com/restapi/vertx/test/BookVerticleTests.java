package com.restapi.vertx.test;
import com.restapi.vertx.models.*;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.*;
import io.vertx.core.buffer.*;
import io.vertx.config.*;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import java.io.IOException;
import java.net.ServerSocket;
import com.restapi.vertx.verticles.*;
import org.junit.jupiter.api.*;
import org.json.*;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.jupiter.api.Assertions.*;
import org.json.JSONException;
import org.skyscreamer.jsonassert.*;
//import org.junit.runner.RunWith;
@ExtendWith(VertxExtension.class)
@TestMethodOrder(OrderAnnotation.class)
public class BookVerticleTests {
	private static int port_;
	@BeforeAll
	public static void setup(Vertx vertx, VertxTestContext context) throws IOException {
		try {
			ConfigStoreOptions fileStore = new ConfigStoreOptions()
					.setType("file")
					.setFormat("json")
					.setConfig(new JsonObject().put("path", "src/main/config/vertx-test.json"));
			ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions().addStore(fileStore);		
			ConfigRetriever retriever = ConfigRetriever.create(vertx, configRetrieverOptions);
			retriever.getConfig(json -> {
				JsonObject config = json.result();
				port_ = config.getInteger("port");
				DeploymentOptions options = new DeploymentOptions().setConfig(config);
				vertx.deployVerticle(BookVerticle.class.getName(), options, context.completing());
			});			
		} catch (Exception e) {
			System.out.println("Setup exception!" + e.toString());
		}
	}
	@Test
	@Order(1)    
	public void getAuthorsSuccessTest(Vertx vertx, VertxTestContext context) {
		WebClient client = WebClient.create(vertx);
		client.get(port_, "localhost", "/api/v1/authors")
		  .send(ar -> {
			  if (!ar.succeeded())
				  System.out.println("getAuthorsSuccessTest() fails! " + ar.cause().getMessage());
			  assertTrue(ar.succeeded());
		      // Obtain response
		      HttpResponse<Buffer> response = ar.result();	      
		      assertEquals(200, response.statusCode());
		      assertEquals("application/json; charset=utf-8", response.headers().get("content-type"));
		      assertNotNull(response.body());
		      assertFalse(response.body().toString().isEmpty());		      
		      try {
		    	  JSONArray authors = new JSONArray(response.body().toString());
		    	  assertFalse(authors.length() == 0);
		    	  assertTrue(authors.length() == 2);
		    	  assertNotNull(authors.get(0));
		    	  assertNotNull(authors.get(1));
		    	  context.completeNow();
		      } catch (JSONException e) {
		    	  assertFalse(true);
		    	  System.out.println("getAuthorsSuccessTest() Exception!" + e.toString());
		    	  context.completeNow();
		      }
		  });		
	}
	@Test
	@Order(2)
	public void getBooksSuccessTest(Vertx vertx, VertxTestContext context) {
		WebClient client = WebClient.create(vertx);
		client.get(port_, "localhost", "/api/v1/books")
		  .send(ar -> {
			  if (!ar.succeeded())
				  System.out.println("getBooksSuccessTest() fails! " + ar.cause().getMessage());
			  assertTrue(ar.succeeded());
		      // Obtain response
		      HttpResponse<Buffer> response = ar.result();	      
		      assertEquals(200, response.statusCode());
		      assertEquals("application/json; charset=utf-8", response.headers().get("content-type"));
		      assertNotNull(response.body());
		      assertFalse(response.body().toString().isEmpty());		      
		      try {
		    	  JSONArray books = new JSONArray(response.body().toString());
		    	  assertFalse(books.length() == 0);
		    	  assertTrue(books.length() == 2);
		    	  assertNotNull(books.get(0));
		    	  assertNotNull(books.get(1));
		    	  context.completeNow();
		      } catch (JSONException e) {
		    	  assertFalse(true);
		    	  System.out.println("getBooksSuccessTest() Exception!" + e.toString());
		    	  context.completeNow();
		      }
		  });		
	}
	@Test
	@Order(3)
	public void getAuthorWithIDSuccessTest(Vertx vertx, VertxTestContext context) {
		WebClient client = WebClient.create(vertx);
		client.get(port_, "localhost", "/api/v1/authors/1")
		  .send(ar -> {
			  if (!ar.succeeded())
				  System.out.println("getAuthorWithIDSuccessTest() fails! " + ar.cause().getMessage());
			  assertTrue(ar.succeeded());
		      // Obtain response
		      HttpResponse<Buffer> response = ar.result();	      
		      assertEquals(200, response.statusCode());
		      assertEquals("application/json; charset=utf-8", response.headers().get("content-type"));
		      assertNotNull(response.body());
		      assertFalse(response.body().toString().isEmpty());
	    	  final Author author = Json.decodeValue(response.body().toString(), Author.class);
	    	  assertNotNull(author);
	    	  assertEquals("JK", author.getFirstName());
	    	  assertEquals("Rowing", author.getLastName());
	    	  assertEquals("jk.rowing@email.com", author.getEmail());
	    	  assertEquals("+49123456789", author.getPhone());
	    	  context.completeNow();
		  });				
	}	
	@Test
	@Order(4)
	public void getBookWithISBNSuccessTest(Vertx vertx, VertxTestContext context) {
		WebClient client = WebClient.create(vertx);
		client.get(port_, "localhost", "/api/v1/books/123456789")
		  .send(ar -> {
			  if (!ar.succeeded())
				  System.out.println("getBookWithISBNSuccessTest() fails! " + ar.cause().getMessage());
			  assertTrue(ar.succeeded());
		      // Obtain response
		      HttpResponse<Buffer> response = ar.result();	      
		      assertEquals(200, response.statusCode());
		      assertEquals("application/json; charset=utf-8", response.headers().get("content-type"));
		      assertNotNull(response.body());
		      assertFalse(response.body().toString().isEmpty());
	    	  final Book book = Json.decodeValue(response.body().toString(), Book.class);
	    	  assertNotNull(book);
	    	  assertEquals("123456789", book.getIsbn());
	    	  assertEquals("Harry Porter", book.getTitle());
	    	  assertEquals(123, book.getPageCount());
	    	  assertEquals(1, book.getAuthorId());
	    	  context.completeNow();
		  });				
	}
	@Test
	@Order(5)
	public void updateAuthorSuccessTest(Vertx vertx, VertxTestContext context) {
		WebClient client = WebClient.create(vertx);
		final String json = Json.encodePrettily(new Author("JK", "Rowing", "jk.rowing@gmail.com", "+4998765432"));
		JsonObject author = new JsonObject(json);
		client.put(port_, "localhost", "/api/v1/authors/1")
			.sendJson(author, ar -> {
				  if (!ar.succeeded())
					  System.out.println("getAuthorWithIDSuccessTest() fails! " + ar.cause().getMessage());
				  assertTrue(ar.succeeded());
			      // Obtain response
			      HttpResponse<Buffer> response = ar.result();	      
			      assertEquals(200, response.statusCode());
			      assertEquals("application/json; charset=utf-8", response.headers().get("content-type"));
			      assertNotNull(response.body());
			      assertFalse(response.body().toString().isEmpty());
		    	  final Author updated = Json.decodeValue(response.body().toString(), Author.class);
		    	  assertNotNull(updated);
		    	  assertEquals("JK", updated.getFirstName());
		    	  assertEquals("Rowing", updated.getLastName());
		    	  assertEquals("jk.rowing@gmail.com", updated.getEmail());
		    	  assertEquals("+4998765432", updated.getPhone());
		    	  context.completeNow();				
			});
	}
	@Test
	@Order(6)
	public void updateBookSuccessTest(Vertx vertx, VertxTestContext context) {
		WebClient client = WebClient.create(vertx);
		final String json = Json.encodePrettily(new Book("Harry Porter", "123456789", 456, 1L));
		JsonObject book = new JsonObject(json);
		client.put(port_, "localhost", "/api/v1/books/123456789")
			.sendJson(book, ar -> {
				  if (!ar.succeeded())
					  System.out.println("getAuthorWithIDSuccessTest() fails! " + ar.cause().getMessage());
				  assertTrue(ar.succeeded());
			      // Obtain response
			      HttpResponse<Buffer> response = ar.result();	      
			      assertEquals(200, response.statusCode());
			      assertEquals("application/json; charset=utf-8", response.headers().get("content-type"));
			      assertNotNull(response.body());
			      assertFalse(response.body().toString().isEmpty());
		    	  final Book updated = Json.decodeValue(response.body().toString(), Book.class);
		    	  assertNotNull(updated);
		    	  assertEquals("Harry Porter", updated.getTitle());
		    	  assertEquals("123456789", updated.getIsbn());
		    	  assertEquals(456, updated.getPageCount());
		    	  assertEquals(1, updated.getAuthorId());
		    	  context.completeNow();				
			});
	}
	@Test
	@Order(7)
	public void deleteBookSuccessTest(Vertx vertx, VertxTestContext context) {
		WebClient client = WebClient.create(vertx);
		client.delete(port_, "localhost", "/api/v1/books/123456789")
		  .send(ar -> {
			  if (!ar.succeeded())
				  System.out.println("deleteBookSuccessTest() fails! " + ar.cause().getMessage());
			  assertTrue(ar.succeeded());
		      // Obtain response
		      HttpResponse<Buffer> response = ar.result();	      
		      assertEquals(204, response.statusCode());
		      context.completeNow();
		  });		
	}
	@Test
	@Order(8)
	public void deleteAuthorSuccessTest(Vertx vertx, VertxTestContext context) {
		WebClient client = WebClient.create(vertx);
		client.delete(port_, "localhost", "/api/v1/authors/0")
		  .send(ar -> {
			  if (!ar.succeeded())
				  System.out.println("deleteAuthorSuccessTest() fails! " + ar.cause().getMessage());
			  assertTrue(ar.succeeded());
		      // Obtain response
		      HttpResponse<Buffer> response = ar.result();	      
		      assertEquals(204, response.statusCode());
		      context.completeNow();
		  });		
	}	
}