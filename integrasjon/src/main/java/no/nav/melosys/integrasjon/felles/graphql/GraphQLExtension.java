package no.nav.melosys.integrasjon.felles.graphql;

public record GraphQLExtension(String code, String classification) {
    boolean hasCode() {
        return this.code != null;
    }

    public boolean hasCode(String code) {
        return hasCode() && this.code.equals(code);
    }
}
