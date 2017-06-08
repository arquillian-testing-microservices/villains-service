package org.lordofthejars.villains.villain;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Villain {

    private String name;
    private String areaOfInfluence;

    private List<Crime> crimes = new ArrayList<>();

    public Villain(JsonObject jsonObject) {
        this.name = jsonObject.getString("NAME");
        this.areaOfInfluence = jsonObject.getString("AREAOFINFLUENCE");
    }

    public void addCrimes(JsonArray crimes) {
        this.crimes.addAll(crimes.stream()
            .map(crime -> (JsonObject) crime)
            .map(Crime::new)
        .collect(Collectors.toList()));
    }

    public String getName() {
        return name;
    }

    public String getAreaOfInfluence() {
        return areaOfInfluence;
    }

    public List<Crime> getCrimes() {
        return crimes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Villain{");
        sb.append("name='").append(name).append('\'');
        sb.append(", areaOfInfluence='").append(areaOfInfluence).append('\'');
        sb.append(", crimes=").append(crimes);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Villain villain = (Villain) o;

        return name.equals(villain.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
