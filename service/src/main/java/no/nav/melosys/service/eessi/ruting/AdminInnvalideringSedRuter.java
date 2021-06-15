package no.nav.melosys.service.eessi.ruting;

import java.util.*;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.SedInformasjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AdminInnvalideringSedRuter extends AdminSedRuter implements SedRuterForSedTyper {

    private static final Logger log = LoggerFactory.getLogger(AdminInnvalideringSedRuter.class);

    private final OppgaveService oppgaveService;
    private final EessiService eessiService;
    private final Unleash unleash;

    @Autowired
    public AdminInnvalideringSedRuter(FagsakService fagsakService,
                                      ProsessinstansService prosessinstansService,
                                      @Qualifier("system") OppgaveService oppgaveService,
                                      BehandlingsresultatService behandlingsresultatService,
                                      MedlPeriodeService medlPeriodeService, EessiService eessiService, Unleash unleash) {
        super(fagsakService,
            behandlingsresultatService,
            medlPeriodeService,
            prosessinstansService,
            log);

        this.oppgaveService = oppgaveService;
        this.eessiService = eessiService;
        this.unleash = unleash;
    }

    @Override
    public Collection<SedType> gjelderSedTyper() {
        if (unleash.isEnabled("melosys.sed.x008")) {
            return Set.of(SedType.X008);
        } else {
            return Collections.emptySet();
        }
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

        var sistAktiveBehandling = fagsak.get().hentSistAktiveBehandling();
        var sedDokument = sistAktiveBehandling.finnSedDokument();
        boolean aktivBehandlingErInvalidert = erAktivBehandlingInvalidert(sedDokument, arkivsakID);
        ;

        if (aktivBehandlingErInvalidert && (sistAktiveBehandling.erRegisteringAvUnntak() || sistAktiveBehandling.erAnmodningOmUnntak())) {
            annullerSakOgBehandling(sistAktiveBehandling);
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
