package no.nav.melosys.service.sak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpprettBehandlingForSak {
    private final FagsakService fagsakService;
    private final ProsessinstansService prosessinstansService;
    private final SaksbehandlingRegler saksbehandlingRegler;
    private final LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;

    public OpprettBehandlingForSak(FagsakService fagsakService,
                                   ProsessinstansService prosessinstansService,
                                   SaksbehandlingRegler saksbehandlingRegler,
                                   LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService,
                                   BehandlingService behandlingService,
                                   BehandlingsresultatService behandlingsresultatService) {
        this.fagsakService = fagsakService;
        this.prosessinstansService = prosessinstansService;
        this.saksbehandlingRegler = saksbehandlingRegler;
        this.lovligeKombinasjonerSaksbehandlingService = lovligeKombinasjonerSaksbehandlingService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Transactional
    public void opprettBehandling(String saksnummer, OpprettSakDto opprettSakDto) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        final Behandling sistBehandling = fagsak.hentSistRegistrertBehandling();
        final Behandlingsresultat sistBehandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(sistBehandling.getId());
        var behandlingstema = opprettSakDto.getBehandlingstema();
        var behandlingstype = opprettSakDto.getBehandlingstype();

        valider(fagsak, opprettSakDto);
        lovligeKombinasjonerSaksbehandlingService.validerBehandlingstemaOgBehandlingstypeForAndregangsbehandling(fagsak, sistBehandling, sistBehandlingsresultat, behandlingstema, behandlingstype);

        if (sistBehandling.erAktiv()) {
            behandlingService.avsluttBehandling(sistBehandling.getId());
        }

        if (saksbehandlingRegler.skalTidligereBehandlingReplikeres(fagsak, behandlingstype, behandlingstema)) {
            prosessinstansService.opprettOgReplikerBehandlingForSak(saksnummer, opprettSakDto.tilOpprettSakRequest());
        } else {
            prosessinstansService.opprettNyBehandlingForSak(saksnummer, opprettSakDto.tilOpprettSakRequest());
        }
    }

    private void valider(Fagsak fagsak, OpprettSakDto opprettSakDto) {
        if (opprettSakDto.getBehandlingstema() == null) {
            throw new FunksjonellException("Behandlingstema mangler");
        }
        if (opprettSakDto.getBehandlingstype() == null) {
            throw new FunksjonellException("Behandlingstype mangler");
        }

        var muligeBehandlingstyper =
            lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyperForKnyttTilSak(Aktoersroller.BRUKER, fagsak.getSaksnummer(), opprettSakDto.getBehandlingstema());

        if (!muligeBehandlingstyper.contains(opprettSakDto.getBehandlingstype())){
            throw new FunksjonellException(String.format("Behandlingstype %s er ikke lovlig for behandlingstema %s og saksnummer %s", opprettSakDto.getBehandlingstype(), opprettSakDto.getBehandlingstema(), fagsak.getSaksnummer()));
        }

        if (opprettSakDto.getMottaksdato() == null) {
            throw new FunksjonellException("Mottaksdato er påkrevd for å opprette behandling");
        }
        if (opprettSakDto.getBehandlingsaarsakType() == null) {
            throw new FunksjonellException("Årsak er påkrevd for å opprette behandling");
        }
        if (StringUtils.isNotEmpty(opprettSakDto.getBehandlingsaarsakFritekst()) && opprettSakDto.getBehandlingsaarsakType() != Behandlingsaarsaktyper.FRITEKST) {
            throw new FunksjonellException("Kan ikke lagre fritekst som årsak når årsakstype er " + opprettSakDto.getBehandlingsaarsakType());
        }
    }
}
