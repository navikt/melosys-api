package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Properties;

import no.nav.melosys.domain.*;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.FagsakRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpprettGsakSakTest {

    @Mock
    private GsakFasade gsakFasade;

    @Mock
    FagsakRepository fagsakRepository;

    private OpprettGsakSak agent;

    @Before
    public void setUp() {
        agent = new OpprettGsakSak(gsakFasade, fagsakRepository);
    }

    @Test
    public void utfoerSteg() {
        Prosessinstans p = new Prosessinstans();
        Properties properties = new Properties();
        String saksnummer = "MEL-009";
        properties.setProperty(ProsessDataKey.SAKSNUMMER.getKode(), saksnummer);
        String aktørID = "FJERNET93";
        properties.setProperty(ProsessDataKey.AKTØR_ID.getKode(), aktørID);
        p.addData(properties);
        when(gsakFasade.opprettSak(anyString(), eq(BehandlingType.SØKNAD), anyString())).thenReturn("GSAK-123");

        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(null);
        when(fagsakRepository.findBySaksnummer(any())).thenReturn(fagsak);

        agent.utførSteg(p);

        verify(gsakFasade, times(1)).opprettSak(saksnummer, BehandlingType.SØKNAD, aktørID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.STATUS_BEH_OPPR);
        Assert.notNull(fagsak.getGsakSaksnummer(), "Fagsak skal ha fått Gsak saksnummert");
    }
}