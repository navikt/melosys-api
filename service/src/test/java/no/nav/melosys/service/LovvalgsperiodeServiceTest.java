package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.medl.GrunnlagMedl;
import no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LovvalgsperiodeServiceTest {
    @Mock
    private LovvalgsperiodeRepository lovvalgsperiodeRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository;
    @Mock
    private BehandlingRepository behandlingRepository;

    private LovvalgsperiodeService lovvalgsperiodeService;

    private static final long BEH_ID = 1L;

    @BeforeEach
    public void setUp() {
        lovvalgsperiodeService = new LovvalgsperiodeService(
            behandlingsresultatRepository,
            lovvalgsperiodeRepository,
            tidligereMedlemsperiodeRepository,
            behandlingRepository);
    }

    @Test
    void hentLovvalgsperioder_ingenLovvalgsperioder_girTomListe() {
        when(lovvalgsperiodeRepository.findByBehandlingsresultatId(BEH_ID)).thenReturn(Collections.emptyList());


        Collection<Lovvalgsperiode> resultat = lovvalgsperiodeService.hentLovvalgsperioder(BEH_ID);


        assertThat(resultat).isEmpty();
    }

    @Test
    void lagreLovvalgsperioderReturnererLovvalgsperiodeMedBehandlingsresultat() {
        var lagretBehandlingsresultat = new Behandlingsresultat();
        lagretBehandlingsresultat.setId(BEH_ID);

        when(behandlingsresultatRepository.findById(BEH_ID)).thenReturn(Optional.of(lagretBehandlingsresultat));
        when(lovvalgsperiodeRepository.saveAllAndFlush(argThat(this::harBehandlingsResultatMedRiktigId))).thenAnswer(i -> i.getArgument(0));


        var lovvalgsPerioderSpy = spy(List.of(new Lovvalgsperiode()));
        var lagretLovvalgsPeriodeMedBehandlingsresultat = lovvalgsperiodeService.lagreLovvalgsperioder(BEH_ID, lovvalgsPerioderSpy);


        assertThat(harBehandlingsResultatMedRiktigId(lovvalgsPerioderSpy)).isFalse();
        assertThat(lagretLovvalgsPeriodeMedBehandlingsresultat).hasSize(1);
        assertThat(harBehandlingsResultatMedRiktigId(lagretLovvalgsPeriodeMedBehandlingsresultat)).isTrue();
    }

    @Test
    void lagreLovvalgsperioderUtenBehandlingsresultatKasterException() {
        var lovvalgsperioder = List.of(new Lovvalgsperiode());
        when(behandlingsresultatRepository.findById(BEH_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> lovvalgsperiodeService
            .lagreLovvalgsperioder(BEH_ID, lovvalgsperioder)).withMessageContaining("fins ikke");
    }

    @Test
    void oppdaterLovvalgsperiode_lovvalgsperiodeFinnes_oppdatererFelt() {
        var eksisterendeLovvalgsperiode = new Lovvalgsperiode();
        eksisterendeLovvalgsperiode.setId(3L);
        when(lovvalgsperiodeRepository.findById(3L)).thenReturn(Optional.of(eksisterendeLovvalgsperiode));

        var request = new Lovvalgsperiode();
        request.setFom(LocalDate.now());
        request.setTom(LocalDate.now());
        request.setLovvalgsland(Land_iso2.BA);
        request.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E);
        request.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);
        request.setInnvilgelsesresultat(InnvilgelsesResultat.DELVIS_INNVILGET);
        request.setDekning(Trygdedekninger.FULL_DEKNING);
        request.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);
        request.setMedlPeriodeID(23L);

        ArgumentCaptor<Lovvalgsperiode> captor = ArgumentCaptor.forClass(Lovvalgsperiode.class);


        lovvalgsperiodeService.oppdaterLovvalgsperiode(3L, request);


        verify(lovvalgsperiodeRepository).save(captor.capture());
        assertThat(captor.getValue())
            .isNotNull()
            .extracting("fom", "tom", "lovvalgsland", "bestemmelse",
                "tilleggsbestemmelse", "innvilgelsesresultat",
                "medlemskapstype", "dekning", "medlPeriodeID")
            .containsExactly(
                request.getFom(), request.getTom(), request.getLovvalgsland(), request.getBestemmelse(),
                request.getTilleggsbestemmelse(), request.getInnvilgelsesresultat(),
                request.getMedlemskapstype(), request.getDekning(), request.getMedlPeriodeID());
    }

    @Test
    void oppdaterLovvalgsperiode_lovvalgsperiodeFinnesIkke_kasterException() {
        var request = new Lovvalgsperiode();
        when(lovvalgsperiodeRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> lovvalgsperiodeService.oppdaterLovvalgsperiode(3L, request))
            .withMessageContaining("Lovvalgsperioden 3 finnes ikke");
    }

    @Test
    void hentTidligereLovvalgsperioder_enValgtMedlemsperiode_returnererEnTidligerLovvalgsperiode() {
        Medlemsperiode medlemsperiode = lagMedlemsperiode(23L, GrunnlagMedl.FO_12_2.kode);
        Medlemsperiode medlemsperiodeFeilId = lagMedlemsperiode(46L, GrunnlagMedl.FO_12_2.kode);

        MedlemskapDokument medlDokument = new MedlemskapDokument();
        medlDokument.getMedlemsperiode().add(medlemsperiode);
        medlDokument.getMedlemsperiode().add(medlemsperiodeFeilId);

        var behandling = lagBehandlingMedMedlOpplysning(medlDokument);
        mockTidligereMedlemsperiodeRepository(medlemsperiode.getId());


        assertThat(lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling))
            .hasSize(1)
            .flatExtracting(
                Lovvalgsperiode::getMedlPeriodeID,
                Lovvalgsperiode::getFom,
                Lovvalgsperiode::getTom,
                Lovvalgsperiode::getBestemmelse
            ).containsExactly(
                medlemsperiode.getId(),
                medlemsperiode.getPeriode().getFom(),
                medlemsperiode.getPeriode().getTom(),
                MedlPeriodeKonverter.tilLovvalgBestemmelse(GrunnlagMedl.valueOf(medlemsperiode.getGrunnlagstype()))
            );
    }

    @Test
    void hentTidligereLovvalgsperioder_ukjentGrunnlagskodeMedl_grunnlagMappetTilAnnet() {
        Medlemsperiode medlemsperiode = lagMedlemsperiode(23L, "MAPPING_SOM_MELOSYS_IKKE_KJENNER_TIL");

        MedlemskapDokument medlDokument = new MedlemskapDokument();
        medlDokument.getMedlemsperiode().add(medlemsperiode);

        var behandling = lagBehandlingMedMedlOpplysning(medlDokument);
        mockTidligereMedlemsperiodeRepository(medlemsperiode.getId());


        Collection<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling);


        assertThat(lovvalgsperioder)
            .hasSize(1)
            .flatExtracting(
                Lovvalgsperiode::getMedlPeriodeID,
                Lovvalgsperiode::getBestemmelse)
            .containsExactly(
                medlemsperiode.getId(),
                Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET);
    }

    @Test
    void hentTidligereLovvalgsperioder_ingenPerioderValgt_returnererTomCollection() {
        Behandling behandling = new Behandling();
        behandling.setId(BEH_ID);
        when(tidligereMedlemsperiodeRepository.findById_BehandlingId(BEH_ID)).thenReturn(Collections.emptyList());


        assertThat(lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling)).isEmpty();
    }

    @Test
    void hentOpprinneligLovvalgsperiode_finnerOpprinneligBehandlingMedTidligerePeriode_returnererPeriode() {
        Behandling opprinneligBehandling = new Behandling();
        opprinneligBehandling.setId(2L);

        Behandling behandling = new Behandling();
        behandling.setOpprinneligBehandling(opprinneligBehandling);
        when(behandlingRepository.findById(BEH_ID))
            .thenReturn(Optional.of(behandling));

        Lovvalgsperiode opprinneligLovvalgsperiode = new Lovvalgsperiode();
        when(lovvalgsperiodeRepository.findByBehandlingsresultatId(opprinneligBehandling.getId()))
            .thenReturn(Collections.singletonList(opprinneligLovvalgsperiode));


        assertThat(lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(BEH_ID))
            .isEqualTo(opprinneligLovvalgsperiode);
    }

    @Test
    void hentOpprinneligLovvalgsperiode_finnerIngenBehandling_kasterException() {
        when(behandlingRepository.findById(BEH_ID)).thenReturn(Optional.empty());


        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(BEH_ID))
            .withMessageContaining("Fant ingen behandling");
    }

    @Test
    void hentOpprinneligLovvalgsperiode_finnerIkkeOpprinneligBehandling_kasterException() {
        when(behandlingRepository.findById(BEH_ID)).thenReturn(Optional.of(new Behandling()));


        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(BEH_ID))
            .withMessageContaining("Fant ingen opprinnelig behandling");
    }

    @Test
    void hentOpprinneligLovvalgsperiode_finnerOpprinneligBehandlingUtenTidligerePeriode_kasterException() {
        Behandling opprinneligBehandling = new Behandling();
        opprinneligBehandling.setId(2L);

        Behandling behandling = new Behandling();
        behandling.setOpprinneligBehandling(opprinneligBehandling);
        when(behandlingRepository.findById(BEH_ID)).thenReturn(Optional.of(behandling));


        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(BEH_ID))
            .withMessageContaining("Fant ingen opprinnelig lovvalgsperiode");
    }

    @Test
    void finnOpprinneligLovvalgsperiode_finnerOpprinneligBehandlingMedTidligerePeriode_returnererPeriode() {
        Behandling opprinneligBehandling = new Behandling();
        opprinneligBehandling.setId(2L);

        Behandling behandling = new Behandling();
        behandling.setOpprinneligBehandling(opprinneligBehandling);
        when(behandlingRepository.findById(BEH_ID))
            .thenReturn(Optional.of(behandling));

        Lovvalgsperiode opprinneligLovvalgsperiode = new Lovvalgsperiode();
        when(lovvalgsperiodeRepository.findByBehandlingsresultatId(opprinneligBehandling.getId()))
            .thenReturn(Collections.singletonList(opprinneligLovvalgsperiode));


        Optional<Lovvalgsperiode> lovvalgsperiode = lovvalgsperiodeService.finnOpprinneligLovvalgsperiode(BEH_ID);


        assertThat(lovvalgsperiode).contains(opprinneligLovvalgsperiode);
    }

    @Test
    void finnOpprinneligLovvalgsperiode_finnerOpprinneligBehandlingUtenTidligerePeriode_optionalEmpty() {
        Behandling opprinneligBehandling = new Behandling();
        opprinneligBehandling.setId(2L);

        Behandling behandling = new Behandling();
        behandling.setOpprinneligBehandling(opprinneligBehandling);
        when(behandlingRepository.findById(BEH_ID))
            .thenReturn(Optional.of(behandling));


        Optional<Lovvalgsperiode> lovvalgsperiode = lovvalgsperiodeService.finnOpprinneligLovvalgsperiode(BEH_ID);


        assertThat(lovvalgsperiode).isNotPresent();
    }

    private void mockTidligereMedlemsperiodeRepository(long periodeID) {
        var tidligereMedlemsperiodeId = new TidligereMedlemsperiodeId();
        tidligereMedlemsperiodeId.setPeriodeId(periodeID);

        var tidligereMedlemsperiode = new TidligereMedlemsperiode();
        tidligereMedlemsperiode.setId(tidligereMedlemsperiodeId);

        when(tidligereMedlemsperiodeRepository.findById_BehandlingId(BEH_ID)).thenReturn(Collections.singletonList(tidligereMedlemsperiode));
    }

    private Behandling lagBehandlingMedMedlOpplysning(MedlemskapDokument medlDokument) {
        Saksopplysning medl = new Saksopplysning();
        medl.setDokument(medlDokument);
        medl.setType(SaksopplysningType.MEDL);

        Behandling behandling = new Behandling();
        behandling.setId(BEH_ID);
        behandling.getSaksopplysninger().add(medl);
        return behandling;
    }

    private Medlemsperiode lagMedlemsperiode(long id, String grunnlagMedlKode) {
        Periode periode = new Periode(LocalDate.now(), LocalDate.now());
        return new Medlemsperiode(
            id, periode, null,
            PeriodestatusMedl.GYLD.kode, grunnlagMedlKode, null, null, null, null, null);
    }

    private boolean harBehandlingsResultatMedRiktigId(Iterable<Lovvalgsperiode> lovvalgsperioder) {
        return StreamSupport.stream(lovvalgsperioder.spliterator(), false)
            .allMatch(item -> item.getBehandlingsresultat() != null &&
                item.getBehandlingsresultat().getId().equals(BEH_ID));
    }
}
