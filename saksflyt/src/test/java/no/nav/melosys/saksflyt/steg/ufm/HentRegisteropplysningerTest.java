package no.nav.melosys.saksflyt.steg.ufm;

import java.time.LocalDate;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentRegisteropplysningerTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private RegisteropplysningerService registeropplysningerService;
    @Captor
    private ArgumentCaptor<RegisteropplysningerRequest> captor;

    private HentRegisteropplysninger hentRegisteropplysninger;

    private static final String FNR = "123";
    private static final String AKTØR_ID = "321";

    @Before
    public void setUp() throws Exception {
        hentRegisteropplysninger = new HentRegisteropplysninger(behandlingService, tpsFasade, new RegisteropplysningerFactory(), registeropplysningerService);

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setSaksopplysninger(Set.of(lagSedSaksopplysning(LocalDate.now(), LocalDate.now().plusYears(1))));

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn(FNR);
        doNothing().when(registeropplysningerService).hentOgLagreOpplysninger(any(RegisteropplysningerRequest.class));
    }

    @Test
    public void utfør() throws MelosysException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, AKTØR_ID);

        hentRegisteropplysninger.utfør(prosessinstans);

        assertThat(prosessinstans.getData(ProsessDataKey.BRUKER_ID)).isEqualTo(FNR);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_REGISTERKONTROLL);

        verify(tpsFasade).hentIdentForAktørId(AKTØR_ID);
        verify(behandlingService).hentBehandling(eq(1L));
        verify(registeropplysningerService).hentOgLagreOpplysninger(captor.capture());

        RegisteropplysningerRequest registeropplysningerRequest = captor.getValue();
        assertThat(registeropplysningerRequest.getFnr()).isEqualTo(FNR);
        assertThat(registeropplysningerRequest.getFom()).isEqualTo(LocalDate.now());
        assertThat(registeropplysningerRequest.getTom()).isEqualTo(LocalDate.now().plusYears(1));
        assertThat(registeropplysningerRequest.getOpplysningstyper()).containsExactlyInAnyOrder(
            SaksopplysningType.PERSOPL,
            SaksopplysningType.PERSHIST,
            SaksopplysningType.MEDL,
            SaksopplysningType.INNTK,
            SaksopplysningType.UTBETAL
        );
    }

    private Saksopplysning lagSedSaksopplysning(LocalDate fom, LocalDate tom) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new Periode(fom, tom));
        sedDokument.setFnr(FNR);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setKilde(SaksopplysningKilde.EESSI);
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(sedDokument);

        return saksopplysning;
    }
}