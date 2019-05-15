package no.nav.melosys.saksflyt.agent.ufm;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ValiderMedlemskapTest {

    @Mock
    private SaksopplysningRepository saksopplysningRepository;
    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private MedlFasade medlFasade;

    private ValiderMedlemskap validerMedlemskap;

    @Before
    public void setUp() throws Exception {
        validerMedlemskap = new ValiderMedlemskap(saksopplysningRepository,avklartefaktaService,medlFasade);
        when(medlFasade.hentPeriodeListe(anyString(),any(LocalDate.class), any()))
            .thenReturn(hentMedlemskapSaksopplysning());
    }

    @Test
    public void utførSteg_ikkeEndringOgIkkeOverlappendePerioder_forventIngenNyAvklarteFakta_1() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now().minusYears(2), LocalDate.now().minusYears(1))));

        Prosessinstans prosessinstans = hentProsessinstans(false);

        validerMedlemskap.utfør(prosessinstans);
        verify(avklartefaktaService, never()).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_YTELSER);
    }

    @Test
    public void utførSteg_ikkeEndringOgIkkeOverlappendePerioder_forventIngenNyAvklarteFakta_2() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now().plusYears(3), LocalDate.now().plusYears(5L))));

        Prosessinstans prosessinstans = hentProsessinstans(false);

        validerMedlemskap.utfør(prosessinstans);
        verify(avklartefaktaService, never()).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_YTELSER);
    }

    @Test
    public void utførSteg_ikkeEndringMedOverlappendePeriode_forventNyAvklarteFakta_1() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now(), LocalDate.now().plusYears(1))));

        Prosessinstans prosessinstans = hentProsessinstans(false);

        validerMedlemskap.utfør(prosessinstans);
        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_YTELSER);
    }

    @Test
    public void utførSteg_ikkeEndringMedOverlappendePeriode_forventNyAvklarteFakta_2() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now().plusYears(1), LocalDate.now().plusYears(5))));

        Prosessinstans prosessinstans = hentProsessinstans(false);

        validerMedlemskap.utfør(prosessinstans);
        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_YTELSER);
    }

    @Test
    public void utførSteg_ikkeEndringMedOverlappendePeriode_forventNyAvklarteFakta_3() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now().minusYears(1), LocalDate.now().plusYears(5))));

        Prosessinstans prosessinstans = hentProsessinstans(false);

        validerMedlemskap.utfør(prosessinstans);
        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_YTELSER);
    }

    @Test
    public void utførSteg_ikkeEndringMedOverlappendePeriode_forventNyAvklarteFakta_4() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))));

        Prosessinstans prosessinstans = hentProsessinstans(false);

        validerMedlemskap.utfør(prosessinstans);
        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_YTELSER);
    }

    @Test
    public void utførSteg_ikkeEndringMedOverlappendePeriodeOgTomErNull_forventNyAvklarteFakta() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now().minusYears(1), null)));

        Prosessinstans prosessinstans = hentProsessinstans(false);

        validerMedlemskap.utfør(prosessinstans);
        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_YTELSER);
    }

    @Test
    public void utførSteg_erEndringMedOverlappendePeriode_ingenNyAvklarteFakta() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))));

        Prosessinstans prosessinstans = hentProsessinstans(true);

        validerMedlemskap.utfør(prosessinstans);
        verify(avklartefaktaService, never()).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_YTELSER);
    }

    @Test
    public void utførSteg_erEndringIngenOverlappendePeriodeOgTomErNull_ingenNyAvklarteFakta() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now().plusYears(5), null)));

        Prosessinstans prosessinstans = hentProsessinstans(true);

        validerMedlemskap.utfør(prosessinstans);
        verify(avklartefaktaService, never()).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_YTELSER);
    }

    private Prosessinstans hentProsessinstans(boolean erEndring) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.ER_ENDRING, erEndring);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "123123");

        Behandling behandling = new Behandling();
        behandling.setId(2L);

        prosessinstans.setBehandling(behandling);
        return prosessinstans;
    }

    private MedlemskapDokument hentMedlemskapsDokument() {
        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();

        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));

        medlemskapDokument.medlemsperiode = Collections.singletonList(medlemsperiode);
        return medlemskapDokument;
    }

    private Saksopplysning hentMedlemskapSaksopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(hentMedlemskapsDokument());
        return saksopplysning;
    }

    private Saksopplysning hentSedSaksopplysning(LocalDate fom, LocalDate tom) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(hentSedDokument(fom, tom));
        return saksopplysning;
    }

    private SedDokument hentSedDokument(LocalDate fom, LocalDate tom) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setPeriode(new no.nav.melosys.domain.dokument.medlemskap.Periode(fom, tom));
        return sedDokument;

    }
}