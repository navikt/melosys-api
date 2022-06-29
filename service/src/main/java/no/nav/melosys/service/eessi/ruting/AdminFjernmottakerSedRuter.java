package no.nav.melosys.service.eessi.ruting;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
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
    private final Unleash unleash;

    public AdminFjernmottakerSedRuter(FagsakService fagsakService,
                                      ProsessinstansService prosessinstansService,
                                      OppgaveService oppgaveService,
                                      BehandlingsresultatService behandlingsresultatService,
                                      MedlPeriodeService medlPeriodeService,
                                      Unleash unleash) {
        super(fagsakService,
            behandlingsresultatService,
            medlPeriodeService,
            prosessinstansService);

        this.oppgaveService = oppgaveService;
        this.unleash = unleash;

    }

    @Override
    public Collection<SedType> gjelderSedTyper() {
        if (unleash.isEnabled("melosys.sed.x006")) {
            return Collections.singleton(SedType.X006);
        } else {
            return Collections.emptySet();
        }
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

        if (melosysEessiMelding.isX006NavErFjernet()) {
            log.info("Nav er fjernet på sed {} i RINA-sak {}, og behandlingen vil bli avsluttet", melosysEessiMelding.getSedId(), melosysEessiMelding.getRinaSaksnummer());
            annullerSakOgBehandling(sistAktiveBehandling);
        } else {
            log.info("Mottakerinstitusjon på sed {} i RINA-sak {} er ikke Nav", melosysEessiMelding.getSedId(), melosysEessiMelding.getRinaSaksnummer());
        }

        opprettJournalføringProsess(melosysEessiMelding, sistAktiveBehandling);
    }
}
