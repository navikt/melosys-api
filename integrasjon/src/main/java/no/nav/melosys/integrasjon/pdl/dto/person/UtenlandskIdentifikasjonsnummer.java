package no.nav.melosys.integrasjon.pdl.dto.person;

public record UtenlandskIdentifikasjonsnummer(String identifikasjonsnummer,
                                              String utstederland,
                                              boolean opphoert) {
}
