package no.nav.melosys.service.avklartefakta;

import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR;
import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl.SAMBOER_UTEN_FELLES_BARN;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import no.nav.melosys.service.behandling.BehandlingsresultatService;
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
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private AvklartefaktaDtoKonverterer avklartefaktaDtoKonverterer;

    private AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;

    @Captor
    private ArgumentCaptor<Avklartefakta> captor;
    
    private static final String uuidBarn = "uuidBarn";
    private static final String uuidEktefelleSamboer = "uuidEktefelleSamboer";
    private static final String fritekstBarn = "fritekstBarn";
    private static final String fritekstEktefelleSamboer = "fritekstEktefelleSamboer";

    @Before
    public void setUp() {
        AvklartefaktaService avklartefaktaService = new AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, avklartefaktaDtoKonverterer);
        avklarteMedfolgendeFamilieService = new AvklarteMedfolgendeFamilieService(behandlingService, behandlingsresultatService, avklartefaktaService);
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklartefakta_ikkeOmfattetFamilie_lagresKorrekt() throws FunksjonellException {
        AvklarteMedfolgendeFamilie avklarteMedfolgendeFamilie =
            new AvklarteMedfolgendeFamilie(Set.of(), Set.of(
                new IkkeOmfattetFamilie(uuidBarn, OVER_18_AR.getKode(), fritekstBarn),
                new IkkeOmfattetFamilie(uuidEktefelleSamboer, SAMBOER_UTEN_FELLES_BARN.getKode(), fritekstEktefelleSamboer)));

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());
        when(behandlingsresultatService.hentBehandlingsresultat(1L)).thenReturn(behandlingsresultat);

        avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie);

        verify(avklarteFaktaRepository, times(2)).save(captor.capture());

        List<Avklartefakta> capturedAvklarteFakta = captor.getAllValues();
        capturedAvklarteFakta.sort(Comparator.comparing(Avklartefakta::getSubjekt));
        assertEquals(2, capturedAvklarteFakta.size());
        Avklartefakta avklartBarn = capturedAvklarteFakta.get(0);
        Avklartefakta avklartEktefelleSamboer = capturedAvklarteFakta.get(1);

        assertEquals(uuidBarn, avklartBarn.getSubjekt());
        assertEquals(Avklartefaktatyper.VURDERING_LOVVALG_BARN, avklartBarn.getType());
        assertEquals(Avklartefaktatyper.VURDERING_LOVVALG_BARN.getKode(), avklartBarn.getReferanse());
        assertEquals(Avklartefakta.IKKE_VALGT_FAKTA, avklartBarn.getFakta());
        assertEquals(fritekstBarn, avklartBarn.getBegrunnelseFritekst());
        assertEquals(behandlingsresultat, avklartBarn.getBehandlingsresultat());
        assertEquals(1, avklartBarn.getRegistreringer().size());

        AvklartefaktaRegistrering registreringBarn = avklartBarn.getRegistreringer().iterator().next();
        assertEquals(OVER_18_AR.getKode(), registreringBarn.getBegrunnelseKode());

        assertEquals(uuidEktefelleSamboer, avklartEktefelleSamboer.getSubjekt());
        assertEquals(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, avklartEktefelleSamboer.getType());
        assertEquals(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.getKode(), avklartEktefelleSamboer.getReferanse());
        assertEquals(Avklartefakta.IKKE_VALGT_FAKTA, avklartEktefelleSamboer.getFakta());
        assertEquals(fritekstEktefelleSamboer, avklartEktefelleSamboer.getBegrunnelseFritekst());
        assertEquals(behandlingsresultat, avklartEktefelleSamboer.getBehandlingsresultat());
        assertEquals(1, avklartEktefelleSamboer.getRegistreringer().size());

        AvklartefaktaRegistrering registreringEktefelleSamboer = avklartEktefelleSamboer.getRegistreringer().iterator().next();
        assertEquals(SAMBOER_UTEN_FELLES_BARN.getKode(), registreringEktefelleSamboer.getBegrunnelseKode());
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklartefakta_omfattetFamilie_lagresKorrekt() throws FunksjonellException {
        AvklarteMedfolgendeFamilie avklarteMedfolgendeFamilie =
            new AvklarteMedfolgendeFamilie(
                Set.of(new OmfattetFamilie(uuidBarn), new OmfattetFamilie(uuidEktefelleSamboer)),
                Set.of());

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());
        when(behandlingsresultatService.hentBehandlingsresultat(1L)).thenReturn(behandlingsresultat);

        avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie);

        verify(avklarteFaktaRepository, times(2)).save(captor.capture());

        List<Avklartefakta> capturedAvklarteFakta = captor.getAllValues();
        capturedAvklarteFakta.sort(Comparator.comparing(Avklartefakta::getSubjekt));
        assertEquals(capturedAvklarteFakta.size(), 2);
        Avklartefakta avklartBarn = capturedAvklarteFakta.get(0);
        Avklartefakta avklartEktefelleSamboer = capturedAvklarteFakta.get(1);

        assertEquals(uuidBarn, avklartBarn.getSubjekt());
        assertEquals(Avklartefaktatyper.VURDERING_LOVVALG_BARN, avklartBarn.getType());
        assertEquals(Avklartefaktatyper.VURDERING_LOVVALG_BARN.getKode(), avklartBarn.getReferanse());
        assertEquals(Avklartefakta.VALGT_FAKTA, avklartBarn.getFakta());
        assertNull(avklartBarn.getBegrunnelseFritekst());
        assertEquals(behandlingsresultat, avklartBarn.getBehandlingsresultat());
        assertTrue(avklartBarn.getRegistreringer().isEmpty());

        assertEquals(uuidEktefelleSamboer, avklartEktefelleSamboer.getSubjekt());
        assertEquals(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, avklartEktefelleSamboer.getType());
        assertEquals(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.getKode(), avklartEktefelleSamboer.getReferanse());
        assertEquals(Avklartefakta.VALGT_FAKTA, avklartEktefelleSamboer.getFakta());
        assertNull(avklartEktefelleSamboer.getBegrunnelseFritekst());
        assertEquals(behandlingsresultat, avklartEktefelleSamboer.getBehandlingsresultat());
        assertTrue(avklartEktefelleSamboer.getRegistreringer().isEmpty());
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklartefakta_omfattetFamilieIkkeLagretIBehandlingsgrunnlaget_kasterFeilmelding() throws FunksjonellException {
        AvklarteMedfolgendeFamilie avklarteMedfolgendeFamilie =
            new AvklarteMedfolgendeFamilie(Set.of(new OmfattetFamilie("uuid3")), Set.of());

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie))
            .withMessageContaining("Medfolgende familie som er omfattet av norsk trygd: uuid3 er ikke lagret i behandlingsgrunnlaget.");

        verify(avklarteFaktaRepository, never()).save(captor.capture());
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklartefakta_ikkeOmfattetFamilieIkkeLagretIBehandlingsgrunnlaget_kasterFeilmelding() throws FunksjonellException {
        AvklarteMedfolgendeFamilie avklarteMedfolgendeFamilie =
            new AvklarteMedfolgendeFamilie(Set.of(), Set.of(
                new IkkeOmfattetFamilie("uuid3", OVER_18_AR.getKode(), fritekstBarn)));

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie))
            .withMessageContaining("Medfolgende familie som ikke er omfattet av norsk trygd: uuid3 er ikke lagret i behandlingsgrunnlaget.");

        verify(avklarteFaktaRepository, never()).save(captor.capture());
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklartefakta_ugyldigBegrunnelseKode_kasterFeilmelding() throws FunksjonellException {
        AvklarteMedfolgendeFamilie avklarteMedfolgendeFamilie =
            new AvklarteMedfolgendeFamilie(Set.of(), Set.of(
                new IkkeOmfattetFamilie(uuidBarn, SAMBOER_UTEN_FELLES_BARN.getKode(), fritekstBarn)));

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie))
            .withMessageContaining("Begrunnelsen til medfolgende familie " + uuidBarn + ": " + SAMBOER_UTEN_FELLES_BARN.getKode() + " er ikke gyldig.");

        verify(avklarteFaktaRepository, never()).save(captor.capture());
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklartefakta_ikkeSattBegrunnelseKode_kasterFeilmelding() throws FunksjonellException {
        AvklarteMedfolgendeFamilie avklarteMedfolgendeFamilie =
            new AvklarteMedfolgendeFamilie(Set.of(), Set.of(
                new IkkeOmfattetFamilie(uuidBarn, null, fritekstBarn)));

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie))
            .withMessageContaining("Begrunnelsen til medfolgende familie " + uuidBarn + ": " + null + " er ikke satt.");

        verify(avklarteFaktaRepository, never()).save(captor.capture());
    }

    private static Behandling mockBehandling() {
        MedfolgendeFamilie medfolgendeFamilieUuid1 = MedfolgendeFamilie.tilMedfolgendeFamilie(uuidBarn, "fnr1", null, MedfolgendeFamilie.Relasjonsrolle.BARN);
        MedfolgendeFamilie medfolgendeFamilieUuid2 = MedfolgendeFamilie.tilMedfolgendeFamilie(uuidEktefelleSamboer, "fnr2", null, MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER);
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.personOpplysninger.medfolgendeFamilie.addAll(List.of(medfolgendeFamilieUuid1, medfolgendeFamilieUuid2));
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagData);
        Behandling behandling = new Behandling();
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);
        return behandling;
    }
}
