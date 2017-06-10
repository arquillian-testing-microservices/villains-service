package org.lordofthejars.villains.villain;

import io.specto.hoverfly.junit.rule.HoverflyRule;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.CountDownLatch;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;

public class VillainsVerticleTest {

    private static Vertx vertx;

    private static String RESPONSE = "{\n"
        + "  \"name\": \"Gru\",\n"
        + "  \"areaOfInfluence\": \"Worldwide\",\n"
        + "  \"crimes\": [\n"
        + "    {\n"
        + "      \"name\": \"Moon\",\n"
        + "      \"wikipedia\": \"https://en.wikipedia.org/wiki/Moon\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"name\": \"Times Square JumboTron\",\n"
        + "      \"wikipedia\": \"https://en.wikipedia.org/wiki/One_Times_Square\"\n"
        + "    }\n"
        + "  ]\n"
        + "}";


    @ClassRule
    public static HoverflyRule hoverflyRule = HoverflyRule.inSimulationMode(dsl(
        service("crimes")
            .get("/crimes/Gru")
            .willReturn(success(RESPONSE, "application/json"))
    ));

    @BeforeClass
    public static void deployVerticle() throws InterruptedException {
        final CountDownLatch waitVerticleDeployed = new CountDownLatch(1);

        new Thread(() -> {
            vertx = Vertx.vertx();
            DeploymentOptions deploymentOptions = new DeploymentOptions().
                setConfig(new JsonObject()
                    .put("services.crimes.host", "crimes")
                    .put("services.crimes.port", 9090));

            vertx.deployVerticle(VillainsVerticle.class.getName(), deploymentOptions, event -> {
                if (event.failed()) {
                    throw new IllegalStateException("Cannot deploy Villains Verticle");
                }
                waitVerticleDeployed.countDown();
            });
        }).start();
        waitVerticleDeployed.await();
    }

    @Test
    public void should_get_villain_information() {
        System.out.println(given()
            .when()
            .get("villains/{villain}", "Gru")
            .then()
            .extract()
            .body()
            .as(String.class));
    }

}
