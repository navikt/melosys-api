package no.nav.melosys.service.unntak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.LovvalgsperiodeLagreEvent;
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
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnmodningsperiodeServiceTest {
    private static final long ANMODNINGSPERIODE_ID = 11L;
    private static final long BEHANDLINGS_ID = 22L;
    @Mock
    private AnmodningsperiodeRepository anmodningsperiodeRepository;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private AnmodningsperiodeSvarRepository anmodningsperiodeSvarRepository;
    private AnmodningsperiodeService anmodningsperiodeService;

    @BeforeEach
    public void setUp() {
        anmodningsperiodeService = new AnmodningsperiodeService(anmodningsperiodeRepository,
            anmodningsperiodeSvarRepository, behandlingsresultatService, applicationEventPublisher);
    }

    @Test
    void hentAnmodningsperiode() {
        anmodningsperiodeService.finnAnmodningsperiode(ANMODNINGSPERIODE_ID);
        verify(anmodningsperiodeRepository).findById(ANMODNINGSPERIODE_ID);
    }

    @Test
    void hentAnmodningsperioder() {
        anmodningsperiodeService.hentAnmodningsperioder(BEHANDLINGS_ID);
        verify(anmodningsperiodeRepository).findByBehandlingsresultatId(BEHANDLINGS_ID);
    }

    @Test
    void lagreAnmodningsperioder_ingenSvarRegistrert_mottarLagredePerioder() {
        Anmodningsperiode anmodningsperiode = lagAnmodningsperiode();
        Collection<Anmodningsperiode> anmodningperioder = Collections.singleton(anmodningsperiode);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(anmodningsperiodeRepository.findByBehandlingsresultatId(BEHANDLINGS_ID)).thenReturn(Collections.singletonList(anmodningsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGS_ID)).thenReturn(behandlingsresultat);


        anmodningsperiodeService.lagreAnmodningsperioder(BEHANDLINGS_ID, anmodningperioder);


        verify(anmodningsperiodeRepository).saveAll(anmodningperioder);
        assertThat(anmodningsperiode.getBehandlingsresultat()).isEqualTo(behandlingsresultat);
    }

    @Test
    void lagreAnmodningsperioder_svarErRegistrert_forventFunksjonellException() {
        Anmodningsperiode anmodningsperiode = lagAnmodningsperiode();
        when(anmodningsperiodeRepository.findByBehandlingsresultatId(BEHANDLINGS_ID)).thenReturn(Collections.singletonList(anmodningsperiode));
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningsperiodeService.lagreAnmodningsperioder(BEHANDLINGS_ID, Collections.singleton(anmodningsperiode)))
            .withMessageContaining("svar er registrert");
    }

    @Test
    void lagreAnmodningsperiodeSvar_svarErInnvilgelse_lagrerAnmodningsperiodeSvarOgLovvalgsperiode() {
        Anmodningsperiode anmodningsperiode = mockAnmodningsperiodeIdPåFindById();

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        svar.setAnmodningsperiode(anmodningsperiode);


        anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar);


        verify(anmodningsperiodeSvarRepository).save(any(AnmodningsperiodeSvar.class));
        verify(applicationEventPublisher).publishEvent(any(LovvalgsperiodeLagreEvent.class));
    }

    @Test
    void lagreAnmodningsperiodeSvar_svarErAvslag_lagrerAnmodningsperiodeSvarOgLovvalgsperiode() {
        Anmodningsperiode anmodningsperiode = mockAnmodningsperiodeIdPåFindById();

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        svar.setAnmodningsperiode(anmodningsperiode);


        anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar);


        verify(anmodningsperiodeSvarRepository).save(svar);
        verify(applicationEventPublisher).publishEvent(any(LovvalgsperiodeLagreEvent.class));
    }

    @Test
    void lagreAnmodningsperiodeSvar_svarErDelvisInnvilgelseIngenPeriode_forventFunksjonellException() {
        Anmodningsperiode anmodningsperiode = mockAnmodningsperiodeIdPåFindById();

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.DELVIS_INNVILGELSE);
        svar.setAnmodningsperiode(anmodningsperiode);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar))
            .withMessageContaining("Periode må være fyllt ut ved " + Anmodningsperiodesvartyper.DELVIS_INNVILGELSE);
    }

    @Test
    void lagreAnmodningsperiodeSvar_manglerBehandlingsresultat_forventFunksjonellException() {
        Anmodningsperiode anmodningsperiode = mockAnmodningsperiodeIdPåFindById();
        anmodningsperiode.setBehandlingsresultat(null);

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        svar.setAnmodningsperiode(anmodningsperiode);


        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar))
            .withMessageContaining(Anmodningsperiode.FEIL_VED_HENT_BEHANDLINGSRESULTAT_ID.formatted(ANMODNINGSPERIODE_ID));
    }

    @Test
    void lagreAnmodningsperiodeSvar_svarManglerType_forventFunksjonellException() {
        mockAnmodningsperiodeIdPåFindById();
        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar))
            .withMessageContaining("Må spesifiseres svarType for svar på anmodningsperiode");
    }

    @Test
    void lagreAnmodningsperiodeSvar_ugyldigPeriodeForDelvisInnvilgelse_forventFunksjonellException() {
        mockAnmodningsperiodeIdPåFindById();
        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.DELVIS_INNVILGELSE);
        svar.setInnvilgetFom(LocalDate.now());
        svar.setInnvilgetTom(LocalDate.now().minusYears(2));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(ANMODNINGSPERIODE_ID, svar))
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

    private Anmodningsperiode lagAnmodningsperiode() {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2),
            Landkoder.NO,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);
        anmodningsperiode.setId(ANMODNINGSPERIODE_ID);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(BEHANDLINGS_ID);
        anmodningsperiode.setBehandlingsresultat(behandlingsresultat);
        return anmodningsperiode;
    }

    private Anmodningsperiode mockAnmodningsperiodeIdPåFindById() {
        Anmodningsperiode anmodningsperiode = lagAnmodningsperiode();
        when(anmodningsperiodeRepository.findById(ANMODNINGSPERIODE_ID)).thenReturn(Optional.of(anmodningsperiode));
        return anmodningsperiode;
    }
}
