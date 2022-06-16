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


    @Test
    void utførKontroller_A009_og_REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING_medPeriodePå2ÅrOgEnDag_forventerIngenFeil() {
        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);

        SedDokument dokument = new SedDokument();
        dokument.setSedType(SedType.A009);
        Periode gyldigPeriodePå2ÅrOgEnDag = new Periode(LocalDate.of(2070, 1, 1), LocalDate.of(2072, 1, 1));
        dokument.setLovvalgsperiode(gyldigPeriodePå2ÅrOgEnDag);

        saksopplysning.setDokument(dokument);
        behandling.setSaksopplysninger(Set.of(saksopplysning));
        when(behandlingService.hentBehandling(1L)).thenReturn(behandling);


        assertDoesNotThrow(() -> godkjennUnntakKontrollService.utførKontroll(1L));
    }


    @Test
    void utførKontroller_A009_og_REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING_medPeriodePå2ÅrOgFemDager_forventerFeil() {
        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);

        SedDokument dokument = new SedDokument();
        dokument.setSedType(SedType.A009);
        Periode gyldigPeriodePå2ÅrOgEnDag = new Periode(LocalDate.of(2070, 1, 1), LocalDate.of(2072, 1, 5));
        dokument.setLovvalgsperiode(gyldigPeriodePå2ÅrOgEnDag);

        saksopplysning.setDokument(dokument);
        behandling.setSaksopplysninger(Set.of(saksopplysning));
        when(behandlingService.hentBehandling(1L)).thenReturn(behandling);


        assertThatThrownBy(() -> godkjennUnntakKontrollService.utførKontroll(1L))
            .isInstanceOf(ValideringException.class);
    }
}
