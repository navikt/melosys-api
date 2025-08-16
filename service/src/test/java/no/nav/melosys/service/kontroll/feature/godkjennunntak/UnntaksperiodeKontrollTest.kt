package no.nav.melosys.service.kontroll.feature.godkjennunntak;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.kontroll.feature.unntaksperiode.UnntaksperiodeKontrollService;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnntaksperiodeKontrollTest {

    @Mock
    private SaksopplysningerService saksopplysningerService;
    private UnntaksperiodeKontrollService unntaksperiodeKontrollService;

    private Behandling behandling;
    private Saksopplysning saksopplysning;
    private SedDokument sedDokument;

    @BeforeEach
    void setupMedA009SedDokument() {
        unntaksperiodeKontrollService = new UnntaksperiodeKontrollService(saksopplysningerService);

        this.saksopplysning = new Saksopplysning();

        this.behandling = BehandlingTestFactory.builderWithDefaults()
            .medSaksopplysninger(Set.of(saksopplysning))
            .build();

        this.sedDokument = new SedDokument();
        sedDokument.setSedType(SedType.A009);

        this.saksopplysning.setDokument(sedDokument);

        when(saksopplysningerService.finnSedOpplysninger(1L)).thenReturn(Optional.of(sedDokument));
    }

    @Test
    void utførKontroll_A009_enTestbarGodkjennUnntakKontroll_medPeriodePå2ÅrOgEnDag_forventerIngenFeil() {
        Periode gyldigPeriode = new Periode(
            LocalDate.of(2070, 1, 1),
            LocalDate.of(2072, 1, 1));

        assertThatCode(() -> unntaksperiodeKontrollService.kontrollPeriode(1L, gyldigPeriode))
            .doesNotThrowAnyException();
    }


    @Test
    void utførKontroll_A009_enTestbarGodkjennUnntakKontroll_medPeriodePå2ÅrOgFemDager_forventerFeil() {
        Periode gyldigPeriode = new Periode(
            LocalDate.of(2070, 1, 1),
            LocalDate.of(2072, 1, 5));

        assertThatThrownBy(() -> unntaksperiodeKontrollService.kontrollPeriode(1L, gyldigPeriode))
            .isInstanceOf(ValideringException.class);
    }

    @Test
    void kontrollPeriode_A009_medPeriodePå2ÅrOgEnDag_forventerIngenFeil() {
        Periode gyldigPeriode = new Periode(
            LocalDate.of(2050, 1, 1),
            LocalDate.of(2052, 1, 1));

        assertThatCode(() -> unntaksperiodeKontrollService.kontrollPeriode(1L, gyldigPeriode))
            .doesNotThrowAnyException();
    }

    @Test
    void kontrollPeriode_A009_enTestbarGodkjennUnntakKontroll_medPeriodePå2ÅrOgFemDager_forventerFeil() {
        Periode ugyldigPeriode = new Periode(
            LocalDate.of(2050, 1, 1),
            LocalDate.of(2052, 1, 5));

        assertThatThrownBy(() -> {
            unntaksperiodeKontrollService.kontrollPeriode(1L, ugyldigPeriode);
        }).isInstanceOf(ValideringException.class);
    }

    @Test
    void kontrollPeriode_A003_medPeriodeLangtOver2År_ikkeRelevantForA003_forventerIngenFeil() {
        Periode gyldigPeriode = new Periode(
            LocalDate.of(2050, 1, 1),
            LocalDate.of(2055, 12, 26));
        sedDokument.setSedType(SedType.A003);

        assertThatCode(() -> unntaksperiodeKontrollService.kontrollPeriode(1L, gyldigPeriode))
            .doesNotThrowAnyException();
    }
}
