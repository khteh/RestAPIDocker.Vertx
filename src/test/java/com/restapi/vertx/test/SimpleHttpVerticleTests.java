package com.restapi.vertx.test;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.*;
import io.vertx.ext.web.codec.*;
import io.vertx.core.buffer.*;
import io.vertx.config.*;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.ServerSocket;

import org.junit.jupiter.api.*;
import com.restapi.vertx.verticles.SimpleHttpVerticle;
@ExtendWith(VertxExtension.class)
public class SimpleHttpVerticleTests {
	@BeforeAll
	@DisplayName("Deploy a verticle")
	static void prepare(Vertx vertx, VertxTestContext testContext) {
	    vertx.deployVerticle(new SimpleHttpVerticle(), testContext.completing());
	    System.out.println(SimpleHttpVerticleTests.class.getName() + " setup completes");
	}	
	@Test
	void SimpleHttpVerticleTest(Vertx vertx, VertxTestContext testContext) {
		System.out.println("Hello");
		JsonObject conf = new JsonObject(vertx.fileSystem().readFileBlocking("src/main/config/vertx.json"));
		System.out.println(SimpleHttpVerticleTests.class.getName() + " port: " + conf.getInteger("port"));
		WebClient client = WebClient.create(vertx);
		client.get(conf.getInteger("port"), "localhost", "/")
				.as(BodyCodec.string())
				.send(testContext.succeeding(response -> testContext.verify(() -> {
			        	assertEquals(200, response.statusCode());
			        	assertTrue(response.body().toString().contains("Vertx HTTP Server"));
				        testContext.completeNow();
				})));
	  }
	@AfterAll
	@DisplayName("Check that the verticle is still there")
	void lastChecks(Vertx vertx) {
		assertEquals(1, vertx.deploymentIDs().size());
	}	
	/*
	@BeforeAll
	public void setup(Vertx vertx, VertxTestContext context) throws IOException {
		vertx.deployVerticle(SimpleHttpVerticle.class.getName(), context.completing());	
	}
	@Test
	public void successTest(Vertx vertx, VertxTestContext testContext) {
		ConfigStoreOptions fileStore = new ConfigStoreOptions()
				.setType("file")
				.setFormat("json")
				.setConfig(new JsonObject().put("path", "src/main/config/vertx.json"));
		ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore);		
		ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
		retriever.getConfig(json -> {
			JsonObject config = json.result();		
		WebClient client = WebClient.create(vertx);
		// Send a GET request
		client
		  .get(config.getInteger("port"), "localhost", "/")
		  .send(ar -> {
			  if (!ar.succeeded())
				  System.out.println("SimpleHttpVerticleTests Something went wrong: " + ar.cause().getMessage());
			  assertTrue(ar.succeeded());
		      // Obtain response
		      HttpResponse<Buffer> response = ar.result();	      
		      assertEquals(200, response.statusCode());
		      assertTrue(response.body().toString().contains("Vertx HTTP Server"));
		  });
		});
	}
	*/
}