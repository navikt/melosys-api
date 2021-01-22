package no.nav.melosys.service.avklartefakta;

import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl.SAMBOER_UTEN_FELLES_BARN;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.familie.OmfattetFamilie;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.service.behandling.BehandlingService;

@RunWith(MockitoJUnitRunner.class)
public class AvklarteMedfolgendeFamilieServiceTest {
    @Mock
    private AvklarteFaktaRepository avklarteFaktaRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private AvklartefaktaDtoKonverterer avklartefaktaDtoKonverterer;

    private AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;

    @Captor
    private ArgumentCaptor<Avklartefakta> captor;

    @Before
    public void setUp() {
        AvklartefaktaService avklartefaktaService = new AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, avklartefaktaDtoKonverterer);
        avklarteMedfolgendeFamilieService = new AvklarteMedfolgendeFamilieService(behandlingService, avklartefaktaService);
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklartefakta_ikkeOmfattetFamilie_lagresKorrekt() throws FunksjonellException {
        Set<IkkeOmfattetFamilie> ikkeOmfattetEktefelleSamboers = Set.of(
            new IkkeOmfattetFamilie("uuid2", SAMBOER_UTEN_FELLES_BARN, "fritekstForUuid2"));
        AvklarteMedfolgendeFamilie avklarteMedfolgendeEktefelleSamboer =
            new AvklarteMedfolgendeFamilie(Set.of(), ikkeOmfattetEktefelleSamboers);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);

        Avklartefakta forventetAvklartefakta = new Avklartefakta();
        forventetAvklartefakta.setSubjekt("uuid2");
        forventetAvklartefakta.setType(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER);
        forventetAvklartefakta.setReferanse(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.getKode());
        forventetAvklartefakta.setFakta("FALSE");
        forventetAvklartefakta.setBegrunnelseFritekst("fritekstForUuid2");
        forventetAvklartefakta.setBehandlingsresultat(behandlingsresultat);

        AvklartefaktaRegistrering forventetRegistrering = new AvklartefaktaRegistrering();
        forventetRegistrering.setAvklartefakta(forventetAvklartefakta);
        forventetRegistrering.setBegrunnelseKode("SAMBOER_UTEN_FELLES_BARN");

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());

        when(behandlingsresultatRepository.findById(1L)).thenReturn(Optional.of(behandlingsresultat));

        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(anyLong(), eq(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER)))
            .thenReturn(Optional.of(forventetAvklartefakta));

        avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(
            1L, new AvklarteMedfolgendeFamilie(Set.of(), Set.of()), avklarteMedfolgendeEktefelleSamboer);

        verify(avklarteFaktaRepository, times(2)).save(captor.capture());

        List<Avklartefakta> capturedAvklarteFakta = captor.getAllValues();
        assertEquals(capturedAvklarteFakta.size(), 2);
        Avklartefakta utenRegistrering = capturedAvklarteFakta.get(0);
        Avklartefakta medRegistrering = capturedAvklarteFakta.get(1);

        assertEquals(utenRegistrering.getSubjekt(), forventetAvklartefakta.getSubjekt());
        assertEquals(utenRegistrering.getType(), forventetAvklartefakta.getType());
        assertEquals(utenRegistrering.getReferanse(), forventetAvklartefakta.getReferanse());
        assertEquals(utenRegistrering.getFakta(), forventetAvklartefakta.getFakta());
        assertEquals(utenRegistrering.getBegrunnelseFritekst(), forventetAvklartefakta.getBegrunnelseFritekst());
        assertEquals(utenRegistrering.getBehandlingsresultat(), forventetAvklartefakta.getBehandlingsresultat());

        assertEquals(medRegistrering.getSubjekt(), forventetAvklartefakta.getSubjekt());
        assertEquals(medRegistrering.getType(), forventetAvklartefakta.getType());
        assertEquals(medRegistrering.getReferanse(), forventetAvklartefakta.getReferanse());
        assertEquals(medRegistrering.getFakta(), forventetAvklartefakta.getFakta());
        assertEquals(medRegistrering.getBegrunnelseFritekst(), forventetAvklartefakta.getBegrunnelseFritekst());
        assertEquals(medRegistrering.getBehandlingsresultat(), forventetAvklartefakta.getBehandlingsresultat());
        assertEquals(medRegistrering.getRegistreringer().size(), 1);

        AvklartefaktaRegistrering registreringFraCaptured = medRegistrering.getRegistreringer().iterator().next();
        assertEquals(registreringFraCaptured.getAvklartefakta(), forventetRegistrering.getAvklartefakta());
        assertEquals(registreringFraCaptured.getBegrunnelseKode(), forventetRegistrering.getBegrunnelseKode());
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklartefakta_omfattetFamilie_lagresKorrekt() throws FunksjonellException {
        Set<OmfattetFamilie> omfattetBarn = Set.of(new OmfattetFamilie("uuid1"));
        AvklarteMedfolgendeFamilie avklarteMedfolgendeBarn =
            new AvklarteMedfolgendeFamilie(omfattetBarn, Set.of());
        Set<OmfattetFamilie> omfattetEktefelleSamboer = Set.of(new OmfattetFamilie("uuid2"));
        AvklarteMedfolgendeFamilie avklarteMedfolgendeEktefelleSamboer =
            new AvklarteMedfolgendeFamilie(omfattetEktefelleSamboer, Set.of());

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        when(behandlingsresultatRepository.findById(1L)).thenReturn(Optional.of(behandlingsresultat));

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());

        avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(
            1L, avklarteMedfolgendeBarn, avklarteMedfolgendeEktefelleSamboer);

        verify(avklarteFaktaRepository, times(2)).save(captor.capture());

        List<Avklartefakta> capturedAvklarteFakta = captor.getAllValues();
        assertEquals(capturedAvklarteFakta.size(), 2);
        Avklartefakta avklartBarn = capturedAvklarteFakta.get(0);
        Avklartefakta avklartEktefelleSamboer = capturedAvklarteFakta.get(1);

        assertEquals(avklartBarn.getSubjekt(), "uuid1");
        assertEquals(avklartBarn.getType(), Avklartefaktatyper.VURDERING_LOVVALG_BARN);
        assertEquals(avklartBarn.getReferanse(), Avklartefaktatyper.VURDERING_LOVVALG_BARN.getKode());
        assertEquals(avklartBarn.getFakta(), "TRUE");
        assertNull(avklartBarn.getBegrunnelseFritekst());
        assertEquals(avklartBarn.getBehandlingsresultat(), behandlingsresultat);
        assertTrue(avklartBarn.getRegistreringer().isEmpty());

        assertEquals(avklartEktefelleSamboer.getSubjekt(), "uuid2");
        assertEquals(avklartEktefelleSamboer.getType(), Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER);
        assertEquals(avklartEktefelleSamboer.getReferanse(), Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.getKode());
        assertEquals(avklartEktefelleSamboer.getFakta(), "TRUE");
        assertNull(avklartEktefelleSamboer.getBegrunnelseFritekst());
        assertEquals(avklartEktefelleSamboer.getBehandlingsresultat(), behandlingsresultat);
        assertTrue(avklartEktefelleSamboer.getRegistreringer().isEmpty());
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklartefakta_uuidIkkeLagretIBehandlingsgrunnlaget_kasterFeilmelding() throws FunksjonellException {
        Set<OmfattetFamilie> omfattetBarn = Set.of(new OmfattetFamilie("uuid3"));
        AvklarteMedfolgendeFamilie avklarteMedfolgendeBarn = new AvklarteMedfolgendeFamilie(omfattetBarn, Set.of());
        AvklarteMedfolgendeFamilie avklarteMedfolgendeEktefelleSamboer = new AvklarteMedfolgendeFamilie(Set.of(), Set.of());

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeBarn, avklarteMedfolgendeEktefelleSamboer))
            .withMessageContaining("Medfolgende familie som er omfattet av norsk trygd: uuid3 er ikke lagret i behandlingsgrunnlaget.");

        verify(avklarteFaktaRepository, never()).save(captor.capture());
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklartefakta_ugyldigBegrunnelseKode_kasterFeilmelding() throws FunksjonellException {
        Set<IkkeOmfattetFamilie> ikkeOmfattetBarn = Set.of(
            new IkkeOmfattetFamilie("uuid1", SAMBOER_UTEN_FELLES_BARN, "fritekstForUuid1"));
        AvklarteMedfolgendeFamilie avklarteMedfolgendeBarn = new AvklarteMedfolgendeFamilie(Set.of(), ikkeOmfattetBarn);
        AvklarteMedfolgendeFamilie avklarteMedfolgendeEktefelleSamboer = new AvklarteMedfolgendeFamilie(Set.of(), Set.of());

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeBarn, avklarteMedfolgendeEktefelleSamboer))
            .withMessageContaining("Begrunnelsen til medfolgende ektefelle/samboer: SAMBOER_UTEN_FELLES_BARN er ikke gyldig.");

        verify(avklarteFaktaRepository, never()).save(captor.capture());
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklartefakta_relasjonskodeMatcherIkke_kasterFeilmelding() throws FunksjonellException {
        Set<OmfattetFamilie> omfattetBarn = Set.of(new OmfattetFamilie("uuid2"));
        AvklarteMedfolgendeFamilie avklarteMedfolgendeBarn = new AvklarteMedfolgendeFamilie(omfattetBarn, Set.of());
        AvklarteMedfolgendeFamilie avklarteMedfolgendeEktefelleSamboer = new AvklarteMedfolgendeFamilie(Set.of(), Set.of());

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeBarn, avklarteMedfolgendeEktefelleSamboer))
            .withMessageContaining("Medfolgende familie som er omfattet av norsk trygd: uuid2 er lagret med feil relasjonsrolle.");

        verify(avklarteFaktaRepository, never()).save(captor.capture());
    }

    private static Behandling mockBehandling() {
        MedfolgendeFamilie medfolgendeFamilieUuid1 = MedfolgendeFamilie.tilMedfolgendeFamilie("uuid1", "fnr1", null, MedfolgendeFamilie.Relasjonsrolle.BARN);
        MedfolgendeFamilie medfolgendeFamilieUuid2 = MedfolgendeFamilie.tilMedfolgendeFamilie("uuid2", "fnr2", null, MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER);
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.personOpplysninger.medfolgendeFamilie.addAll(List.of(medfolgendeFamilieUuid1, medfolgendeFamilieUuid2));
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagData);
        Behandling behandling = new Behandling();
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);
        return behandling;
    }
}
