package no.nav.melosys.domain.dokument.soeknad;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//Ekstra opplysning for bruker
public class OpplysningerOmBrukeren {

    public List<UtenlandskIdent> utenlandskIdent = new ArrayList<>();
    public List<String> medfolgendeFamilie = new ArrayList<>();
    public String medfolgendeAndre;

    public Set<String> hentAllePersonnummer() {
        Set<String> personnummere = new HashSet(medfolgendeFamilie);
        if (medfolgendeAndre != null) {
            personnummere.add(medfolgendeAndre);
        }
        return personnummere;
    }
}
