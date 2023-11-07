package no.nav.melosys.saksflyt;

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

import java.time.LocalDate;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;

public final class TestdataFactory {
    public static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setSaksopplysninger(singleton(lagPersonopplysning()));
        behandling.setFagsak(lagFagsak());
        return behandling;
    }

    public static Behandling lagBehandlingNyVurdering() {
        var behandling = lagBehandling();
        behandling.setId(3L);
        behandling.setType(Behandlingstyper.NY_VURDERING);
        return behandling;
    }

    public static OrganisasjonDokument lagOrgMedPostadresse() {
        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.setOrgnummer("122344");
        organisasjonDokument.setOrganisasjonDetaljer(lagOrgDetaljerMedPostadresse());
        return organisasjonDokument;
    }

    public static OrganisasjonDokument lagOrgMedForretningsadresse() {
        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.setOrgnummer("122344");
        organisasjonDokument.setOrganisasjonDetaljer(lagOrgDetaljerMedForretningsadresse());
        return organisasjonDokument;
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
        var fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        fagsak.getAktører().add(lagBruker());
        fagsak.setGsakSaksnummer(123L);
        return fagsak;
    }

    public static Aktoer lagBruker() {
        var aktoer = new Aktoer();
        aktoer.setRolle(BRUKER);
        aktoer.setAktørId("aktørID");
        return aktoer;
    }

    static OrganisasjonsDetaljer lagOrgDetaljerMedPostadresse() {
        OrganisasjonsDetaljer organisasjonsDetaljer = new OrganisasjonsDetaljer();
        organisasjonsDetaljer.postadresse = singletonList(lagOrgadresse("1234"));
        return organisasjonsDetaljer;
    }

    static OrganisasjonsDetaljer lagOrgDetaljerMedForretningsadresse() {
        OrganisasjonsDetaljer organisasjonsDetaljer = new OrganisasjonsDetaljer();
        organisasjonsDetaljer.forretningsadresse = singletonList(lagOrgadresse("9876"));
        return organisasjonsDetaljer;
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
