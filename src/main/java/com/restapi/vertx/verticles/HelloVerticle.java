package com.restapi.vertx.verticles;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.Promise;
public class HelloVerticle extends AbstractVerticle {
	private static final Log log = LogFactory.getLog(HelloVerticle.class);
	 public static void main(String[] args) {
	        Vertx vertx = Vertx.vertx();
	        vertx.deployVerticle(new HelloVerticle());
	}	
	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		log.info("HelloVerticle start");
	}
	public void stop() {
		log.info("HelloVerticle stop");
	}
}