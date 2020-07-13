package com.restapi.vertx;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.config.*;
import io.vertx.core.json.*;
import io.vertx.ext.web.handler.*;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.restapi.vertx.verticles.*;
public class Launcher extends AbstractVerticle {
	private static final Log log = LogFactory.getLog(Launcher.class);
	public void start() {
		ConfigStoreOptions fileStore = new ConfigStoreOptions()
				.setType("file")
				.setFormat("json")
				.setConfig(new JsonObject().put("path", "src/main/config/vertx.json"));
		ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions().addStore(fileStore);		
		ConfigRetriever retriever = ConfigRetriever.create(vertx, configRetrieverOptions);
		retriever.getConfig(json -> {
			JsonObject config = json.result();
			DeploymentOptions options = new DeploymentOptions().setConfig(config).setInstances(4);
			vertx.deployVerticle(LibraryVerticle.class.getName(), options, result -> {
				if (result.succeeded())
					log.info("Successfully launched "+LibraryVerticle.class.getName() + " id: " + result.result());
				else
					log.error("Failed to launch "+ LibraryVerticle.class.getName() + " " + result);
			});			
		});
	}
}