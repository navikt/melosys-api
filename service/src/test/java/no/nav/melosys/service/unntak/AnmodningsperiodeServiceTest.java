package no.nav.melosys.service.unntak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.AnmodningsperiodeSvarRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnmodningsperiodeServiceTest {
    @Mock
    private AnmodningsperiodeRepository anmodningsperiodeRepository;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private AnmodningsperiodeSvarRepository anmodningsperiodeSvarRepository;

    private AnmodningsperiodeService anmodningsperiodeService;

    @BeforeEach
    public void setUp() {
        anmodningsperiodeService = new AnmodningsperiodeService(anmodningsperiodeRepository, anmodningsperiodeSvarRepository,
                                                                behandlingsresultatService);
    }

    @Test
    void hentAnmodningsperiode() {
        long anmodningsperiodeID = 1;
        anmodningsperiodeService.finnAnmodningsperiode(anmodningsperiodeID);
        verify(anmodningsperiodeRepository).findById(anmodningsperiodeID);
    }

    @Test
    void hentAnmodningsperioder() {
        long behandlingID = 1;
        anmodningsperiodeService.hentAnmodningsperioder(behandlingID);
        verify(anmodningsperiodeRepository).findByBehandlingsresultatId(behandlingID);
    }

    @Test
    void lagreAnmodningsperioder_ingenSvarRegistrert_mottarLagredePerioder() {
        long behandlingID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);
        Collection<Anmodningsperiode> anmodningperioder = Collections.singleton(anmodningsperiode);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(anmodningsperiodeRepository.findByBehandlingsresultatId(2)).thenReturn(Collections.singletonList(anmodningsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(2)).thenReturn(behandlingsresultat);
        anmodningsperiodeService.lagreAnmodningsperioder(behandlingID, anmodningperioder);
        verify(anmodningsperiodeRepository).saveAll(anmodningperioder);
        assertThat(anmodningsperiode.getBehandlingsresultat()).isEqualTo(behandlingsresultat);
    }

    @Test
    void lagreAnmodningsperioder_svarErRegistrert_forventFunksjonellException() {
        long behandlingID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());

        when(anmodningsperiodeRepository.findByBehandlingsresultatId(behandlingID)).thenReturn(Collections.singletonList(anmodningsperiode));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningsperiodeService.lagreAnmodningsperioder(behandlingID, Collections.singleton(anmodningsperiode)))
            .withMessageContaining("svar er registrert");
    }

    @Test
    void lagreAnmodningsperiodeSvar_svarErInnvilgelse_lagrerAnmodningsperiodeSvar() {
        long anmodningsperiodeID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        svar.setAnmodningsperiode(anmodningsperiode);
        when(anmodningsperiodeRepository.findById(anmodningsperiodeID)).thenReturn(Optional.of(anmodningsperiode));

        anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, svar);

        verify(anmodningsperiodeSvarRepository).save(any(AnmodningsperiodeSvar.class));
    }

    @Test
    void lagreAnmodningsperiodeSvar_svarErDelvisInnvilgelseIngenPeriode_forventFunksjonellException() {
        long anmodningsperiodeID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.DELVIS_INNVILGELSE);
        svar.setAnmodningsperiode(anmodningsperiode);
        when(anmodningsperiodeRepository.findById(anmodningsperiodeID)).thenReturn(Optional.of(anmodningsperiode));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, svar))
            .withMessageContaining("Periode må være fyllt ut ved " + Anmodningsperiodesvartyper.DELVIS_INNVILGELSE);
    }

    @Test
    void lagreAnmodningsperiodeSvar_svarErAvslag_ingenFeil() {
        long anmodningsperiodeID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        svar.setAnmodningsperiode(anmodningsperiode);
        when(anmodningsperiodeRepository.findById(anmodningsperiodeID)).thenReturn(Optional.of(anmodningsperiode));

        anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, svar);

        verify(anmodningsperiodeSvarRepository).save(svar);
    }

    @Test
    void lagreAnmodningsperiodeSvar_svarManglerType_forventFunksjonellException() {
        long anmodningsperiodeID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        when(anmodningsperiodeRepository.findById(anmodningsperiodeID)).thenReturn(Optional.of(anmodningsperiode));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, svar))
            .withMessageContaining("Må spesifiseres svarType for svar på anmodningsperiode");
    }

    @Test
    void lagreAnmodningsperiodeSvar_ugyldigPeriodeForDelvisInnvilgelse_forventFunksjonellException() {
        long anmodningsperiodeID = 2;
        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.DELVIS_INNVILGELSE);
        svar.setInnvilgetFom(LocalDate.now());
        svar.setInnvilgetTom(LocalDate.now().minusYears(2));
        when(anmodningsperiodeRepository.findById(anmodningsperiodeID)).thenReturn(Optional.of(new Anmodningsperiode()));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, svar))
            .withMessageContaining("Periode er ikke gyldig");
    }

    @Test
    void oppdaterAnmodningsperiodeSendtForBehandling_verifiserOppdatert() {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        when(anmodningsperiodeRepository.findByBehandlingsresultatId(anyLong())).thenReturn(Collections.singletonList(anmodningsperiode));
        anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(1L);

        assertThat(anmodningsperiode.erSendtUtland()).isTrue();
        verify(anmodningsperiodeRepository).save(anmodningsperiode);
    }

    @Test
    void oppdaterAnmodetAvForBehandling_erIkkeSattFraFør_oppdateres() {
        final var anmodetAv = "MEG";
        var anmodningsperiode = new Anmodningsperiode();
        when(anmodningsperiodeRepository.findByBehandlingsresultatId(anyLong())).thenReturn(Collections.singletonList(anmodningsperiode));

        anmodningsperiodeService.oppdaterAnmodetAvForBehandling(1L, anmodetAv);
        assertThat(anmodningsperiode.getAnmodetAv()).isEqualTo(anmodetAv);
        verify(anmodningsperiodeRepository).save(anmodningsperiode);
    }

    @Test
    void oppdaterAnmodetAvForBehandling_erSattFraFør_kasterException() {
        var anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodetAv("DEG");
        when(anmodningsperiodeRepository.findByBehandlingsresultatId(anyLong())).thenReturn(Collections.singletonList(anmodningsperiode));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningsperiodeService.oppdaterAnmodetAvForBehandling(1L, "MEG"))
            .withMessageContaining("allerede anmodet av DEG");
    }
}
