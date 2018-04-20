package org.lordofthejars.villains.villain;

import io.specto.hoverfly.junit.rule.HoverflyRule;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.CountDownLatch;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

public class VillainsVerticleTest {

    private static Vertx vertx;

    private static String RESPONSE = "[\n"
        + "    {\n"
        + "        \"name\": \"Moon\",\n"
        + "        \"villain\": \"Gru\",\n"
        + "        \"wiki\": \"https://en.wikipedia.org/wiki/Moon\"\n"
        + "    },\n"
        + "    {\n"
        + "        \"name\": \"Times Square JumboTron\",\n"
        + "        \"villain\": \"Gru\",\n"
        + "        \"wiki\": \"https://en.wikipedia.org/wiki/One_Times_Square\"\n"
        + "    }\n"
        + "]";


    /**@ClassRule
    public static HoverflyRule hoverflyRule = HoverflyRule.inSimulationMode(dsl(
        service("localhost:8080")
            .get("/crimes/Gru")
            .willReturn(success(RESPONSE, "application/json"))
    ));**/

    @ClassRule
    public static HoverflyRule hoverflyRule = HoverflyRule.inCaptureOrSimulationMode("hoverfly/simulation.json");

    @BeforeClass
    public static void deployVerticle() throws InterruptedException {
        final CountDownLatch waitVerticleDeployed = new CountDownLatch(1);

        new Thread(() -> {
            vertx = Vertx.vertx();
            DeploymentOptions deploymentOptions = new DeploymentOptions().
                setConfig(new JsonObject()
                    .put("services.crimes.host", "localhost")
                    .put("services.crimes.port", 8080));

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
       given()
            .port(8081)
            .when()
            .get("villains/{villain}", "Gru")
            .then()
            .assertThat()
            .body("name", is("Gru"))
            .body("areaOfInfluence", is("Worldwide"))
            .body("crimes.name", hasItems("Moon", "Times Square JumboTron"));

    }

}
