package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsnotat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.BehandlingsnotatService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import no.nav.melosys.tjenester.gui.dto.BehandlingsnotatGetDto;
import no.nav.melosys.tjenester.gui.dto.BehandlingsnotatPostDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BehandlingsnotatTjenesteTest extends JsonSchemaTestParent {

    private static final String BEHANDLINGNOTAT_GET_SCHEMA = "fagsaker-notater-schema.json";
    private static final String BEHANDLINGNOTAT_POST_SCHEMA = "fagsaker-notater-post-schema.json";
    private static final String BEHANDLINGNOTAT_PUT_SCHEMA = "fagsaker-notater-put-schema.json";

    private final String saksbehandler = "Z224234";
    private final String saksbehandlerNavn = "Morteni Mortenseni";

    @Mock
    private BehandlingsnotatService behandlingsnotatService;
    @Mock
    private SaksbehandlerService saksbehandlerService;
    @Mock
    private TilgangService tilgangService;

    private BehandlingsnotatTjeneste behandlingsnotatTjeneste;

    @BeforeEach
    public void setup() {
        behandlingsnotatTjeneste = new BehandlingsnotatTjeneste(behandlingsnotatService, saksbehandlerService, tilgangService);
        when(saksbehandlerService.finnNavnForIdent(eq(saksbehandler))).thenReturn(Optional.of(saksbehandlerNavn));
    }

    @Test
    public void hentBehandlingsnotaterForFagsak_hentes_validerSchema() throws IOException {

        final String saksnummer = "MEL-222";
        Behandlingsnotat behandlingsnotat = lagBehandlingsnotat();
        when(behandlingsnotatService.hentNotatForFagsak(saksnummer)).thenReturn(List.of(behandlingsnotat));

        ResponseEntity res = behandlingsnotatTjeneste.hentBehandlingsnotaterForFagsak(saksnummer);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isInstanceOf(List.class);
        assertThat(((List) res.getBody())).hasSize(1);

        BehandlingsnotatGetDto dto = (BehandlingsnotatGetDto) ((List) res.getBody()).get(0);
        assertThat(dto.getEndretDato()).isEqualTo(behandlingsnotat.getEndretDato());
        assertThat(dto.getNotatId()).isEqualTo(behandlingsnotat.getId());
        assertThat(dto.getRegistrertAvNavn()).isEqualTo(saksbehandlerNavn);
        assertThat(dto.getTekst()).isEqualTo(behandlingsnotat.getTekst());


        validerArray((Collection) res.getBody(), BEHANDLINGNOTAT_GET_SCHEMA);
    }

    @Test
    public void oppdaterBehandlingsnotat_blirOppdatert_validerSchema() throws IOException {

        BehandlingsnotatPostDto req = new BehandlingsnotatPostDto();
        req.setTekst("teteteksssst");
        valider(req, BEHANDLINGNOTAT_PUT_SCHEMA);

        final String saksnummer = "MEL-222";
        Behandlingsnotat behandlingsnotat = lagBehandlingsnotat();
        when(behandlingsnotatService.oppdaterNotat(eq(behandlingsnotat.getId()), anyString())).thenReturn(behandlingsnotat);

        ResponseEntity res = behandlingsnotatTjeneste.oppdaterBehandlingsnotat(saksnummer, 1L, req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isInstanceOf(BehandlingsnotatGetDto.class);
    }

    @Test
    public void opprettBehandlingsnotat_blirOpprettet_validerSchema() throws IOException {

        BehandlingsnotatPostDto req = new BehandlingsnotatPostDto();
        req.setTekst("teteteksssst");
        valider(req, BEHANDLINGNOTAT_POST_SCHEMA);

        final String saksnummer = "MEL-222";
        Behandlingsnotat behandlingsnotat = lagBehandlingsnotat();
        when(behandlingsnotatService.opprettNotat(eq(saksnummer), anyString())).thenReturn(behandlingsnotat);

        ResponseEntity res = behandlingsnotatTjeneste.opprettBehandlingsnotatForFagsak(saksnummer, req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isInstanceOf(BehandlingsnotatGetDto.class);
    }

    private Behandlingsnotat lagBehandlingsnotat() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setType(Behandlingstyper.SED);
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);

        Behandlingsnotat behandlingsnotat = new Behandlingsnotat();
        behandlingsnotat.setTekst("hei");
        behandlingsnotat.setRegistrertAv(saksbehandler);
        behandlingsnotat.setEndretAv(saksbehandler);
        behandlingsnotat.setBehandling(behandling);
        behandlingsnotat.setId(1L);
        behandlingsnotat.setEndretDato(Instant.now());
        behandlingsnotat.setRegistrertDato(Instant.now());
        behandlingsnotat.setBehandling(behandling);

        return behandlingsnotat;
    }
}
