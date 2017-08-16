package org.lordofthejars.villains.villain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLRowStream;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import rx.Single;

public class VillainsVerticle extends AbstractVerticle {

    private static final JsonArray SENTINEL = Villain.sentinel();

    private JDBCClient jdbcClient;
    private WebClient client;
    private CrimesGateway crimesGateway;

    public static void main(String args[]) {
        Vertx vertx = Vertx.vertx();

        DeploymentOptions deploymentOptions = new DeploymentOptions().
            setConfig(new JsonObject()
                .put("services.crimes.host", resolveCrimesHost())
                .put("services.crimes.port", resolvePort()));

        vertx.deployVerticle(VillainsVerticle.class.getName(), deploymentOptions);
    }

    private static String resolveCrimesHost() {
        return System.getProperty("crimes.host", "crimes");
    }

    private static Integer resolvePort() {
        return Integer.parseInt(System.getProperty("crimes.port", "8080"));
    }

    @Override
    public void start(Future<Void> fut) {

        final Optional<ProxyOptions> proxyOptions = useSystemPropertiesProxyOptions();
        if (proxyOptions.isPresent()) {
            final WebClientOptions webClientOptions = new WebClientOptions();
            webClientOptions.setProxyOptions(proxyOptions.get());
            client = WebClient.create(vertx, webClientOptions);
        } else {
            client = WebClient.create(vertx);
        }

        this.crimesGateway = new CrimesGateway(client,
            config().getString("services.crimes.host"),
            config().getInteger("services.crimes.port"));

        jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
            .put("url", "jdbc:h2:mem:villains;DB_CLOSE_DELAY=-1")
            .put("driver_class", "org.h2.Driver")
            .put("user", "sa")
            .put("password", "")
            .put("max_pool_size", 30));

        Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        jdbcClient.rxGetConnection()
            .flatMap(connection -> connection
                .rxExecute("create table villain(id int primary key, name varchar, areaOfInfluence varchar)")
                .flatMap(v ->
                    connection.rxExecute("insert into villain values(1, 'Gru', 'Worldwide'), "
                        + "(2, 'Lex Luthor', 'Metropolis'), "
                        + "(3, 'Jack Sparrow', 'Caribbean'), "
                        + "(4, 'Gargamel', 'Forest')"
                    )
                )
                .doAfterTerminate(connection::close))
            .flatMap(v -> {
                Router router = Router.router(vertx);
                router.route().handler(BodyHandler.create());
                router.get("/villains/:villain").handler(this::handleGetVillains);
                router.get("/health").handler(this::handleHealtchCheck);
                router.get("/version").handler(this::handleGetVersion);
                return vertx.createHttpServer().requestHandler(router::accept).rxListen(8080);
            })
            .toCompletable()
            .subscribe(RxHelper.toSubscriber(fut));
    }

    private Optional<ProxyOptions> useSystemPropertiesProxyOptions() {
        final String httpsProxy = System.getProperty("https.proxyHost");
        if (httpsProxy != null) {
            return Optional.of(new ProxyOptions()
                .setHost(httpsProxy)
                .setPort(Integer.parseInt(System.getProperty("https.proxyPort", "443"))));
        } else {
            final String httpProxy = System.getProperty("http.proxyHost");
            if (httpProxy != null) {
                return Optional.of(new ProxyOptions()
                    .setHost(httpsProxy)
                    .setPort(Integer.parseInt(System.getProperty("http.proxyPort", "80"))));
            }
        }

        return Optional.empty();
    }

    private void handleGetVillains(RoutingContext routingContext) {

        String villainName = routingContext.request().getParam("villain");
        HttpServerResponse response = routingContext.response();
        if (villainName == null) {
            sendError(400, response);
        } else {
            // Step 1 retrieve villain
            jdbcClient.rxGetConnection()
                .flatMap(connection ->
                    connection.rxQueryStreamWithParams("SELECT id, name, areaOfInfluence FROM villain WHERE name=?",
                        new JsonArray().add(villainName)
                    )
                        .flatMapObservable((SQLRowStream::toObservable))
                        .firstOrDefault(SENTINEL)
                        .map(Villain::new)
                        .toSingle()
                        .doAfterTerminate(connection::close)
                )
                // Step 2 retrieve crimes
                .flatMap(villain -> {
                        if (villain.isSentinel()) {
                            return Single.just(villain);
                        }
                        return crimesGateway.getCrimesByVillainName(villainName)
                            .map(villain::addCrimes);
                    }
                )

                // Send result
                .subscribe(villain -> {
                        if (villain.isSentinel()) {
                            sendError(404, response);
                        } else {
                            response.putHeader("content-type", "application/json").end(Json.encode(villain));
                        }
                    },
                    routingContext::fail
                );
        }
    }

    private void sendError(int statusCode, HttpServerResponse response) {
        response.setStatusCode(statusCode).end();
    }

    private void sendError(int statusCode, String message, HttpServerResponse response) {
        response.setStatusCode(statusCode).end(message);
    }

    private void handleGetVersion(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        vertx.fileSystem()
            .rxReadFile("build-info.json")
            .map(Buffer::toJsonObject)
            .subscribe(json -> response.end(json.getString("version")), routingContext::fail);
    }

    private void handleHealtchCheck(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(200).end();
    }
}
