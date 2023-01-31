package no.nav.melosys.service.brev;

import java.time.LocalDate;
import java.util.List;

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
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.Personopplysninger;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.brevmalliste.BrevAdresse;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Deprecated(since = "Ta vekk med melosys.MEL-4835.refactor1, ertattes av Component tester (som allerede er på plass)")
@ExtendWith(MockitoExtension.class)
class BrevmalListeServiceTestGammel {

    @Mock
    private BehandlingService behandlingService;

    @Mock
    private BrevmottakerService brevmottakerService;

    @Mock
    private PersondataFasade persondataFasade;

    @Mock
    private KontaktopplysningService kontaktopplysningService;

    @Mock
    private UtenlandskMyndighetService utenlandskMyndighetService;

    @Mock
    private EregFasade eregFasade;

    @InjectMocks
    private BrevmalListeService brevmalListeService;

    private final Behandling behandling = lagBehandling();

    @Test
    void hentBrevMaler_tilBruker_returnererKorrektListe() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);

        List<Produserbaredokumenter> brevMaler = brevmalListeService.hentMuligeProduserbaredokumenterGammel(123L, BRUKER);

        assertThat(brevMaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                MANGELBREV_BRUKER,
                GENERELT_FRITEKSTBREV_BRUKER
            );
    }

    @Test
    void hentBrevMaler_tilArbeidsgiver_returnererKorrektListe() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);

        List<Produserbaredokumenter> brevMaler = brevmalListeService.hentMuligeProduserbaredokumenterGammel(123L, ARBEIDSGIVER);

        assertThat(brevMaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                MANGELBREV_ARBEIDSGIVER,
                GENERELT_FRITEKSTBREV_ARBEIDSGIVER
            );
    }

    @Test
    void hentBrevMaler_tilVirksomhet_returnererKorrektListe() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);

        List<Produserbaredokumenter> brevMaler = brevmalListeService.hentMuligeProduserbaredokumenterGammel(123L, VIRKSOMHET);

        assertThat(brevMaler).hasSize(1).containsExactly(GENERELT_FRITEKSTBREV_VIRKSOMHET);
    }

    @Test
    void hentBrevMaler_behandlingAvsluttet_returnererTomListe() {
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        when(behandlingService.hentBehandlingMedSaksopplysninger(321L)).thenReturn(behandling);
        List<Produserbaredokumenter> brevMaler = brevmalListeService.hentMuligeProduserbaredokumenterGammel(321L, BRUKER);

        assertThat(brevMaler).isEmpty();
    }

    @Test
    void hentBrevMaler_behandlingErFørstegangMedSakstemaMedlemskapLovvalg_returnererForventetSaksbehandlingstidMalITillegg() {
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.getFagsak().setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        when(behandlingService.hentBehandlingMedSaksopplysninger(321L)).thenReturn(behandling);
        List<Produserbaredokumenter> brevMaler = brevmalListeService.hentMuligeProduserbaredokumenterGammel(321L, BRUKER);

        assertThat(brevMaler)
            .hasSize(3)
            .containsExactlyInAnyOrder(
                MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                MANGELBREV_BRUKER,
                GENERELT_FRITEKSTBREV_BRUKER
            );
    }

    @Test
    void hentBrevMaler_behandlingErKlage_returnererKorrekt() {
        behandling.setType(Behandlingstyper.KLAGE);
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        List<Produserbaredokumenter> brevMaler = brevmalListeService.hentMuligeProduserbaredokumenterGammel(123L, BRUKER);

        assertThat(brevMaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                MANGELBREV_BRUKER,
                GENERELT_FRITEKSTBREV_BRUKER
            );
    }

    @Test
    void hentBrevAdresseTilMottakere_brukerSomMottaker_returnererBrukeradresse() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(Aktoersroller.BRUKER, null)));
        when(persondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse());

        var brevAdresser = brevmalListeService.hentBrevAdresseTilMottakereGammel(Aktoersroller.BRUKER, 123);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
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
    void hentBrevAdresseTilMottakere_brukersFullmaktOrganisasjonSomMottaker_returnererFullmektigsAdresse() {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(Aktoersroller.REPRESENTANT, "orgNr")));
        when(eregFasade.hentOrganisasjon("orgNr")).thenReturn(lagOrgSaksopplysning("orgNr", "Ola Nordmann Fullmektig"));

        var brevAdresser = brevmalListeService.hentBrevAdresseTilMottakereGammel(Aktoersroller.BRUKER, 123);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
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
                "orgNr",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
    }

    @Test
    void hentBrevAdresseTilMottakere_brukersFullmaktPersonSomMottaker_returnererFullmektigsAdresse() {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerPerson(Aktoersroller.REPRESENTANT, "fnr")));
        when(persondataFasade.hentPerson("fnr")).thenReturn(lagPersonopplysningerUtenOppholdsadresseOgKontaktadresse());

        var brevAdresser = brevmalListeService.hentBrevAdresseTilMottakereGammel(Aktoersroller.BRUKER, 123);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
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
    void hentBrevAdresseTilMottakere_arbeidsgiverSomMottaker_returnererArbeidsgiverAdresser() {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());

        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.ARBEIDSGIVER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(Aktoersroller.ARBEIDSGIVER, "orgNr1"), lagAktoerOrg(Aktoersroller.ARBEIDSGIVER, "orgNr2")));
        when(eregFasade.hentOrganisasjon("orgNr1")).thenReturn(lagOrgSaksopplysning("orgNr1", "Ola Nordmann Rørleggerfirma"));
        when(eregFasade.hentOrganisasjon("orgNr2")).thenReturn(lagOrgSaksopplysning("orgNr2", "Ida Nordmann Rørleggerfirma"));

        var brevAdresser = brevmalListeService.hentBrevAdresseTilMottakereGammel(Aktoersroller.ARBEIDSGIVER, 123);

        assertThat(brevAdresser).hasSize(2);
        assertThat(brevAdresser.get(0))
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
                "orgNr1",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
        assertThat(brevAdresser.get(1))
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr,
                BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Ida Nordmann Rørleggerfirma",
                "orgNr2",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
    }

    @Test
    void hentBrevAdresseTilMottakere_arbeidsgiverSomMottakerMenIngenArbeidsgivere_returnererTomListe() {
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.ARBEIDSGIVER)), any(), eq(false), eq(false)))
            .thenReturn(emptyList());

        var brevAdresser = brevmalListeService.hentBrevAdresseTilMottakereGammel(Aktoersroller.ARBEIDSGIVER, 123L);

        assertThat(brevAdresser).isEmpty();
    }

    @Test
    void hentBrevAdresseTilMottakere_arbeidsgiversFullmaktSomMottaker_returnererFullmektigsAdresse() {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.ARBEIDSGIVER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(Aktoersroller.REPRESENTANT, "orgNr")));
        when(eregFasade.hentOrganisasjon("orgNr")).thenReturn(lagOrgSaksopplysning("orgNr", "Ola Nordmann Fullmektig"));

        var brevAdresser = brevmalListeService.hentBrevAdresseTilMottakereGammel(Aktoersroller.ARBEIDSGIVER, 123);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(
                BrevAdresse::getMottakerNavn,
                BrevAdresse::getOrgnr, BrevAdresse::getAdresselinjer,
                BrevAdresse::getPostnr,
                BrevAdresse::getPoststed,
                BrevAdresse::getRegion,
                BrevAdresse::getLand)
            .containsExactly(
                "Ola Nordmann Fullmektig",
                "orgNr",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
    }

    @Test
    void hentBrevAdresseTilMottakere_virksomhetSomMottaker_returnererVirksomhetAdresse() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(VIRKSOMHET)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(VIRKSOMHET, "orgNr1")));
        when(eregFasade.hentOrganisasjon("orgNr1")).thenReturn(lagOrgSaksopplysning("orgNr1", "Ola Nordmann Rørleggerfirma"));

        var brevAdresser = brevmalListeService.hentBrevAdresseTilMottakereGammel(VIRKSOMHET, 123);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
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
                "orgNr1",
                List.of("Gateadresse 43A"),
                "0123",
                "Oslo",
                null,
                Land.NORGE);
    }

    @Test
    void hentBrevAdresseTilMottakere_returnererAdresseFelterSomNull_nårGjeldendePostadresseErNull() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(Aktoersroller.BRUKER, null)));
        Personopplysninger persondata = PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser();
        when(persondataFasade.hentPerson(anyString())).thenReturn(persondata);

        var brevAdresser = brevmalListeService.hentBrevAdresseTilMottakereGammel(Aktoersroller.BRUKER, 123L);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
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
        when(behandlingService.hentBehandlingMedSaksopplysninger(123L)).thenReturn(behandling);
        when(brevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoerOrg(Aktoersroller.BRUKER, null)));
        when(persondataFasade.hentPerson(anyString())).thenReturn(lagPersondataMedTomCo());

        var brevAdresser = brevmalListeService.hentBrevAdresseTilMottakereGammel(Aktoersroller.BRUKER, 123L);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0).getAdresselinjer()).isEqualTo(List.of("gatenavnFraBostedsadresse 3"));
    }

    private Persondata lagPersondataMedTomCo() {
        var bostedsadresse = new Bostedsadresse(
            new StrukturertAdresse("gatenavnFraBostedsadresse 3", null, null, null, null, null),
            "", null, null, null, null, false);
        return new Personopplysninger(
            emptyList(), bostedsadresse, null, emptySet(), null, null,
            null, emptySet(), new Navn(null, null, null), emptySet(), emptySet());
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

    private Aktoer lagAktoerOrg(Aktoersroller aktoersroller, String orgNummer) {
        var aktoer = new Aktoer();
        aktoer.setRolle(aktoersroller);
        aktoer.setOrgnr(orgNummer);
        return aktoer;
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

    private Aktoer lagAktoerPerson(Aktoersroller aktoersroller, String personIdent) {
        var aktoer = new Aktoer();
        aktoer.setRolle(aktoersroller);
        aktoer.setPersonIdent(personIdent);
        return aktoer;
    }
}
