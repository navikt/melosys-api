package no.nav.melosys.saksflyt.steg.hs;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_HENLAGT_SAK;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.HS_SEND_BREV;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_STATUS_BEH_AVSL;

/**
 * Sender henleggelsesbrev til bruker og arbeidsgiver
 *
 * Transisjoner:
 * HS_SEND_BREV -> IV_STATUS_BEH_AVSL eller FEILET_MASKINELT hvis feil
 */
@Component
public class SendHenleggelsesbrev implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(SendHenleggelsesbrev.class);

    private final BrevBestiller brevBestiller;
    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public SendHenleggelsesbrev(BrevBestiller brevBestiller, BehandlingsresultatService behandlingsresultatService) {
        this.brevBestiller = brevBestiller;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return HS_SEND_BREV;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.info("Sender henleggelsesbrev for behandling {}", prosessinstans.getBehandling().getId());
        final List<String> henleggelsesGrunnerKoder = Arrays.stream(Henleggelsesgrunner.values())
            .map(Kodeverk::getKode).collect(Collectors.toList());

        Behandling behandling = prosessinstans.getBehandling();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        String saksbehandler = prosessinstans.getData(SAKSBEHANDLER);
        String fritekst = behandlingsresultat.getBegrunnelseFritekst();
        String begrunnelseKode = behandlingsresultat.getBehandlingsresultatBegrunnelser()
            .stream()
            .map(BehandlingsresultatBegrunnelse::getKode)
            .filter(henleggelsesGrunnerKoder::contains)
            .findFirst().orElseThrow(() -> new IkkeFunnetException("Finner ingen henleggelsesgrunn"));

        Brevbestilling brevbestilling = new Brevbestilling.Builder()
            .medDokumentType(MELDING_HENLAGT_SAK)
            .medAvsender(saksbehandler)
            .medMottakere(Mottaker.av(Aktoersroller.BRUKER))
            .medBehandling(behandling)
            .medBegrunnelseKode(begrunnelseKode)
            .medFritekst(fritekst)
            .build();
        brevBestiller.bestill(brevbestilling);

        prosessinstans.setSteg(IV_STATUS_BEH_AVSL);
    }
}
