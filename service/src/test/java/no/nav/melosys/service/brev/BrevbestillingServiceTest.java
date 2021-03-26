package no.nav.melosys.service.brev;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrevbestillingServiceTest {

    @Mock
    private DokumentServiceFasade mockDokServiceFasade;
    @Mock
    private BrevmottakerService mockBrevmottakerService;
    @Mock
    private DokgenService mockDokgenService;
    @Mock
    private PersondataFasade mockPersondataFasade;
    @Mock
    private EregFasade mockEregFasade;
    @Mock
    private KontaktopplysningService mockKontaktopplysningService;
    @Mock
    private KodeverkService mockKodeverkService;

    private BrevbestillingService brevbestillingService;

    @BeforeEach
    void init() {
        brevbestillingService = new BrevbestillingService(
            mockDokServiceFasade, mockDokgenService, mockBrevmottakerService, mockPersondataFasade, mockEregFasade, mockKontaktopplysningService, mockKodeverkService);
    }

    @Test
    void hentBrevMaler_behandlingIkkeAvsluttet_returnererMaler() {
        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentBrevMaler(new Behandling());

        assertThat(brevMaler)
            .hasSize(2)
            .containsExactlyInAnyOrder(MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER);
    }

    @Test
    void hentBrevMaler_behandlingAvsluttet_returnererTomListe() {
        var behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentBrevMaler(behandling);

        assertThat(brevMaler).isEmpty();
    }

    @Test
    void hentBrevMaler_behandlingErSoeknad_returnererSoeknadMalITillegg() {
        var behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentBrevMaler(behandling);

        assertThat(brevMaler)
            .hasSize(3)
            .containsExactlyInAnyOrder(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER);
    }

    @Test
    void hentBrevMaler_behandlingErKlage_returnererKlageMalITillegg() {
        var behandling = new Behandling();
        behandling.setType(Behandlingstyper.KLAGE);
        List<Produserbaredokumenter> brevMaler = brevbestillingService.hentBrevMaler(behandling);

        assertThat(brevMaler)
            .hasSize(3)
            .containsExactlyInAnyOrder(MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE, MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER);
    }

    @Test
    void hentBrevAdresseTilMottakere_brukerSomMottaker_returnererBrukeradresse() throws Exception {
        var saksbehandling = lagPERSOPLSaksopplysning();
        var behandling = new Behandling();
        behandling.setSaksopplysninger(Set.of(saksbehandling));

        when(mockBrevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoer(Aktoersroller.BRUKER, null)));
        when(mockPersondataFasade.hentPerson(any(), eq(Informasjonsbehov.STANDARD)))
            .thenReturn(saksbehandling);
        when(mockKodeverkService.dekod(eq(FellesKodeverk.POSTNUMMER), anyString(), any())).thenReturn("Oslo");

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(MANGELBREV_BRUKER, Aktoersroller.BRUKER, behandling);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(BrevAdresse::getMottakerNavn, BrevAdresse::getOrgnr, BrevAdresse::getAdresselinjer, BrevAdresse::getPostnr, BrevAdresse::getPoststed, BrevAdresse::getLand)
            .containsExactly("Ola Nordmann", null, List.of("Gateadresse 43A"), "0123", "Oslo", Land.NORGE);
    }

    @Test
    void hentBrevAdresseTilMottakere_brukersFullmaktSomMottaker_returnererFullmektigsAdresse() throws Exception {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());

        when(mockBrevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoer(Aktoersroller.REPRESENTANT, "orgNr")));
        when(mockEregFasade.hentOrganisasjon(eq("orgNr"))).thenReturn(lagORGSaksopplysning("orgNr", "Ola Nordmann Fullmektig"));

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(MANGELBREV_BRUKER, Aktoersroller.BRUKER, behandling);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(BrevAdresse::getMottakerNavn, BrevAdresse::getOrgnr, BrevAdresse::getAdresselinjer, BrevAdresse::getPostnr, BrevAdresse::getPoststed, BrevAdresse::getLand)
            .containsExactly("Ola Nordmann Fullmektig", "orgNr", List.of("Gateadresse 43A"), "0123", "Oslo", Land.NORGE);
    }

    @Test
    void hentBrevAdresseTilMottakere_arbeidsgiverSomMottaker_returnererArbeidsgiverAdresser() throws Exception {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());

        when(mockBrevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.ARBEIDSGIVER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoer(Aktoersroller.ARBEIDSGIVER, "orgNr1"), lagAktoer(Aktoersroller.ARBEIDSGIVER, "orgNr2")));
        when(mockEregFasade.hentOrganisasjon(eq("orgNr1"))).thenReturn(lagORGSaksopplysning("orgNr1", "Ola Nordmann Rørleggerfirma"));
        when(mockEregFasade.hentOrganisasjon(eq("orgNr2"))).thenReturn(lagORGSaksopplysning("orgNr2", "Ida Nordmann Rørleggerfirma"));

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(MANGELBREV_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER, behandling);

        assertThat(brevAdresser).hasSize(2);
        assertThat(brevAdresser.get(0))
            .extracting(BrevAdresse::getMottakerNavn, BrevAdresse::getOrgnr, BrevAdresse::getAdresselinjer, BrevAdresse::getPostnr, BrevAdresse::getPoststed, BrevAdresse::getLand)
            .containsExactly("Ola Nordmann Rørleggerfirma", "orgNr1", List.of("Gateadresse 43A"), "0123", "Oslo", Land.NORGE);
        assertThat(brevAdresser.get(1))
            .extracting(BrevAdresse::getMottakerNavn, BrevAdresse::getOrgnr, BrevAdresse::getAdresselinjer, BrevAdresse::getPostnr, BrevAdresse::getPoststed, BrevAdresse::getLand)
            .containsExactly("Ida Nordmann Rørleggerfirma", "orgNr2", List.of("Gateadresse 43A"), "0123", "Oslo", Land.NORGE);
    }

    @Test
    void hentBrevAdresseTilMottakere_arbeidsgiversFullmaktSomMottaker_returnererFullmektigsAdresse() throws Exception {
        var behandling = new Behandling();
        behandling.setFagsak(new Fagsak());

        when(mockBrevmottakerService.avklarMottakere(any(), eq(Mottaker.av(Aktoersroller.ARBEIDSGIVER)), any(), eq(false), eq(false)))
            .thenReturn(List.of(lagAktoer(Aktoersroller.REPRESENTANT, "orgNr")));
        when(mockEregFasade.hentOrganisasjon(eq("orgNr"))).thenReturn(lagORGSaksopplysning("orgNr", "Ola Nordmann Fullmektig"));

        var brevAdresser = brevbestillingService.hentBrevAdresseTilMottakere(MANGELBREV_ARBEIDSGIVER, Aktoersroller.ARBEIDSGIVER, behandling);

        assertThat(brevAdresser).hasSize(1);
        assertThat(brevAdresser.get(0))
            .extracting(BrevAdresse::getMottakerNavn, BrevAdresse::getOrgnr, BrevAdresse::getAdresselinjer, BrevAdresse::getPostnr, BrevAdresse::getPoststed, BrevAdresse::getLand)
            .containsExactly("Ola Nordmann Fullmektig", "orgNr", List.of("Gateadresse 43A"), "0123", "Oslo", Land.NORGE);
    }


    @Test
    void skalBestilleProduseringAvBrev() throws Exception {
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder().medProduserbardokument(MANGELBREV_BRUKER).build();
        brevbestillingService.produserBrev(123L, brevbestillingDto);

        verify(mockDokgenService).produserOgDistribuerBrev(eq(MANGELBREV_BRUKER), anyLong(), any());
    }

    @Test
    void skalReturnereUtkast() throws Exception {
        byte[] pdf = "UTKAST".getBytes(StandardCharsets.UTF_8);
        when(mockDokServiceFasade.produserUtkast(any(), anyLong(), any())).thenReturn(pdf);
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder().medProduserbardokument(MANGELBREV_BRUKER).build();

        byte[] utkast = brevbestillingService.produserUtkast(123L, brevbestillingDto);

        assertThat(utkast).isEqualTo(pdf);
        verify(mockDokServiceFasade).produserUtkast(eq(MANGELBREV_BRUKER), anyLong(), any());
    }

    private Aktoer lagAktoer(Aktoersroller aktoersroller, String orgNummer) {
        var aktoer = new Aktoer();
        aktoer.setRolle(aktoersroller);
        aktoer.setOrgnr(orgNummer);
        return aktoer;
    }

    private Saksopplysning lagORGSaksopplysning(String orgNummer, String navn) {
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

    private Saksopplysning lagPERSOPLSaksopplysning() {
        var dokument = new PersonDokument();
        dokument.fnr = "12345678910";
        dokument.sammensattNavn = "Ola Nordmann";
        dokument.gjeldendePostadresse.adresselinje1 = "Gateadresse 43A";
        dokument.gjeldendePostadresse.postnr = "0123";
        dokument.gjeldendePostadresse.land = Land.av(Land.NORGE);
        var saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(dokument);
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        return saksopplysning;
    }
}