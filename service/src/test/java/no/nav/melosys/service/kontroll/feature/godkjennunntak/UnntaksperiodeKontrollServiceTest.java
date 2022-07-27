package no.nav.melosys.service.kontroll.feature.godkjennunntak;

import java.time.LocalDate;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.feature.unntaksperiode.UnntaksperiodeKontrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnntaksperiodeKontrollServiceTest {

    @Mock
    private BehandlingService behandlingService;

    private UnntaksperiodeKontrollService unntaksperiodeKontrollService;

    private Behandling behandling;
    private Saksopplysning saksopplysning;
    private SedDokument sedDokument;

    @BeforeEach
    void setupA009Behandling() {
        unntaksperiodeKontrollService = new UnntaksperiodeKontrollService(behandlingService);

        this.behandling = new Behandling();
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);

        this.saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);

        behandling.setSaksopplysninger(Set.of(saksopplysning));

        this.sedDokument = new SedDokument();
        sedDokument.setSedType(SedType.A009);

        this.saksopplysning.setDokument(sedDokument);

        when(behandlingService.hentBehandling(1L)).thenReturn(behandling);
    }

    @Test
    void utførKontroll_A009_enTestbarGodkjennUnntakKontroll_medPeriodePå2ÅrOgEnDag_forventerIngenFeil() {
        Periode gyldigPeriode = new Periode(
            LocalDate.of(2070, 1, 1),
            LocalDate.of(2072, 1, 1));
        sedDokument.setLovvalgsperiode(gyldigPeriode);


        assertThatCode(() -> unntaksperiodeKontrollService.kontrollPeriode(1L, gyldigPeriode))
            .doesNotThrowAnyException();
    }


    @Test
    void utførKontroll_A009_enTestbarGodkjennUnntakKontroll_medPeriodePå2ÅrOgFemDager_forventerFeil() {
        Periode gyldigPeriode = new Periode(
            LocalDate.of(2070, 1, 1),
            LocalDate.of(2072, 1, 5));
        sedDokument.setLovvalgsperiode(gyldigPeriode);


        assertThatThrownBy(() -> unntaksperiodeKontrollService.kontrollPeriode(1L, gyldigPeriode))
            .isInstanceOf(ValideringException.class);
    }

    @Test
    void kontrollPeriode_A009_og_REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING_medPeriodePå2ÅrOgEnDag_forventerIngenFeil() {
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
    void kontrollPeriode_A003_enUtestbarGodkjennUnntakKontroll_medPeriodePå2ÅrOgFemDager_forventerIngenFeil() {
        Periode gyldigPeriode = new Periode(
            LocalDate.of(2050, 1, 1),
            LocalDate.of(2052, 1, 5));
        sedDokument.setSedType(SedType.A003);

        assertThatCode(() -> unntaksperiodeKontrollService.kontrollPeriode(1L, gyldigPeriode))
            .doesNotThrowAnyException();
    }
}
