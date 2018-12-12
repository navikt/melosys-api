package no.nav.melosys.saksflyt.agent.iv;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import no.nav.melosys.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.domain.ProsessSteg.FEILET_MASKINELT;
import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_RESULTAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IverksettVedtakValideringTest {

    private IverksettVedtakValidering agent;

    private Prosessinstans p;

    @Before
    public void setUp() {
        agent = new IverksettVedtakValidering();

        p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        p.setData(ProsessDataKey.SAKSBEHANDLER, "Z999");

        p.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, BehandlingsresultatType.FASTSATT_LOVVALGSLAND.getKode());

    }

    @Test
    public void utfoerSteg() {
        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(IV_OPPDATER_RESULTAT);
    }

    @Test
    public void utfoerSteg_feilProsessType() {
        p.setType(ProsessType.OPPFRISKNING);

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public void utfoerSteg_manglerBehandlingsresultatType() {
        p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        p.setData(ProsessDataKey.SAKSBEHANDLER, "Z999");

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        root.addAppender(mockAppender);

        agent.utførSteg(p);

        verify(mockAppender, times(3)).doAppend(argThat(o -> ((LoggingEvent)o).getFormattedMessage().contains("behandlingsResultatType er ikke oppgitt")));
        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

}