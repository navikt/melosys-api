package no.nav.melosys.service.kontroll.feature.ufm;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.repository.KontrollresultatRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UfmKontrollServiceTest {

    @Mock
    private KontrollresultatRepository kontrollresultatRepository;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private PersondataFasade persondataFasade;
    @Captor
    private ArgumentCaptor<List<Kontrollresultat>> kontrollresultaterCaptor;

    private UfmKontrollService ufmKontrollService;

    private final Behandling behandling = lagBehandling();

    @BeforeEach
    public void setup() {
        ufmKontrollService = new UfmKontrollService(kontrollresultatRepository, behandlingsresultatService, behandlingService, persondataFasade);
        SedDokument sedDokument = new SedDokument();
        sedDokument.setSedType(SedType.A009);
        sedDokument.setLovvalgslandKode(Landkoder.NO);
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now().plusMonths(1)));
        behandling.getSaksopplysninger().add(lagSaksopplysning(sedDokument, SaksopplysningType.SEDOPPL));
        behandling.getSaksopplysninger().add(lagSaksopplysning(new MedlemskapDokument(), SaksopplysningType.MEDL));
        behandling.getSaksopplysninger().add(lagSaksopplysning(new InntektDokument(), SaksopplysningType.INNTK));
        behandling.getSaksopplysninger().add(lagSaksopplysning(new UtbetalingDokument(), SaksopplysningType.UTBETAL));
    }

    @Test
    public void utførKontrollerOgRegistrerFeil() {
        final long BEHANDLING_ID = 1L;
        when(persondataFasade.hentPerson(any())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        when(behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(lagBehandlingsresultat());
        ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID);

        verify(behandlingService).hentBehandlingMedSaksopplysninger(anyLong());
        verify(kontrollresultatRepository).deleteByBehandlingsresultat(any(Behandlingsresultat.class));
        verify(kontrollresultatRepository).saveAll(kontrollresultaterCaptor.capture());

        List<Kontrollresultat> kontrollresultater = kontrollresultaterCaptor.getValue();

        assertThat(kontrollresultater).hasSize(2);

        assertThat(kontrollresultater).extracting(Kontrollresultat::getBegrunnelse)
            .containsExactlyInAnyOrder(
                Kontroll_begrunnelser.LOVVALGSLAND_NORGE,
                Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND
            );

        assertThat(kontrollresultater).extracting(Kontrollresultat::getBehandlingsresultat)
            .extracting(Behandlingsresultat::getId)
            .containsExactlyInAnyOrder(2L, 2L);
    }

    @Test
    void utførKontroller_periodeIkkeGyldig_forventEttTreff() {
        SedDokument sedDokument = behandling.hentSedDokument();
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now().minusYears(1)));
        assertThat(ufmKontrollService.utførKontroller(behandling))
            .containsExactly(Kontroll_begrunnelser.FEIL_I_PERIODEN);
    }

    private Saksopplysning lagSaksopplysning(SaksopplysningDokument saksopplysningDokument, SaksopplysningType type) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(saksopplysningDokument);
        saksopplysning.setType(type);
        return saksopplysning;
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(2L);
        return behandlingsresultat;
    }
}
