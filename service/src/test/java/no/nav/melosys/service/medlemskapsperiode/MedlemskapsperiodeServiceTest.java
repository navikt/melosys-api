package no.nav.melosys.service.medlemskapsperiode;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.MedlemskapsperiodeRepository;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedlemskapsperiodeServiceTest {

    @Mock
    private MedlemskapsperiodeRepository medlemskapsperiodeRepository;
    @Mock
    private MedlemAvFolketrygdenService medlemAvFolketrygdenService;

    private MedlemskapsperiodeService medlemskapsperiodeService;

    @Captor
    private ArgumentCaptor<Medlemskapsperiode> medlemskapsperiodeCaptor;

    private final long behandlingsresultatID = 14L;
    private final long medlemskapsperiodeID = 432L;

    @BeforeEach
    void setup() {
        medlemskapsperiodeService = new MedlemskapsperiodeService(medlemskapsperiodeRepository, medlemAvFolketrygdenService);
    }

    @Test
    void hentMedlemskapsperioder() {
        medlemskapsperiodeService.hentMedlemskapsperioder(1L);
        verify(medlemAvFolketrygdenService).finnMedlemAvFolketrygden(1L);
    }

    @Test
    void opprettMedlemskapsperiode_finnesIngenEksisterende_kasterException() {
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> medlemskapsperiodeService.opprettMedlemskapsperiode(
                behandlingsresultatID, LocalDate.now(), LocalDate.now().plusYears(1),
                InnvilgelsesResultat.DELVIS_INNVILGET, Trygdedekninger.FULL_DEKNING_FTRL))
            .withMessageContaining("ingen medlemskapsperiode");
    }

    @Test
    void opprettMedlemskapsperiode_finnesEksisterende_verifiserFårSammeArbeidslandOgBestemmelse() {
        final var eksisterende = lagMedlemskapsperiode();
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden(eksisterende));

        medlemskapsperiodeService.opprettMedlemskapsperiode(behandlingsresultatID, LocalDate.now().minusYears(1), LocalDate.now(),
            InnvilgelsesResultat.AVSLAATT, Trygdedekninger.HELSEDEL);

        verify(medlemskapsperiodeRepository).save(medlemskapsperiodeCaptor.capture());
        assertThat(medlemskapsperiodeCaptor.getValue()).isNotNull()
            .extracting(
                Medlemskapsperiode::getArbeidsland,
                Medlemskapsperiode::getInnvilgelsesresultat,
                Medlemskapsperiode::getTrygdedekning,
                Medlemskapsperiode::getMedlemskapstype)
            .containsExactly(
                eksisterende.getArbeidsland(),
                InnvilgelsesResultat.AVSLAATT,
                Trygdedekninger.HELSEDEL,
                eksisterende.getMedlemskapstype());
    }

    @Test
    void oppdaterMedlemskapsperiode_medlemskapsperoideFinnes_oppdateres() {
        final var medlemskapsperiode = lagMedlemskapsperiode();
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden(medlemskapsperiode));

        LocalDate nå = LocalDate.now();
        medlemskapsperiodeService.oppdaterMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID, nå, nå,
            InnvilgelsesResultat.AVSLAATT, Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER);

        verify(medlemskapsperiodeRepository).save(medlemskapsperiode);
        assertThat(medlemskapsperiode)
            .extracting(Medlemskapsperiode::getFom, Medlemskapsperiode::getTom,
                Medlemskapsperiode::getInnvilgelsesresultat, Medlemskapsperiode::getTrygdedekning)
            .containsExactly(nå, nå, InnvilgelsesResultat.AVSLAATT, Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER);
    }

    @Test
    void oppdaterMedlemskapsperiode_trygdedekningStøttesIkke_kasterException() {
        final var medlemskapsperiode = lagMedlemskapsperiode();
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden(medlemskapsperiode));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> medlemskapsperiodeService.oppdaterMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID, LocalDate.now(),
                LocalDate.now(), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.FULL_DEKNING_EOSFO))
            .withMessageContaining("støttes ikke for en medlemskapsperiode");
    }

    @Test
    void oppdaterMedlemskapsperiode_tomDatoErFørFomDato_kasterException() {
        final var medlemskapsperiode = lagMedlemskapsperiode();
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden(medlemskapsperiode));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> medlemskapsperiodeService.oppdaterMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID, LocalDate.now(),
                LocalDate.now().minusDays(1), InnvilgelsesResultat.AVSLAATT, Trygdedekninger.HELSE_OG_PENSJONSDEL))
            .withMessageContaining("kan ikke være før");
    }

    @Test
    void oppdaterMedlemskapsperiode_utenTrygdedekning_oppdateres() {
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden(lagMedlemskapsperiode()));

        LocalDate nå = LocalDate.now();
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> medlemskapsperiodeService.oppdaterMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID, nå, nå,
                InnvilgelsesResultat.AVSLAATT, null))
            .withMessageContaining("er påkrevd");
    }

    @Test
    void oppdaterMedlemskapsperiode_finnerIkkeMedlemskapsperiode_kasterException() {
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden());
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> medlemskapsperiodeService.oppdaterMedlemskapsperiode(behandlingsresultatID, 0, LocalDate.now(),
                LocalDate.now(), InnvilgelsesResultat.DELVIS_INNVILGET, Trygdedekninger.HELSE_OG_PENSJONSDEL))
            .withMessageContaining("har ingen");
    }

    @Test
    void slettMedlemskapsperiode_erEnesteMedlemskapsperiode_kasterException() {
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(lagMedlemAvFolketrygden(lagMedlemskapsperiode()));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> medlemskapsperiodeService.slettMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID))
            .withMessageContaining("minst en medlemskapsperiode");
    }

    @Test
    void slettMedlemskapsperiode_finnerIkkeMedlemskapsperiode_kasterException() {
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID))
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
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID))
            .thenReturn(medlemAvFolketrygden);

        medlemskapsperiodeService.slettMedlemskapsperiode(behandlingsresultatID, medlemskapsperiodeID);
        assertThat(medlemAvFolketrygden.getMedlemskapsperioder()).hasSize(1);
    }

    private MedlemAvFolketrygden lagMedlemAvFolketrygden(Medlemskapsperiode... medlemskapsperioder) {
        MedlemAvFolketrygden medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setMedlemskapsperioder(new LinkedList<>(Arrays.asList(medlemskapsperioder)));
        medlemAvFolketrygden.setBestemmelse(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_ANDRE_LEDD);
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
        return medlemskapsperiode;
    }
}
