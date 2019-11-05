package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.SoeknadService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpprettSoeknadTest {

    @Mock
    private SoeknadService søknadService;

    private OpprettSoeknad opprettSoeknad;

    @Before
    public void setUp() {
        opprettSoeknad = new OpprettSoeknad(søknadService);
    }

    @Test
    public void utfoerSteg() throws IkkeFunnetException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        p.setBehandling(behandling);
        p.setType(ProsessType.JFR_NY_SAK);

        opprettSoeknad.utførSteg(p);

        verify(søknadService, times(1)).registrerSøknad(anyLong(), any(SoeknadDokument.class));
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_OPPRETT_GSAK_SAK);
    }
}