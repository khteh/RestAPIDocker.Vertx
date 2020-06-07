package com.restapi.vertx;
import io.vertx.core.Vertx;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.restapi.vertx.verticles.*;
public class Launcher extends AbstractVerticle {
	private static final Log log = LogFactory.getLog(Launcher.class);
	public void start() {
		vertx.deployVerticle(BookVerticle.class.getName(), result -> {
			if (result.succeeded())
				log.info("Successfully launched "+BookVerticle.class.getName());
			else
				log.error("Failed to launched "+ BookVerticle.class.getName() + " " + result);
		});					
	}
}