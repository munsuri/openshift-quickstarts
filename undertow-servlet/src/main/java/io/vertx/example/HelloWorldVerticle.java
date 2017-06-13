package io.vertx.example;

import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.mongo.MongoClient;
import rx.Scheduler;
import rx.Single;
import rx.schedulers.Schedulers;

public class HelloWorldVerticle extends AbstractVerticle{
  private static Logger logger = LoggerFactory.getLogger( HelloWorldVerticle.class.getSimpleName() );

  public final static String COLLECTION = "collection";
  public final static String DBNAME = "sampledb";
  private String mongoVersion = "unknown";
  //public final static String CONNECTION_STRING = "mongodb://127.0.0.1:27017/sampledb";
  /*public final static String CONNECTION_STRING = System.getProperty("MONGO_URL") != null
    ? System.getProperty("MONGO_URL") : System.getProperty("OPENSHIFT_MONGODB_DB_URL");*/

  @Override
  public void start(){
    logger.info( "start" );
    //System.setProperty( "java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %2$s [%4$s] %5$s%6$s%n" );
    initMongo();
    initServer();
  }

  @Override
  public void stop(){
    logger.info( "stop" );
  }

  private void initMongo(){
    logger.info( "initMongo" );
    vertx.rxExecuteBlocking( fut -> {
      MongoClient mongo = setupMongo( vertx );
      if( mongo != null ){
        String commandName = "buildInfo";
        Single<JsonObject> res = mongo.rxRunCommand( commandName, new JsonObject().put( commandName, "" ) );
        //mongoVersion = res.map( obj -> obj.getString( "version" ) ).toBlocking().value();
        res
          .map( obj -> obj.getString( "version" ) )
          .doAfterTerminate( () -> {
            logger.info( "initMongo: complete" );
            fut.complete();
          } )
          .doOnError( Throwable::printStackTrace )
          .subscribe( str -> mongoVersion = str )
          ;
        mongo.close();
      }
    })
      .subscribe()
    ;
  }

  private void initServer(){
    logger.info( "initServer" );
    /*vertx.rxExecuteBlocking( fut -> {
      // Create an HTTP server which simply returns "Hello World!" to each request.
      // If a configuration is set it get the specified name
      String name = config().getString("name", "World");
      vertx.createHttpServer()
        .requestHandler( req -> req.response().end("Hello " + "Vertx v3.4.1" +
          " and MongoDB v" + mongoVersion + "!") )
        .listen(8080);

      logger.info( "initServer: complete" );
      fut.complete();
    } )
      .subscribe()
    ;*/
    vertx.createHttpServer()
      .requestHandler( req -> req.response().end("Hello " + "Vertx v3.4.1 and MongoDB v" + mongoVersion + "!") )
      .rxListen( 8080 )
      .observeOn( Schedulers.io() )
      .subscribeOn( Schedulers.io() )
      .doAfterTerminate( () -> logger.info( "initServer: complete" ) )
      .doOnError( (e) -> {
        e.printStackTrace();
        vertx.close();
      } )
      .subscribe()
    ;
  }

  private MongoClient setupMongo( Vertx vertx ){
    String connectionString = System.getProperty("MONGO_URL") != null ?
     System.getProperty("MONGO_URL") : System.getProperty("OPENSHIFT_MONGODB_DB_URL");
    if( connectionString == null ){
      String serviceName = System.getProperty("DATABASE_SERVICE_NAME");
      if( serviceName != null ){
        serviceName = serviceName.toUpperCase();
        String host = System.getProperty( serviceName + "_SERVICE_HOST" );
        String port = System.getProperty( serviceName + "_SERVICE_PORT" );
        String user = System.getProperty( serviceName + "_USER" );
        String pwd = System.getProperty( serviceName + "_PASSWORD" );
        String db = System.getProperty( serviceName + "_DATABASE" );
        connectionString = "mongodb://";
        if( pwd != null )
          connectionString += "mongodb://" + user + ':' + pwd + '@';
        connectionString += host + ':' +  port + '/' + db;
      }
    }

    JsonObject mongoConfig = new JsonObject()
      .put( "db_name", DBNAME )
      .put( "connection_string", connectionString );
      //.put( "connection_string", CONNECTION_STRING );
    return MongoClient.createShared( vertx, mongoConfig );
  }
}

