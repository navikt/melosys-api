package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestBuilder;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.INNVILGELSE_YRKESAKTIV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BestillBrevTest {

    @Mock
    BrevBestiller brevBestiller;

    private BestillBrev bestillBrev;

    @BeforeEach
    void setUp() {
        bestillBrev = new BestillBrev(brevBestiller);
    }

    @Test
    void utfør_altOk_kallerBestill() {
        var behandling = BehandlingTestBuilder.builderWithDefaults().build();
        var prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder()
            .medProduserbartDokument(INNVILGELSE_YRKESAKTIV)
            .medMottakere(Mottaker.medRolle(Mottakerroller.BRUKER))
            .build();
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, brevbestilling);
        ArgumentCaptor<DoksysBrevbestilling> captor = ArgumentCaptor.forClass(DoksysBrevbestilling.class);


        bestillBrev.utfør(prosessinstans);


        verify(brevBestiller).bestill(captor.capture());
        DoksysBrevbestilling bestiltBrevbestilling = captor.getValue();
        assertThat(brevbestilling.getBehandling()).isNull();
        assertThat(bestiltBrevbestilling.getBehandling()).isEqualTo(behandling);
        assertThat(bestiltBrevbestilling.getProduserbartdokument()).isEqualTo(brevbestilling.getProduserbartdokument());
        assertThat(bestiltBrevbestilling.getMottakere()).isEqualTo(brevbestilling.getMottakere());
    }

    @Test
    void utfør_manglerBehandling_kasterFeilmelding() {
        var prosessinstans = new Prosessinstans();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> bestillBrev.utfør(prosessinstans))
            .withMessageContaining("Prosessinstans mangler behandling");
    }

    @Test
    void utfør_manglerBrevbestilling_kasterFeilmelding() {
        var prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(BehandlingTestBuilder.builderWithDefaults().build());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> bestillBrev.utfør(prosessinstans))
            .withMessageContaining("Prosessinstans mangler brevbestilling");
    }

    @Test
    void utfør_flereEnnEnMottaker_kasterFeilmelding() {
        var behandling = BehandlingTestBuilder.builderWithDefaults().build();
        var prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING,
            new DoksysBrevbestilling.Builder()
                .medMottakere(Mottaker.medRolle(Mottakerroller.BRUKER), Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER))
                .build());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> bestillBrev.utfør(prosessinstans))
            .withMessageContaining("Prosessinstans skal sende brev til én mottaker, fant 2");
    }
}
