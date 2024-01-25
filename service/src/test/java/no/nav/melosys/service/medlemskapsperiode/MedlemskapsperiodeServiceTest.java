package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import io.getunleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avgift.Inntektsperiode;
import no.nav.melosys.domain.avgift.Penger;
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge;
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.MedlemskapsperiodeRepository;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedlemskapsperiodeServiceTest {

    @Mock
    private MedlemskapsperiodeRepository medlemskapsperiodeRepositoryMock;
    @Mock
    private MedlemAvFolketrygdenService medlemAvFolketrygdenServiceMock;
    @Mock
    private TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagServiceMock;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;

    private FakeUnleash fakeUnleash;

    private MedlemskapsperiodeService medlemskapsperiodeService;

    @Captor
    private ArgumentCaptor<Medlemskapsperiode> medlemskapsperiodeCaptor;

    private final long behandlingsresultatID = 14L;
    private final long medlemskapsperiodeID = 432L;

    @BeforeEach
    void setup() {
        medlemskapsperiodeService = new MedlemskapsperiodeService(medlemskapsperiodeRepositoryMock,
            medlemAvFolketrygdenServiceMock,
            trygdeavgiftsgrunnlagServiceMock,
            behandlingsresultatService,
            medlPeriodeService,
            fakeUnleash);
    }

    @Test
    void hentMedlemskapsperioder() {
        medlemskapsperiodeService.hentMedlemskapsperioder(1L);
        verify(medlemAvFolketrygdenServiceMock).finnMedlemAvFolketrygden(1L);
    }

    @Test
    void opprettMedlemskapsperiode_lagrerKorrektMedlemskapsperiode() {
        MedlemAvFolketrygden medlemAvFolketrygden = lagMedlemAvFolketrygden();
        medlemAvFolketrygden.setFastsattTrygdeavgift(lagFastsattTrygdeavgift());
        medlemAvFolketrygden.setBehandlingsresultat(lagBehandlingsresultat());
        when(medlemAvFolketrygdenServiceMock.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(medlemAvFolketrygden);


        medlemskapsperiodeService.opprettMedlemskapsperiode(behandlingsresultatID, LocalDate.now().minusYears(1), LocalDate.now(),
            InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD);


        verify(medlemskapsperiodeRepositoryMock).save(medlemskapsperiodeCaptor.capture());
        verify(trygdeavgiftsgrunnlagServiceMock).fjernTrygdeavgiftsperioderOmDeFinnes(any());
        assertThat(medlemskapsperiodeCaptor.getValue()).isNotNull()
            .extracting(
                Medlemskapsperiode::getArbeidsland,
                Medlemskapsperiode::getInnvilgelsesresultat,
                Medlemskapsperiode::getTrygdedekning,
                Medlemskapsperiode::getBestemmelse,
                Medlemskapsperiode::getMedlemskapstype)
            .containsExactly(
                Land_iso2.AU.getKode(),
                InnvilgelsesResultat.AVSLAATT,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD,
                Medlemskapstyper.FRIVILLIG);
    }

    @Test
    void oppdaterMedlemskapsperiode_medlemskapsperiodeFinnes_oppdateres() {
        final var medlemskapsperiode = lagMedlemskapsperiode();
        MedlemAvFolketrygden medlemAvFolketrygden = lagMedlemAvFolketrygden(medlemskapsperiode);
        medlemskapsperiode.setMedlemAvFolketrygden(medlemAvFolketrygden);
        when(medlemAvFolketrygdenServiceMock.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(medlemAvFolketrygden);

        LocalDate nå = LocalDate.now();
        medlemskapsperiodeService.oppdaterMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID, nå, nå,
            InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD);

        verify(medlemskapsperiodeRepositoryMock).save(medlemskapsperiode);
        verifyNoInteractions(trygdeavgiftsgrunnlagServiceMock);
        assertThat(medlemskapsperiode)
            .extracting(
                Medlemskapsperiode::getFom,
                Medlemskapsperiode::getTom,
                Medlemskapsperiode::getInnvilgelsesresultat,
                Medlemskapsperiode::getBestemmelse,
                Medlemskapsperiode::getTrygdedekning)
            .containsExactly(
                nå,
                nå,
                InnvilgelsesResultat.AVSLAATT,
                Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1_FØRSTE_LEDD,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER);
    }

    @Test
    void oppdaterMedlemskapsperiode_medlemskapsperiodeOgFastsattTrygdeavgiftFinnes_oppdateres() {
        final var medlemskapsperiode = lagMedlemskapsperiode();
        MedlemAvFolketrygden medlemAvFolketrygden = lagMedlemAvFolketrygden(medlemskapsperiode);
        medlemAvFolketrygden.setFastsattTrygdeavgift(lagFastsattTrygdeavgift());
        medlemskapsperiode.setMedlemAvFolketrygden(medlemAvFolketrygden);
        when(medlemAvFolketrygdenServiceMock.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(medlemAvFolketrygden);

        LocalDate nå = LocalDate.now();
        medlemskapsperiodeService.oppdaterMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID, nå, nå,
            InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD);

        verify(medlemskapsperiodeRepositoryMock).save(medlemskapsperiode);
        assertThat(medlemskapsperiode)
            .extracting(Medlemskapsperiode::getFom, Medlemskapsperiode::getTom,
                Medlemskapsperiode::getInnvilgelsesresultat, Medlemskapsperiode::getTrygdedekning)
            .containsExactly(nå, nå, InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER);
        verify(trygdeavgiftsgrunnlagServiceMock).fjernTrygdeavgiftsperioderOmDeFinnes(any());
    }

    private FastsattTrygdeavgift lagFastsattTrygdeavgift() {
        FastsattTrygdeavgift fastsattTrygdeavgift = new FastsattTrygdeavgift();
        fastsattTrygdeavgift.setId(1L);

        Trygdeavgiftsgrunnlag trygdeavgiftsgrunnlag = new Trygdeavgiftsgrunnlag();
        trygdeavgiftsgrunnlag.setId(1L);

        Inntektsperiode inntektsperiode = new Inntektsperiode();
        inntektsperiode.setFomDato(LocalDate.of(2023, 1, 1));
        inntektsperiode.setTomDato(LocalDate.of(2023, 12, 1));
        inntektsperiode.setType(Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE);
        inntektsperiode.setAvgiftspliktigInntektMnd(new Penger(100.00));
        List<Inntektsperiode> inntektsperiodeList = List.of(inntektsperiode);

        SkatteforholdTilNorge skatteforholdTilNorge = new SkatteforholdTilNorge();
        skatteforholdTilNorge.setFomDato(LocalDate.of(2023, 1, 1));
        skatteforholdTilNorge.setTomDato(LocalDate.of(2023, 12, 1));
        skatteforholdTilNorge.setSkatteplikttype(Skatteplikttype.SKATTEPLIKTIG);
        List<SkatteforholdTilNorge> skatteforholdTilNorgeList = List.of(skatteforholdTilNorge);

        trygdeavgiftsgrunnlag.setInntektsperioder(inntektsperiodeList);
        trygdeavgiftsgrunnlag.setSkatteforholdTilNorge(skatteforholdTilNorgeList);
        fastsattTrygdeavgift.setTrygdeavgiftsgrunnlag(trygdeavgiftsgrunnlag);
        return fastsattTrygdeavgift;
    }

    @Test
    void oppdaterMedlemskapsperiode_trygdedekningStøttesIkke_kasterException() {
        final var medlemskapsperiode = lagMedlemskapsperiode();
        when(medlemAvFolketrygdenServiceMock.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden(medlemskapsperiode));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> medlemskapsperiodeService.oppdaterMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID, LocalDate.now(),
                LocalDate.now(), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FULL_DEKNING_EOSFO, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD))
            .withMessageContaining("støttes ikke for en medlemskapsperiode");
    }

    @Test
    void oppdaterMedlemskapsperiode_tomDatoErFørFomDato_kasterException() {
        final var medlemskapsperiode = lagMedlemskapsperiode();
        when(medlemAvFolketrygdenServiceMock.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden(medlemskapsperiode));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> medlemskapsperiodeService.oppdaterMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID, LocalDate.now(),
                LocalDate.now().minusDays(1), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD))
            .withMessageContaining("kan ikke være før");
    }

    @Test
    void oppdaterMedlemskapsperiode_utenTrygdedekning_kasterFeil() {
        when(medlemAvFolketrygdenServiceMock.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden(lagMedlemskapsperiode()));

        LocalDate nå = LocalDate.now();
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> medlemskapsperiodeService.oppdaterMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID, nå, nå,
                InnvilgelsesResultat.AVSLAATT, null, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD))
            .withMessageContaining("er påkrevd");
    }

    @Test
    void oppdaterMedlemskapsperiode_utenBestemmelse_kasterFeil() {
        when(medlemAvFolketrygdenServiceMock.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden(lagMedlemskapsperiode()));

        LocalDate nå = LocalDate.now();
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> medlemskapsperiodeService.oppdaterMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID, nå, nå,
                InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON, null))
            .withMessageContaining("er påkrevd");
    }

    @Test
    void oppdaterMedlemskapsperiode_finnerIkkeMedlemskapsperiode_kasterException() {
        when(medlemAvFolketrygdenServiceMock.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden());
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> medlemskapsperiodeService.oppdaterMedlemskapsperiode(behandlingsresultatID, 0, LocalDate.now(),
                LocalDate.now(), InnvilgelsesResultat.DELVIS_INNVILGET, Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD))
            .withMessageContaining("har ingen");
    }

    @Test
    void slettMedlemskapsperiode_finnerIkkeMedlemskapsperiode_kasterException() {
        when(medlemAvFolketrygdenServiceMock.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> medlemskapsperiodeService.slettMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID))
            .withMessageContaining("ingen medlemskapsperiode med id");
    }

    @Test
    void slettMedlemskapsperiode_finnesToMedlemskapsperioder_slettes() {
        var medlemskapsperiode1 = lagMedlemskapsperiode();
        var medlemskapsperiode2 = lagMedlemskapsperiode();
        medlemskapsperiode2.setId(123321L);
        var medlemAvFolketrygden = lagMedlemAvFolketrygden(medlemskapsperiode1, medlemskapsperiode2);
        when(medlemAvFolketrygdenServiceMock.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(medlemAvFolketrygden);


        medlemskapsperiodeService.slettMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID);


        verifyNoInteractions(trygdeavgiftsgrunnlagServiceMock);
        assertThat(medlemAvFolketrygden.getMedlemskapsperioder()).hasSize(1);
    }

    @Test
    void slettMedlemskapsperiode_fjernTrygdeavgiftsperioder_slettes() {
        var medlemskapsperiode1 = lagMedlemskapsperiode();
        var medlemskapsperiode2 = lagMedlemskapsperiode();
        medlemskapsperiode2.setId(123321L);
        var medlemAvFolketrygden = lagMedlemAvFolketrygden(medlemskapsperiode1, medlemskapsperiode2);
        medlemAvFolketrygden.setFastsattTrygdeavgift(lagFastsattTrygdeavgift());
        when(medlemAvFolketrygdenServiceMock.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(medlemAvFolketrygden);


        medlemskapsperiodeService.slettMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID);


        verify(trygdeavgiftsgrunnlagServiceMock).fjernTrygdeavgiftsperioderOmDeFinnes(any());
        assertThat(medlemAvFolketrygden.getMedlemskapsperioder()).hasSize(1);
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(new Behandling());
        behandlingsresultat.getBehandling().setMottatteOpplysninger(new MottatteOpplysninger());
        var søknad = new SøknadNorgeEllerUtenforEØS();
        søknad.soeknadsland = new Soeknadsland(List.of(Land_iso2.AU.getKode()), false);
        behandlingsresultat.getBehandling().getMottatteOpplysninger().setMottatteOpplysningerData(søknad);
        return behandlingsresultat;
    }

    private MedlemAvFolketrygden lagMedlemAvFolketrygden(Medlemskapsperiode... medlemskapsperioder) {
        MedlemAvFolketrygden medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setMedlemskapsperioder(new LinkedList<>(Arrays.asList(medlemskapsperioder)));
        return medlemAvFolketrygden;
    }

    private Medlemskapsperiode lagMedlemskapsperiode() {
        var medlemskapsperiode = new Medlemskapsperiode();
        medlemskapsperiode.setId(medlemskapsperiodeID);
        medlemskapsperiode.setFom(LocalDate.now());
        medlemskapsperiode.setTom(LocalDate.now().plusYears(1));
        medlemskapsperiode.setTrygdedekning(Trygdedekninger.FULL_DEKNING_FTRL);
        medlemskapsperiode.setMedlemskapstype(Medlemskapstyper.PLIKTIG);
        medlemskapsperiode.setArbeidsland("BR");
        medlemskapsperiode.setBestemmelse(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD);
        return medlemskapsperiode;
    }
}
