package no.nav.melosys.tjenester.gui.graphql.dto;

public record SemistrukturertAdresseformatDto(
    String adresselinje1,
    String adresselinje2,
    String adresselinje3,
    String adresselinje4,
    String postnummer,
    String poststed,
    String land
) {
}
