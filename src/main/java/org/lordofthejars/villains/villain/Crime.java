package org.lordofthejars.villains.villain;

import io.vertx.core.json.JsonObject;
import java.net.MalformedURLException;
import java.net.URL;

public class Crime {

    private String name;
    private URL wikipedia;

    public Crime(JsonObject jsonObject) {
        this.name = jsonObject.getString("name");
        try {
            this.wikipedia = new URL(jsonObject.getString("wiki"));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String getName() {
        return name;
    }

    public URL getWikipedia() {
        return wikipedia;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Crime{");
        sb.append("name='").append(name).append('\'');
        sb.append(", wikipedia=").append(wikipedia);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Crime crime = (Crime) o;

        return name.equals(crime.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
