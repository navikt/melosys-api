package no.nav.melosys.domain;

import java.time.LocalDate;

import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;

public interface Organisasjon {
    String getOrgnummer();
    String getNavn();
    StrukturertAdresse getForretningsadresse();
    StrukturertAdresse getPostadresse();
    LocalDate getOppstartsdato();
    String getEnhetstype();
}
