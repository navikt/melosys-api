package no.nav.melosys.saksflyt.agent.hs;

import java.util.Map;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Henleggelsesgrunner;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataBygger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.*;
import static no.nav.melosys.domain.ProsessSteg.HS_SEND_BREV;
import static no.nav.melosys.domain.ProsessSteg.IV_STATUS_BEH_AVSL;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.MELDING_HENLAGT_SAK;

/**
 * Sender henleggelsesbrev til bruker og arbeidsgiver
 *
 * Transisjoner:
 * HS_SEND_BREV -> IV_STATUS_BEH_AVSL eller FEILET_MASKINELT hvis feil
 */
@Component
public class SendHenleggelsesbrev extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendHenleggelsesbrev.class);

    private final DokumentSystemService dokumentService;

    private final BrevDataByggerVelger brevDataByggerVelger;

    @Autowired
    public SendHenleggelsesbrev(DokumentSystemService dokumentService, BrevDataByggerVelger brevDataByggerVelger) {
        this.dokumentService = dokumentService;
        this.brevDataByggerVelger = brevDataByggerVelger;
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

        Behandling behandling = prosessinstans.getBehandling();
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);

        BrevDataBygger brevDataBygger = brevDataByggerVelger.hent(MELDING_HENLAGT_SAK);
        BrevData brevdata = brevDataBygger.lag(behandling, saksbehandler);
        brevdata.mottaker = Aktoersroller.BRUKER;
        brevdata.begrunnelseKode = prosessinstans.getData(BEGRUNNELSEKODE, Henleggelsesgrunner.class).getKode();
        brevdata.fritekst = prosessinstans.getData(FRITEKST);
        dokumentService.produserDokument(behandling.getId(), MELDING_HENLAGT_SAK, brevdata);
        log.info("Send henleggelsesbrev til bruker for prosess {}.", prosessinstans.getId());

        Aktoer fullmektig = behandling.getFagsak().hentAktørMedRolleType(Aktoersroller.REPRESENTANT);
        if (fullmektig != null) {
            brevdata.mottaker = Aktoersroller.REPRESENTANT;
            dokumentService.produserDokument(behandling.getId(), MELDING_HENLAGT_SAK, brevdata);
            log.info("HENLEGGELSESBREV FOR REPRESENTANT ER FORELØPIG IKKE STØTTET: prosess {}.", prosessinstans.getId());
        }

        prosessinstans.setSteg(IV_STATUS_BEH_AVSL);
    }
}
