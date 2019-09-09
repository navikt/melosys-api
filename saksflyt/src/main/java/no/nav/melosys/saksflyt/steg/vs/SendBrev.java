package no.nav.melosys.saksflyt.steg.vs;

import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.HS_SEND_BREV;
import static no.nav.melosys.domain.ProsessSteg.IV_STATUS_BEH_AVSL;

/**
 * Sender orienteringsbrev til bruker og et brev med søknad som vedlegg til utenlandsk myndighet
 *
 * Transisjoner:
 * VS_SEND_BREV -> IV_STATUS_BEH_AVSL eller FEILET_MASKINELT hvis feil
 */
@Component("VideresendSoknadSendBrev")
public class SendBrev extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendBrev.class);

    private final BrevBestiller brevBestiller;

    @Autowired
    public SendBrev(BrevBestiller brevBestiller) {
        this.brevBestiller = brevBestiller;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return HS_SEND_BREV;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());


        prosessinstans.setSteg(IV_STATUS_BEH_AVSL);
    }
}
