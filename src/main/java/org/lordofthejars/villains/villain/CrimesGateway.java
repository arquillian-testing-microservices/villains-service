package org.lordofthejars.villains.villain;

import io.vertx.core.json.JsonArray;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import rx.Single;

public class CrimesGateway {

    private WebClient client;
    private String host;
    private Integer port;

    public CrimesGateway(WebClient webClient, String host, Integer port) {
        this.client = webClient;
        this.host = host;
        this.port = port;
    }

    public Single<JsonArray> getCrimesByVillainName(String villainName) {

        return client.get(this.port,
            this.host,
            "/crimes/" + villainName)
            .rxSend()
            .map(HttpResponse::bodyAsJsonArray);

    }

}
