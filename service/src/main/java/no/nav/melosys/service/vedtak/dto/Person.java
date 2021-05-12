package no.nav.melosys.service.vedtak.dto;

import java.util.List;

import no.nav.melosys.integrasjon.pdl.dto.person.Navn;
import no.nav.melosys.integrasjon.pdl.dto.person.Sivilstand;
import no.nav.melosys.integrasjon.pdl.dto.person.Statsborgerskap;

public record Person(String ident, Navn navn, Statsborgerskap statsborgerskap, Sivilstand sivilstand, List<Adresse> adresser) {
}
