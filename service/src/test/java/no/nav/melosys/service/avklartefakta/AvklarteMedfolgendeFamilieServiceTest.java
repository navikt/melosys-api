package no.nav.melosys.service.avklartefakta;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR;
import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl.SAMBOER_UTEN_FELLES_BARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvklarteMedfolgendeFamilieServiceTest {
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
    @Mock
    private BehandlingsgrunnlagService mockBehandlingsgrunnlagService;

    private AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;

    @Captor
    private ArgumentCaptor<Avklartefakta> captor;

    private static final String uuidBarn = "uuidBarn";
    private static final String uuidEktefelleSamboer = "uuidEktefelleSamboer";
    private static final String fritekstBarn = "fritekstBarn";
    private static final String fritekstEktefelleSamboer = "fritekstEktefelleSamboer";

    @BeforeEach
    void setUp() {
        AvklartefaktaService avklartefaktaService = new AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, avklartefaktaDtoKonverterer);
        avklarteMedfolgendeFamilieService = new AvklarteMedfolgendeFamilieService(behandlingService, behandlingsresultatService, avklartefaktaService, mockBehandlingsgrunnlagService);
    }

    @Test
    void lagreMedfolgendeFamilieSomAvklartefakta_ikkeOmfattetFamilie_lagresKorrekt() {
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
        assertThat(capturedAvklarteFakta.size()).isEqualTo(2);
        Avklartefakta avklartBarn = capturedAvklarteFakta.get(0);
        Avklartefakta avklartEktefelleSamboer = capturedAvklarteFakta.get(1);

        assertThat(avklartBarn.getSubjekt()).isEqualTo(uuidBarn);
        assertThat(avklartBarn.getType()).isEqualTo(Avklartefaktatyper.VURDERING_LOVVALG_BARN);
        assertThat(avklartBarn.getReferanse()).isEqualTo(Avklartefaktatyper.VURDERING_LOVVALG_BARN.getKode());
        assertThat(avklartBarn.getFakta()).isEqualTo(Avklartefakta.IKKE_VALGT_FAKTA);
        assertThat(avklartBarn.getBegrunnelseFritekst()).isEqualTo(fritekstBarn);
        assertThat(avklartBarn.getBehandlingsresultat()).isEqualTo(behandlingsresultat);
        assertThat(avklartBarn.getRegistreringer().size()).isEqualTo(1);

        AvklartefaktaRegistrering registreringBarn = avklartBarn.getRegistreringer().iterator().next();
        assertThat(registreringBarn.getBegrunnelseKode()).isEqualTo(OVER_18_AR.getKode());

        assertThat(avklartEktefelleSamboer.getSubjekt()).isEqualTo(uuidEktefelleSamboer);
        assertThat(avklartEktefelleSamboer.getType()).isEqualTo(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER);
        assertThat(avklartEktefelleSamboer.getReferanse()).isEqualTo(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.getKode());
        assertThat(avklartEktefelleSamboer.getFakta()).isEqualTo(Avklartefakta.IKKE_VALGT_FAKTA);
        assertThat(avklartEktefelleSamboer.getBegrunnelseFritekst()).isEqualTo(fritekstEktefelleSamboer);
        assertThat(avklartEktefelleSamboer.getBehandlingsresultat()).isEqualTo(behandlingsresultat);
        assertThat(avklartEktefelleSamboer.getRegistreringer().size()).isEqualTo(1);

        AvklartefaktaRegistrering registreringEktefelleSamboer = avklartEktefelleSamboer.getRegistreringer().iterator().next();
        assertThat(registreringEktefelleSamboer.getBegrunnelseKode()).isEqualTo(SAMBOER_UTEN_FELLES_BARN.getKode());
    }

    @Test
    void lagreMedfolgendeFamilieSomAvklartefakta_omfattetFamilie_lagresKorrekt() {
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
        assertThat(capturedAvklarteFakta.size()).isEqualTo(2);
        Avklartefakta avklartBarn = capturedAvklarteFakta.get(0);
        Avklartefakta avklartEktefelleSamboer = capturedAvklarteFakta.get(1);

        assertThat(avklartBarn.getSubjekt()).isEqualTo(uuidBarn);
        assertThat(avklartBarn.getType()).isEqualTo(Avklartefaktatyper.VURDERING_LOVVALG_BARN);
        assertThat(avklartBarn.getReferanse()).isEqualTo(Avklartefaktatyper.VURDERING_LOVVALG_BARN.getKode());
        assertThat(avklartBarn.getFakta()).isEqualTo(Avklartefakta.VALGT_FAKTA);
        assertThat(avklartBarn.getBegrunnelseFritekst()).isNull();
        assertThat(avklartBarn.getBehandlingsresultat()).isEqualTo(behandlingsresultat);
        assertThat(avklartBarn.getRegistreringer().isEmpty()).isTrue();

        assertThat(avklartEktefelleSamboer.getSubjekt()).isEqualTo(uuidEktefelleSamboer);
        assertThat(avklartEktefelleSamboer.getType()).isEqualTo(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER);
        assertThat(avklartEktefelleSamboer.getReferanse()).isEqualTo(Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.getKode());
        assertThat(avklartEktefelleSamboer.getFakta()).isEqualTo(Avklartefakta.VALGT_FAKTA);
        assertThat(avklartEktefelleSamboer.getBegrunnelseFritekst()).isNull();
        assertThat(avklartEktefelleSamboer.getBehandlingsresultat()).isEqualTo(behandlingsresultat);
        assertThat(avklartEktefelleSamboer.getRegistreringer().isEmpty()).isTrue();
    }

    @Test
    void lagreMedfolgendeFamilieSomAvklartefakta_omfattetFamilieIkkeLagretIBehandlingsgrunnlaget_kasterFeilmelding() {
        AvklarteMedfolgendeFamilie avklarteMedfolgendeFamilie =
            new AvklarteMedfolgendeFamilie(Set.of(new OmfattetFamilie("uuid3")), Set.of());

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie))
            .withMessageContaining("Medfolgende familie som er omfattet av norsk trygd: uuid3 er ikke lagret i behandlingsgrunnlaget.");

        verify(avklarteFaktaRepository, never()).save(captor.capture());
    }

    @Test
    void lagreMedfolgendeFamilieSomAvklartefakta_ikkeOmfattetFamilieIkkeLagretIBehandlingsgrunnlaget_kasterFeilmelding() {
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
    void lagreMedfolgendeFamilieSomAvklartefakta_ugyldigBegrunnelseKodeForBarn_kasterFeilmelding() {
        AvklarteMedfolgendeFamilie avklarteMedfolgendeFamilie =
            new AvklarteMedfolgendeFamilie(Set.of(), Set.of(
                new IkkeOmfattetFamilie(uuidBarn, SAMBOER_UTEN_FELLES_BARN.getKode(), fritekstBarn)));

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie))
            .withMessageContaining("Begrunnelsen til medfolgende barn " + uuidBarn + ": " + SAMBOER_UTEN_FELLES_BARN.getKode() + " er ikke gyldig.");

        verify(avklarteFaktaRepository, never()).save(captor.capture());
    }

    @Test
    void lagreMedfolgendeFamilieSomAvklartefakta_ugyldigBegrunnelseKodeForEktefelleSamboer_kasterFeilmelding() {
        AvklarteMedfolgendeFamilie avklarteMedfolgendeFamilie =
            new AvklarteMedfolgendeFamilie(Set.of(), Set.of(
                new IkkeOmfattetFamilie(uuidEktefelleSamboer, OVER_18_AR.getKode(), fritekstEktefelleSamboer)));

        when(behandlingService.hentBehandlingUtenSaksopplysninger(1L)).thenReturn(mockBehandling());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> avklarteMedfolgendeFamilieService.lagreMedfolgendeFamilieSomAvklartefakta(1L, avklarteMedfolgendeFamilie))
            .withMessageContaining("Begrunnelsen til medfolgende ektefelle/samboer " + uuidEktefelleSamboer + ": " + OVER_18_AR.getKode() + " er ikke gyldig.");

        verify(avklarteFaktaRepository, never()).save(captor.capture());
    }

    @Test
    void lagreMedfolgendeFamilieSomAvklartefakta_ikkeSattBegrunnelseKode_kasterFeilmelding() {
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
