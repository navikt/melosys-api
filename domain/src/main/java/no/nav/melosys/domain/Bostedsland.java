package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Landkoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bostedsland {

    private static final Logger log = LoggerFactory.getLogger(Bostedsland.class);
    private String landkode;

    public Bostedsland(String landkode) {
        this.landkode = landkode;
    }

    public Bostedsland(Landkoder landkode) {
        this.landkode = landkode.getKode();
    }

    public String getLandkode() {
        return landkode;
    }

    public void setLandkode(String landkode) {
        this.landkode = landkode;
    }

    public Landkoder getLandkodeobjekt() {
        try {
            return Landkoder.valueOf(landkode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Prøvde å lese landkode fra %s, men støtter bare EU/EØS land", landkode));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Bostedsland)) {
            return false;
        }

        Bostedsland bostedsland = (Bostedsland) o;

        return this.getLandkode().equals(bostedsland.getLandkode());
    }
}
