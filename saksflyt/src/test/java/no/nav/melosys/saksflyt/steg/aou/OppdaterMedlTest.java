package no.nav.melosys.saksflyt.steg.aou;

import java.util.Collections;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.service.BehandlingsresultatService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterMedlTest {
    private OppdaterMedl agent;

    @Mock
    private MedlFasade medlFasade;
    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LovvalgsperiodeRepository lovvalgsperiodeRepository;
    @Mock
    private AnmodningsperiodeRepository anmodningsperiodeRepository;

    private Prosessinstans p;
    private Behandlingsresultat behandlingsresultat;

    @Before
    public void setUp() throws IkkeFunnetException {
        OppdaterMedlFelles felles = new OppdaterMedlFelles(tpsFasade, medlFasade, behandlingsresultatService, lovvalgsperiodeRepository, anmodningsperiodeRepository);
        agent = new OppdaterMedl(medlFasade, felles);

        p = new Prosessinstans();
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

        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.ANMODNING_OM_UNNTAK);
    }

    @Test
    public void sjekkNestSteg() {
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.AOU_SEND_BREV);
    }

    @Test
    public void lagreMedlPeriodeId() {
        agent.utførSteg(p);
        verify(anmodningsperiodeRepository).save(any(Anmodningsperiode.class));
    }

    @Test
    public void utførStegNårBehandlingsresultatTypeErAnmodning_om_unntak() throws FunksjonellException, TekniskException {
        behandlingsresultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);

        agent.utførSteg(p);
        verify(medlFasade).opprettPeriodeUnderAvklaring(any(), any(), eq(KildedokumenttypeMedl.HENV_SOKNAD));
    }

    @Test
    public void utførStegNårBehandlingsresultatHarIngenLovvalgPeriode() {
        behandlingsresultat.setAnmodningsperioder(new HashSet<>());

        agent.utførSteg(p);
        assertEquals(ProsessSteg.FEILET_MASKINELT, p.getSteg());
    }
}