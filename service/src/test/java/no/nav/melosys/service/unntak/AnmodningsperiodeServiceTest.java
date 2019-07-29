package no.nav.melosys.service.unntak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.AnmodningsperiodeSvarType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.AnmodningsperiodeSvarRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningsperiodeServiceTest {
    @Mock
    private AnmodningsperiodeRepository anmodningsperiodeRepository;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private AnmodningsperiodeSvarRepository anmodningsperiodeSvarRepository;
    private AnmodningsperiodeService anmodningsperiodeService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        anmodningsperiodeService = new AnmodningsperiodeService(anmodningsperiodeRepository, behandlingsresultatService, anmodningsperiodeSvarRepository);
    }

    @Test
    public void hentAnmodningsperiode() {
        long anmodningsperiodeID = 1;
        anmodningsperiodeService.hentAnmodningsperiode(anmodningsperiodeID);
        verify(anmodningsperiodeRepository).findById(eq(anmodningsperiodeID));
    }

    @Test
    public void hentAnmodningsperioder() {
        long behandlingID = 1;
        anmodningsperiodeService.hentAnmodningsperioder(behandlingID);
        verify(anmodningsperiodeRepository).findByBehandlingsresultatId(eq(behandlingID));
    }

    @Test
    public void lagreAnmodningsperioder_ingenSvarRegistrert_mottarLagredePerioder() throws FunksjonellException {
        long behandlingID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, null);
        Collection<Anmodningsperiode> anmodningperioder = Collections.singleton(anmodningsperiode);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(anmodningsperiodeRepository.findByBehandlingsresultatId(2)).thenReturn(Collections.singletonList(anmodningsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(2)).thenReturn(behandlingsresultat);
        anmodningsperiodeService.lagreAnmodningsperioder(behandlingID, anmodningperioder);
        verify(anmodningsperiodeRepository).saveAll(anmodningperioder);
        assertThat(anmodningsperiode.getBehandlingsresultat()).isEqualTo(behandlingsresultat);
    }

    @Test
    public void lagreAnmodningsperioder_svarErRegistrert_forventFunksjonellException() throws FunksjonellException {
        long behandlingID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, new AnmodningsperiodeSvar());

        expectedException.expect(FunksjonellException.class);
        when(anmodningsperiodeRepository.findByBehandlingsresultatId(behandlingID)).thenReturn(Collections.singletonList(anmodningsperiode));
        anmodningsperiodeService.lagreAnmodningsperioder(behandlingID, Collections.singleton(anmodningsperiode));
    }

    @Test
    public void lagreAnmodningsperiodeSvar_svarErInnvilgelse_lagrerAnmodningsperiodeSvar() throws FunksjonellException {
        long anmodningsperiodeID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, new AnmodningsperiodeSvar());

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.INNVILGELSE);
        when(anmodningsperiodeRepository.findById(anmodningsperiodeID)).thenReturn(Optional.of(anmodningsperiode));

        anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, svar);

        verify(anmodningsperiodeSvarRepository).save(any(AnmodningsperiodeSvar.class));
    }

    @Test
    public void lagreAnmodningsperiodeSvar_svarErDelvisInnvilgelseIngenPeriode_forventFunksjonellException() throws FunksjonellException {
        long anmodningsperiodeID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, new AnmodningsperiodeSvar());

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.DELVIS_INNVILGELSE);
        when(anmodningsperiodeRepository.findById(anmodningsperiodeID)).thenReturn(Optional.of(anmodningsperiode));
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Periode og begrunnelse må være fyllt ut ved " + AnmodningsperiodeSvarType.DELVIS_INNVILGELSE);

        anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, svar);
    }

    @Test
    public void lagreAnmodningsperiodeSvar_svarErAvslagIngenBegrunnelse_forventFunksjonellException() throws FunksjonellException {
        long anmodningsperiodeID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, new AnmodningsperiodeSvar());

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.AVSLAG);
        when(anmodningsperiodeRepository.findById(anmodningsperiodeID)).thenReturn(Optional.of(anmodningsperiode));
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Begrunnelse må være fyllt ut ved " + AnmodningsperiodeSvarType.AVSLAG);

        anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, svar);
    }

    @Test
    public void lagreAnmodningsperiodeSvar_svarManglerType_forventFunksjonellException() throws FunksjonellException {
        long anmodningsperiodeID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, new AnmodningsperiodeSvar());

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.AVSLAG);
        when(anmodningsperiodeRepository.findById(anmodningsperiodeID)).thenReturn(Optional.of(anmodningsperiode));
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Begrunnelse må være fyllt ut ved " + AnmodningsperiodeSvarType.AVSLAG);

        anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, svar);
    }
}