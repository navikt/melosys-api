package no.nav.melosys.service.brev.bestilling;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.brev.BrevAdresse;
import no.nav.melosys.service.brev.TilBrevAdresseService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysninger;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TilBrevAdresseServiceTest {

    @Mock
    private PersondataFasade persondataFasade;

    @Mock
    private KontaktopplysningService kontaktopplysningService;

    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;

    @Mock
    private EregFasade eregFasade;

    @InjectMocks
    private TilBrevAdresseService tilBrevAdresseService;

    private final Behandling behandling = lagBehandling();

    @Test
    void tilBrevAdresse_brukerSomMottaker_returnererBrukeradresse() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse());

        Mottaker mottaker = Mottaker.medRolle(Mottakerroller.BRUKER);

        var brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand,
                BrevAdresse::getUgyldig)
            .containsExactly(
                "Nordmann Ola",
                null,
                List.of("gatenavnFraBostedsadresse 3"),
                "1234",
                "Oslo",
                "Norge",
                "NO",
                false);
    }

    @Test
    void tilBrevAdresse_brukersFullmaktOrganisasjonSomMottaker_returnererFullmektigsAdresse() {
        when(eregFasade.hentOrganisasjon("orgnr")).thenReturn(lagOrgSaksopplysning("orgnr", "Ola Nordmann Fullmektig"));

        Mottaker mottaker = Mottaker.medRolle(Mottakerroller.FULLMEKTIG);
        mottaker.setOrgnr("orgnr");


        var brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand,
                BrevAdresse::getUgyldig)
            .containsExactly(
                "Ola Nordmann Fullmektig",
                "orgnr",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE,
                false);
    }


    @Test
    void tilBrevAdresse_brukersFullmaktPersonSomMottaker_returnererFullmektigsAdresse() {
        when(persondataFasade.hentPerson("fnr")).thenReturn(lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse());

        Mottaker mottaker = Mottaker.medRolle(Mottakerroller.FULLMEKTIG);
        mottaker.setPersonIdent("fnr");


        var brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand,
                BrevAdresse::getUgyldig)
            .containsExactly(
                "Nordmann Ola",
                null,
                List.of("gatenavnFraBostedsadresse 3"),
                "1234",
                "Oslo",
                "Norge",
                "NO",
                false);
    }


    @Test
    void tilBrevAdresse_arbeidsgiverSomMottaker_returnererArbeidsgiverAdresser() {
        when(eregFasade.hentOrganisasjon("orgnr")).thenReturn(lagOrgSaksopplysning("orgnr", "Ola Nordmann Rørleggerfirma"));

        Mottaker mottaker = Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER);
        mottaker.setOrgnr("orgnr");


        var brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand,
                BrevAdresse::getUgyldig)
            .containsExactly(
                "Ola Nordmann Rørleggerfirma",
                "orgnr",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE,
                false);
    }

    @Test
    void tilBrevAdresse_virksomhetSomMottaker_returnererVirksomhetAdresser() {
        when(eregFasade.hentOrganisasjon("orgnr")).thenReturn(lagOrgSaksopplysning("orgnr", "Ola Nordmann Rørleggerfirma"));

        Mottaker mottaker = Mottaker.medRolle(Mottakerroller.VIRKSOMHET);
        mottaker.setOrgnr("orgnr");


        var brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand,
                BrevAdresse::getUgyldig)
            .containsExactly(
                "Ola Nordmann Rørleggerfirma",
                "orgnr",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE,
                false);
    }


    @Test
    void tilBrevAdresse_norskMyndighetSomMottaker_kasterFeil() {
        Mottaker mottaker = Mottaker.medRolle(Mottakerroller.NORSK_MYNDIGHET);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> tilBrevAdresseService.tilBrevAdresse(mottaker, behandling))
            .withMessageContaining("Mottakersrolle støttes ikke: NORSK_MYNDIGHET");
    }

    @Test
    void tilBrevAdresse_utenlandsakTrygdemyndighetSomMottaker_kasterFeil() {
        Mottaker mottaker = Mottaker.medRolle(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> tilBrevAdresseService.tilBrevAdresse(mottaker, behandling))
            .withMessageContaining("Mottakersrolle støttes ikke: UTENLANDSK_TRYGDEMYNDIGHET");
    }

    @Test
    void tilBrevAdresse_returnererAdresseFelterSomNull_nårGjeldendePostadresseErNull() {
        Personopplysninger persondata = PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser();
        when(persondataFasade.hentPerson(anyString())).thenReturn(persondata);

        Mottaker mottaker = Mottaker.medRolle(Mottakerroller.BRUKER);
        mottaker.setOrgnr(null);


        var brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand,
                BrevAdresse::getUgyldig)
            .containsExactly(
                "Nordmann Ola",
                null,
                null,
                null,
                null,
                null,
                null,
                true);
    }

    @Test
    void hentBrevAdresseTilMottakere_returnererAdresseMedKorrektAdresselinjer_nårCoAdresseErTomStreng() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(lagPersondataMedTomCo());

        Mottaker mottaker = Mottaker.medRolle(Mottakerroller.BRUKER);
        mottaker.setOrgnr(null);


        var brevAdresser = tilBrevAdresseService.tilBrevAdresse(mottaker, behandling);


        assertThat(brevAdresser)
            .isNotNull()
            .extracting(BrevAdresse::getAdresselinjer)
            .isEqualTo(List.of("gatenavnFraBostedsadresse 3"));
    }

    @Test
    void tilBrevAdresse_verkenPersonIdentEllerOrgnr_kasterFeil() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> tilBrevAdresseService.tilBrevAdresse((String) null, null))
            .withMessageContaining("Kan ikke finne adresse uten personIdent og organisasjonsnummer");
    }

    @Test
    void tilBrevAdresse_finnerIkkePersonDataFraPersonIdent_kasterFeil() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> tilBrevAdresseService.tilBrevAdresse("123", null))
            .withMessageContaining("Finner ikke persondata for personIdent");
    }

    @Test
    void tilBrevAdresse_personIdentSendesInn_returnererPersonAdresse() {
        when(persondataFasade.hentPerson("123")).thenReturn(lagPersonopplysninger());


        var brevAdresse = tilBrevAdresseService.tilBrevAdresse("123", null);


        verifyNoInteractions(eregFasade);
        assertThat(brevAdresse)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand,
                BrevAdresse::getUgyldig)
            .containsExactly(
                "Nordmann Ola",
                null,
                List.of("gatenavnKontaktadressePDL"),
                "0123",
                "Poststed",
                null,
                "NO",
                false);
    }

    @Test
    void tilBrevAdresse_orgnrSendesInn_returnererOrganisasjonsAdresse() {
        when(eregFasade.hentOrganisasjon("orgnr")).thenReturn(lagOrgSaksopplysning("orgnr", "Ola Nordmann Rørleggerfirma"));


        var brevAdresse = tilBrevAdresseService.tilBrevAdresse(null, "orgnr");


        verifyNoInteractions(persondataFasade);
        assertThat(brevAdresse)
            .isNotNull()
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand,
                BrevAdresse::getUgyldig)
            .containsExactly(
                "Ola Nordmann Rørleggerfirma",
                "orgnr",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE,
                false);
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
        bruker.setRolle(Aktoersroller.BRUKER);
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
