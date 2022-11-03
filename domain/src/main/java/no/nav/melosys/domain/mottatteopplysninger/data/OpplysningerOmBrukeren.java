package no.nav.melosys.domain.mottatteopplysninger.data;

import java.util.ArrayList;
import java.util.List;

//Ekstra opplysning for bruker
public class OpplysningerOmBrukeren {
    public List<UtenlandskIdent> utenlandskIdent = new ArrayList<>();
    public List<MedfolgendeFamilie> medfolgendeFamilie = new ArrayList<>();
    public FoedestedOgLand foedestedOgLand;
}
