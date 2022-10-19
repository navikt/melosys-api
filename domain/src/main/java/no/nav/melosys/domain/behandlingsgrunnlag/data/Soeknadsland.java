package no.nav.melosys.domain.behandlingsgrunnlag.data;

import no.nav.melosys.domain.kodeverk.Trygdeavtale_myndighetsland;
import no.nav.melosys.exception.TekniskException;

import java.util.ArrayList;
import java.util.List;

public class Soeknadsland {
    public List<String> landkoder = new ArrayList<>();

    public boolean erUkjenteEllerAlleEosLand;

    public Soeknadsland() {
    }

    public Soeknadsland(List<String> landkoder, boolean erUkjenteEllerAlleEosLand) {
        this.landkoder = landkoder;
        this.erUkjenteEllerAlleEosLand = erUkjenteEllerAlleEosLand;
    }

    public boolean erGyldig() {
        return !landkoder.isEmpty() || erUkjenteEllerAlleEosLand;
    }

    public Trygdeavtale_myndighetsland hentSoeknadslandForTrygdeavtale() {
        if (landkoder.size() > 1) {
            throw new TekniskException("Trygdeavtale kan kun ha et søknadsland, denne har: " + landkoder.size());
        }
        return landkoder.isEmpty() ? null : Trygdeavtale_myndighetsland.valueOf(landkoder.get(0));
    }
}
