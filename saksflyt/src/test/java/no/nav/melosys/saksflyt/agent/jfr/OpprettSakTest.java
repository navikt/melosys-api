package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Collections;
import java.util.Properties;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpprettSakTest {

    @Mock
    private FagsakService fagsakService;

    private OpprettSak agent;

    @Before
    public void setUp() {
        agent = new OpprettSak(fagsakService);
    }

    @Test
    public void utfoerSteg() throws SikkerhetsbegrensningException {
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
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_OPPRETT_GSAK_SAK);
    }
}