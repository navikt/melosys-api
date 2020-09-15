package no.nav.melosys.saksflyt.steg.aou.ut;

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(null, null, Landkoder.CH,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, null, null, null, Trygdedekninger.FULL_DEKNING_EOSFO);

        behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        prosessinstans.setBehandling(behandling);
        prosessinstans.getBehandling().setType(Behandlingstyper.SOEKNAD);
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
    }

    @Test
    public void utførNårBehandlingsresultatTypeErAnmodning_om_unntak() throws FunksjonellException, TekniskException {
        behandlingsresultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);

        oppdaterMedl.utfør(prosessinstans);
        verify(medlPeriodeService).opprettPeriodeUnderAvklaring(any(Anmodningsperiode.class), anyLong(), eq(false));
    }

    @Test
    public void utførNårBehandlingsresultatHarIngenLovvalgPeriode() {
        behandlingsresultat.setAnmodningsperioder(new HashSet<>());

        assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(() -> oppdaterMedl.utfør(prosessinstans))
            .withMessageContaining("Ingen anmodningsperioder finnes");
    }
}