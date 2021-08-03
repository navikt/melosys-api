package no.nav.melosys.domain.person;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.brev.Postadresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;

public interface Persondata {
    boolean erPersonDød();

    Optional<Familiemedlem> hentAnnenForelder(String fnrGjeldendeForelder);

    boolean harStrengtAdressebeskyttelse();

    boolean harIkkeRegistrertAdresse();

    boolean manglerBostedsadresse();

    String hentFolkeregisterIdent();

    Set<Land> hentAlleStatsborgerskap();

    KjoennType hentKjønnType();

    String getFornavn();

    String getMellomnavn();

    String getEtternavn();

    String getSammensattNavn();

    List<Familiemedlem> getFamiliemedlemmer();

    LocalDate getFødselsdato();

    @Deprecated // knyttet til TPS
    no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse getBostedsadresse();

    Optional<Bostedsadresse> finnBostedsadresse();

    Optional<Kontaktadresse> finnKontaktadresse();

    Optional<Oppholdsadresse> finnOppholdsadresse();

    Postadresse hentGjeldendePostadresse();
}
