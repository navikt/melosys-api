package no.nav.melosys.saksflyt.steg.sob;

import java.util.HashSet;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPFRISK_SAKSOPPLYSNINGER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentSakOgBehandlingSakerTest {

    @Mock
    private RegisteropplysningerService registeropplysningerService;
    @Mock
    private TpsFasade tpsFasade;

    private HentSakOgBehandlingSaker hentSakOgBehandlingSaker;

    @Captor
    private ArgumentCaptor<RegisteropplysningerRequest> captor;

    @Before
    public void setUp() {
        hentSakOgBehandlingSaker = new HentSakOgBehandlingSaker(registeropplysningerService, tpsFasade);
    }

    @Test
    public void utfoerSteg() throws MelosysException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        p.setBehandling(behandling);
        p.getBehandling().setSaksopplysninger(new HashSet<>());

        String aktørId = "test";
        p.setData(AKTØR_ID, aktørId);

        when(tpsFasade.hentIdentForAktørId(eq(aktørId))).thenReturn("fnr");

        hentSakOgBehandlingSaker.utfør(p);

        verify(registeropplysningerService).hentOgLagreOpplysninger(captor.capture());

        RegisteropplysningerRequest registeropplysningerRequest = captor.getValue();
        assertThat(registeropplysningerRequest.getFnr()).isEqualTo("fnr");
        assertThat(registeropplysningerRequest.getBehandlingID()).isEqualTo(1L);
        assertThat(registeropplysningerRequest.getOpplysningstyper().size()).isEqualTo(1);
        assertThat(registeropplysningerRequest.getOpplysningstyper().iterator().next()).isEqualTo(SaksopplysningType.SOB_SAK);
        assertThat(p.getSteg()).isEqualTo(OPPFRISK_SAKSOPPLYSNINGER);
    }
}
