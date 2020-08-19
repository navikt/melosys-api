package no.nav.melosys.saksflyt.steg.msa;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID;

@Component("MottakSoknadAltinnSendForvaltningsmelding")
public class SendForvaltningsmelding implements StegBehandler {

    private final BrevBestiller brevBestiller;
    private final BehandlingService behandlingService;

    public SendForvaltningsmelding(BrevBestiller brevBestiller, BehandlingService behandlingService) {
        this.brevBestiller = brevBestiller;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.MSA_SEND_FORVALTNINGSMELDING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        brevBestiller.bestill(MELDING_FORVENTET_SAKSBEHANDLINGSTID, null, Mottaker.av(BRUKER), behandling);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
