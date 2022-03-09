package no.nav.melosys.service.sak;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final Unleash unleash;

    public HenleggFagsakService(FagsakService fagsakService,
                                BehandlingsresultatService behandlingsresultatService,
                                ProsessinstansService prosessinstansService,
                                @Qualifier("system") OppgaveService oppgaveService,
                                BehandlingService behandlingService, Unleash unleash) {
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.prosessinstansService = prosessinstansService;
        this.oppgaveService = oppgaveService;
        this.behandlingService = behandlingService;
        this.unleash = unleash;
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

        Behandling aktivBehandling = fagsak.hentAktivBehandling();
        oppdaterBehandlingsresultat(aktivBehandling.getId(), begrunnelseKode, fritekst);
        if (aktivBehandling.erNyVurdering() && unleash.isEnabled("melosys.behandling.AVSLUTTE_UTEN_ENDRING")) {
            behandlingService.avsluttNyVurdering(aktivBehandling.getId(), Behandlingsresultattyper.HENLEGGELSE);
            prosessinstansService.opprettProsessinstansFagsakHenlagt(aktivBehandling);
        } else {
            henleggFagsakOgBehandling(fagsak);
        }
    }

    private void henleggFagsakOgBehandling(Fagsak fagsak) {
        Behandling aktivBehandling = fagsak.hentAktivBehandling();

        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.HENLAGT);
        prosessinstansService.opprettProsessinstansFagsakHenlagt(aktivBehandling);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(aktivBehandling.getFagsak().getSaksnummer());
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
        Behandling aktivBehandling = fagsak.hentAktivBehandling();

        if (aktivBehandling.erNyVurdering() && unleash.isEnabled("melosys.behandling.AVSLUTTE_UTEN_ENDRING") ) {
            behandlingService.avsluttNyVurdering(aktivBehandling.getId(), Behandlingsresultattyper.HENLEGGELSE_BORTFALT);
        } else {
            henleggSakSomBortfalt(fagsak);
        }
    }

    private void henleggSakSomBortfalt(Fagsak fagsak) {
        log.info("Fagsak {}: {}", fagsak.getSaksnummer(), Saksstatuser.HENLAGT_BORTFALT.getBeskrivelse());
        fagsak.getBehandlinger().forEach(behandling -> behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.getId(), Behandlingsresultattyper.HENLEGGELSE_BORTFALT));
        fagsak.getBehandlinger().forEach(behandling -> behandling.setStatus(Behandlingsstatus.AVSLUTTET));
        fagsak.setStatus(Saksstatuser.HENLAGT_BORTFALT);
        fagsakService.lagre(fagsak);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
    }
}
