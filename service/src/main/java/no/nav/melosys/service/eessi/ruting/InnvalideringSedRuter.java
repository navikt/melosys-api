package no.nav.melosys.service.eessi.ruting;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Component
public class InnvalideringSedRuter implements SedRuterForSedTyper {

    private static final Logger log = LoggerFactory.getLogger(InnvalideringSedRuter.class);

    private final FagsakService fagsakService;
    private final ProsessinstansService prosessinstansService;
    private final OppgaveService oppgaveService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;

    public InnvalideringSedRuter(FagsakService fagsakService,
                                 ProsessinstansService prosessinstansService,
                                 OppgaveService oppgaveService,
                                 BehandlingsresultatService behandlingsresultatService,
                                 MedlPeriodeService medlPeriodeService) {
        this.fagsakService = fagsakService;
        this.prosessinstansService = prosessinstansService;
        this.oppgaveService = oppgaveService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    public Collection<SedType> gjelderSedTyper() {
        return Set.of(SedType.X008);
    }

    @Override
    public void rutSedTilBehandling(Prosessinstans prosessinstans, Long arkivsakID) {

        final MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        Optional<Fagsak> fagsak = arkivsakID != null ? fagsakService.finnFagsakFraArkivsakID(arkivsakID) : Optional.empty();

        // arkivsakID == null -> opprett journalføringsoppgave
        if (fagsak.isEmpty()) {
            log.info("Oppretter jfr-oppgave for SED {} i RINA-sak {}", melosysEessiMelding.getSedId(), melosysEessiMelding.getRinaSaksnummer());
            oppgaveService.opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
            return;
        }

        var sistAktiveBehandling = fagsak.get().hentSistAktiveBehandling();

        if (sistAktiveBehandling.erRegisteringAvUnntak() || sistAktiveBehandling.erAnmodningOmUnntak()) {
            annullerSakOgBehandling(sistAktiveBehandling);
        } else {
            // oppdater oppgave med tekst
        }
        opprettJournalføringProsess(melosysEessiMelding, sistAktiveBehandling);

    }

    private void opprettJournalføringProsess(MelosysEessiMelding melosysEessiMelding, Behandling sistAktiveBehandling) {
        prosessinstansService.opprettProsessinstansSedJournalføring(
            sistAktiveBehandling,
            melosysEessiMelding
        );
    }

    private void annullerSakOgBehandling(Behandling behandling) {
        if (behandling.erAktiv()) {
            log.info("Behandling vil bli avsluttet og settes til annulert");
            fagsakService.avsluttFagsakOgBehandling(behandling.getFagsak(), Saksstatuser.AVSLUTTET); //FIXME: Saksstatuser.ANNULLERT
        } else {
            fagsakService.oppdaterStatus(behandling.getFagsak(),Saksstatuser.AVSLUTTET);//FIXME: ANNULLERT
            behandlingsresultatService.hentBehandlingsresultat(behandling.getId())
                .getLovvalgsperioder()
                .stream()
                .filter(l -> l.getMedlPeriodeID() != null)
                .findFirst()
                .ifPresent(l -> medlPeriodeService.avvisPeriodeOpphørt(l.getMedlPeriodeID()));
        }
    }
}
