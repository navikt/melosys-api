package no.nav.melosys.tjenester.gui;

import no.nav.melosys.integrasjon.doksys.DokSysService;
import no.nav.melosys.integrasjon.doksys.DokSysSystemService;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.dokument.DokumentService;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevDataService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    DokumentService.class,
    DokumentSystemService.class,
    DokumentTjenesteTest.TestConfiguration.class
})
public class DokumentTjenesteTest {

    @Autowired
    private DokumentService dokumentService;

    @Test
    public void testAvhengighet() {
        assertThat(dokumentService).isNotNull();
        assertThat(dokumentService).isInstanceOf(DokumentService.class);
        assertThat(dokumentService).isNotInstanceOf(DokumentSystemService.class);
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        BehandlingRepository behandlingRepository() {
            return mock(BehandlingRepository.class);
        }

        @Bean
        BrevDataService brevDataService() {
            return mock(BrevDataService.class);
        }

        @Bean
        @Primary
        DokSysService dokSysService() {
            return mock(DokSysService.class);
        }

        @Bean
        @Qualifier("system")
        DokSysSystemService dokSysSystemService() {
            return mock(DokSysSystemService.class);
        }

        @Bean
        JoarkFasade joarkFasade() {
            return mock(JoarkFasade.class);
        }
    }
}
