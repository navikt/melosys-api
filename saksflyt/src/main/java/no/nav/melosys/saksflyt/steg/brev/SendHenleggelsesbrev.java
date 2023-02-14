package no.nav.melosys.saksflyt.steg.brev;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_HENLAGT_SAK;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;

@Component
public class SendHenleggelsesbrev implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendHenleggelsesbrev.class);

    private final BrevBestiller brevBestiller;
    private final BehandlingsresultatService behandlingsresultatService;

    public SendHenleggelsesbrev(BrevBestiller brevBestiller, BehandlingsresultatService behandlingsresultatService) {
        this.brevBestiller = brevBestiller;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.SEND_HENLEGGELSESBREV;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        log.info("Sender henleggelsesbrev for behandling {}", prosessinstans.getBehandling().getId());
        final List<String> henleggelsesGrunnerKoder = Arrays.stream(Henleggelsesgrunner.values())
            .map(Kodeverk::getKode).toList();

        Behandling behandling = prosessinstans.getBehandling();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        String fritekst = behandlingsresultat.getBegrunnelseFritekst();
        String begrunnelseKode = behandlingsresultat.getBehandlingsresultatBegrunnelser()
            .stream()
            .map(BehandlingsresultatBegrunnelse::getKode)
            .filter(henleggelsesGrunnerKoder::contains)
            .findFirst().orElseThrow(() -> new IkkeFunnetException("Finner ingen henleggelsesgrunn"));

        brevBestiller.bestill(MELDING_HENLAGT_SAK, Collections.singleton(Mottaker.av(Mottakerroller.BRUKER)), fritekst,
            saksbehandler, begrunnelseKode, behandling);
    }
}
