package no.nav.melosys.service.eessi.ruting;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.SedInformasjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AdminInnvalideringSedRuter extends AdminSedRuter implements SedRuterForSedTyper {

    private static final Logger log = LoggerFactory.getLogger(AdminInnvalideringSedRuter.class);

    private final EessiService eessiService;
    private final OppgaveService oppgaveService;
    private final BehandlingService behandlingService;
    public AdminInnvalideringSedRuter(FagsakService fagsakService,
                                      ProsessinstansService prosessinstansService,
                                      OppgaveService oppgaveService,
                                      BehandlingsresultatService behandlingsresultatService,
                                      MedlPeriodeService medlPeriodeService,
                                      EessiService eessiService,
                                      BehandlingService behandlingService) {
        super(fagsakService,
            behandlingsresultatService,
            medlPeriodeService,
            prosessinstansService);

        this.eessiService = eessiService;
        this.oppgaveService = oppgaveService;
        this.behandlingService = behandlingService;
    }

    @Override
    public Collection<SedType> gjelderSedTyper() {
        return Set.of(SedType.X008);
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
        var sedDokument = sistAktiveBehandling.finnSedDokument();
        boolean aktivBehandlingErInvalidert = erAktivBehandlingInvalidert(sedDokument, arkivsakID);

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

        if (aktivBehandlingErInvalidert && (sistAktiveBehandling.erRegisteringAvUnntak() || sistAktiveBehandling.erAnmodningOmUnntak())) {
            annullerSakOgBehandling(sistAktiveBehandling);
            behandlingsresultatService.oppdaterBehandlingsresultattype(sistAktiveBehandling.getId(), Behandlingsresultattyper.HENLEGGELSE);
        } else {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(sistAktiveBehandling, melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId(), null);
        }
        opprettJournalføringProsess(melosysEessiMelding, sistAktiveBehandling);
    }

    private boolean erAktivBehandlingInvalidert(Optional<SedDokument> sedDokument, Long arkivsakID) {
        return sedDokument.filter(dokument -> eessiService.hentTilknyttedeBucer(arkivsakID, List.of())
            .stream()
            .filter(b -> b.getId().equals(dokument.getRinaSaksnummer()))
            .flatMap(b -> b.getSeder().stream())
            .filter(s -> s.getSedId().equals(dokument.getRinaDokumentID()))
            .anyMatch(SedInformasjon::erAvbrutt)).isPresent();
    }
}
