package no.nav.melosys.service.eessi.ruting;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class AdminFjernmottakerSedRuter extends AdminSedRuter implements SedRuterForSedTyper {
    private static final Logger log = LoggerFactory.getLogger(AdminFjernmottakerSedRuter.class);

    private final OppgaveService oppgaveService;
    private final String institusjonKode = "NO";

    public AdminFjernmottakerSedRuter(FagsakService fagsakService,
                                      ProsessinstansService prosessinstansService,
                                      OppgaveService oppgaveService,
                                      BehandlingsresultatService behandlingsresultatService,
                                      MedlPeriodeService medlPeriodeService) {
        super(fagsakService,
            behandlingsresultatService,
            medlPeriodeService,
            prosessinstansService);

        this.oppgaveService = oppgaveService;
    }

    @Override
    public Collection<SedType> gjelderSedTyper() {
        return Collections.singleton(SedType.X006);
    }

    @Override
    public void rutSedTilBehandling(Prosessinstans prosessinstans, Long arkivsakID) {
        final MelosysEessiMelding melosysEessiMelding = hentMelosysEessiMelding(prosessinstans);
        Optional<Fagsak> fagsak = hentFagsakDersomArkivsakIDEksisterer(arkivsakID);

        if (fagsak.isEmpty()) {
            log.info("Oppretter jfr-oppgave for SED {} i RINA-sak {}", melosysEessiMelding.getSedId(), melosysEessiMelding.getRinaSaksnummer());
            oppgaveService.opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
            return;
        }

        Optional<Institusjon> mottakerInstitusjonFraMelding = Optional.ofNullable(melosysEessiMelding.getInstitusjon());
        var sistAktiveBehandling = fagsak.get().hentSistAktiveBehandling();

        if (mottakerInstitusjonFraMelding.isEmpty()) {
            log.info("Mottakerinstitusjon på sed {} i RINA-sak {} er null", melosysEessiMelding.getSedId(), melosysEessiMelding.getRinaSaksnummer());
        } else {
            if (mottakerInstitusjonFraMelding.get().getId().split(":")[0].equals(institusjonKode)) {
                annullerSakOgBehandling(sistAktiveBehandling);
            } else {
                log.info("Mottakerinstitusjon på sed {} i RINA-sak {} er ikke norsk", melosysEessiMelding.getSedId(), melosysEessiMelding.getRinaSaksnummer());
            }
        }
        opprettJournalføringProsess(melosysEessiMelding, sistAktiveBehandling);
    }

    private void annullerSakOgBehandling(Behandling behandling) {
        if (behandling.erAktiv()) {
            log.info("Behandling {} vil bli avsluttet og status settes til annullert", behandling.getId());
            avsluttBehandlingOgReturnerMedlPeriodeFraAnmodningsperiode(behandling);
        } else {
            log.info("Saksstatus settes til annullert for behandling {}", behandling.getId());
            oppdaterStatusOgReturnerMedlPeriodeFraLovvalgsperiode(behandling);
        }
    }
}
