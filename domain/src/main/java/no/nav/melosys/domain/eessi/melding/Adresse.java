package no.nav.melosys.domain.eessi.melding;

import java.util.Objects;

public class Adresse {
    public String by;
    public String bygning;
    public String gate;
    public String land;
    public String postnummer;
    public String region;
    public String type;

    public Adresse() {
    }

    public Adresse(String by, String bygning, String gate, String land, String postnummer, String region, String type) {
        this.by = by;
        this.bygning = bygning;
        this.gate = gate;
        this.land = land;
        this.postnummer = postnummer;
        this.region = region;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Adresse)) return false;
        Adresse adresse = (Adresse) o;
        return Objects.equals(by, adresse.by) &&
            Objects.equals(bygning, adresse.bygning) &&
            Objects.equals(gate, adresse.gate) &&
            Objects.equals(land, adresse.land) &&
            Objects.equals(postnummer, adresse.postnummer) &&
            Objects.equals(region, adresse.region) &&
            Objects.equals(type, adresse.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(by, bygning, gate, land, postnummer, region, type);
    }
}
