package no.nav.melosys.saksflyt.steg.vs;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.VS_SEND_ORIENTERINGSBREV;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.VS_SEND_SOKNAD;

/**
 * Sender orienteringsbrev til bruker
 *
 * Transisjoner:
 * VS_SEND_ORIENTERINGSBREV -> VS_SEND_SOKNAD eller FEILET_MASKINELT hvis feil
 */
@Component("VideresendSoknadOrienteringsbrev")
public class SendOrienteringsbrev implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendOrienteringsbrev.class);

    private final BehandlingService behandlingService;
    private final BrevBestiller brevBestiller;

    @Autowired
    public SendOrienteringsbrev(BehandlingService behandlingService, BrevBestiller brevBestiller) {
        this.behandlingService = behandlingService;
        this.brevBestiller = brevBestiller;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return VS_SEND_ORIENTERINGSBREV;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        String fritekst = prosessinstans.getData(BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST);

        Brevbestilling brevbestilling = new Brevbestilling.Builder()
            .medAvsender(saksbehandler)
            .medDokumentType(Produserbaredokumenter.ORIENTERING_VIDERESENDT_SOEKNAD)
            .medMottakere(Mottaker.av(Aktoersroller.BRUKER))
            .medBehandling(behandling)
            .medFritekst(fritekst)
            .build();
        brevBestiller.bestill(brevbestilling);

        log.info("Sendt orienteringsbrev om videresending av søknad for prosessinstans {}", prosessinstans.getId());

        prosessinstans.setSteg(VS_SEND_SOKNAD);
    }
}
