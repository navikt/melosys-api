package no.nav.melosys.saksflyt.steg.brev;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestBuilder;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendOrienteringAnmodningUnntakTest {

    @Mock
    private BrevBestiller brevBestiller;
    @Mock
    private BehandlingService behandlingService;

    private Behandling behandling;
    private Prosessinstans prosessinstans;
    private SendOrienteringAnmodningUnntak sendOrienteringAnmodningUnntak;

    private static final String SAKSBEHANDLER = "Z121212";

    @BeforeEach
    public void setUp() {
        behandling = BehandlingTestBuilder.builderWithDefaults().build();
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setId(1L);

        when(behandlingService.hentBehandlingMedSaksopplysninger(behandling.getId())).thenReturn(behandling);

        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, Behandlingsresultattyper.ANMODNING_OM_UNNTAK.getKode());

        sendOrienteringAnmodningUnntak = new SendOrienteringAnmodningUnntak(brevBestiller, behandlingService);
    }

    @Test
    void utfoerSteg() {
        ArgumentCaptor<DoksysBrevbestilling> captor = ArgumentCaptor.forClass(DoksysBrevbestilling.class);

        sendOrienteringAnmodningUnntak.utfør(prosessinstans);

        verify(brevBestiller).bestill(captor.capture());
        assertThat(captor.getValue())
            .extracting(
                Brevbestilling::getProduserbartdokument,
                Brevbestilling::getBehandling,
                Brevbestilling::getAvsenderID,
                DoksysBrevbestilling::getMottakere)
            .containsExactly(
                ORIENTERING_ANMODNING_UNNTAK,
                behandling,
                SAKSBEHANDLER,
                List.of(Mottaker.medRolle(Mottakerroller.BRUKER))
            );
    }
}
