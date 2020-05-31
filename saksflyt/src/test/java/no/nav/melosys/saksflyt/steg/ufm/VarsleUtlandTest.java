package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VarsleUtlandTest {
    @Mock
    private EessiService eessiService;
    @Mock
    private SaksopplysningerService saksopplysningerService;

    private VarsleUtland varsleUtland;

    @Before
    public void setUp() {
        varsleUtland = new VarsleUtland(eessiService, saksopplysningerService);
    }

    @Test
    public void varsleUtland_skalVarslesOgRettBehandlingstype_forventSedSendt() throws MelosysException {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setData(ProsessDataKey.VARSLE_UTLAND, true);
        when(saksopplysningerService.hentSedOpplysninger(anyLong())).thenReturn(lagSedDokument(true));

        varsleUtland.utfør(prosessinstans);

        verify(saksopplysningerService).hentSedOpplysninger(anyLong());
        verify(eessiService).sendGodkjenningArbeidFlereLand(anyLong(), isNull());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void varsleUtland_sedDokumentIkkeElektronisk_forventException() throws MelosysException {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setData(ProsessDataKey.VARSLE_UTLAND, true);
        when(saksopplysningerService.hentSedOpplysninger(anyLong())).thenReturn(lagSedDokument(false));

        varsleUtland.utfør(prosessinstans);
    }

    @Test
    public void varsleUtland_sendA012IkkeValgtAvSaksbehandler_forventIngenSedSendt() throws MelosysException {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setData(ProsessDataKey.VARSLE_UTLAND, false);

        varsleUtland.utfør(prosessinstans);

        verify(saksopplysningerService, never()).hentSedOpplysninger(anyLong());
        verify(eessiService, never()).sendGodkjenningArbeidFlereLand(anyLong(), isNull());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING);
    }

    @Test
    public void varsleUtland_utlandIkkeUtpekt_forventIngenSedSendt() throws MelosysException {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.getBehandling().setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);

        varsleUtland.utfør(prosessinstans);

        verify(saksopplysningerService, never()).hentSedOpplysninger(anyLong());
        verify(eessiService, never()).sendGodkjenningArbeidFlereLand(anyLong(), isNull());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_AVSLUTT_BEHANDLING);
    }

    private static Prosessinstans lagProsessinstans() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        return prosessinstans;
    }

    private static SedDokument lagSedDokument(boolean erElektronisk) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setErElektronisk(erElektronisk);
        return sedDokument;
    }
}