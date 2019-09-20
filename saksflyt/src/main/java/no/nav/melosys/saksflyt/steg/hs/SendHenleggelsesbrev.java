package no.nav.melosys.saksflyt.steg.hs;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.*;
import static no.nav.melosys.domain.ProsessSteg.HS_SEND_BREV;
import static no.nav.melosys.domain.ProsessSteg.IV_STATUS_BEH_AVSL;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_HENLAGT_SAK;

/**
 * Sender henleggelsesbrev til bruker og arbeidsgiver
 *
 * Transisjoner:
 * HS_SEND_BREV -> IV_STATUS_BEH_AVSL eller FEILET_MASKINELT hvis feil
 */
@Component
public class SendHenleggelsesbrev extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendHenleggelsesbrev.class);

    private final BrevBestiller brevBestiller;

    @Autowired
    public SendHenleggelsesbrev(BrevBestiller brevBestiller) {
        this.brevBestiller = brevBestiller;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return HS_SEND_BREV;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        String begrunnelseKode = prosessinstans.getData(BEGRUNNELSEKODE, Henleggelsesgrunner.class).getKode();
        String fritekst = prosessinstans.getData(FRITEKST);

        Brevbestilling brevbestilling = new Brevbestilling.Builder().medDokumentType(MELDING_HENLAGT_SAK)
            .medAvsender(saksbehandler)
            .medMottakere(Mottaker.av(Aktoersroller.BRUKER))
            .medBehandling(behandling)
            .medBegrunnelseKode(begrunnelseKode)
            .medFritekst(fritekst).build();
        brevBestiller.bestill(brevbestilling);

        prosessinstans.setSteg(IV_STATUS_BEH_AVSL);
    }
}
