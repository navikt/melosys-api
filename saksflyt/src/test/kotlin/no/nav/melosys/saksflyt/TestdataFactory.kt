package no.nav.melosys.saksflyt;

import java.time.LocalDate;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.integrasjon.joark.DokumentKategoriKode;
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;

public final class TestdataFactory {
    public static Behandling lagBehandling() {
        return BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medTema(Behandlingstema.UTSENDT_ARBEIDSTAKER)
            .medSaksopplysninger(singleton(lagPersonopplysning()))
            .medFagsak(lagFagsak())
            .build();
    }

    public static Behandling lagBehandlingNyVurdering() {
        var behandling = lagBehandling();
        behandling.setId(3L);
        behandling.setType(Behandlingstyper.NY_VURDERING);
        return behandling;
    }

    public static OrganisasjonDokument lagOrgMedPostadresse() {
        return OrganisasjonDokumentTestFactory.builder()
            .orgnummer("122344")
            .organisasjonsDetaljer(lagOrgDetaljerMedPostadresse()).build();
    }

    public static OrganisasjonDokument lagOrgMedForretningsadresse() {
        return OrganisasjonDokumentTestFactory.builder()
            .orgnummer("122344")
            .organisasjonsDetaljer(lagOrgDetaljerMedForretningsadresse()).build();
    }

    public static Kontaktopplysning lagKontaktOpplysning() {
        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktNavn("Donald Duck");
        return kontaktopplysning;
    }

    public static DokumentproduksjonsInfo lagDokumentInfo() {
        return new DokumentproduksjonsInfo("dummy_mal", DokumentKategoriKode.IB.getKode(), "Dummy tittel", null);
    }

    static Saksopplysning lagPersonopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        PersonDokument personDokument = new PersonDokument();
        personDokument.setFnr("99887766554");
        saksopplysning.setDokument(personDokument);
        return saksopplysning;
    }

    public static Fagsak lagFagsak() {
        return lagFagsak("MEL-test");
    }

    public static Fagsak lagFagsak(String saksnummer) {
        return new Fagsak(
            saksnummer,
            123L,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET,
            null,
            Set.of(lagBruker()),
            emptyList()
        );
    }

    public static Aktoer lagBruker() {
        var aktoer = new Aktoer();
        aktoer.setRolle(BRUKER);
        aktoer.setAktørId("aktørID");
        return aktoer;
    }

    static OrganisasjonsDetaljer lagOrgDetaljerMedPostadresse() {
        return OrganisasjonsDetaljerTestFactory.builder()
            .postadresse(lagOrgadresse("1234"))
            .build();
    }

    static OrganisasjonsDetaljer lagOrgDetaljerMedForretningsadresse() {
        return OrganisasjonsDetaljerTestFactory.builder()
            .forretningsadresse(lagOrgadresse("1234"))
            .build();
    }

    static GeografiskAdresse lagOrgadresse(String postnummer) {
        SemistrukturertAdresse semistrukturertAdresse = new SemistrukturertAdresse();
        semistrukturertAdresse.setGyldighetsperiode(new Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(2)));
        semistrukturertAdresse.setAdresselinje1("Testgata 3");
        semistrukturertAdresse.setPostnr(postnummer);
        semistrukturertAdresse.setLandkode("NO");
        return semistrukturertAdresse;
    }
}
