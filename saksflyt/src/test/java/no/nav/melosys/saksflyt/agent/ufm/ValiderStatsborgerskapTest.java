package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Arrays;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ValiderStatsborgerskapTest {

    @Mock
    private SaksopplysningRepository saksopplysningRepository;
    @Mock
    private AvklartefaktaService avklartefaktaService;

    private ValiderStatsborgerskap validerStatsborgerskap;

    @Before
    public void setUp() throws Exception {
        validerStatsborgerskap = new ValiderStatsborgerskap(saksopplysningRepository, avklartefaktaService);
    }

    @Test
    public void utførSteg_gyldigStatsborgerskap_ingenNyAvklarteFakta() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning("SE")));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerStatsborgerskap.utfør(prosessinstans);

        verify(avklartefaktaService, never()).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_BESTEM_BEHANDLINGSMAATE);
    }

    @Test
    public void utførSteg_flereGyldigeStatsborgerskap_ingenNyAvklarteFakta() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning("SE", "IT", "GB")));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerStatsborgerskap.utfør(prosessinstans);

        verify(avklartefaktaService, never()).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_BESTEM_BEHANDLINGSMAATE);
    }

    @Test
    public void utførSteg_ikkeGyldigStatsborgerskap_NyAvklarteFakta() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning("US")));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerStatsborgerskap.utfør(prosessinstans);

        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_BESTEM_BEHANDLINGSMAATE);
    }

    @Test
    public void utførSteg_bådeGyldigOgIkkeGyldigStatsborgerskap_NyAvklarteFakta() throws Exception {
        when(saksopplysningRepository.findByBehandlingAndType(any(Behandling.class), eq(SaksopplysningType.SED_OPPLYSNINGER)))
            .thenReturn(Optional.of(hentSedSaksopplysning("US", "SE")));

        Prosessinstans prosessinstans = hentProsessinstans();
        validerStatsborgerskap.utfør(prosessinstans);

        verify(avklartefaktaService, never()).leggTilAvklarteFakta(anyLong(), any(Avklartefaktatype.class), any(), any(), anyString());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_BESTEM_BEHANDLINGSMAATE);
    }

    private Prosessinstans hentProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "123123");

        Behandling behandling = new Behandling();
        behandling.setId(2L);

        prosessinstans.setBehandling(behandling);
        return prosessinstans;
    }

    private Saksopplysning hentSedSaksopplysning(String... landkoder) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(hentSedDokument(landkoder));
        return saksopplysning;
    }

    private SedDokument hentSedDokument(String... landkoder) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setStatsborgerskap(Arrays.asList(landkoder));
        return sedDokument;

    }
}