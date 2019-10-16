package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SendSedTest {

    @Mock
    private EessiService eessiService;

    private SendSed sendSed;

    @Before
    public void setup() {
        sendSed = new SendSed(eessiService);
    }

    @Test
    public void utfør() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        sendSed.utfør(prosessinstans);

        verify(eessiService).sendAnmodningUnntakSvar(anyLong());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_SVAR_OPPDATER_MEDL);
    }
}