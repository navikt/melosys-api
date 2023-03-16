package no.nav.melosys.service;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.mottatteopplysninger.AnmodningEllerAttest;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Utfallregistreringunntak.IKKE_GODKJENT;

@Service
public class UnntaksregistreringService {
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;

    public UnntaksregistreringService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService, OppgaveService oppgaveService, ProsessinstansService prosessinstansService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
    }

    @Transactional
    public void registrerUnntakFraMedlemskap(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        oppdaterBehandlingsresultatet(behandling, behandlingsresultat);

        var saksstatus = behandlingsresultat.getUtfallRegistreringUnntak() == IKKE_GODKJENT
            ? Saksstatuser.AVSLUTTET
            : Saksstatuser.LOVVALG_AVKLART;
        prosessinstansService.opprettProsessinstansRegistrerUnntakFraMedlemskap(behandling, saksstatus);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private void oppdaterBehandlingsresultatet(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        if (behandling.getMottatteOpplysninger().getMottatteOpplysningerData() instanceof AnmodningEllerAttest anmodningEllerAttest) {
            var fastsattAvLand = behandling.getFagsak().getType() == Sakstyper.TRYGDEAVTALE
                ? anmodningEllerAttest.getLovvalgsland()
                : anmodningEllerAttest.getAvsenderland();
            var type = behandlingsresultat.getUtfallRegistreringUnntak() == IKKE_GODKJENT
                ? Behandlingsresultattyper.FERDIGBEHANDLET
                : Behandlingsresultattyper.REGISTRERT_UNNTAK;

            behandlingsresultat.setFastsattAvLand(fastsattAvLand);
            behandlingsresultat.setType(type);
            behandlingsresultatService.lagre(behandlingsresultat);
        } else {
            throw new FunksjonellException("Unntaksregistrering er kun tilgjengelig for behandlinger med AnmodningEllerAttest. Det har ikke behandling " + behandling.getId());
        }
    }
}
