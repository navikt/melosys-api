package no.nav.melosys.domain.eessi.melding;

import java.util.Objects;

public class Statsborgerskap {
    private String landkode;

    public String getLandkode() {
        return landkode;
    }

    public void setLandkode(String landkode) {
        this.landkode = landkode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Statsborgerskap)) return false;
        Statsborgerskap that = (Statsborgerskap) o;
        return getLandkode().equals(that.getLandkode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLandkode());
    }
}
