package no.nav.melosys.service;

import no.nav.melosys.integrasjon.regelmodul.RegelmodulFasade;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.repository.BehandlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.xml.parsers.ParserConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource")
@RunWith(MockitoJUnitRunner.class)
public class RegelmodulServiceTest {

    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private RegelmodulFasade regelmodulFasade;

    private RegelmodulService regelmodulService;

    @Before
    public void setUp() throws ParserConfigurationException {
        regelmodulService = new RegelmodulService(behandlingRepository, saksopplysningerService, regelmodulFasade);
    }

    @Test
    public void fastsettLovvalg_behandlingIkkeFunnet() {
        when(behandlingRepository.findWithSaksopplysningerById(0L)).thenReturn(null);

        FastsettLovvalgReply fastsettLovvalgReply = regelmodulService.fastsettLovvalg(0L);
        assertThat(fastsettLovvalgReply).isNull();
    }
}