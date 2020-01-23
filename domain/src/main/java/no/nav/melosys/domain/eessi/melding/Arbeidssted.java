package no.nav.melosys.domain.eessi.melding;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Arbeidssted {
    public final String navn;
    public final Adresse adresse;
    public final String hjemmebase;
    public final boolean erIkkeFastAdresse;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Arbeidssted(@JsonProperty("navn") String navn,
                       @JsonProperty("adresse") Adresse adresse,
                       @JsonProperty("hjemmebase") String hjemmebase,
                       @JsonProperty("erIkkeFastAdresse") boolean erIkkeFastAdresse) {
        this.navn = navn;
        this.adresse = adresse;
        this.hjemmebase = hjemmebase;
        this.erIkkeFastAdresse = erIkkeFastAdresse;
    }

    public Arbeidssted(String navn, Adresse adresse) {
        this(navn, adresse, null, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Arbeidssted)) return false;
        Arbeidssted that = (Arbeidssted) o;
        return erIkkeFastAdresse == that.erIkkeFastAdresse &&
            navn.equals(that.navn) &&
            Objects.equals(adresse, that.adresse) &&
            Objects.equals(hjemmebase, that.hjemmebase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(navn, adresse, hjemmebase, erIkkeFastAdresse);
    }
}
