package no.nav.melosys.service;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.medl.GrunnlagMedl;
import no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
    void lagreLovvalgsperioderGirKopiMedBehandlingsresultat() {
        var lovvalgsperioder = List.of(new Lovvalgsperiode());
        @SuppressWarnings("unchecked")
        Collection<Lovvalgsperiode> anyCollection = any(Collection.class);
        when(lovvalgsperiodeRepository.saveAll(anyCollection)).thenAnswer(i -> i.getArgument(0));
        when(behandlingsresultatRepository.findById(BEH_ID)).thenReturn(Optional.of(new Behandlingsresultat()));
        assertThat(lovvalgsperioder.get(0).getBehandlingsresultat()).isNull();


        Collection<Lovvalgsperiode> resultat = lovvalgsperiodeService.lagreLovvalgsperioder(BEH_ID, lovvalgsperioder);


        assertThat(resultat).hasSize(lovvalgsperioder.size());
        assertThat(resultat.iterator().next().getBehandlingsresultat()).isNotNull();
    }

    @Test
    void lagreLovvalgsperioderUtenBehandlingsresultatKasterException() {
        var lovvalgsperioder = List.of(new Lovvalgsperiode());
        when(behandlingsresultatRepository.findById(BEH_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() ->
            lovvalgsperiodeService.lagreLovvalgsperioder(BEH_ID, lovvalgsperioder)).withMessageContaining("fins ikke");
    }

    @Test
    void hentTidligereLovvalgsperioder_enValgtMedlemsperiode_returnererEnTidligerLovvalgsperiode() {
        Medlemsperiode medlemsperiode = lagMedlemsperiode(23L, GrunnlagMedl.FO_12_2.getKode());
        Medlemsperiode medlemsperiodeFeilId = lagMedlemsperiode(46L, GrunnlagMedl.FO_12_2.getKode());

        MedlemskapDokument medlDokument = new MedlemskapDokument();
        medlDokument.getMedlemsperiode().add(medlemsperiode);
        medlDokument.getMedlemsperiode().add(medlemsperiodeFeilId);

        var behandling = lagBehandlingMedMedlOpplysning(medlDokument);
        mockTidligereMedlemsperiodeRepository(medlemsperiode.id);


        assertThat(lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling))
            .hasSize(1)
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
    void hentTidligereLovvalgsperioder_ukjentGrunnlagskodeMedl_grunnlagMappetTilAnnet() {
        Medlemsperiode medlemsperiode = lagMedlemsperiode(23L, "MAPPING_SOM_MELOSYS_IKKE_KJENNER_TIL");

        MedlemskapDokument medlDokument = new MedlemskapDokument();
        medlDokument.getMedlemsperiode().add(medlemsperiode);

        var behandling = lagBehandlingMedMedlOpplysning(medlDokument);
        mockTidligereMedlemsperiodeRepository(medlemsperiode.id);


        Collection<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling);


        assertThat(lovvalgsperioder)
            .hasSize(1)
            .flatExtracting(
                Lovvalgsperiode::getMedlPeriodeID,
                Lovvalgsperiode::getBestemmelse)
            .containsExactly(
                medlemsperiode.id,
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
        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.id = id;
        medlemsperiode.periode = periode;
        medlemsperiode.grunnlagstype = grunnlagMedlKode;
        return medlemsperiode;
    }
}
