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
    Optional<Familiemedlem> hentAnnenForelder(String fnrGjeldendeForelder);

    boolean harBeskyttelsesbehov();

    boolean harIkkeRegistrertAdresse();

    boolean manglerBostedsadresse();

    String hentFolkeregisterIdent();

    @Deprecated // Brukes bare til visning, som skal gå gjennom GraphQL
    Sivilstand getSivilstand();

    @Deprecated // Brukes bare til visning, som skal gå gjennom GraphQL
    LocalDate getSivilstandGyldighetsperiodeFom();

    @Deprecated // Flere statsborgerskap må støttes
    Land getStatsborgerskap();

    Set<Land> hentAlleStatsborgerskap();

    KjoennsType getKjønn();

    String getFornavn();

    String getMellomnavn();

    String getEtternavn();

    String getSammensattNavn();

    List<Familiemedlem> getFamiliemedlemmer();

    LocalDate getFødselsdato();

    LocalDate getDødsdato();

    Diskresjonskode getDiskresjonskode();

    Personstatus getPersonstatus();

    LocalDate getStatsborgerskapDato();

    Bostedsadresse getBostedsadresse();

    UstrukturertAdresse getPostadresse();

    MidlertidigPostadresse getMidlertidigPostadresse();

    UstrukturertAdresse getGjeldendePostadresse();
}
