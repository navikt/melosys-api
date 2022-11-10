package no.nav.melosys.service.sak;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpprettBehandlingForSak {
    private final FagsakService fagsakService;
    private final ProsessinstansService prosessinstansService;
    private final SaksbehandlingRegler saksbehandlingRegler;

    public OpprettBehandlingForSak(FagsakService fagsakService,
                                   ProsessinstansService prosessinstansService,
                                   SaksbehandlingRegler saksbehandlingRegler) {
        this.fagsakService = fagsakService;
        this.prosessinstansService = prosessinstansService;
        this.saksbehandlingRegler = saksbehandlingRegler;
    }

    @Transactional
    public void opprettBehandling(String saksnummer, OpprettSakDto opprettSakDto) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);

        valider(fagsak, opprettSakDto);

        if (saksbehandlingRegler.skalTidligereBehandlingReplikeres(fagsak, opprettSakDto.getBehandlingstype(), opprettSakDto.getBehandlingstema())) {
            prosessinstansService.opprettOgReplikerBehandlingForSak(saksnummer, opprettSakDto);
        } else {
            prosessinstansService.opprettNyBehandlingForSak(saksnummer, opprettSakDto);
        }
    }

    private void valider(Fagsak fagsak, OpprettSakDto opprettSakDto) {
        if (opprettSakDto.getMottaksdato() == null) {
            throw new FunksjonellException("Mottaksdato er påkrevd for å opprette behandling");
        }
        if (StringUtils.isNotEmpty(opprettSakDto.getBehandlingsaarsakFritekst()) && opprettSakDto.getBehandlingsaarsakType() != Behandlingsaarsaktyper.FRITEKST) {
            throw new FunksjonellException("Kan ikke lagre fritekst som årsak når årsakstype er " + opprettSakDto.getBehandlingsaarsakType());
        }
        if (fagsak.hentAktivBehandling() != null) {
            throw new FunksjonellException(String.format("Det finnes allerede en aktiv behandling på fagsak %s", fagsak.getSaksnummer()));
        }
        if (opprettSakDto.getBehandlingstema() == null) {
            throw new FunksjonellException("Behandlingstema mangler");
        }
        if (opprettSakDto.getBehandlingstype() == null) {
            throw new FunksjonellException("Behandlingstype mangler");
        }
    }
}
