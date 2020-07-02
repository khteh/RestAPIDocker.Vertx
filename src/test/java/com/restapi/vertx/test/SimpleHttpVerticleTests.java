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
	private static int port_;
	@BeforeAll
	@DisplayName("Deploy a verticle")
	public static void prepare(Vertx vertx, VertxTestContext context) {
	    System.out.println(SimpleHttpVerticleTests.class.getName() + " setup completes");
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
				vertx.deployVerticle(SimpleHttpVerticle.class.getName(), options, context.completing());			});			
		} catch (Exception e) {
			System.out.println(SimpleHttpVerticle.class.getName() + " setup exception!" + e.toString());
		}	    
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
	public static void lastChecks(Vertx vertx) {
		assertEquals(1, vertx.deploymentIDs().size());
	}	
}