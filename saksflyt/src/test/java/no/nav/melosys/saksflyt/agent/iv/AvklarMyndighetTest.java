package no.nav.melosys.saksflyt.agent.iv;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklarMyndighetTest {

    @Mock
    private AvklarteFaktaRepository avklarteFaktaRepository;

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    private FagsakService fagsakService;

    @Mock
    private UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    private AvklarMyndighet steg;

    private Prosessinstans p;

    @Before
    public void setUp() {
        steg = new AvklarMyndighet(avklarteFaktaRepository, behandlingsresultatRepository, fagsakService, utenlandskMyndighetRepository);

        p = new Prosessinstans();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnr");
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        when(behandlingsresultatRepository.findById(eq(1L))).thenReturn(Optional.of(behandlingsresultat));
    }

    @Test
    public void utfør_utenMyndighet_myndighetOpprettes() throws FunksjonellException, TekniskException {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setFakta("DK");
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(1L, Avklartefaktatype.ARBEIDSLAND)).thenReturn(Optional.of(avklartefakta));
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.land = "DK";
        utenlandskMyndighet.institusjonskode = "zfga";
        when(utenlandskMyndighetRepository.findByLandkode(eq(Landkoder.DK))).thenReturn(utenlandskMyndighet);

        steg.utfør(p);

        String forventetID = utenlandskMyndighet.land + ":" + utenlandskMyndighet.institusjonskode;
        verify(fagsakService).leggTilAktør(eq("saksnr"), eq(Aktoersroller.MYNDIGHET), eq(forventetID));
    }

    @Test
    public void utfør_medMyndighet_myndighetOpprettesIkke() throws FunksjonellException, TekniskException {
        Fagsak fagsakMedMyndighet = new Fagsak();
        fagsakMedMyndighet.setSaksnummer("med myndighet");
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.MYNDIGHET);
        fagsakMedMyndighet.getAktører().add(myndighet);
        p.getBehandling().setFagsak(fagsakMedMyndighet);
        
        steg.utfør(p);

        verify(fagsakService, never()).leggTilAktør(any(), any(), any());
    }

    @Test
    public void avklarLand() throws FunksjonellException {
        //steg.avklarLand(1L);
    }
}