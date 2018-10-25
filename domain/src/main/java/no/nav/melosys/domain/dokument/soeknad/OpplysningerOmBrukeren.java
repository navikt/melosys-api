package no.nav.melosys.domain.dokument.soeknad;

import java.util.ArrayList;
import java.util.List;

//Ekstra opplysning for bruker
public class OpplysningerOmBrukeren {

    public List<UtenlandskIdent> utenlandskIdent = new ArrayList<>();
    public List<String> medfolgendeFamilie = new ArrayList<>();
    public String medfolgendeAndre;
}
