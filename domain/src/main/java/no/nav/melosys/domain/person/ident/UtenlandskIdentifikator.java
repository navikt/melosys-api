package no.nav.melosys.domain.person.ident;

public record UtenlandskIdentifikator(String identifikasjonsnummer, String utstederland, String type)
    implements Identifikator {

    @Override
    public String toString() {
        return "UtenlandskIdentifikator{" + "utstederland='" + utstederland + '\'' + ", type='" + type + '\'' + '}';
    }
}
