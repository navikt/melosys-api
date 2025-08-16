package no.nav.melosys.service.brev.bestilling;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static no.nav.melosys.domain.brev.NorskMyndighet.HELFO;
import static no.nav.melosys.domain.brev.NorskMyndighet.SKATTEETATEN;
import static no.nav.melosys.domain.kodeverk.Mottakerroller.NORSK_MYNDIGHET;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.FRITEKSTBREV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HentBrevmottakereNorskMyndighetServiceTest {

    @Mock
    private EregFasade eregFasade;

    @InjectMocks
    private HentBrevmottakereNorskMyndighetService hentBrevmottakereNorskMyndighetService;

    @Test
    void hentMuligeBrevmottakereNorskMyndighet_spørEtterSkatteetatenOgHelfo_fårSkatteetatenOgHelfoMottakere() {
        when(eregFasade.hentOrganisasjonNavn(SKATTEETATEN.getOrgnr())).thenReturn("Skatteetaten");
        when(eregFasade.hentOrganisasjonNavn(HELFO.getOrgnr())).thenReturn("Helfo");

        var orgnrNorskeMyndigheter = List.of(SKATTEETATEN.getOrgnr(), HELFO.getOrgnr());


        var muligeBrevmottakereForNorskMyndighet = hentBrevmottakereNorskMyndighetService.hentMuligeBrevmottakereNorskMyndighet(orgnrNorskeMyndigheter);


        assertThat(muligeBrevmottakereForNorskMyndighet)
            .hasSize(2)
            .first()
            .hasFieldOrPropertyWithValue("rolle", NORSK_MYNDIGHET)
            .hasFieldOrPropertyWithValue("dokumentNavn", FRITEKSTBREV.getBeskrivelse())
            .hasFieldOrPropertyWithValue("orgnr", SKATTEETATEN.getOrgnr())
            .hasFieldOrPropertyWithValue("mottakerNavn", "Skatteetaten");
        assertThat(muligeBrevmottakereForNorskMyndighet)
            .last()
            .hasFieldOrPropertyWithValue("rolle", NORSK_MYNDIGHET)
            .hasFieldOrPropertyWithValue("dokumentNavn", FRITEKSTBREV.getBeskrivelse())
            .hasFieldOrPropertyWithValue("orgnr", HELFO.getOrgnr())
            .hasFieldOrPropertyWithValue("mottakerNavn", "Helfo");
    }

    @Test
    void hentMuligeBrevmottakereNorskMyndighet_spørEtterSkatteetatenOgUkjentOrgNr_fårIkkeFunnetFeilmelding() {
        when(eregFasade.hentOrganisasjonNavn(anyString())).thenThrow(new IkkeFunnetException("Fant ikke orgnr i testen :)"));
        var orgnrNorskeMyndigheter = List.of("111111111");

        assertThatThrownBy(() -> hentBrevmottakereNorskMyndighetService.hentMuligeBrevmottakereNorskMyndighet(orgnrNorskeMyndigheter))
            .isInstanceOf(IkkeFunnetException.class);
    }
}
