package no.nav.melosys.saksflyt.steg.behandling;

import java.time.LocalDate;
import java.util.Optional;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsaarsak;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.REPLIKER_BEHANDLING;


@Component
public class ReplikerBehandling implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(ReplikerBehandling.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final SaksbehandlingRegler behandlingReplikeringsRegler;

    private final Unleash unleash;

    public ReplikerBehandling(FagsakService fagsakService, BehandlingService behandlingService, SaksbehandlingRegler behandlingReplikeringsRegler, Unleash unleash) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.behandlingReplikeringsRegler = behandlingReplikeringsRegler;
        this.unleash = unleash;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return REPLIKER_BEHANDLING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        String saksnummer = prosessinstans.getData(ProsessDataKey.SAKSNUMMER);
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        var behandlingstype = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class);
        var behandlingstema = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTEMA, Behandlingstema.class);

        Behandling nyBehandling;
        Optional<Behandling> behandlingBruktForReplikering;

        if (unleash.isEnabled("melosys.behandle_alle_saker")) {
            behandlingBruktForReplikering = Optional.ofNullable(behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak));
            if (behandlingBruktForReplikering.isEmpty()) {
                throw new FunksjonellException("Finner ikke behandling som kan replikeres. Denne fantes ved opprettelse av prosessen");
            }
        } else {
            behandlingBruktForReplikering = fagsakService.hentBehandlingSomErUtgangspunktForRevurdering(fagsak);
        }

        if (behandlingBruktForReplikering.isPresent()) {
            nyBehandling = behandlingService.replikerBehandlingOgBehandlingsresultat(behandlingBruktForReplikering.get(), behandlingstype);
        } else {
            behandlingBruktForReplikering = Optional.of(fagsak.hentSistOppdatertBehandling());
            nyBehandling = behandlingService.replikerBehandlingMedNyttBehandlingsresultat(behandlingBruktForReplikering.get(), behandlingstype);
        }

        if (behandlingBruktForReplikering.get().erAktiv()) {
            throw new FunksjonellException("Støtter ikke opprettelse av ny behandling når behandling som er utgangspunkt for revurdering er aktiv");
        }
        if (behandlingstema != null) {
            nyBehandling.setTema(behandlingstema);
        }

        if (unleash.isEnabled("melosys.ny_opprett_sak")) settBehandlingsårsak(nyBehandling, prosessinstans);

        prosessinstans.setBehandling(nyBehandling);

        fagsakService.lagre(fagsak);

        log.info("Behandling {} replikert og behandling {} har blitt opprettet for {}",
            behandlingBruktForReplikering.get().getId(), nyBehandling.getId(), saksnummer);
    }

    private void settBehandlingsårsak(Behandling nyBehandling, Prosessinstans prosessinstans) {
        var behandlingsårsaktype = prosessinstans.getData(BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.class);
        var behandlingsårsakFritekst = prosessinstans.getData(BEHANDLINGSÅRSAK_FRITEKST);
        LocalDate mottaksdato = prosessinstans.getData(MOTTATT_DATO, LocalDate.class);

        if (behandlingsårsaktype == null || mottaksdato == null) {
            throw new FunksjonellException("Mangler mottaksdato eller behandlingsårsaktype");
        }

        var behandlingsårsak = new Behandlingsaarsak(behandlingsårsaktype, behandlingsårsakFritekst, mottaksdato);
        nyBehandling.setBehandlingsårsak(behandlingsårsak);
    }
}
