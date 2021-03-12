package no.nav.melosys.integrasjon.felles.graphql;

import java.util.List;

public record GraphQLResponse<T>(T data, List<GraphQLError> errorList) {
}
