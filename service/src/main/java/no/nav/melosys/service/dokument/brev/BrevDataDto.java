package no.nav.melosys.service.dokument.brev;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.RolleType;
import no.nav.melosys.domain.YrkesgruppeType;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.sikkerhet.context.SubjectHandler;

public class BrevDataDto {

    public String saksbehandler;

    public RolleType mottaker;

    public String fritekst;
    public List<ForetakUtland> utenlandskeVirksomheter;
    public Set<OrganisasjonDokument> norskeVirksomheter;
    public Set<String> selvstendigeForetak;
    public YrkesgruppeType yrkesgruppe;
    public SoeknadDokument søknad;

    public BrevDataDto() {
        saksbehandler = SubjectHandler.getInstance().getUserID();
    }
}
