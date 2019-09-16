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
 * Sender orienteringsbrev til bruker
 *
 * Transisjoner:
 * VS_SEND_ORIENTERINGSBREV -> VS_SEND_SOKNAD eller FEILET_MASKINELT hvis feil
 */
@Component("VideresendSoknadOrienteringsbrev")
public class SendOrienteringsbrev extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendOrienteringsbrev.class);

    private final BrevBestiller brevBestiller;

    @Autowired
    public SendOrienteringsbrev(BrevBestiller brevBestiller) {
        this.brevBestiller = brevBestiller;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return VS_SEND_ORIENTERINGSBREV;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());


        prosessinstans.setSteg(VS_SEND_SOKNAD);
    }
}
