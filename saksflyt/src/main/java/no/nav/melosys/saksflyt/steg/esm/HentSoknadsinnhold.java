package no.nav.melosys.saksflyt.steg.esm;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.altinn.SoknadMottakConsumer;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HentSoknadsinnhold implements StegBehandler {

    private final SoknadMottakConsumer soknadMottakConsumer;

    @Autowired
    public HentSoknadsinnhold(SoknadMottakConsumer soknadMottakConsumer) {
        this.soknadMottakConsumer = soknadMottakConsumer;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.MSA_HENT_INNHOLD;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String søknadID = prosessinstans.getData(ProsessDataKey.MOTTATT_SOKNAD_ID);
        String søknad = soknadMottakConsumer.hentSøknad(søknadID);
        // todo mappe og lagre søknaden -  MELOSYS-3572
    }
}
