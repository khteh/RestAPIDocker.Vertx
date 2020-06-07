package com.restapi.vertx.verticles;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.Promise;
import io.vertx.config.*;
import io.vertx.core.json.*;
public class SimpleHttpVerticle extends AbstractVerticle {
	private static final Log log = LogFactory.getLog(SimpleHttpVerticle.class);
	@Override
	 public void start(Promise<Void> startPromise) throws Exception {
		ConfigRetriever retriever = ConfigRetriever.create(vertx);
		retriever.getConfig(json -> {
			JsonObject config = json.result();
			//log.info("SimpleHttpVerticle verticle port: " + config.getInteger("http.port"));		  
			vertx.createHttpServer()
			.requestHandler(r -> r.response().end("Vertx HTTP Server"))
			.listen(config.getInteger("http.port"), result -> {
				if (result.succeeded())
					startPromise.complete();
				else
					startPromise.fail(result.cause());
			});
		});		
	}
}