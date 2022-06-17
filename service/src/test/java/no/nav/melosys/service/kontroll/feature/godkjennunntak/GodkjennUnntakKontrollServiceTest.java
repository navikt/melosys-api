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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GodkjennUnntakKontrollServiceTest {

    @Mock
    private BehandlingService behandlingService;

    @InjectMocks
    private GodkjennUnntakKontrollService godkjennUnntakKontrollService;

    private Behandling behandling;
    private Saksopplysning saksopplysning;
    private SedDokument sedDokument;

    @BeforeEach
    void setupA009Behandling() {
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
        Periode gyldigPeriodePå2ÅrOgEnDag = new Periode(
            LocalDate.of(2070, 1, 1),
            LocalDate.of(2072, 1, 1));
        sedDokument.setLovvalgsperiode(gyldigPeriodePå2ÅrOgEnDag);


        assertDoesNotThrow(() -> godkjennUnntakKontrollService.utførKontroll(1L));
    }


    @Test
    void utførKontroll_A009_enTestbarGodkjennUnntakKontroll_medPeriodePå2ÅrOgFemDager_forventerFeil() {
        Periode gyldigPeriodePå2ÅrOgEnDag = new Periode(
            LocalDate.of(2070, 1, 1),
            LocalDate.of(2072, 1, 5));
        sedDokument.setLovvalgsperiode(gyldigPeriodePå2ÅrOgEnDag);


        assertThatThrownBy(() -> godkjennUnntakKontrollService.utførKontroll(1L))
            .isInstanceOf(ValideringException.class);
    }

    @Test
    void kontrollPeriode_A009_og_REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING_medPeriodePå2ÅrOgEnDag_forventerIngenFeil() {
        LocalDate ugyldigPeriodeFom = LocalDate.of(2050, 1, 1);
        LocalDate ugyldigPeriodeTom = LocalDate.of(2052, 1, 1);


        assertDoesNotThrow(() -> godkjennUnntakKontrollService.kontrollPeriode(1L, ugyldigPeriodeFom, ugyldigPeriodeTom));
    }

    @Test
    void kontrollPeriode_A009_enTestbarGodkjennUnntakKontroll_medPeriodePå2ÅrOgFemDager_forventerFeil() {
        LocalDate ugyldigPeriodeFom = LocalDate.of(2050, 1, 1);
        LocalDate ugyldigPeriodeTom = LocalDate.of(2052, 1, 5);


        assertThatThrownBy(() -> {
            godkjennUnntakKontrollService.kontrollPeriode(1L, ugyldigPeriodeFom, ugyldigPeriodeTom);
        }).isInstanceOf(ValideringException.class);
    }

    @Test
    void kontrollPeriode_A003_enUtestbarGodkjennUnntakKontroll_medPeriodePå2ÅrOgFemDager_forventerIngenFeil() {
        sedDokument.setSedType(SedType.A003);
        LocalDate ugyldigPeriodeFom = LocalDate.of(2050, 1, 1);
        LocalDate ugyldigPeriodeTom = LocalDate.of(2052, 1, 5);

        assertDoesNotThrow(() -> godkjennUnntakKontrollService.kontrollPeriode(1L, ugyldigPeriodeFom, ugyldigPeriodeTom));
    }
}
