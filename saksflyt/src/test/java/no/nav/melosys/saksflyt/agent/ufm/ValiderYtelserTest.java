package no.nav.melosys.saksflyt.agent.ufm;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.integrasjon.inntk.InntektService;
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
public class ValiderYtelserTest {

    @Mock
    private InntektService inntektService;
    @Mock
    private SaksopplysningRepository saksopplysningRepository;
    @Mock
    private AvklartefaktaService avklartefaktaService;

    private ValiderYtelser validerYtelser;

    @Before
    public void setUp() {
        validerYtelser = new ValiderYtelser(saksopplysningRepository, avklartefaktaService, inntektService);
    }

    @Test
    public void utførSteg_finnerIngenTreff_ingenNyAvklarteFakta() throws Exception {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(fom, tom)));
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(hentInntektSaksopplysning(false, fom));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerYtelser.utfør(prosessinstans);

        verify(avklartefaktaService, never()).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_STATSBORGERSKAP);
    }

    @Test
    public void utførSteg_finnerTreff_nyAvklarteFakta() throws Exception {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(fom, tom)));
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(hentInntektSaksopplysning(true, fom.minusMonths(1)));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerYtelser.utfør(prosessinstans);

        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_STATSBORGERSKAP);
    }

    @Test
    public void utførSteg_periodeFerdigIngenOffentligeYtelser_ingenNyAvklarteFakta() throws Exception {

        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = LocalDate.now().minusYears(1);

        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(fom, tom)));
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(hentInntektSaksopplysning(true, fom.minusYears(4)));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerYtelser.utfør(prosessinstans);

        verify(avklartefaktaService, never()).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_STATSBORGERSKAP);
    }

    @Test
    public void utførSteg_tomTilDato_forespørTomTilDato() throws Exception {

        LocalDate fom = LocalDate.now().minusYears(2);

        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(fom, null)));
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(hentInntektSaksopplysning(true, fom));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerYtelser.utfør(prosessinstans);

        verify(inntektService).hentInntektListe(anyString(),any(YearMonth.class), isNull());
        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_STATSBORGERSKAP);
    }

    @Test
    public void utførSteg_periodePåbegynt_verifiserInntektPeriode() throws Exception {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);

        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(fom, tom)));
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(hentInntektSaksopplysning(true, fom));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerYtelser.utfør(prosessinstans);

        verify(inntektService).hentInntektListe(anyString(),eq(YearMonth.from(fom.minusMonths(2))), eq(YearMonth.from(tom)));
        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_STATSBORGERSKAP);
    }

    @Test
    public void utførSteg_periodeIkkePåbegynt_verifiserInntektPeriode() throws Exception {
        LocalDate fom = LocalDate.now().plusYears(1);
        LocalDate tom = LocalDate.now().plusYears(2);

        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning(fom, tom)));
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(hentInntektSaksopplysning(true, fom));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerYtelser.utfør(prosessinstans);

        verify(inntektService).hentInntektListe(anyString(),eq(YearMonth.from(LocalDate.now().minusMonths(2))), eq(YearMonth.from(LocalDate.now())));
        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_STATSBORGERSKAP);
    }

    private Prosessinstans hentProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "123123");

        Behandling behandling = new Behandling();
        behandling.setId(2L);

        prosessinstans.setBehandling(behandling);
        return prosessinstans;
    }

    private Saksopplysning hentInntektSaksopplysning(boolean medYtelserFraOffentlig, LocalDate fom) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(hentInntektDokument(medYtelserFraOffentlig, fom));
        return saksopplysning;
    }

    private InntektDokument hentInntektDokument(boolean medYtelserFraOffentlig, LocalDate fom) {
        InntektDokument inntektDokument = new InntektDokument();

        inntektDokument.arbeidsInntektMaanedListe = new ArrayList<>();
        inntektDokument.arbeidsInntektMaanedListe.add(hentArbeidsInntektMaaned(medYtelserFraOffentlig, fom));

        return inntektDokument;
    }

    private ArbeidsInntektMaaned hentArbeidsInntektMaaned(boolean medYtelserFraOffentlig, LocalDate fom) {
        ArbeidsInntektMaaned arbeidsInntektMaaned = new ArbeidsInntektMaaned();
        arbeidsInntektMaaned.arbeidsInntektInformasjon = new ArbeidsInntektInformasjon();
        arbeidsInntektMaaned.arbeidsInntektInformasjon.inntektListe = hentInntektsListe(medYtelserFraOffentlig, fom);

        return arbeidsInntektMaaned;
    }

    private List<Inntekt> hentInntektsListe(boolean medYtelserFraOffentlig, LocalDate fom) {
        List<Inntekt> inntektsListe = new ArrayList<>();
        inntektsListe.add(new Inntekt());

        if (medYtelserFraOffentlig) {
            YtelseFraOffentlige ytelseFraOffentlige = new YtelseFraOffentlige();
            ytelseFraOffentlige.utbetaltIPeriode = YearMonth.from(fom).plusMonths(1);
            inntektsListe.add(ytelseFraOffentlige);
        }

        return inntektsListe;
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