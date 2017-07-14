package org.lordofthejars.villains.villain;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.client.WebClient;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import org.arquillian.algeron.consumer.StubServer;
import org.arquillian.algeron.pact.consumer.spi.Pact;
import org.arquillian.algeron.pact.consumer.spi.PactVerification;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Single;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@RunWith(Arquillian.class)
public class CrimesConsumerContractTest {

    private static Vertx vertx;

    @StubServer
    URL pactServer;

    @Pact(provider = "crimes", consumer = "villains")
    public RequestResponsePact returnListOfCrimes(PactDslWithProvider builder) {

        return builder
            .uponReceiving("Gru villain to get all Crimes")
            .path("/crimes/Gru")
            .method("GET")
            .willRespondWith()
            .status(200)
            .body(RESPONSE)
            .toPact();
    }

    @Test
    @PactVerification("crimes")
    public void should_get_list_of_crimes_by_villain() {

        // given
        final WebClient webClient = WebClient.create(new io.vertx.rxjava.core.Vertx(vertx));
        CrimesGateway crimesGateway = new CrimesGateway(webClient, pactServer);

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
}
