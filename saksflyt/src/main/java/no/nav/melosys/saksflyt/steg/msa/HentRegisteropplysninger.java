package no.nav.melosys.saksflyt.steg.msa;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

@Component("MottakSoknadAltinnHentRegisteropplysninger")
public class HentRegisteropplysninger implements StegBehandler {

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.MSA_HENT_REGISTEROPPLYSNINGER;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        throw new NotImplementedException("Ikke implementert å hente registeropplysnigner for altinn-søknad");
    }
}
