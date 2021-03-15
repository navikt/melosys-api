package no.nav.melosys.integrasjon.felles.graphql;

import java.util.Map;

public record GraphQLRequest(String query, Map<String, Object> variables) {
}
