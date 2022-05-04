package no.nav.melosys.service.persondata;

import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.PersonMedHistorikk;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.domain.person.familie.Familiemedlem;

public interface PersondataFasade {
    String hentAktørIdForIdent(String ident);

    Set<Familiemedlem> hentFamiliemedlemmerFraBehandlingID(long behandlingID);

    Set<Familiemedlem> hentFamiliemedlemmerFraIdent(String ident);

    Optional<String> finnFolkeregisterident(String ident);

    String hentFolkeregisterident(String ident);

    Persondata hentPerson(String ident);

    Persondata hentPerson(String ident, Informasjonsbehov informasjonsbehov);

    PersonMedHistorikk hentPersonMedHistorikk(long behandlingID);

    PersonMedHistorikk hentPersonMedHistorikk(String ident);

    String hentSammensattNavn(String ident);

    Set<Statsborgerskap> hentStatsborgerskap(String ident);

    boolean harStrengtFortroligAdresse(String ident);
}
