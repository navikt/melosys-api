package no.nav.melosys.integrasjon.pdl.dto.person

import graphql.ExecutionInput
import graphql.ParseAndValidate
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.InputStream

class QueryTest {

    @Test
    fun `valider person`() {
        erGyldigQuery(Query.HENT_PERSON_QUERY) shouldBe true
    }

    @Test
    fun `valider person historikk`() {
        erGyldigQuery(Query.HENT_PERSON_HISTORIKK_QUERY) shouldBe true
    }

    @Test
    fun `valider adressebeskyttelse`() {
        erGyldigQuery(Query.HENT_ADRESSEBESKYTTELSE_QUERY) shouldBe true
    }

    @Test
    fun `valider familierelasjoner`() {
        erGyldigQuery(Query.HENT_FAMILIERELASJONER_QUERY) shouldBe true
    }

    @Test
    fun `valider navn`() {
        erGyldigQuery(Query.HENT_NAVN_QUERY) shouldBe true
    }

    @Test
    fun `valider relatert`() {
        erGyldigQuery(Query.HENT_EKTEFELLE_ELLER_PARTNER_QUERY) shouldBe true
    }

    @Test
    fun `valider barn`() {
        erGyldigQuery(Query.HENT_BARN_QUERY) shouldBe true
    }

    @Test
    fun `valider forelder`() {
        erGyldigQuery(Query.HENT_FORELDER_QUERY) shouldBe true
    }

    @Test
    fun `valider statsborgerskap`() {
        erGyldigQuery(Query.HENT_STATSBORGERSKAP_QUERY) shouldBe true
    }

    private fun erGyldigQuery(query: String): Boolean {
        val variables = mapOf("ident" to "42", "historikk" to true)
        val inputStream: InputStream = javaClass.classLoader.getResourceAsStream("pdl-schema.graphqls")!!
        val typeRegistry: TypeDefinitionRegistry = SchemaParser().parse(inputStream)
        val schema: GraphQLSchema = SchemaGenerator().makeExecutableSchema(typeRegistry, RuntimeWiring.MOCKED_WIRING)


        val executionInput = ExecutionInput.newExecutionInput().query(query).variables(variables).build()


        return ParseAndValidate.parseAndValidate(schema, executionInput).errors.isEmpty()
    }
}