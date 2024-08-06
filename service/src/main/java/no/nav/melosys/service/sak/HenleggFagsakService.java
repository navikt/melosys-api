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
public class HenleggFagsakService {
    private static final Logger log = LoggerFactory.getLogger(HenleggFagsakService.class);

    private final FagsakService fagsakService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final ProsessinstansService prosessinstansService;
    private final OppgaveService oppgaveService;
    private final BehandlingService behandlingService;

    public HenleggFagsakService(FagsakService fagsakService,
                                BehandlingsresultatService behandlingsresultatService,
                                ProsessinstansService prosessinstansService,
                                OppgaveService oppgaveService,
                                BehandlingService behandlingService) {
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.prosessinstansService = prosessinstansService;
        this.oppgaveService = oppgaveService;
        this.behandlingService = behandlingService;
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
        if (aktivBehandling.erAndregangsbehandling()) {
            behandlingService.avsluttAndregangsbehandling(aktivBehandling.getId(), Behandlingsresultattyper.HENLEGGELSE);
        } else {
            fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.HENLAGT);
        }
        prosessinstansService.opprettProsessinstansFagsakHenlagt(aktivBehandling);
        oppgaveService.ferdigstillOppgaveMedBehandlingID(aktivBehandling.getId());
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

    @Transactional
    public void henleggSakEllerBehandlingSomBortfalt(String saksnummer) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        Behandling aktivBehandling = fagsak.finnAktivBehandlingIkkeÅrsavregning();

        if (aktivBehandling.erAndregangsbehandling()) {
            henleggBehandlingSomBortfalt(aktivBehandling.getId());
        } else {
            henleggSakSomBortfalt(fagsak);
        }
    }

    private void henleggBehandlingSomBortfalt(long behandlingId) {
        behandlingService.avsluttAndregangsbehandling(behandlingId, Behandlingsresultattyper.HENLEGGELSE_BORTFALT);
        oppgaveService.ferdigstillOppgaveMedBehandlingID(behandlingId);
    }

    private void henleggSakSomBortfalt(Fagsak fagsak) {
        log.info("Fagsak {}: {}", fagsak.getSaksnummer(), Saksstatuser.HENLAGT_BORTFALT.getBeskrivelse());
        fagsak.getBehandlinger().forEach(behandling -> {
            behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.getId(), Behandlingsresultattyper.HENLEGGELSE_BORTFALT);
            oppgaveService.ferdigstillOppgaveMedBehandlingID(behandling.getId());
            if (behandling.getStatus() != Behandlingsstatus.AVSLUTTET) behandlingService.avsluttBehandling(behandling.getId());
        });
        fagsak.setStatus(Saksstatuser.HENLAGT_BORTFALT);
        fagsakService.lagre(fagsak);
    }
}
