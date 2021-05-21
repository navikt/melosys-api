package no.nav.melosys.integrasjon.felles.graphql;

public record GraphQLError(String message, GraphQLExtension extensions) {
    public boolean hasExtension() {
        return extensions != null;
    }
}
