package no.nav.melosys.domain.eessi.melding;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Avsender {
    private final String avsenderID;
    private final String landkode;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Avsender(@JsonProperty("avsenderID") String avsenderID,
                    @JsonProperty("landkode") String landkode) {
        this.avsenderID = avsenderID;
        this.landkode = landkode;
    }

    public String getAvsenderID() {
        return avsenderID;
    }

    public String getLandkode() {
        return landkode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Avsender avsender = (Avsender) o;
        return Objects.equals(avsenderID, avsender.avsenderID) &&
            Objects.equals(landkode, avsender.landkode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(avsenderID, landkode);
    }

    @Override
    public String toString() {
        return "Avsender{" +
            "avsenderID='" + avsenderID + '\'' +
            ", landkode='" + landkode + '\'' +
            '}';
    }
}
