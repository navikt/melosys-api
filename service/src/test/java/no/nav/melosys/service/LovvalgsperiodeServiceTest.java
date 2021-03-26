package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.GrunnlagMedl;
import no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LovvalgsperiodeServiceTest {

    private final BehandlingRepository behandlingRepositoryMock = mock(BehandlingRepository.class);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private LovvalgsperiodeService instanse;

    private static final Collection<Lovvalgsperiode> LOVVALGSPERIODER = Collections.singletonList(new Lovvalgsperiode());
    private LovvalgsperiodeRepository lovvalgsperiodeRepositoryMock = mock(LovvalgsperiodeRepository.class);

    @BeforeEach
    public void setUp() {
        TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository = lagTidligerePerioderRepo();

        lovvalgsperiodeRepositoryMock = mockLovvalgsperiodeRepo();
        this.instanse = new LovvalgsperiodeService(mockBehandlingsresultatRepo(), lovvalgsperiodeRepositoryMock, tidligereMedlemsperiodeRepository, behandlingRepositoryMock);
    }

    private static TidligereMedlemsperiodeRepository lagTidligerePerioderRepo() {
        TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository = mock(TidligereMedlemsperiodeRepository.class);
        TidligereMedlemsperiodeId medlemsperiodeId = new TidligereMedlemsperiodeId();
        medlemsperiodeId.setPeriodeId(23L);

        TidligereMedlemsperiode tidligerePeriode = new TidligereMedlemsperiode();
        tidligerePeriode.setId(medlemsperiodeId);

        when(tidligereMedlemsperiodeRepository.findById_BehandlingId(eq(1L))).thenReturn(Collections.singletonList(tidligerePeriode));
        return tidligereMedlemsperiodeRepository;
    }

    private static LovvalgsperiodeRepository mockLovvalgsperiodeRepo() {
        LovvalgsperiodeRepository mock = mock(LovvalgsperiodeRepository.class);
        @SuppressWarnings("unchecked")
        Collection<Lovvalgsperiode> anyCollection = any(Collection.class);
        when(mock.saveAll(anyCollection)).thenAnswer(i -> i.getArgument(0));
        return mock;
    }

    private static BehandlingsresultatRepository mockBehandlingsresultatRepo() {
        BehandlingsresultatRepository mock = mock(BehandlingsresultatRepository.class);
        when(mock.findById(eq(13L))).thenReturn(Optional.of(new Behandlingsresultat()));
        return mock;
    }

    @Test
    void hentIngenLovvalgsperioderGirTomListe() {
        Collection<Lovvalgsperiode> resultat = instanse.hentLovvalgsperioder(42L);
        assertThat(resultat).isEmpty();
    }

    @Test
    void lagreLovvalgsperioderGirKopiMedBehandlingsresultat() {
        assertThat(LOVVALGSPERIODER.iterator().next().getBehandlingsresultat()).isNull();
        Collection<Lovvalgsperiode> resultat = instanse.lagreLovvalgsperioder(13L, LOVVALGSPERIODER);
        assertThat(resultat).size().isEqualTo(LOVVALGSPERIODER.size());
        assertThat(resultat.iterator().next().getBehandlingsresultat()).isNotNull();
    }

    @Test
    void lagreLovvalgsperioderUtenBehandlingsresultatKasterException() {
        Throwable thrown = catchThrowable(() ->
            instanse.lagreLovvalgsperioder(42L, LOVVALGSPERIODER)
        );
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessageEndingWith("fins ikke.");
    }

    @Test
    void tidligereLovvalgsperioder_enValgtMedlemsperiode_returnererEnTidligerLovvalgsperiode() throws TekniskException {
        Medlemsperiode medlemsperiode = lagMedlemsperiode(23L, GrunnlagMedl.FO_12_2.getKode());
        Medlemsperiode medlemsperiodeFeilId = lagMedlemsperiode(46L, GrunnlagMedl.FO_12_2.getKode());

        MedlemskapDokument medlDokument = new MedlemskapDokument();
        medlDokument.getMedlemsperiode().add(medlemsperiode);
        medlDokument.getMedlemsperiode().add(medlemsperiodeFeilId);

        Behandling behandling = lagBehandlingMedMedlOpplysning(medlDokument);

        assertThat(instanse.hentTidligereLovvalgsperioder(behandling))
            .flatExtracting(
                Lovvalgsperiode::getMedlPeriodeID,
                Lovvalgsperiode::getFom,
                Lovvalgsperiode::getTom,
                Lovvalgsperiode::getBestemmelse
            ).containsExactly(
                medlemsperiode.id,
                medlemsperiode.getPeriode().getFom(),
                medlemsperiode.getPeriode().getTom(),
                MedlPeriodeKonverter.tilLovvalgBestemmelse(GrunnlagMedl.valueOf(medlemsperiode.getGrunnlagstype()))
            );
    }

    @Test
    void tidligerePerioder_ukjentGrunnlagskodeMedl_grunnlagMappetTilAnnet() throws TekniskException {
        Medlemsperiode medlemsperiode = lagMedlemsperiode(23L, "AV_ANNET"); // Eksempel på mapping som ikke melosys kjenner til

        MedlemskapDokument medlDokument = new MedlemskapDokument();
        medlDokument.getMedlemsperiode().add(medlemsperiode);

        Behandling behandling = lagBehandlingMedMedlOpplysning(medlDokument);

        Collection<Lovvalgsperiode> lovvalgsperioder = instanse.hentTidligereLovvalgsperioder(behandling);
        AssertionsForInterfaceTypes.assertThat(lovvalgsperioder.stream().map(Lovvalgsperiode::getMedlPeriodeID)).containsOnly(medlemsperiode.id);
        AssertionsForInterfaceTypes.assertThat(lovvalgsperioder.stream()
                .map(Lovvalgsperiode::getBestemmelse)).containsOnly(Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET);
    }

    @Test
    void tidligerePerioder_ingenPerioderValgt_returnererTomCollection() throws TekniskException {
        Behandling behandling = new Behandling();
        behandling.setId(2L);
        assertThat(instanse.hentTidligereLovvalgsperioder(behandling)).isEmpty();
    }

    @Test
    void hentOpprinneligLovvalgsperiode_finnerOpprinneligBehandlingMedTidligerePeriode_returnererPeriode() throws IkkeFunnetException {
        Behandling behandling = new Behandling();
        Behandling opprinneligBehandling = new Behandling();
        opprinneligBehandling.setId(5L);
        behandling.setOpprinneligBehandling(opprinneligBehandling);

        Lovvalgsperiode opprinneligLovvalgsperiode = new Lovvalgsperiode();
        doReturn(Collections.singletonList(opprinneligLovvalgsperiode)).when(lovvalgsperiodeRepositoryMock).findByBehandlingsresultatId(5L);

        Optional<Behandling> optionalBehandling = Optional.of(behandling);
        doReturn(optionalBehandling).when(behandlingRepositoryMock).findById(5L);

        assertThat(instanse.hentOpprinneligLovvalgsperiode(5L)).isEqualTo(opprinneligLovvalgsperiode);
    }

    @Test
    void hentOpprinneligLovvalgsperiode_finnerIngenBehandling_kasterException() {
        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> instanse.hentOpprinneligLovvalgsperiode(5L))
            .withMessageContaining("Fant ingen behandling");
    }

    @Test
    void hentOpprinneligLovvalgsperiode_finnerIkkeOpprinneligBehandling_kasterException() {
        Optional<Behandling> behandling = Optional.of(new Behandling());
        doReturn(behandling).when(behandlingRepositoryMock).findById(5L);

        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> instanse.hentOpprinneligLovvalgsperiode(5L))
            .withMessageContaining("Fant ingen opprinnelig behandling");
    }

    @Test
    void hentOpprinneligLovvalgsperiode_finnerOpprinneligBehandlingUtenTidligerePeriode_kasterException() {
        Behandling behandling = new Behandling();
        Optional<Behandling> optionalBehandling = Optional.of(behandling);
        doReturn(optionalBehandling).when(behandlingRepositoryMock).findById(5L);

        Behandling opprinneligBehandling = new Behandling();
        opprinneligBehandling.setId(5L);
        behandling.setOpprinneligBehandling(opprinneligBehandling);

        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> instanse.hentOpprinneligLovvalgsperiode(5L))
            .withMessageContaining("Fant ingen opprinnelig lovvalgsperiode");
    }

    @Test
    void finnOpprinneligLovvalgsperiode_finnerOpprinneligBehandlingMedTidligerePeriode_returnererPeriode() {
        Behandling behandling = new Behandling();
        Behandling opprinneligBehandling = new Behandling();
        opprinneligBehandling.setId(5L);
        behandling.setOpprinneligBehandling(opprinneligBehandling);

        Lovvalgsperiode opprinneligLovvalgsperiode = new Lovvalgsperiode();
        doReturn(Collections.singletonList(opprinneligLovvalgsperiode)).when(lovvalgsperiodeRepositoryMock).findByBehandlingsresultatId(5L);

        Optional<Behandling> optionalBehandling = Optional.of(behandling);
        doReturn(optionalBehandling).when(behandlingRepositoryMock).findById(5L);

        Optional<Lovvalgsperiode> lovvalgsperiode = instanse.finnOpprinneligLovvalgsperiode(5L);
        assertThat(lovvalgsperiode).contains(opprinneligLovvalgsperiode);
    }

    @Test
    void finnOpprinneligLovvalgsperiode_finnerOpprinneligBehandlingUtenTidligerePeriode_optionalEmpty() {
        Behandling behandling = new Behandling();
        Optional<Behandling> optionalBehandling = Optional.of(behandling);
        doReturn(optionalBehandling).when(behandlingRepositoryMock).findById(5L);

        Behandling opprinneligBehandling = new Behandling();
        opprinneligBehandling.setId(5L);
        behandling.setOpprinneligBehandling(opprinneligBehandling);

        Optional<Lovvalgsperiode> lovvalgsperiode = instanse.finnOpprinneligLovvalgsperiode(5L);
        assertThat(lovvalgsperiode).isNotPresent();
    }

    private Behandling lagBehandlingMedMedlOpplysning(MedlemskapDokument medlDokument) {
        Saksopplysning medl = new Saksopplysning();
        medl.setDokument(medlDokument);
        medl.setType(SaksopplysningType.MEDL);

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.getSaksopplysninger().add(medl);
        return behandling;
    }

    private Medlemsperiode lagMedlemsperiode(long id, String grunnlagMedlKode) {
        Periode periode = new Periode(LocalDate.now(), LocalDate.now());
        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.id = id;
        medlemsperiode.periode = periode;
        medlemsperiode.grunnlagstype = grunnlagMedlKode;
        return medlemsperiode;
    }
}
