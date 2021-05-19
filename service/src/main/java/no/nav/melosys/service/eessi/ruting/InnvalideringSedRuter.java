package no.nav.melosys.service.eessi.ruting;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.eessi.SedInformasjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InnvalideringSedRuter implements SedRuterForSedTyper {

    private static final Logger log = LoggerFactory.getLogger(InnvalideringSedRuter.class);

    private final FagsakService fagsakService;
    private final ProsessinstansService prosessinstansService;
    private final OppgaveService oppgaveService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;
    private final EessiService eessiService;

    public InnvalideringSedRuter(FagsakService fagsakService,
                                 ProsessinstansService prosessinstansService,
                                 OppgaveService oppgaveService,
                                 BehandlingsresultatService behandlingsresultatService,
                                 MedlPeriodeService medlPeriodeService, EessiService eessiService) {
        this.fagsakService = fagsakService;
        this.prosessinstansService = prosessinstansService;
        this.oppgaveService = oppgaveService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
        this.eessiService = eessiService;
    }

    @Override
    public Collection<SedType> gjelderSedTyper() {
        return Set.of(SedType.X008);
    }

    @Override
    public void rutSedTilBehandling(Prosessinstans prosessinstans, Long arkivsakID) {

        final MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        Optional<Fagsak> fagsak = arkivsakID != null ? fagsakService.finnFagsakFraArkivsakID(arkivsakID) : Optional.empty();

        if (fagsak.isEmpty()) {
            log.info("Oppretter jfr-oppgave for SED {} i RINA-sak {}", melosysEessiMelding.getSedId(), melosysEessiMelding.getRinaSaksnummer());
            oppgaveService.opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
            return;
        }

        var sistAktiveBehandling = fagsak.get().hentSistAktiveBehandling();

        var sedDokument = sistAktiveBehandling.finnSedDokument();

        boolean aktivBehandlingErInvalidert = false;
        if (sedDokument.isPresent()) {
            aktivBehandlingErInvalidert = eessiService.hentTilknyttedeBucer(arkivsakID, List.of())
                .stream()
                .filter(b -> b.getId().equals(sedDokument.get().getRinaSaksnummer()))
                .flatMap(b -> b.getSeder().stream())
                .filter(s -> s.getSedId().equals(sedDokument.get().getRinaDokumentID()))
                .anyMatch(SedInformasjon::erAvbrutt);
        }

        if (aktivBehandlingErInvalidert && (sistAktiveBehandling.erRegisteringAvUnntak() || sistAktiveBehandling.erAnmodningOmUnntak())) {
            annullerSakOgBehandling(sistAktiveBehandling);
        } else {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(sistAktiveBehandling, melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId(), null);
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
        Optional<? extends PeriodeOmLovvalg> periodeOmLovvalgMedMedlPeriode;
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());

        if (behandling.erAktiv()) {
            log.info("Behandling {} vil bli avsluttet og status settes til annullert", behandling.getId());
            fagsakService.avsluttFagsakOgBehandling(behandling.getFagsak(), Saksstatuser.AVSLUTTET); //FIXME: Saksstatuser.ANNULLERT
            periodeOmLovvalgMedMedlPeriode = behandlingsresultat.finnValidertAnmodningsperiode().filter(a -> a.getMedlPeriodeID() != null);
        } else {
            log.info("Saksstatus settes til annullert for behandling {}", behandling.getId());
            fagsakService.oppdaterStatus(behandling.getFagsak(),Saksstatuser.AVSLUTTET);//FIXME: ANNULLERT
            periodeOmLovvalgMedMedlPeriode = behandlingsresultatService.hentBehandlingsresultat(behandling.getId())
                .finnValidertLovvalgsperiode()
                .filter(l -> l.getMedlPeriodeID() != null);
        }

        periodeOmLovvalgMedMedlPeriode.ifPresent(periode -> medlPeriodeService.avvisPeriodeOpphørt(periode.getMedlPeriodeID()));
    }
}
