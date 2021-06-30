package no.nav.melosys.domain.behandlingsgrunnlag.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Soeknadsland {
    public List<String> landkoder = new ArrayList<>();

    public boolean erUkjenteEllerAlleEosLand = false;

    public static Soeknadsland av(Collection<String> landkoder) {
        Soeknadsland soeknadsland = new Soeknadsland();
        soeknadsland.landkoder = List.copyOf(landkoder);
        soeknadsland.erUkjenteEllerAlleEosLand = false;
        return soeknadsland;
    }
}
