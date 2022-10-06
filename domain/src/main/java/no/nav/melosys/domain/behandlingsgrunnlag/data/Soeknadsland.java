package no.nav.melosys.domain.behandlingsgrunnlag.data;

import java.util.ArrayList;
import java.util.List;

public class Soeknadsland {
    public List<String> landkoder = new ArrayList<>();

    public boolean erUkjenteEllerAlleEosLand;

    public Soeknadsland() {}

    public Soeknadsland(List<String> landkoder, boolean erUkjenteEllerAlleEosLand) {
        this.landkoder = landkoder;
        this.erUkjenteEllerAlleEosLand = erUkjenteEllerAlleEosLand;
    }

    public boolean erGyldig() {
        return !landkoder.isEmpty() || erUkjenteEllerAlleEosLand;
    }
}
