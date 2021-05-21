package no.nav.melosys.service.persondata;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.person.Informasjonsbehov;

public interface PersondataFasade {
    String hentAktørIdForIdent(String ident);

    String hentFolkeregisterIdent(String ident);

    Saksopplysning hentPerson(String ident, Informasjonsbehov behov);

    Saksopplysning hentPersonhistorikk(String ident, LocalDate dato);

    String hentSammensattNavn(String fnr);

    boolean harStrengtFortroligAdresse(String fnr);
}
