package no.nav.melosys.saksflyt.steg.iv;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.FEILET_MASKINELT;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_SEND_BREV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterMedlTest {
    private OppdaterMedl oppdaterMedl;

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;

    private Prosessinstans prosessinstans;
    private Behandlingsresultat behandlingsresultat;
    private Lovvalgsperiode lovvalgsperiode;

    @Before
    public void setUp() throws IkkeFunnetException {
        oppdaterMedl = new OppdaterMedl(behandlingsresultatService, medlPeriodeService);

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
    public void sjekkNestSteg() {
        oppdaterMedl.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_SEND_BREV);
    }

    @Test
    public void utførSteg_behandlingsresultatTypeErFastsattLovvalgslandOgInnvilgelsesResultat_Innvilget() throws FunksjonellException, TekniskException {
        oppdaterMedl.utførSteg(prosessinstans);
        verify(medlPeriodeService).opprettPeriodeEndelig(eq(lovvalgsperiode), eq(1L), eq(false));
    }

    @Test
    public void utførSteg_behandlingsresultatHarIngenLovvalgPeriode_feiler() throws IkkeFunnetException {
        behandlingsresultat.setLovvalgsperioder(new HashSet<>());
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        oppdaterMedl.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public void utfør_avslagManglendeOpplysningerUtenPeriode_oppdaterIkkeMedl() throws Exception {
        behandlingsresultat.setType(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        oppdaterMedl.utfør(prosessinstans);
        verifyNoInteractions(medlPeriodeService);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_SEND_BREV);
    }

    @Test
    public void utfør_avslagManglendeOpplysningerMedPeriode_oppdaterMedl() throws Exception {
        behandlingsresultat.setType(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        lovvalgsperiode.setMedlPeriodeID(123L);
        oppdaterMedl.utfør(prosessinstans);
        verify(medlPeriodeService).avvisPeriode(123L);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_SEND_BREV);
    }

    @Test
    public void medlperiodeIDFinnesLovvalgsperiodeInnvilget_oppdaterPeriodeEndelig() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setMedlPeriodeID(123L);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        oppdaterMedl.utfør(prosessinstans);

        verify(medlPeriodeService).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(false));
        assertThat(prosessinstans.getSteg()).isEqualTo(IV_SEND_BREV);
    }

    @Test
    public void medlperiodeIDFinnesLovvalgsperiodeAvslått_avvisPeriode() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setMedlPeriodeID(123L);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        oppdaterMedl.utfør(prosessinstans);

        verify(medlPeriodeService).avvisPeriode(eq(lovvalgsperiode.getMedlPeriodeID()));
        assertThat(prosessinstans.getSteg()).isEqualTo(IV_SEND_BREV);
    }

    @Test
    public void utførSteg_erArtikkel13_opprettForeløpigPeriode() throws FunksjonellException, TekniskException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);

        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        oppdaterMedl.utfør(prosessinstans);

        verify(medlPeriodeService).opprettPeriodeForeløpig(eq(lovvalgsperiode), eq(1L), eq(true));
        assertThat(prosessinstans.getSteg()).isEqualTo(IV_SEND_BREV);
    }
}