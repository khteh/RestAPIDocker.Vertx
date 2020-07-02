package com.restapi.vertx.test;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
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
import java.io.IOException;
import java.net.ServerSocket;
import com.restapi.vertx.verticles.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.json.JSONException;
import org.skyscreamer.jsonassert.*;
//import org.junit.runner.RunWith;
@ExtendWith(VertxExtension.class)
public class BookVerticleTests {
	private static int port_;
	@BeforeAll
	public static void setup(Vertx vertx, VertxTestContext context) throws IOException {
		try {
			ConfigStoreOptions fileStore = new ConfigStoreOptions()
					.setType("file")
					.setFormat("json")
					.setConfig(new JsonObject().put("path", "src/main/config/vertx.json"));
			ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions().addStore(fileStore);		
			ConfigRetriever retriever = ConfigRetriever.create(vertx, configRetrieverOptions);
			retriever.getConfig(json -> {
				JsonObject config = json.result();
				port_ = config.getInteger("port");
				DeploymentOptions options = new DeploymentOptions().setConfig(config);
				vertx.deployVerticle(BookVerticle.class.getName(), options, context.completing());
			});			
		} catch (Exception e) {
			System.out.println(BookVerticleTests.class.getName() + " setup exception!" + e.toString());
		}
	}
	@Test
	public void successTest(Vertx vertx, VertxTestContext context) {
		String expect = "{\r\n" + 
				"  \"id\": \"123\",\r\n" + 
				"  \"content\": \"This is an intro to vertx\",\r\n" + 
				"  \"author\": \"Donald Trump\",\r\n" + 
				"  \"datePublished\": \"01-02-2017\",\r\n" + 
				"  \"wordCount\": 1578\r\n" + 
				"}";
		WebClient client = WebClient.create(vertx);
		client.get(port_, "localhost", "/api/v1/books/book/123")
		  .send(ar -> {
			  if (!ar.succeeded())
				  System.out.println(BookVerticleTests.class.getName() + " get error: " + ar.cause().getMessage());
			  assertTrue(ar.succeeded());
		      // Obtain response
		      HttpResponse<Buffer> response = ar.result();
			  System.out.println("Status Code: " + response.statusCode());		      
		      assertEquals(200, response.statusCode());
		      //context.assertTrue(response.body().toString().contains("Vertx HTTP Server"));
		      try {
		    	  JSONAssert.assertEquals(expect, response.body().toString(), false);
		      } catch (JSONException e) {
		    	  System.out.println(BookVerticleTests.class.getName() + " exception!" + e.toString());
		      }
		  });		
	}
}