package no.nav.melosys.saksflyt.felles;

import java.time.LocalDate;
import java.time.YearMonth;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.integrasjon.utbetaldata.UtbetaldataService;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.BehandlingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentOpplysningerFellesTest {

    private static final String AKTØR_ID = "123321";
    private static final String FNR = "432234";

    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private MedlFasade medlFasade;
    @Mock
    private InntektService inntektService;
    @Mock
    private UtbetaldataService utbetaldataService;
    @Mock
    private SaksopplysningRepository saksopplysningRepository;

    private HentOpplysningerFelles hentOpplysningerFelles;

    @Before
    public void setUp() throws Exception {
        hentOpplysningerFelles = new HentOpplysningerFelles(tpsFasade, behandlingService, medlFasade, inntektService, utbetaldataService, saksopplysningRepository);
        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn(FNR);
        when(tpsFasade.hentPersonMedAdresse(anyString())).thenReturn(new Saksopplysning());
    }

    @Test
    public void hentOgLagrePersonopplysninger() throws Exception {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);

        hentOpplysningerFelles.hentOgLagrePersonopplysninger(AKTØR_ID, behandling);

        verify(tpsFasade).hentIdentForAktørId(eq(AKTØR_ID));
        verify(tpsFasade).hentPersonMedAdresse(FNR);
        verify(saksopplysningRepository).save(any(Saksopplysning.class));
    }

    @Test
    public void hentOgLagreMedlemskapsopplysninger() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(medlFasade.hentPeriodeListe(anyString(), any(), any())).thenReturn(saksopplysning);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(hentBehandling(saksopplysning));

        hentOpplysningerFelles.hentOgLagreMedlemskapsopplysninger(2L, FNR);

        verify(medlFasade).hentPeriodeListe(anyString(), any(), any());
    }

    @Test
    public void hentOgLagreInntektsopplysninger_tomTilDato_forespørTomTilDato() throws Exception {
        LocalDate fom = LocalDate.now().minusYears(2);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, null);
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(saksopplysning);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(hentBehandling(saksopplysning));

        hentOpplysningerFelles.hentOgLagreInntektsopplysninger(2L, FNR);

        verify(behandlingService).hentBehandling(anyLong());
        verify(inntektService).hentInntektListe(anyString(),eq(YearMonth.from(fom)), eq(YearMonth.from(fom.plusYears(2))));
    }

    @Test
    public void hentOgLagreInntektsopplysninger_periodePåbegynt_verifiserInntektPeriode() throws Exception {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(saksopplysning);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(hentBehandling(saksopplysning));

        hentOpplysningerFelles.hentOgLagreInntektsopplysninger(2L, FNR);

        verify(behandlingService).hentBehandling(anyLong());
        verify(inntektService).hentInntektListe(anyString(),eq(YearMonth.from(fom.minusMonths(2))), eq(YearMonth.from(tom)));
    }

    @Test
    public void hentOgLagreInntektsopplysninger_periodeIkkePåbegynt_verifiserInntektPeriode() throws Exception {
        LocalDate fom = LocalDate.now().plusYears(1);
        LocalDate tom = LocalDate.now().plusYears(2);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(saksopplysning);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(hentBehandling(saksopplysning));

        hentOpplysningerFelles.hentOgLagreInntektsopplysninger(2L, FNR);

        verify(behandlingService).hentBehandling(anyLong());
        verify(inntektService).hentInntektListe(anyString(),eq(YearMonth.from(LocalDate.now().minusMonths(2))), eq(YearMonth.from(LocalDate.now())));
    }

    @Test
    public void hentOgLagreInntektsopplysninger_periodeAvsluttet_verifiserInntektPeriode() throws Exception {
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(new Saksopplysning());
        LocalDate fom = LocalDate.now().minusYears(3);
        LocalDate tom = LocalDate.now().minusYears(2);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(saksopplysning);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(hentBehandling(saksopplysning));

        hentOpplysningerFelles.hentOgLagreInntektsopplysninger(2L, FNR);

        verify(behandlingService).hentBehandling(anyLong());
        verify(inntektService).hentInntektListe(anyString(),eq(YearMonth.from(fom)), eq(YearMonth.from(tom)));
    }

    @Test
    public void hentOgLagreUtbetalingsopplysninger() throws FunksjonellException, TekniskException {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(utbetaldataService.hentUtbetalingerBarnetrygd(anyString(), any(), any())).thenReturn(saksopplysning);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(hentBehandling(saksopplysning));

        hentOpplysningerFelles.hentOgLagreUtbetalingsopplysninger(2L, FNR);

        verify(behandlingService).hentBehandling(anyLong());
        verify(utbetaldataService).hentUtbetalingerBarnetrygd(anyString(), any(), any());
    }

    @Test
    public void hentOgLagreUtbetalingsopplysninger_periode5ÅrTilbakeITid_kanIkkeHenteUtbetalOpplysninger() throws FunksjonellException, TekniskException {
        LocalDate fom = LocalDate.now().minusYears(5);
        LocalDate tom = LocalDate.now().minusYears(4);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(hentBehandling(saksopplysning));

        hentOpplysningerFelles.hentOgLagreUtbetalingsopplysninger(2L, FNR);

        verify(behandlingService).hentBehandling(anyLong());
        verify(utbetaldataService, never()).hentUtbetalingerBarnetrygd(anyString(), any(), any());
    }

    private Behandling hentBehandling(Saksopplysning saksopplysning) {
        Behandling behandling = new Behandling();
        behandling.setId(2L);
        behandling.getSaksopplysninger().add(saksopplysning);

        return behandling;
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