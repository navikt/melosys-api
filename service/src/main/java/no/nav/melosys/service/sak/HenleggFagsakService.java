package no.nav.melosys.service.sak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HenleggFagsakService {

    private final FagsakService fagsakService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final ProsessinstansService prosessinstansService;
    private final OppgaveService oppgaveService;

    public HenleggFagsakService(FagsakService fagsakService,
                                BehandlingsresultatService behandlingsresultatService,
                                ProsessinstansService prosessinstansService,
                                @Qualifier("system") OppgaveService oppgaveService) {
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.prosessinstansService = prosessinstansService;
        this.oppgaveService = oppgaveService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void henleggFagsak(String saksnummer, String begrunnelseKodeString, String fritekst) throws TekniskException, FunksjonellException {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);

        Henleggelsesgrunner begrunnelseKode;
        try {
            begrunnelseKode = Henleggelsesgrunner.valueOf(begrunnelseKodeString.toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new TekniskException(begrunnelseKodeString.toUpperCase() + " er ingen gyldig henleggelsesgrunn");
        }

        Behandling aktivBehandling = fagsak.hentAktivBehandling();
        oppdaterBehandlingsresultat(aktivBehandling.getId(), begrunnelseKode, fritekst);
        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.HENLAGT);
        prosessinstansService.opprettProsessinstansFagsakHenlagt(aktivBehandling);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(aktivBehandling.getFagsak().getSaksnummer());
    }

    private void oppdaterBehandlingsresultat(long behandlingID, Henleggelsesgrunner begrunnelseKode, String fritekst) throws IkkeFunnetException {
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
