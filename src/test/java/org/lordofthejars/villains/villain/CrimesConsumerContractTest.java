package org.lordofthejars.villains.villain;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.CountDownLatch;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Single;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class CrimesConsumerContractTest {

    private static Vertx vertx;

    @Test
    public void should_get_list_of_crimes_by_villain() {

        // given
        CrimesGateway crimesGateway = new CrimesGateway();

        // when
        final Single<JsonArray> gruCrimes = crimesGateway.getCrimesByVillainName("Gru");
        final JsonArray crimes = gruCrimes.toBlocking().value();

        // then
        assertThat(crimes)
            .extracting(crime -> ((JsonObject)crime).getString("name"), crime -> ((JsonObject)crime).getString("wiki"))
            .contains(tuple("Moon", "https://en.wikipedia.org/wiki/Moon"), tuple("Times Square JumboTron", "https://en.wikipedia.org/wiki/One_Times_Square"));
    }


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
}
