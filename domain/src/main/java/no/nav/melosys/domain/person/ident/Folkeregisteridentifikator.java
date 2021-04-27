package no.nav.melosys.domain.person.ident;

public record Folkeregisteridentifikator(String identifikasjonsnummer, Type type) implements Identifikator {
    public enum Type {
        DNR, FNR
    }

    @Override
    public String toString() {
        return "Folkeregisteridentifikator{" + "type=" + type + '}';
    }
}
