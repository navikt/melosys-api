package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsmaate;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeGodkjenning;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BestemBehandlingsmåteSed implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(BestemBehandlingsmåteSed.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;
    private final UnntaksperiodeService unntaksperiodeService;

    public BestemBehandlingsmåteSed(BehandlingService behandlingService,
                                    BehandlingsresultatService behandlingsresultatService,
                                    OppgaveService oppgaveService,
                                    UnntaksperiodeService unntaksperiodeService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.unntaksperiodeService = unntaksperiodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.BESTEM_BEHANDLINGMÅTE_SED;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {

        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(prosessinstans.getBehandling().getId());
        final long behandlingID = behandling.getId();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        if (skalGodkjenneUnntaksperiode(behandling, behandlingsresultat)) {
            log.info("Behandling {} tema {} behandles automatisk", behandlingID, behandling.getTema());
            behandlingsresultatService.oppdaterBehandlingsMaate(behandlingID, Behandlingsmaate.AUTOMATISERT);
            MelosysEessiMelding eessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
            UnntaksperiodeGodkjenning unntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder()
                .varsleUtland(false)
                .fritekst(null)
                .eessiMelding(eessiMelding)
                .build();
            unntaksperiodeService.godkjennPeriode(behandling.getId(), unntaksperiodeGodkjenning);
        } else {
            log.info("Oppretter oppgave for behandling {}", behandlingID);
            behandlingsresultatService.oppdaterBehandlingsMaate(behandlingID, Behandlingsmaate.DELVIS_AUTOMATISERT);
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                behandling,
                prosessinstans.hentJournalpostID(),
                behandling.getFagsak().hentBrukersAktørID(),
                prosessinstans.hentSaksbehandlerHvisTilordnes()
            );
        }
    }

    private boolean skalGodkjenneUnntaksperiode(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        return behandling.erRegisteringAvUnntak() && behandlingsresultat.getKontrollresultater().isEmpty();
    }
}
