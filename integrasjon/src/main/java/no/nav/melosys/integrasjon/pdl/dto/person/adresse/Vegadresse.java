package no.nav.melosys.integrasjon.pdl.dto.person.adresse;

public record Vegadresse(
    String adressenavn,
    String husnummer,
    String husbokstav,
    String tilleggsnavn,
    String postnummer
) {
}
