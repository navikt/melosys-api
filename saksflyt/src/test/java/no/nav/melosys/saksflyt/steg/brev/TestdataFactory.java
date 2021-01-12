package no.nav.melosys.saksflyt.steg.brev;

import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

public final class TestdataFactory {
    static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setSaksopplysninger(singleton(lagPersonopplysning()));
        behandling.setFagsak(lagFagsak());
        return behandling;
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

    static OrganisasjonDokument lagOrg() {
        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.setOrgnummer("122344");
        organisasjonDokument.setOrganisasjonDetaljer(lagOrgDetaljer());
        return organisasjonDokument;
    }

    static OrganisasjonsDetaljer lagOrgDetaljer() {
        OrganisasjonsDetaljer organisasjonsDetaljer = new OrganisasjonsDetaljer();
        organisasjonsDetaljer.postadresse = singletonList(lagOrgAdresse());
        return organisasjonsDetaljer;
    }

    static GeografiskAdresse lagOrgAdresse() {
        SemistrukturertAdresse semistrukturertAdresse = new SemistrukturertAdresse();
        semistrukturertAdresse.setGyldighetsperiode(new Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(2)));
        return semistrukturertAdresse;
    }

    static Kontaktopplysning lagKontaktOpplysning() {
        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktNavn("Donald Duck");
        return kontaktopplysning;
    }
}
