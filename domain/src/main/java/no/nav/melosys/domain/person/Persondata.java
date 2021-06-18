package no.nav.melosys.domain.person;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.*;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.UstrukturertAdresse;

public interface Persondata {
    boolean erPersonDød();

    Optional<Familiemedlem> hentAnnenForelder(String fnrGjeldendeForelder);

    boolean harStrengtAdressebeskyttelse();

    boolean harIkkeRegistrertAdresse();

    boolean manglerBostedsadresse();

    String hentFolkeregisterIdent();

    @Deprecated // Brukes bare til visning, som skal gå gjennom GraphQL
    Sivilstand getSivilstand();

    @Deprecated // Brukes bare til visning, som skal gå gjennom GraphQL
    LocalDate getSivilstandGyldighetsperiodeFom();

    Set<Land> hentAlleStatsborgerskap();

    KjoennType hentKjønnType();

    String getFornavn();

    String getMellomnavn();

    String getEtternavn();

    String getSammensattNavn();

    List<Familiemedlem> getFamiliemedlemmer();

    LocalDate getFødselsdato();

    @Deprecated // Brukes bare til visning, som skal gå gjennom GraphQL
    Personstatus getPersonstatus();

    @Deprecated // Brukes bare til visning, som skal gå gjennom GraphQL
    LocalDate getStatsborgerskapDato();

    Bostedsadresse getBostedsadresse();

    UstrukturertAdresse getPostadresse();

    MidlertidigPostadresse getMidlertidigPostadresse();

    UstrukturertAdresse getGjeldendePostadresse();
}
