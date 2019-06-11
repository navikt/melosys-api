package no.nav.melosys.saksflyt.agent.ufm;

import java.time.LocalDate;
import java.time.YearMonth;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HentInntektOpplysningerTest {

    @Mock
    private InntektService inntektService;
    @Mock
    private SaksopplysningRepository saksopplysningRepository;
    @Mock
    private BehandlingRepository behandlingRepository;

    private HentInntektOpplysninger hentInntektOpplysninger;

    @Before
    public void setUp() throws Exception {
        hentInntektOpplysninger = new HentInntektOpplysninger(inntektService, saksopplysningRepository, behandlingRepository);
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(new Saksopplysning());
    }

    @Test
    public void utførSteg_tomTilDato_forespørTomTilDato() throws Exception {

        LocalDate fom = LocalDate.now().minusYears(2);

        Prosessinstans prosessinstans = hentProsessinstans(hentSedSaksopplysning(fom, null));
        when(behandlingRepository.findWithSaksopplysningerById(anyLong())).thenReturn(prosessinstans.getBehandling());
        hentInntektOpplysninger.utfør(prosessinstans);

        verify(inntektService).hentInntektListe(anyString(),eq(YearMonth.from(fom)), eq(YearMonth.from(fom.plusYears(2))));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_REGISTERKONTROLL);
    }

    @Test
    public void utførSteg_periodePåbegynt_verifiserInntektPeriode() throws Exception {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);

        Prosessinstans prosessinstans = hentProsessinstans(hentSedSaksopplysning(fom, tom));
        when(behandlingRepository.findWithSaksopplysningerById(anyLong())).thenReturn(prosessinstans.getBehandling());
        hentInntektOpplysninger.utfør(prosessinstans);

        verify(inntektService).hentInntektListe(anyString(),eq(YearMonth.from(fom.minusMonths(2))), eq(YearMonth.from(tom)));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_REGISTERKONTROLL);
    }

    @Test
    public void utførSteg_periodeIkkePåbegynt_verifiserInntektPeriode() throws Exception {
        LocalDate fom = LocalDate.now().plusYears(1);
        LocalDate tom = LocalDate.now().plusYears(2);

        Prosessinstans prosessinstans = hentProsessinstans(hentSedSaksopplysning(fom, tom));
        when(behandlingRepository.findWithSaksopplysningerById(anyLong())).thenReturn(prosessinstans.getBehandling());
        hentInntektOpplysninger.utfør(prosessinstans);

        verify(inntektService).hentInntektListe(anyString(),eq(YearMonth.from(LocalDate.now().minusMonths(2))), eq(YearMonth.from(LocalDate.now())));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_REGISTERKONTROLL);
    }

    @Test
    public void utførSteg_periodeAvsluttet_verifiserInntektPeriode() throws Exception {
        LocalDate fom = LocalDate.now().minusYears(3);
        LocalDate tom = LocalDate.now().minusYears(2);

        Prosessinstans prosessinstans = hentProsessinstans(hentSedSaksopplysning(fom, tom));
        when(behandlingRepository.findWithSaksopplysningerById(anyLong())).thenReturn(prosessinstans.getBehandling());
        hentInntektOpplysninger.utfør(prosessinstans);

        verify(inntektService).hentInntektListe(anyString(),eq(YearMonth.from(fom)), eq(YearMonth.from(tom)));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_REGISTERKONTROLL);
    }

    private Prosessinstans hentProsessinstans(Saksopplysning saksopplysning) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "123123");

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
        sedDokument.setFnr("123");
        return sedDokument;

    }
}