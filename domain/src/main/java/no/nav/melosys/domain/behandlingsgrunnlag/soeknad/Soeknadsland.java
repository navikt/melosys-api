package no.nav.melosys.domain.behandlingsgrunnlag.soeknad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Soeknadsland {
    public List<String> landkoder = new ArrayList<>();

    public static Soeknadsland av(Collection<String> landkoder) {
        Soeknadsland soeknadsland = new Soeknadsland();
        soeknadsland.landkoder = List.copyOf(landkoder);
        return soeknadsland;
    }
}
