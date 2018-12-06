package no.nav.melosys.saksflyt.agent.iv;

import java.util.Collections;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingsresultatRepository;
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

    private Prosessinstans p;

    private Behandlingsresultat behandlingsresultat;

    @Before
    public void setUp() {
        agent = new OppdaterMedl(medlFasade, tpsFasade, behandlingsresultatRepository);

        p = new Prosessinstans();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("TEST-MEDL");
        HashSet<Aktoer> aktører = new HashSet<>();
        fagsak.setAktører(aktører);

        Aktoer aktør = new Aktoer();
        String aktørID = "12345678912";
        aktør.setAktørId(aktørID);
        aktør.setFagsak(fagsak);
        aktør.setRolle(RolleType.BRUKER);
        aktører.add(aktør);

        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.CH);
        lovvalgsperiode.setDekning(TrygdeDekning.UTEN_DEKNING);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);

        behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(BehandlingsresultatType.FASTSATT_LOVVALGSLAND);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        when(behandlingsresultatRepository.findOne(anyLong())).thenReturn(behandlingsresultat);

        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);
    }

    @Test
    public void sjekkNestSteg() throws FunksjonellException, TekniskException {
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.IV_SEND_BREV);
    }

    @Test
    public void utførStegNårBehandlingsresultatTypeErFastsatt_lovvalgslandOgInnvilgelsesResultat_Innvilget() throws FunksjonellException, TekniskException {

        agent.utførSteg(p);
        verify(medlFasade ,times(1)).opprettPeriodeSomEndelig(any(), any());
    }

    @Test
    public void utførStegNårBehandlingsresultatTypeErAnmodning_om_unntak() throws FunksjonellException, TekniskException {

        behandlingsresultat.setType(BehandlingsresultatType.ANMODNING_OM_UNNTAK);
        when(behandlingsresultatRepository.findOne(anyLong())).thenReturn(behandlingsresultat);

        agent.utførSteg(p);

        verify(medlFasade ,times(1)).opprettPeriodeSomUnderAvklaring(any(), any());
    }

    @Test
    public void utførStegNårBehandlingsresultatHarIngenLovvalgPeriode() throws FunksjonellException, TekniskException {

        behandlingsresultat.setLovvalgsperioder(new HashSet<>());
        when(behandlingsresultatRepository.findOne(anyLong())).thenReturn(behandlingsresultat);

        agent.utførSteg(p);
        assertEquals(ProsessSteg.FEILET_MASKINELT, p.getSteg());
    }

}