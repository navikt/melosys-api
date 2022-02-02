package no.nav.melosys.tjenester.gui.graphql.dto;

public record StrukturertAdresseformatDto(
    String tilleggsnavn,
    String gatenavn,
    String husnummerEtasjeLeilighet,
    String postboks,
    String postnummer,
    String poststed,
    String region,
    String land
) {
}
