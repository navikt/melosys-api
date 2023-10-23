package no.nav.melosys.service.eessi.ruting;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AdminFjernmottakerSedRuter extends AdminSedRuter implements SedRuterForSedTyper {
    private static final Logger log = LoggerFactory.getLogger(AdminFjernmottakerSedRuter.class);

    private final OppgaveService oppgaveService;
    private final BehandlingService behandlingService;

    public AdminFjernmottakerSedRuter(FagsakService fagsakService,
                                      ProsessinstansService prosessinstansService,
                                      OppgaveService oppgaveService,
                                      BehandlingsresultatService behandlingsresultatService,
                                      MedlPeriodeService medlPeriodeService,
                                      BehandlingService behandlingService) {
        super(fagsakService,
            behandlingsresultatService,
            medlPeriodeService,
            prosessinstansService);

        this.oppgaveService = oppgaveService;
        this.behandlingService = behandlingService;

    }

    @Override
    public Collection<SedType> gjelderSedTyper() {
        return Collections.singleton(SedType.X006);
    }

    @Override
    public void rutSedTilBehandling(Prosessinstans prosessinstans, Long arkivsakID) {
        final MelosysEessiMelding melosysEessiMelding = prosessinstans.hentMelosysEessiMelding();
        Optional<Fagsak> fagsak = hentFagsakDersomArkivsakIDEksisterer(arkivsakID);

        if (fagsak.isEmpty()) {
            log.info("Oppretter jfr-oppgave for SED {} i RINA-sak {}", melosysEessiMelding.getSedId(), melosysEessiMelding.getRinaSaksnummer());
            oppgaveService.opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
            return;
        }

        var sistAktiveBehandling = fagsak.get().hentSistAktivBehandling();

        if (sistAktiveBehandling.erNorgeUtpekt()) {
            if (sistAktiveBehandling.erAktiv()) {
                behandlingService.endreStatus(sistAktiveBehandling.getId(), Behandlingsstatus.VURDER_DOKUMENT);
                opprettJournalføringProsess(melosysEessiMelding, sistAktiveBehandling);
                return;
            } else {
                oppgaveService.opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
                return;
            }
        }

        if (melosysEessiMelding.isX006NavErFjernet()) {
            log.info("Nav er fjernet på sed {} i RINA-sak {}, og behandlingen vil bli avsluttet", melosysEessiMelding.getSedId(), melosysEessiMelding.getRinaSaksnummer());
            annullerSakOgBehandling(sistAktiveBehandling);
            behandlingsresultatService.oppdaterBehandlingsresultattype(sistAktiveBehandling.getId(), Behandlingsresultattyper.HENLEGGELSE);
        } else {
            log.info("Mottakerinstitusjon på sed {} i RINA-sak {} er ikke Nav", melosysEessiMelding.getSedId(), melosysEessiMelding.getRinaSaksnummer());
        }

        opprettJournalføringProsess(melosysEessiMelding, sistAktiveBehandling);
    }
}
