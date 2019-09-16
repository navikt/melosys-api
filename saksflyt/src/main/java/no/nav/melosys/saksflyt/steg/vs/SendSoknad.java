package no.nav.melosys.saksflyt.steg.vs;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.*;

/**
 * Sender et brev med søknad som vedlegg til utenlandsk myndighet
 *
 * Transisjoner:
 * VS_SEND_SOKNAD -> VS_SEND_SOKNAD eller FEILET_MASKINELT hvis feil
 */
@Component("VideresendSoknadbrev")
public class SendSoknad extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendSoknad.class);

    private final BrevBestiller brevBestiller;

    @Autowired
    public SendSoknad(BrevBestiller brevBestiller) {
        this.brevBestiller = brevBestiller;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return VS_SEND_SOKNAD;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());


        prosessinstans.setSteg(IV_STATUS_BEH_AVSL);
    }
}
