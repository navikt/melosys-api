package no.nav.melosys.saksflyt;

import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.integrasjon.joark.DokumentKategoriKode;
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

public final class TestdataFactory {
    public static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setSaksopplysninger(singleton(lagPersonopplysning()));
        behandling.setFagsak(lagFagsak());
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
        return new DokumentproduksjonsInfo("dummy_mal", DokumentKategoriKode.IB.getKode(), "Dummy tittel");
    }

    static Saksopplysning lagPersonopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "99887766554";
        saksopplysning.setDokument(personDokument);
        return saksopplysning;
    }

    static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        return fagsak;
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
        semistrukturertAdresse.setLandkode("NO");
        semistrukturertAdresse.setPostnr(postnummer);
        return semistrukturertAdresse;
    }
}
