package no.nav.melosys.tjenester.gui.graphql.dto;

public record StrukturertAdresseformatDto(
    String tilleggsavn,
    String gatenavn,
    String husnummeEtasjeLeilighet,
    String postboks,
    String postnummr,
    String poststed,
    String region,
    String landkode
) {
}
