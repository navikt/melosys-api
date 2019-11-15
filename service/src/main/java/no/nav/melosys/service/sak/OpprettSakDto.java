package no.nav.melosys.service.sak;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class OpprettSakDto {
    public String brukerID;
    public Sakstyper sakstype;
    public Behandlingstyper behandlingstype;
    public SøknadDto soknadDto;
    public boolean skalTilordnes;
}
