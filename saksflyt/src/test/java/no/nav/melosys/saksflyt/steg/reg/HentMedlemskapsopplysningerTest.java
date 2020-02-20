package no.nav.melosys.saksflyt.steg.reg;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.HENT_SOB_SAKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HentMedlemskapsopplysningerTest {

    @Mock
    private RegisteropplysningerService registeropplysningerService;
    @Captor
    private ArgumentCaptor<RegisteropplysningerRequest> captor;

    private HentMedlemskapsopplysninger hentMedlemskapsopplysninger;

    @Before
    public void setUp() {
        hentMedlemskapsopplysninger = new HentMedlemskapsopplysninger(registeropplysningerService);
    }

    @Test
    public void utfoerSteg() throws MelosysException {
        final long behandlingID = 222L;
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);
        Periode periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(1));
        p.setData(ProsessDataKey.SØKNADSPERIODE, periode);

        hentMedlemskapsopplysninger.utførSteg(p);

        verify(registeropplysningerService).hentOgLagreOpplysninger(captor.capture());
        RegisteropplysningerRequest registeropplysningerRequest = captor.getValue();
        assertThat(captor.getValue().getBehandlingID()).isEqualTo(behandlingID);
        assertThat(captor.getValue().getFom()).isEqualTo(periode.getFom());
        assertThat(captor.getValue().getTom()).isEqualTo(periode.getTom());
        assertThat(registeropplysningerRequest.getOpplysningstyper()).containsExactly(SaksopplysningType.MEDL);

        assertThat(p.getSteg()).isEqualTo(HENT_SOB_SAKER);
    }

}
