package no.nav.melosys.domain.dokument.soeknad;

import java.util.List;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Familiemedlem;

//Ekstra opplysning for bruker
public class OpplysningerOmBrukeren {

    public List<UtenlandskID> utenlandskID;
    public List<String> medfolgendeFamilie; // Er del av personopplysninger og  kommer in med barn og medfølgene.
    public String medfolgendeAndre;
}
