package no.nav.melosys.saksflyt.steg.iv;

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OppdaterMedlVedVedtakTest {
    private OppdaterMedlVedVedtak oppdaterMedlVedVedtak;

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;

    private Prosessinstans prosessinstans;
    private Behandlingsresultat behandlingsresultat;
    private Lovvalgsperiode lovvalgsperiode;

    @BeforeEach
    public void setUp() throws IkkeFunnetException {
        oppdaterMedlVedVedtak = new OppdaterMedlVedVedtak(behandlingsresultatService, medlPeriodeService);

        prosessinstans = new Prosessinstans();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("TEST-MEDL");
        HashSet<Aktoer> aktører = new HashSet<>();
        fagsak.setAktører(aktører);

        Aktoer aktør = new Aktoer();
        String aktørID = "12345678912";
        aktør.setAktørId(aktørID);
        aktør.setFagsak(fagsak);
        aktør.setRolle(Aktoersroller.BRUKER);
        aktører.add(aktør);

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);

        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setDekning(Trygdedekninger.UTEN_DEKNING);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);

        behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        prosessinstans.setBehandling(behandling);
        prosessinstans.getBehandling().setType(Behandlingstyper.SOEKNAD);
        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK);
    }

    @Test
    void utfør_behandlingsresultatTypeErFastsattLovvalgslandOgInnvilgelsesResultat_Innvilget() throws FunksjonellException, TekniskException {
        oppdaterMedlVedVedtak.utfør(prosessinstans);
        verify(medlPeriodeService).opprettPeriodeEndelig(eq(lovvalgsperiode), eq(1L), eq(false));
    }

    @Test
    void utfør_behandlingsresultatHarIngenLovvalgPeriode_feiler() throws FunksjonellException {
        behandlingsresultat.setLovvalgsperioder(new HashSet<>());
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(() -> oppdaterMedlVedVedtak.utfør(prosessinstans))
            .withMessageContaining("Ingen lovvalgsperiode finnes");
    }

    @Test
    void utfør_avslagManglendeOpplysningerUtenPeriode_oppdaterIkkeMedl() throws Exception {
        behandlingsresultat.setType(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        oppdaterMedlVedVedtak.utfør(prosessinstans);
        verifyNoInteractions(medlPeriodeService);
    }

    @Test
    void utfør_avslagManglendeOpplysningerMedPeriode_oppdaterMedl() throws Exception {
        behandlingsresultat.setType(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        lovvalgsperiode.setMedlPeriodeID(123L);
        oppdaterMedlVedVedtak.utfør(prosessinstans);
        verify(medlPeriodeService).avvisPeriode(123L);
    }

    @Test
    void medlperiodeIDFinnesLovvalgsperiodeInnvilget_oppdaterPeriodeEndelig() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setMedlPeriodeID(123L);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        oppdaterMedlVedVedtak.utfør(prosessinstans);

        verify(medlPeriodeService).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(false));
    }

    @Test
    void medlperiodeIDFinnesLovvalgsperiodeAvslått_avvisPeriode() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setMedlPeriodeID(123L);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        oppdaterMedlVedVedtak.utfør(prosessinstans);

        verify(medlPeriodeService).avvisPeriode(eq(lovvalgsperiode.getMedlPeriodeID()));
    }

    @Test
    void utfør_erArtikkel13_opprettForeløpigPeriode() throws FunksjonellException, TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);

        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        oppdaterMedlVedVedtak.utfør(prosessinstans);

        verify(medlPeriodeService).opprettPeriodeForeløpig(eq(lovvalgsperiode), eq(1L), eq(true));
    }
}