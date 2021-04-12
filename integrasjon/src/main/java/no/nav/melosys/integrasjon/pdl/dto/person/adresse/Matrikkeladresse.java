package no.nav.melosys.integrasjon.pdl.dto.person.adresse;

public record Matrikkeladresse(
    String bruksenhetsnummer,
    String tilleggsnavn,
    String kommunenummer,
    String postnummer
) {
}
