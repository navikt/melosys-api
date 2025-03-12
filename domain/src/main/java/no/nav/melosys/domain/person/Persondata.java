package no.nav.melosys.domain.person;

import no.nav.melosys.domain.brev.Postadresse;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import no.nav.melosys.domain.person.familie.Familiemedlem;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

public interface Persondata extends SaksopplysningDokument {
    boolean erPersonDød();

    boolean harStrengtAdressebeskyttelse();

    boolean manglerGyldigRegistrertAdresse();

    String hentFolkeregisterident();

    Set<Land> hentAlleStatsborgerskap();

    KjoennType hentKjønnType();

    String getFornavn();

    String getMellomnavn();

    String getEtternavn();

    String getSammensattNavn();

    Set<Familiemedlem> hentFamiliemedlemmer();

    LocalDate hentFødselsdato();

    Optional<Bostedsadresse> finnBostedsadresse();

    Optional<Kontaktadresse> finnKontaktadresse();

    Optional<Oppholdsadresse> finnOppholdsadresse();

    @Nullable
    Postadresse hentGjeldendePostadresse();
}
