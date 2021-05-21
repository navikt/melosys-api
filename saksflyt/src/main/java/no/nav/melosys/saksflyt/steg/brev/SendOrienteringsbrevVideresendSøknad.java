package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;

@Component
public class SendOrienteringsbrevVideresendSøknad implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendOrienteringsbrevVideresendSøknad.class);

    private final BehandlingService behandlingService;
    private final BrevBestiller brevBestiller;

    @Autowired
    public SendOrienteringsbrevVideresendSøknad(BehandlingService behandlingService, BrevBestiller brevBestiller) {
        this.behandlingService = behandlingService;
        this.brevBestiller = brevBestiller;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SEND_ORIENTERINGSBREV_VIDERESENDING_SØKNAD;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        String fritekst = prosessinstans.getData(BEHANDLINGSRESULTAT_BEGRUNNELSE_FRITEKST);

        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder()
            .medAvsenderNavn(saksbehandler)
            .medProduserbartDokument(Produserbaredokumenter.ORIENTERING_VIDERESENDT_SOEKNAD)
            .medMottakere(Mottaker.av(Aktoersroller.BRUKER))
            .medBehandling(behandling)
            .medFritekst(fritekst)
            .build();
        brevBestiller.bestill(brevbestilling);

        log.info("Sendt orienteringsbrev om videresending av søknad for behandling {}", behandling.getId());
    }
}
