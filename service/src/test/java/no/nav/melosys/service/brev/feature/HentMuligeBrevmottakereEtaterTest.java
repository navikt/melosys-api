package no.nav.melosys.service.brev.feature;

import java.util.List;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.brev.FastMottakerMedOrgnr.HELFO;
import static no.nav.melosys.domain.brev.FastMottakerMedOrgnr.SKATTEETATEN;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.ETAT;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.FRITEKSTBREV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HentMuligeBrevmottakereEtaterTest {

    @Mock
    private EregFasade eregFasade;

    @InjectMocks
    private HentMuligeBrevmottakereEtater hentMuligeBrevmottakereEtater;


    @Test
    void hentMuligeMottakereEtater_spørEtterSkatteetatenOgHelfo_fårSkatteetatenOgHelfoMottakere() {
        when(eregFasade.hentOrganisasjonNavn(SKATTEETATEN.getOrgnr())).thenReturn("Skatteetaten");
        when(eregFasade.hentOrganisasjonNavn(HELFO.getOrgnr())).thenReturn("Helfo");

        var orgnrEtater = List.of(SKATTEETATEN.getOrgnr(), HELFO.getOrgnr());


        var muligMottakerDto = hentMuligeBrevmottakereEtater.hentMuligeMottakereEtater(orgnrEtater);


        assertThat(muligMottakerDto)
            .hasSize(2)
            .first()
            .hasFieldOrPropertyWithValue("rolle", ETAT)
            .hasFieldOrPropertyWithValue("dokumentNavn", FRITEKSTBREV.getBeskrivelse())
            .hasFieldOrPropertyWithValue("orgnr", SKATTEETATEN.getOrgnr())
            .hasFieldOrPropertyWithValue("mottakerNavn", "Skatteetaten");
        assertThat(muligMottakerDto)
            .last()
            .hasFieldOrPropertyWithValue("rolle", ETAT)
            .hasFieldOrPropertyWithValue("dokumentNavn", FRITEKSTBREV.getBeskrivelse())
            .hasFieldOrPropertyWithValue("orgnr", HELFO.getOrgnr())
            .hasFieldOrPropertyWithValue("mottakerNavn", "Helfo");
    }

    @Test
    void hentMuligeMottakereEtater_spørEtterSkatteetatenOgUkjentOrgNr_fårIkkeFunnetFeilmelding() {
        when(eregFasade.hentOrganisasjonNavn(anyString())).thenThrow(new IkkeFunnetException("Fant ikke orgnr i testen :)"));
        var orgnrEtater = List.of("111111111");

        assertThatThrownBy(() -> hentMuligeBrevmottakereEtater.hentMuligeMottakereEtater(orgnrEtater))
            .isInstanceOf(IkkeFunnetException.class);
    }
}
