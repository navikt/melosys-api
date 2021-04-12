package no.nav.melosys.integrasjon.pdl.dto.person.adresse;

public record UtenlandskAdresse(
    String adressenavnNummer,
    String bygningEtasjeLeilighet,
    String postboksNummerNavn,
    String postkode,
    String bySted,
    String regionDistriktOmraade,
    String landkode
) {
}
