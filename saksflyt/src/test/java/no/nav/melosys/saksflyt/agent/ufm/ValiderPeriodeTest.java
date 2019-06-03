package no.nav.melosys.saksflyt.agent.ufm;

import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ValiderPeriodeTest {

    @Mock
    private AvklartefaktaService avklartefaktaService;

    private ValiderPeriode validerPeriode;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    @Before
    public void setUp() {
        validerPeriode = new ValiderPeriode(avklartefaktaService);
    }

    @Test
    public void utførSteg_gyldigPeriode_ingenNyAvklarteFakta() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(hentSedSaksopplysning(LocalDate.now().plusMonths(6), LocalDate.now().plusYears(2)));
        validerPeriode.utfør(prosessinstans);

        verify(avklartefaktaService, never()).leggTilRegistrering(anyLong(), any(), any());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
    }

    @Test
    public void utførSteg_ingenTilDato_nyAvklarteFakta() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(hentSedSaksopplysning(LocalDate.now().plusMonths(6), null));
        validerPeriode.utfør(prosessinstans);

        verify(avklartefaktaService, atLeastOnce()).leggTilRegistrering(anyLong(), any(Avklartefaktatype.class), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsExactly(
          Unntak_periode_begrunnelser.INGEN_SLUTTDATO.getKode()
        );
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
    }

    @Test
    public void utførSteg_tomFørFom_nyAvklarteFakta() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(hentSedSaksopplysning(LocalDate.now().plusYears(1), LocalDate.now()));
        validerPeriode.utfør(prosessinstans);

        verify(avklartefaktaService, atLeastOnce()).leggTilRegistrering(anyLong(), any(Avklartefaktatype.class), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsExactly(
            Unntak_periode_begrunnelser.FEIL_I_PERIODEN.getKode()
        );
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_BESTEM_BEHANDLINGSMAATE);
    }

    @Test
    public void utførSteg_periodeOver24Mnd_nyAvklarteFakta() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(hentSedSaksopplysning(LocalDate.now(), LocalDate.now().plusYears(3)));
        validerPeriode.utfør(prosessinstans);

        verify(avklartefaktaService, atLeastOnce()).leggTilRegistrering(anyLong(), any(Avklartefaktatype.class), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsExactly(
            Unntak_periode_begrunnelser.PERIODEN_OVER_24_MD.getKode()
        );
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
    }

    @Test
    public void utførSteg_periodeEldreEnn5År_nyAvklarteFakta() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(hentSedSaksopplysning(LocalDate.now().minusYears(6L), LocalDate.now().minusYears(5L)));
        validerPeriode.utfør(prosessinstans);

        verify(avklartefaktaService, atLeastOnce()).leggTilRegistrering(anyLong(), any(Avklartefaktatype.class), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsExactly(
            Unntak_periode_begrunnelser.PERIODE_FOR_GAMMEL.getKode()
        );
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
    }

    @Test
    public void utførSteg_periodeLangtFremITid_nyAvklarteFakta() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans(hentSedSaksopplysning(LocalDate.now().plusMonths(13L), LocalDate.now().plusYears(2L)));
        validerPeriode.utfør(prosessinstans);

        verify(avklartefaktaService, atLeastOnce()).leggTilRegistrering(anyLong(), any(Avklartefaktatype.class), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsExactly(
            Unntak_periode_begrunnelser.PERIODE_LANGT_FREM_I_TID.getKode()
        );
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
    }

    @Test
    public void utførSteg_norgeLovvalgsland_nyAvklarteFakta() throws Exception {
        Saksopplysning saksopplysning = hentSedSaksopplysning(LocalDate.now(), LocalDate.now().plusYears(1L));
        ((SedDokument)saksopplysning.getDokument()).setLovvalgslandKode(Landkoder.NO);
        Prosessinstans prosessinstans = hentProsessinstans(saksopplysning);
        validerPeriode.utfør(prosessinstans);

        verify(avklartefaktaService, atLeastOnce()).leggTilRegistrering(anyLong(), any(Avklartefaktatype.class), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsExactly(
            Unntak_periode_begrunnelser.LOVVALGSLAND_NORGE.getKode()
        );
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
    }

    private Prosessinstans hentProsessinstans(Saksopplysning saksopplysning) {
        Prosessinstans prosessinstans = new Prosessinstans();

        Behandling behandling = new Behandling();
        behandling.setId(2L);
        behandling.getSaksopplysninger().add(saksopplysning);

        prosessinstans.setBehandling(behandling);
        return prosessinstans;
    }

    private Saksopplysning hentSedSaksopplysning(LocalDate fom, LocalDate tom) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(hentSedDokument(fom, tom));
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        return saksopplysning;
    }

    private SedDokument hentSedDokument(LocalDate fom, LocalDate tom) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new no.nav.melosys.domain.dokument.medlemskap.Periode(fom, tom));
        return sedDokument;

    }
}