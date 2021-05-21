package no.nav.melosys.integrasjon.tps;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.person.Informasjonsbehov;

public interface TpsFasade {
    Saksopplysning hentPerson(String ident, Informasjonsbehov behov);

    /**
     * Henter all historikk fram til angitt dato (start av søknadsperioden).
     */
    Saksopplysning hentPersonhistorikk(String ident, LocalDate dato)
    ;

    String hentSammensattNavn(String fnr);

    boolean harStrengtFortroligAdresse(String fnr);
}
