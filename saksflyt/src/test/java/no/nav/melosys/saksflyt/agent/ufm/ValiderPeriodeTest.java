package no.nav.melosys.saksflyt.agent.ufm;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ValiderPeriodeTest {

    @Mock
    private SaksopplysningRepository saksopplysningRepository;
    @Mock
    private AvklartefaktaService avklartefaktaService;

    private ValiderPeriode validerPeriode;

    @Before
    public void setUp() {
        validerPeriode = new ValiderPeriode(saksopplysningRepository, avklartefaktaService);
    }

    @Test
    public void utførSteg_gyldigPeriode_ingenNyAvklarteFakta() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SEDOPPL)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now().plusYears(1), LocalDate.now().plusYears(2))));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerPeriode.utfør(prosessinstans);

        verify(avklartefaktaService, never()).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
    }

    @Test
    public void utførSteg_ingenTilDato_nyAvklarteFakta() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SEDOPPL)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now().plusYears(1), null)));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerPeriode.utfør(prosessinstans);

        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
    }

    @Test
    public void utførSteg_tomFørFom_nyAvklarteFakta() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SEDOPPL)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now().plusYears(1), LocalDate.now())));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerPeriode.utfør(prosessinstans);

        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_BESTEM_BEHANDLINGSMAATE);
    }

    @Test
    public void utførSteg_periodeOver24Mnd_nyAvklarteFakta() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SEDOPPL)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now(), LocalDate.now().plusYears(3))));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerPeriode.utfør(prosessinstans);

        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
    }

    @Test
    public void utførSteg_periodeEldreEnn5År_nyAvklarteFakta() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SEDOPPL)))
            .thenReturn(Optional.of(hentSedSaksopplysning(LocalDate.now().minusYears(6L), LocalDate.now().minusYears(5L))));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerPeriode.utfør(prosessinstans);

        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_VALIDER_MEDLEMSKAP);
    }

    private Prosessinstans hentProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();

        Behandling behandling = new Behandling();
        behandling.setId(2L);

        prosessinstans.setBehandling(behandling);
        return prosessinstans;
    }

    private Saksopplysning hentSedSaksopplysning(LocalDate fom, LocalDate tom) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(hentSedDokument(fom, tom));
        return saksopplysning;
    }

    private SedDokument hentSedDokument(LocalDate fom, LocalDate tom) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new no.nav.melosys.domain.dokument.medlemskap.Periode(fom, tom));
        return sedDokument;

    }
}