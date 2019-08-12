package no.nav.melosys.service.unntak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.AnmodningsperiodeSvarRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.LovvalgsperiodeService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningsperiodeServiceTest {
    @Mock
    private AnmodningsperiodeRepository anmodningsperiodeRepository;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private AnmodningsperiodeSvarRepository anmodningsperiodeSvarRepository;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    private AnmodningsperiodeService anmodningsperiodeService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        anmodningsperiodeService = new AnmodningsperiodeService(anmodningsperiodeRepository, behandlingsresultatService, anmodningsperiodeSvarRepository, lovvalgsperiodeService);
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
            null, Landkoder.SE, LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);
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
            null, Landkoder.SE, LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());

        expectedException.expect(FunksjonellException.class);
        when(anmodningsperiodeRepository.findByBehandlingsresultatId(behandlingID)).thenReturn(Collections.singletonList(anmodningsperiode));
        anmodningsperiodeService.lagreAnmodningsperioder(behandlingID, Collections.singleton(anmodningsperiode));
    }

    @Test
    public void lagreAnmodningsperiodeSvar_svarErInnvilgelse_lagrerAnmodningsperiodeSvar() throws FunksjonellException {
        long anmodningsperiodeID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);

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
            null, Landkoder.SE, LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.DELVIS_INNVILGELSE);
        when(anmodningsperiodeRepository.findById(anmodningsperiodeID)).thenReturn(Optional.of(anmodningsperiode));
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Periode må være fyllt ut ved " + AnmodningsperiodeSvarType.DELVIS_INNVILGELSE);

        anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, svar);
    }

    @Test
    public void lagreAnmodningsperiodeSvar_svarErAvslag_ingenFeil() throws FunksjonellException {
        long anmodningsperiodeID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.AVSLAG);
        when(anmodningsperiodeRepository.findById(anmodningsperiodeID)).thenReturn(Optional.of(anmodningsperiode));

        anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, svar);

        verify(anmodningsperiodeSvarRepository).save(svar);
    }

    @Test
    public void lagreAnmodningsperiodeSvar_svarManglerType_forventFunksjonellException() throws FunksjonellException {
        long anmodningsperiodeID = 2;
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        when(anmodningsperiodeRepository.findById(anmodningsperiodeID)).thenReturn(Optional.of(anmodningsperiode));
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Må spesifiseres svarType for svar på anmodningsperiode");

        anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, svar);
    }

    @Test
    public void oppdaterAnmodningsperiodeSendtForBehandling_verifiserOppdatert() throws FunksjonellException {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        when(anmodningsperiodeRepository.findByBehandlingsresultatId(anyLong())).thenReturn(Collections.singletonList(anmodningsperiode));
        anmodningsperiodeService.oppdaterAnmodningsperiodeSendtForBehandling(1L);

        assertThat(anmodningsperiode.erSendtUtland()).isTrue();
        verify(anmodningsperiodeRepository).save(anmodningsperiode);
    }

    @Test
    public void opprettLovvalgsperiodeFraAnmodningsperiode_innvilgelse_verifiserLovvalgsperiodeLagret() throws FunksjonellException, TekniskException {

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setBehandlingsresultat(behandlingsresultat);
        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.INNVILGELSE);
        anmodningsperiode.setAnmodningsperiodeSvar(svar);
        when(anmodningsperiodeRepository.findByBehandlingsresultatId(1L)).thenReturn(Collections.singletonList(anmodningsperiode));

        anmodningsperiodeService.opprettLovvalgsperiodeFraAnmodningsperiode(1L , Medlemskapstyper.PLIKTIG);
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(eq(1L), anyCollection());
    }

    @Test
    public void opprettLovvalgsperiodeFraAnmodningsperiode_delvisInnvilgelse_verifiserLovvalgsperiodeLagret() throws FunksjonellException, TekniskException {

        LocalDate nå = LocalDate.now();
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.DELVIS_INNVILGELSE);
        anmodningsperiodeSvar.setInnvilgetFom(nå);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);
        anmodningsperiode.setBehandlingsresultat(behandlingsresultat);
        when(anmodningsperiodeRepository.findByBehandlingsresultatId(1L)).thenReturn(Collections.singletonList(anmodningsperiode));

        ArgumentCaptor<Collection<Lovvalgsperiode>> captor = ArgumentCaptor.forClass(Collection.class);

        anmodningsperiodeService.opprettLovvalgsperiodeFraAnmodningsperiode(1L, Medlemskapstyper.PLIKTIG);
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(eq(1L), captor.capture());

        Lovvalgsperiode lovvalgsperiode = captor.getValue().stream().findFirst()
            .orElseThrow(() -> new TekniskException("Ingen lovvalgsperiode registrert"));

        assertThat(lovvalgsperiode.getFom()).isEqualTo(anmodningsperiodeSvar.getInnvilgetFom());
    }

    @Test
    public void opprettLovvalgsperiode_avslag_verifiserLovvalgsperiodeLagretMedResultatAvslaatt() throws FunksjonellException, TekniskException {
        LocalDate nå = LocalDate.now();
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(AnmodningsperiodeSvarType.AVSLAG);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(nå, nå.plusYears(1), Landkoder.NO, LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, null, null);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);
        anmodningsperiode.setBehandlingsresultat(behandlingsresultat);

        when(anmodningsperiodeRepository.findByBehandlingsresultatId(1L)).thenReturn(Collections.singletonList(anmodningsperiode));

        ArgumentCaptor<Collection<Lovvalgsperiode>> captor = ArgumentCaptor.forClass(Collection.class);

        anmodningsperiodeService.opprettLovvalgsperiodeFraAnmodningsperiode(1L, Medlemskapstyper.PLIKTIG);
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(eq(1L), captor.capture());

        Lovvalgsperiode lovvalgsperiode = captor.getValue().stream().findFirst()
            .orElseThrow(() -> new TekniskException("Ingen lovvalgsperiode registrert"));

        assertThat(lovvalgsperiode.getFom()).isEqualTo(anmodningsperiode.getFom());
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.AVSLAATT);
    }
}