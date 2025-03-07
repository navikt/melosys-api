package no.nav.melosys.domain.person;

import java.util.Collection;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;

public record PersonMedHistorikk(
    Collection<Bostedsadresse> bostedsadresser,
    Doedsfall dødsfall,
    Foedselsdato fødselsdato,
    Folkeregisteridentifikator folkeregisteridentifikator,
    Collection<Folkeregisterpersonstatus> folkeregisterpersonstatuser,
    KjoennType kjønn,
    Collection<Kontaktadresse> kontaktadresser,
    Navn navn,
    Collection<Oppholdsadresse> oppholdsadresser,
    Collection<Sivilstand> sivilstand,
    Collection<Statsborgerskap> statsborgerskap) implements SaksopplysningDokument {
}
