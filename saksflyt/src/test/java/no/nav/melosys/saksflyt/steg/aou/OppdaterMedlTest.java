package no.nav.melosys.saksflyt.steg.aou;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterMedlTest {

    private OppdaterMedl agent;

    @Mock
    private MedlFasade medlFasade;

    @Mock
    private TpsFasade tpsFasade;

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    private LovvalgsperiodeRepository lovvalgsperiodeRepository;

    private Prosessinstans p;

    private Behandlingsresultat behandlingsresultat;

    @Before
    public void setUp() {
        OppdaterMedlFelles felles = new OppdaterMedlFelles(tpsFasade, behandlingsresultatRepository, lovvalgsperiodeRepository);
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

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.CH);
        lovvalgsperiode.setDekning(Trygdedekninger.UTEN_DEKNING);

        behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        when(behandlingsresultatRepository.findById(anyLong())).thenReturn(Optional.of(behandlingsresultat));

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
        verify(lovvalgsperiodeRepository).save(any(Lovvalgsperiode.class));
    }

    @Test
    public void utførStegNårBehandlingsresultatTypeErAnmodning_om_unntak() throws FunksjonellException, TekniskException {
        behandlingsresultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);

        agent.utførSteg(p);
        verify(medlFasade).opprettPeriodeUnderAvklaring(any(), any(), eq(KildedokumenttypeMedl.HENV_SOKNAD));
    }

    @Test
    public void utførStegNårBehandlingsresultatHarIngenLovvalgPeriode() {
        behandlingsresultat.setLovvalgsperioder(new HashSet<>());

        agent.utførSteg(p);
        assertEquals(ProsessSteg.FEILET_MASKINELT, p.getSteg());
    }
}