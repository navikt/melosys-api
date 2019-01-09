package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Collections;
import java.util.Properties;

import no.nav.melosys.audit.AuditorProvider;
import no.nav.melosys.domain.*;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.service.datavarehus.BehandlingOpprettetEvent;
import no.nav.melosys.service.datavarehus.FagsakOpprettetEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpprettFagsakOgBehandlingTest {

    @Mock
    private FagsakService fagsakService;

    @Mock
    private BehandlingService behandlingService;

    private OpprettFagsakOgBehandling agent;

    private ApplicationEventPublisher applicationEventPublisher;


    @Before
    public void setUp() {
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        AuditorProvider auditorAware = mock(AuditorProvider.class);
        agent = new OpprettFagsakOgBehandling(fagsakService, behandlingService, applicationEventPublisher, auditorAware);
    }

    @Test
    public void utførSteg_typeJfrNySak_tilStegJfrOpprettSøknad() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        Properties properties = new Properties();
        String aktørId = "FJERNET93";
        properties.setProperty(ProsessDataKey.AKTØR_ID.getKode(), "FJERNET93");
        properties.setProperty(ProsessDataKey.ARBEIDSGIVER.getKode(), "104568393");

        p.addData(properties);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-333");
        fagsak.setBehandlinger(Collections.singletonList(new Behandling()));
        when(fagsakService.nyFagsakOgBehandling(anyString(), anyString(), any(), eq(Behandlingstype.SØKNAD))).thenReturn(fagsak);

        agent.utførSteg(p);

        verify(fagsakService, times(1)).nyFagsakOgBehandling(aktørId, "104568393", null, Behandlingstype.SØKNAD);
        verify(applicationEventPublisher, times(1)).publishEvent(any(FagsakOpprettetEvent.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(BehandlingOpprettetEvent.class));

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_OPPRETT_SØKNAD);
    }

    @Test
    public void utførSteg_typeJfrNyBehandling_tilStegStatusBehOppr() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstype.SØKNAD);
        p.setData(ProsessDataKey.SAKSNUMMER, "MELTEST-333");

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-333");
        when(fagsakService.hentFagsak("MELTEST-333")).thenReturn(fagsak);
        when(behandlingService.nyBehandling(eq(fagsak), any(), any())).thenReturn(new Behandling());

        agent.utførSteg(p);

        verify(behandlingService, times(1)).nyBehandling(fagsak, Behandlingsstatus.VURDER_DOKUMENT, Behandlingstype.SØKNAD);
        verify(applicationEventPublisher, times(0)).publishEvent(any(FagsakOpprettetEvent.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(BehandlingOpprettetEvent.class));

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.STATUS_BEH_OPPR);
    }

    @Test
    public void utførSteg_ukjentType_feiler() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_KNYTT);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstype.SØKNAD);
        p.setData(ProsessDataKey.SAKSNUMMER, "MELTEST-333");

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }
}