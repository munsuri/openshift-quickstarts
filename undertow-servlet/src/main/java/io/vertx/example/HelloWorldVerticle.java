package io.vertx.example;

import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.mongo.MongoClient;
import rx.Single;

public class HelloWorldVerticle extends AbstractVerticle{
  private static Logger logger = LoggerFactory.getLogger( HelloWorldVerticle.class.getSimpleName() );

  public final static String COLLECTION = "collection";
  public final static String DBNAME = "sampledb";
  public final static String CONNECTION_STRING = "mongodb://userWGN:gudvldUPj43ixvtk@mongodb/sampledb";

  @Override
  public void start() {

    System.setProperty( "java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %2$s [%4$s] %5$s%6$s%n" );
    MongoClient mongo = setupMongo( vertx );
    String commandName = "buildInfo";
    Single<JsonObject> res = mongo.rxRunCommand( commandName, new JsonObject().put( commandName, "" ) );
    // Create an HTTP server which simply returns "Hello World!" to each request.
    // If a configuration is set it get the specified name
    String name = config().getString("name", "World");
    vertx.createHttpServer().requestHandler(req -> req.response().end("Hello " + "v3.4.1" +
      " and MongoDB" + res.map( obj -> obj.getString( "version" ) ).toBlocking().value() + "!") ).listen(8080);
  }

  private MongoClient setupMongo( Vertx vertx ){
    JsonObject mongoConfig = new JsonObject()
      .put( "db_name", DBNAME )
      .put( "connection_string", CONNECTION_STRING );
    return MongoClient.createShared( vertx, mongoConfig );
  }
}

