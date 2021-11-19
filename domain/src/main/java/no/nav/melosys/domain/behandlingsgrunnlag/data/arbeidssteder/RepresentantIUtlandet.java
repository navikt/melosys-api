package no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder;

import no.nav.melosys.domain.kodeverk.Landkoder;

import java.util.ArrayList;
import java.util.List;

public class RepresentantIUtlandet {
    public String representantNavn;
    public List<String> adresselinjer = new ArrayList<>();
    public String representantLand;
}
