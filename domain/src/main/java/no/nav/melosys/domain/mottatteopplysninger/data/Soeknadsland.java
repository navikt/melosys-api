package no.nav.melosys.domain.mottatteopplysninger.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Trygdeavtale_myndighetsland;
import no.nav.melosys.exception.TekniskException;

public class Soeknadsland {
    private List<String> landkoder = new ArrayList<>();

    private boolean flereLandUkjentHvilke;

    public Soeknadsland() {
    }

    public Soeknadsland(List<String> landkoder, boolean flereLandUkjentHvilke) {
        this.landkoder = landkoder;
        this.flereLandUkjentHvilke = flereLandUkjentHvilke;
    }

    public static Soeknadsland av(Land_iso2... lovvalgsland) {
        return new Soeknadsland(Arrays.stream(lovvalgsland).filter(Objects::nonNull).map(Land_iso2::getKode).toList(), false);
    }

    public boolean erGyldig() {
        return !landkoder.isEmpty() || flereLandUkjentHvilke;
    }

    public Trygdeavtale_myndighetsland hentSoeknadslandForTrygdeavtale() {
        if (landkoder.size() > 1) {
            throw new TekniskException("Trygdeavtale kan kun ha et søknadsland, denne har: " + landkoder.size());
        }
        return landkoder.isEmpty() ? null : Trygdeavtale_myndighetsland.valueOf(landkoder.get(0));
    }

    public List<String> getLandkoder() {
        return landkoder;
    }

    public void setLandkoder(List<String> landkoder) {
        this.landkoder = landkoder;
    }

    public boolean isFlereLandUkjentHvilke() {
        return flereLandUkjentHvilke;
    }

    public void setFlereLandUkjentHvilke(boolean flereLandUkjentHvilke) {
        this.flereLandUkjentHvilke = flereLandUkjentHvilke;
    }
}
