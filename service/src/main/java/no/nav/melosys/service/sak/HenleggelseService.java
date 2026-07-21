package no.nav.melosys.service.sak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HenleggelseService {
    private static final Logger log = LoggerFactory.getLogger(HenleggelseService.class);

    private final FagsakService fagsakService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final ProsessinstansService prosessinstansService;
    private final OppgaveService oppgaveService;
    private final BehandlingService behandlingService;
    private final SkjemaSaksstatusSyncService skjemaSaksstatusSyncService;

    public HenleggelseService(FagsakService fagsakService,
                              BehandlingsresultatService behandlingsresultatService,
                              ProsessinstansService prosessinstansService,
                              OppgaveService oppgaveService,
                              BehandlingService behandlingService,
                              SkjemaSaksstatusSyncService skjemaSaksstatusSyncService) {
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.prosessinstansService = prosessinstansService;
        this.oppgaveService = oppgaveService;
        this.behandlingService = behandlingService;
        this.skjemaSaksstatusSyncService = skjemaSaksstatusSyncService;
    }

    /**
     * Henlegger fagsak. Dersom den forespurte behandlingen {@param behandlingID} er ny vurdering,
     * skal man ikke avslutte saken, men kun avslutte behandlingen. Behandlingsresultattype blir da HENLEGGELSE.
     */
    @Transactional
    public void henleggFagsakEllerBehandling(String saksnummer, String begrunnelseKodeString, String fritekst) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);

        Henleggelsesgrunner begrunnelseKode;
        try {
            begrunnelseKode = Henleggelsesgrunner.valueOf(begrunnelseKodeString.toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new TekniskException(begrunnelseKodeString.toUpperCase() + " er ingen gyldig henleggelsesgrunn");
        }

        Behandling aktivBehandling = fagsak.finnAktivBehandlingIkkeÅrsavregning();
        oppdaterBehandlingsresultat(aktivBehandling.getId(), begrunnelseKode, fritekst);

        if (fagsak.erEnesteBehandling(aktivBehandling.getId())) {
            fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.HENLAGT, SkjemaSaksstatusSynk.SYNKRONISER);
        } else {
            behandlingService.avsluttAndregangsbehandling(aktivBehandling.getId(), Behandlingsresultattyper.HENLEGGELSE);
            // Kun behandlingen lukkes (ingen fagsak-statusendring/event), men utledet skjema-status
            // avhenger også av om saken har aktiv behandling — bestill synk eksplisitt
            skjemaSaksstatusSyncService.bestillSynkHvisSkjemakoblet(saksnummer);
        }
        prosessinstansService.opprettProsessinstansFagsakHenlagt(aktivBehandling);
        oppgaveService.ferdigstillOppgaveMedBehandlingID(aktivBehandling.getId());
    }

    @Transactional
    public void henleggSakEllerBehandlingSomBortfalt(long behandlingID) {
        var behandling = behandlingService.hentBehandling(behandlingID);
        Fagsak fagsak = fagsakService.hentFagsak(behandling.getFagsak().getSaksnummer());

        if (fagsak.erEnesteBehandling(behandlingID)) {
            henleggSakSomBortfalt(fagsak);
        } else {
            henleggBehandlingSomBortfalt(behandling);
            // Kun behandlingen lukkes (ingen fagsak-statusendring/event), men utledet skjema-status
            // avhenger også av om saken har aktiv behandling — bestill synk eksplisitt
            skjemaSaksstatusSyncService.bestillSynkHvisSkjemakoblet(fagsak.getSaksnummer());
        }
    }

    private void henleggBehandlingSomBortfalt(Behandling behandling) {
        long behandlingId = behandling.getId();

        behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingId, Behandlingsresultattyper.HENLEGGELSE_BORTFALT);
        if (behandling.getStatus() != Behandlingsstatus.AVSLUTTET) behandlingService.avsluttBehandling(behandling.getId());
        oppgaveService.ferdigstillOppgaveMedBehandlingID(behandlingId);
    }

    private void henleggSakSomBortfalt(Fagsak fagsak) {
        log.info("Fagsak {}: {}", fagsak.getSaksnummer(), Saksstatuser.HENLAGT_BORTFALT.getBeskrivelse());
        fagsak.getBehandlinger().forEach(behandling -> {
            henleggBehandlingSomBortfalt(behandling);
        });
        fagsakService.oppdaterStatus(fagsak, Saksstatuser.HENLAGT_BORTFALT, SkjemaSaksstatusSynk.SYNKRONISER);
    }

    private void oppdaterBehandlingsresultat(long behandlingID, Henleggelsesgrunner begrunnelseKode, String fritekst) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        behandlingsresultat.setType(Behandlingsresultattyper.HENLEGGELSE);
        behandlingsresultat.setBegrunnelseFritekst(fritekst);

        BehandlingsresultatBegrunnelse begrunnelse = new BehandlingsresultatBegrunnelse();
        begrunnelse.setBehandlingsresultat(behandlingsresultat);
        begrunnelse.setKode(begrunnelseKode.getKode());
        behandlingsresultat.getBehandlingsresultatBegrunnelser().add(begrunnelse);

        behandlingsresultatService.lagre(behandlingsresultat);
    }
}
