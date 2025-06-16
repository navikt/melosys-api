package no.nav.melosys.service.brev;

import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.brev.bestilling.HentBrevAdresseTilMottakereService;
import no.nav.melosys.service.brev.bestilling.HentMuligeProduserbaredokumenterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrevmalListeServiceTest {

    @Mock
    private HentMuligeProduserbaredokumenterService hentMuligeProduserbaredokumenterService;

    @Mock
    private HentBrevAdresseTilMottakereService hentBrevAdresseTilMottakereService;

    private BrevmalListeService brevmalListeService;

    @BeforeEach
    void setUp() {
        brevmalListeService = new BrevmalListeService(
            hentMuligeProduserbaredokumenterService,
            hentBrevAdresseTilMottakereService
        );
    }

    @Test
    void hentMuligeProduserbaredokumenter_skalReturnereListeFraService() {
        long behandlingId = 123L;
        Mottakerroller rolle = Mottakerroller.BRUKER;
        List<Produserbaredokumenter> forventetListe = Arrays.asList(
            Produserbaredokumenter.MANGELBREV_BRUKER,
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER
        );
        when(hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(behandlingId, rolle))
            .thenReturn(forventetListe);

        List<Produserbaredokumenter> resultat = brevmalListeService.hentMuligeProduserbaredokumenter(behandlingId, rolle);

        assertThat(resultat).isEqualTo(forventetListe);
        verify(hentMuligeProduserbaredokumenterService).hentMuligeProduserbaredokumenter(behandlingId, rolle);
    }

    @Test
    void hentBrevAdresseTilMottakere_skalReturnereListeFraService() {
        long behandlingId = 123L;
        Mottakerroller rolle = Mottakerroller.BRUKER;
        List<BrevAdresse> forventetListe = Arrays.asList(
            new BrevAdresse("Test Person", null, Arrays.asList("Testveien 1"), "0123", "Oslo", null, "NO"),
            new BrevAdresse("Test Bedrift AS", "123456789", Arrays.asList("Bedriftsveien 2"), "0456", "Bergen", null, "NO")
        );
        when(hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(behandlingId, rolle))
            .thenReturn(forventetListe);

        List<BrevAdresse> resultat = brevmalListeService.hentBrevAdresseTilMottakere(behandlingId, rolle);

        assertThat(resultat).isEqualTo(forventetListe);
        verify(hentBrevAdresseTilMottakereService).hentBrevAdresseTilMottakere(behandlingId, rolle);
    }

    @Test
    void hentMuligeProduserbaredokumenter_medArbeidsgiverRolle_skalReturnereKorrektListe() {
        long behandlingId = 123L;
        Mottakerroller rolle = Mottakerroller.ARBEIDSGIVER;
        List<Produserbaredokumenter> forventetListe = Arrays.asList(
            Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER,
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER
        );
        when(hentMuligeProduserbaredokumenterService.hentMuligeProduserbaredokumenter(behandlingId, rolle))
            .thenReturn(forventetListe);

        List<Produserbaredokumenter> resultat = brevmalListeService.hentMuligeProduserbaredokumenter(behandlingId, rolle);

        assertThat(resultat).isEqualTo(forventetListe);
        verify(hentMuligeProduserbaredokumenterService).hentMuligeProduserbaredokumenter(behandlingId, rolle);
    }

    @Test
    void hentBrevAdresseTilMottakere_medArbeidsgiverRolle_skalReturnereKorrektListe() {
        long behandlingId = 123L;
        Mottakerroller rolle = Mottakerroller.ARBEIDSGIVER;
        List<BrevAdresse> forventetListe = Arrays.asList(
            new BrevAdresse("Arbeidsgiver AS", "987654321", Arrays.asList("Arbeidsgiverveien 1"), "0789", "Trondheim", null, "NO")
        );
        when(hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(behandlingId, rolle))
            .thenReturn(forventetListe);

        List<BrevAdresse> resultat = brevmalListeService.hentBrevAdresseTilMottakere(behandlingId, rolle);

        assertThat(resultat).isEqualTo(forventetListe);
        verify(hentBrevAdresseTilMottakereService).hentBrevAdresseTilMottakere(behandlingId, rolle);
    }
}
