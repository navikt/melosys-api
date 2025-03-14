package no.nav.melosys.integrasjon.pdl.dto.person;

import graphql.ExecutionInput;
import graphql.ParseAndValidate;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QueryTest {
    @Test
    void validerPerson() {
        assertThat(erGyldigQuery(Query.HENT_PERSON_QUERY)).isTrue();
    }

    @Test
    void validerPersonHistorikk() {
        assertThat(erGyldigQuery(Query.HENT_PERSON_HISTORIKK_QUERY)).isTrue();
    }

    @Test
    void validerAdressebeskyttelse() {
        assertThat(erGyldigQuery(Query.HENT_ADRESSEBESKYTTELSE_QUERY)).isTrue();
    }

    @Test
    void validerFamilierelasjoner() {
        assertThat(erGyldigQuery(Query.HENT_FAMILIERELASJONER_QUERY)).isTrue();
    }

    @Test
    void validerNavn() {
        assertThat(erGyldigQuery(Query.HENT_NAVN_QUERY)).isTrue();
    }

    @Test
    void validerRelatert() {
        assertThat(erGyldigQuery(Query.HENT_EKTEFELLE_ELLER_PARTNER_QUERY)).isTrue();
    }

    @Test
    void validerBarn() { assertThat(erGyldigQuery(Query.HENT_BARN_QUERY)).isTrue(); }

    @Test
    void validerForelder() {
        assertThat(erGyldigQuery(Query.HENT_FORELDER_QUERY)).isTrue();
    }

    @Test
    void validerStatsborgerskap() {
        assertThat(erGyldigQuery(Query.HENT_STATSBORGERSKAP_QUERY)).isTrue();
    }

    private boolean erGyldigQuery(String query) {
        Map<String, Object> variables = Map.of("ident", "42", "historikk", true);
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("pdl-schema.graphqls");
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(inputStream);
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(typeRegistry, RuntimeWiring.MOCKED_WIRING);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(query).variables(variables).build();
        return ParseAndValidate.parseAndValidate(schema, executionInput).getErrors().isEmpty();
    }
}
