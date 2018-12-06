package no.nav.melosys.service.dokument.brev;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.YrkesgruppeType;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

public class BrevDataA1 extends BrevData {

    public BrevDataA1(String saksbehandler) {
        super(saksbehandler);
    }

    public List<Virksomhet> utenlandskeVirksomheter;
    public List<Virksomhet> norskeVirksomheter;
    public Set<String> selvstendigeForetak;
    public YrkesgruppeType yrkesgruppe;
    public SoeknadDokument søknad;

    public Bostedsadresse bostedsadresse;
}
