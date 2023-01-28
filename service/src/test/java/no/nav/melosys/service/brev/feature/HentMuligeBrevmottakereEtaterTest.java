package no.nav.melosys.service.brev.feature;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HentMuligeBrevmottakereEtaterTest {

    @Mock
    private EregFasade eregFasade;

    @InjectMocks
    private HentMuligeBrevmottakereEtater hentMuligeBrevmottakereEtater;


    @Test
    void hentMuligeMottakereEtater_spørEtterSkatteetatenOgHelfo_fårSkatteetatenOgHelfoMottakere() {
        var orgnrEtater = List.of(SKATTEETATEN.getOrgnr(), HELFO.getOrgnr());
        mockFinnOrganisasjon(SKATTEETATEN.getOrgnr(), "Skatteetaten-NAVNET");
        mockFinnOrganisasjon(HELFO.getOrgnr(), "HELFO-NAVNET");


        var muligMottakerDto = hentMuligeBrevmottakereEtater.hentMuligeMottakereEtater(orgnrEtater);


        assertThat(muligMottakerDto)
            .hasSize(2)
            .first()
            .hasFieldOrPropertyWithValue("mottakerNavn", "Skatteetaten-NAVNET")
            .hasFieldOrPropertyWithValue("rolle", ETAT)
            .hasFieldOrPropertyWithValue("orgnr", SKATTEETATEN.getOrgnr());
        assertThat(muligMottakerDto)
            .last()
            .hasFieldOrPropertyWithValue("mottakerNavn", "HELFO-NAVNET")
            .hasFieldOrPropertyWithValue("rolle", ETAT)
            .hasFieldOrPropertyWithValue("orgnr", HELFO.getOrgnr());
    }

    @Test
    void hentMuligeMottakereEtater_spørEtterSkatteetatenOgUkjentOrgNr_fårIkkeFunnetFeilmelding() {
        var orgnrEtater = List.of("111111111");

        assertThatThrownBy(() -> hentMuligeBrevmottakereEtater.hentMuligeMottakereEtater(orgnrEtater))
            .isInstanceOf(IkkeFunnetException.class);
    }

    private void mockFinnOrganisasjon(String orgnr, String navn) {
        when(eregFasade.finnOrganisasjon(orgnr)).thenReturn(Optional.of(lagOrgSaksopplysning(orgnr, navn)));
    }

    private Saksopplysning lagOrgSaksopplysning(String orgNummer, String navn) {
        var geogragiskAdresse = new SemistrukturertAdresse();
        geogragiskAdresse.setAdresselinje1("Gateadresse 43A");
        geogragiskAdresse.setPostnr("0123");
        geogragiskAdresse.setPoststed("Oslo");
        geogragiskAdresse.setLandkode(Land.NORGE);
        geogragiskAdresse.setGyldighetsperiode(new Periode(LocalDate.MIN, LocalDate.MAX));
        var organisasjonsDetaljer = new OrganisasjonsDetaljer();
        organisasjonsDetaljer.postadresse.add(geogragiskAdresse);
        var dokument = new OrganisasjonDokument();
        dokument.setOrganisasjonDetaljer(organisasjonsDetaljer);
        dokument.setNavn(List.of(navn));
        dokument.setOrgnummer(orgNummer);
        var saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(dokument);
        saksopplysning.setType(SaksopplysningType.ORG);
        return saksopplysning;
    }
}
