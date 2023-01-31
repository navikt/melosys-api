package no.nav.melosys.service.brev.components;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.brev.brevmalliste.BrevAdresse;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TilBrevAdresseComponentTest {

    @Mock
    private PersondataFasade persondataFasade;

    @Mock
    private KontaktopplysningService kontaktopplysningService;

    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;

    @Mock
    private EregFasade eregFasade;

    @InjectMocks
    private TilBrevAdresseComponent tilBrevAdresseComponent;

    private final Behandling behandling = lagBehandling();

    @Test
    void tilBrevAdresse_brukerSomMottaker_returnererBrukeradresse() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse());

        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.BRUKER);


        var brevAdresser = tilBrevAdresseComponent.tilBrevAdresse(aktør, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Nordmann Ola",
                null,
                List.of("gatenavnFraBostedsadresse 3"),
                "1234",
                "Oslo",
                "Norge",
                "NO");
    }

    @Test
    void tilBrevAdresse_brukersFullmaktOrganisasjonSomMottaker_returnererFullmektigsAdresse() {
        when(eregFasade.hentOrganisasjon("orgnr")).thenReturn(lagOrgSaksopplysning("orgnr", "Ola Nordmann Fullmektig"));

        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.REPRESENTANT);
        aktør.setOrgnr("orgnr");


        var brevAdresser = tilBrevAdresseComponent.tilBrevAdresse(aktør, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Ola Nordmann Fullmektig",
                "orgnr",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
    }


    @Test
    void tilBrevAdresse_brukersFullmaktPersonSomMottaker_returnererFullmektigsAdresse() {
        when(persondataFasade.hentPerson("fnr")).thenReturn(lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse());

        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.REPRESENTANT);
        aktør.setPersonIdent("fnr");


        var brevAdresser = tilBrevAdresseComponent.tilBrevAdresse(aktør, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Nordmann Ola",
                null,
                List.of("gatenavnFraBostedsadresse 3"),
                "1234",
                "Oslo",
                "Norge",
                "NO");
    }


    @Test
    void tilBrevAdresse_arbeidsgiverSomMottaker_returnererArbeidsgiverAdresser() {
        when(eregFasade.hentOrganisasjon("orgnr")).thenReturn(lagOrgSaksopplysning("orgnr", "Ola Nordmann Rørleggerfirma"));

        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.ARBEIDSGIVER);
        aktør.setOrgnr("orgnr");


        var brevAdresser = tilBrevAdresseComponent.tilBrevAdresse(aktør, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Ola Nordmann Rørleggerfirma",
                "orgnr",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
    }

    @Test
    void tilBrevAdresse_virksomhetSomMottaker_returnererVirksomhetAdresser() {
        when(eregFasade.hentOrganisasjon("orgnr")).thenReturn(lagOrgSaksopplysning("orgnr", "Ola Nordmann Rørleggerfirma"));

        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.VIRKSOMHET);
        aktør.setOrgnr("orgnr");


        var brevAdresser = tilBrevAdresseComponent.tilBrevAdresse(aktør, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Ola Nordmann Rørleggerfirma",
                "orgnr",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
    }


    @Test
    void tilBrevAdresse_etatSomMottaker_returnererEtatAdresser() {
        when(eregFasade.hentOrganisasjon("orgnr-skatteetaten")).thenReturn(lagOrgSaksopplysning("orgnr-skatteetaten", "SKATTEETATEN"));

        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.ETAT);
        aktør.setOrgnr("orgnr-skatteetaten");


        var brevAdresser = tilBrevAdresseComponent.tilBrevAdresse(aktør, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "SKATTEETATEN",
                "orgnr-skatteetaten",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
    }

    @Test
    void tilBrevAdresse_returnererAdresseFelterSomNull_nårGjeldendePostadresseErNull() {
        Personopplysninger persondata = PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser();
        when(persondataFasade.hentPerson(anyString())).thenReturn(persondata);

        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.BRUKER);
        aktør.setOrgnr(null);


        var brevAdresser = tilBrevAdresseComponent.tilBrevAdresse(aktør, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly("Nordmann Ola", null, null, null, null, null, null);
    }

    @Test
    void hentBrevAdresseTilMottakere_returnererAdresseMedKorrektAdresselinjer_nårCoAdresseErTomStreng() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(lagPersondataMedTomCo());

        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.BRUKER);
        aktør.setOrgnr(null);


        var brevAdresser = tilBrevAdresseComponent.tilBrevAdresse(aktør, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(BrevAdresse::getAdresselinjer)
            .isEqualTo(List.of("gatenavnFraBostedsadresse 3"));
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setFagsak(lagFagsak());
        behandling.getSaksopplysninger().add(lagPERSOPLSaksopplysning());
        return behandling;
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        Aktoer bruker = new Aktoer();
        bruker.setRolle(BRUKER);
        bruker.setAktørId("aktørId");
        fagsak.getAktører().add(bruker);
        return fagsak;
    }

    private Saksopplysning lagPERSOPLSaksopplysning() {
        var dokument = new PersonDokument();
        dokument.setFnr("12345678910");
        dokument.setSammensattNavn("Ola Nordmann");
        dokument.getGjeldendePostadresse().adresselinje1 = "Gateadresse 43A";
        dokument.getGjeldendePostadresse().postnr = "0123";
        dokument.getGjeldendePostadresse().land = Land.av(Land.NORGE);
        var saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(dokument);
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        return saksopplysning;
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

    private Persondata lagPersondataMedTomCo() {
        var bostedsadresse = new Bostedsadresse(
            new StrukturertAdresse("gatenavnFraBostedsadresse 3", null, null, null, null, null),
            "", null, null, null, null, false);
        return new Personopplysninger(
            emptyList(), bostedsadresse, null, emptySet(), null, null,
            null, emptySet(), new Navn(null, null, null), emptySet(), emptySet());
    }
}
