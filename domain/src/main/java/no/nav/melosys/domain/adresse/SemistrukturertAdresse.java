package no.nav.melosys.domain.adresse;

public record SemistrukturertAdresse(
    String adresselinje1,
    String adresselinje2,
    String adresselinje3,
    String adresselinje4,
    String postnr,
    String poststed,
    String landkode
) {
}
