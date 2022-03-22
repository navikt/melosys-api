package no.nav.melosys.integrasjon.pdl.dto.person;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import graphql.ExecutionInput;
import graphql.GraphQLError;
import graphql.ParseAndValidate;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryTest {
    @Test
    void validerPerson() {
        assertThat(validerQuery(Query.HENT_PERSON_QUERY)).isEmpty();
    }

    @Test
    void validerPersonHistorikk() {
        assertThat(validerQuery(Query.HENT_PERSON_HISTORIKK_QUERY)).isEmpty();
    }

    @Test
    void validerAdressebeskyttelse() {
        assertThat(validerQuery(Query.HENT_ADRESSEBESKYTTELSE_QUERY)).isEmpty();
    }

    @Test
    void validerFamilierelasjoner() {
        assertThat(validerQuery(Query.HENT_FAMILIERELASJONER_QUERY)).isEmpty();
    }

    @Test
    void validerNavn() {
        assertThat(validerQuery(Query.HENT_NAVN_QUERY)).isEmpty();
    }

    @Test
    void validerRelatert() {
        assertThat(validerQuery(Query.HENT_RELATERT_VED_SIVILSTAND_QUERY)).isEmpty();
    }

    @Test
    void validerBarn() {
        assertThat(validerQuery(Query.HENT_BARN_QUERY)).isEmpty();
    }

    @Test
    void validerForelder() {
        assertThat(validerQuery(Query.HENT_FORELDER_QUERY)).isEmpty();
    }

    @Test
    void validerStatsborgerskap() {
        assertThat(validerQuery(Query.HENT_STATSBORGERSKAP_QUERY)).isEmpty();
    }

    private List<GraphQLError> validerQuery(String query) {
        Map<String, Object> variables = Map.of("ident", "42", "historikk", true);
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("pdl-schema.graphqls");
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(inputStream);
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(typeRegistry, RuntimeWiring.MOCKED_WIRING);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(query).variables(variables).build();
        return ParseAndValidate.parseAndValidate(schema, executionInput).getErrors();
    }
}
