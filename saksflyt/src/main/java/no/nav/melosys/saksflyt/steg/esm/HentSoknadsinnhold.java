package no.nav.melosys.saksflyt.steg.esm;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.altinn.SoknadMottakConsumer;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HentSoknadsinnhold extends AbstraktStegBehandler {

    private final SoknadMottakConsumer soknadMottakConsumer;

    @Autowired
    public HentSoknadsinnhold(SoknadMottakConsumer soknadMottakConsumer) {
        this.soknadMottakConsumer = soknadMottakConsumer;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.MSA_HENT_INNHOLD;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String søknadID = prosessinstans.getData(ProsessDataKey.MOTTATT_SOKNAD_ID);
        String søknad = soknadMottakConsumer.hentSøknad(søknadID);
        // todo mappe og lagre søknaden -  MELOSYS-3572
    }
}
