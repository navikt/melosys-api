package no.nav.melosys.service.persondata;

import java.time.LocalDate;
import java.util.Set;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.Statsborgerskap;

public interface PersondataFasade {
    String hentAktørIdForIdent(String ident);

    String hentFolkeregisterIdent(String ident);

    @Deprecated
    Saksopplysning hentPersonFraTps(String fnr, Informasjonsbehov behov);

    Persondata hentPerson(String ident);

    Saksopplysning hentPersonhistorikk(String fnr, LocalDate dato);

    String hentSammensattNavn(String fnr);

    Set<Statsborgerskap> hentStatsborgerskap(String ident);

    boolean harStrengtFortroligAdresse(String fnr);
}
