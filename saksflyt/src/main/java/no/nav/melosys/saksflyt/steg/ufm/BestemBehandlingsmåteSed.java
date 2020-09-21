package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Collection;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsmaate;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.BESLUTNING_LOVVALG_NORGE;

@Component
public class BestemBehandlingsmåteSed implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(BestemBehandlingsmåteSed.class);

    private static final Collection<Behandlingstema> MANUELLE_BEHANDLINGSTEMA = Set.of(ANMODNING_OM_UNNTAK_HOVEDREGEL, BESLUTNING_LOVVALG_NORGE);

    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;

    @Autowired
    public BestemBehandlingsmåteSed(BehandlingsresultatService behandlingsresultatService,
                                    @Qualifier("system") OppgaveService oppgaveService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.BESTEM_BEHANDLINGMÅTE_SED;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        Behandling behandling = prosessinstans.getBehandling();
        final long behandlingID = behandling.getId();
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        if (skalOpprettesOppgave(behandling, behandlingsresultat)) {
            log.info("Oppretter oppgave for behandling {}", behandlingID);
            behandlingsresultatService.oppdaterBehandlingsMaate(behandlingID, Behandlingsmaate.DELVIS_AUTOMATISERT);
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                behandling,
                prosessinstans.hentJournalpostID(),
                behandling.getFagsak().hentBruker().getAktørId(),
                prosessinstans.hentSaksbehandlerHvisTilordnes()
            );
        } else {
            log.info("Behandling {} tema {} behandles automatisk", behandlingID, behandling.getTema());
            behandlingsresultatService.oppdaterBehandlingsMaate(behandlingID, Behandlingsmaate.AUTOMATISERT);
            behandleSedAutomatisk(prosessinstans);
        }
    }

    private void behandleSedAutomatisk(Prosessinstans prosessinstans) {
        //TODO opprett prosessinstans godkjent unntaksperiode (både ved A003, A009 og A010)
    }

    private boolean skalOpprettesOppgave(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        return MANUELLE_BEHANDLINGSTEMA.contains(behandling.getTema())
            || !behandlingsresultat.getKontrollresultater().isEmpty();
    }
}
