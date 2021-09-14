package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;

public record Bostedsland(String landkode) {


    public Bostedsland(String landkode) {
        this.landkode = landkode;
    }

    public Bostedsland(Landkoder landkode) {
        this(landkode.getKode());
    }

    public Landkoder getLandkodeobjekt() {
        try {
            return Landkoder.valueOf(landkode);
        } catch (IllegalArgumentException e) {
            throw new FunksjonellException(String.format("Prøvde å lese landkode fra %s, men støtter bare EU/EØS land", landkode));
        }
    }
}
