package no.nav.melosys.saksflyt.agent.sob;

import java.util.HashSet;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.ProsessSteg.OPPFRISK_SAKSOPPLYSNINGER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentSakOgBehandlingSakerTest {

    @Mock
    private SakOgBehandlingFasade sakOgBehandlingFasade;

    private HentSakOgBehandlingSaker agent;

    @Before
    public void setUp() {
        agent = new HentSakOgBehandlingSaker(sakOgBehandlingFasade, mock(SaksopplysningRepository.class));
    }

    @Test
    public void utfoerSteg() throws IntegrasjonException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setSaksopplysninger(new HashSet<>());

        String aktørId = "test";
        p.setData(AKTØR_ID, aktørId);

        when(sakOgBehandlingFasade.finnSakOgBehandlingskjedeListe(any())).thenReturn(new Saksopplysning());

        agent.utførSteg(p);

        verify(sakOgBehandlingFasade, times(1)).finnSakOgBehandlingskjedeListe(aktørId);
        assertThat(p.getSteg()).isEqualTo(OPPFRISK_SAKSOPPLYSNINGER);
    }
}
