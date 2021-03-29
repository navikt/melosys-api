package no.nav.melosys.integrasjon.pdl.dto.person.adresse;

public record UtenlandskAdresseIFrittFormat(String adresselinje1,
                                            String adresselinje2,
                                            String adresselinje3,
                                            String postkode,
                                            String byEllerStedsnavn,
                                            String landkode) {
}
