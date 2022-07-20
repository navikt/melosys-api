package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
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
        var behandling = new Behandling();
        var prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, new DoksysBrevbestilling.Builder().medProduserbartDokument(INNVILGELSE_YRKESAKTIV).build());
        ArgumentCaptor<DoksysBrevbestilling> captor = ArgumentCaptor.forClass(DoksysBrevbestilling.class);


        bestillBrev.utfør(prosessinstans);


        verify(brevBestiller).bestill(captor.capture());
        DoksysBrevbestilling brevbestilling = captor.getValue();
        assertThat(brevbestilling.getBehandling()).isEqualTo(behandling);
        assertThat(brevbestilling.getProduserbartdokument()).isEqualTo(INNVILGELSE_YRKESAKTIV);
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
        prosessinstans.setBehandling(new Behandling());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> bestillBrev.utfør(prosessinstans))
            .withMessageContaining("Prosessinstans mangler brevbestilling");
    }
}
