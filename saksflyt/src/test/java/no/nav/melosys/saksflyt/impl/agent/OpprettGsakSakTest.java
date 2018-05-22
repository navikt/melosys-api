package no.nav.melosys.saksflyt.impl.agent;

import java.util.Properties;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettGsakSakTest {

    @Mock
    private Binge binge;

    @Mock
    private ProsessinstansRepository repo;

    @Mock
    private GsakFasade gsakFasade;

    private OpprettGsakSak agent;

    @Before
    public void setUp() {
        agent = new OpprettGsakSak(binge, repo, gsakFasade);
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

        agent.utfoerSteg(p);

        verify(gsakFasade, times(1)).opprettSak(saksnummer, BehandlingType.SØKNAD, aktørID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_OPPDATER_JOURNALPOST);
    }
}