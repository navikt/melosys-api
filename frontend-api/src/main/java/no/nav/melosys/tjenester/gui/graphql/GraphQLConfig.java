package no.nav.melosys.tjenester.gui.graphql;

import java.io.InputStream;
import java.util.Collection;

import graphql.GraphQL;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class GraphQLConfig {
    @Bean
    static GraphQL graphQL(Collection<GraphQLScalarType> scalars, Collection<TypeRuntimeWiring> wirings) {
        InputStream schemaStream = GraphQLConfig.class
            .getClassLoader().getResourceAsStream("saksopplysninger.graphqls");
        var typeDefinitionRegistry = new SchemaParser().parse(schemaStream);

        RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();
        scalars.forEach(runtimeWiringBuilder::scalar);
        wirings.forEach(runtimeWiringBuilder::type);

        GraphQLSchema graphQLSchema = new SchemaGenerator()
            .makeExecutableSchema(typeDefinitionRegistry, runtimeWiringBuilder.build());

        return GraphQL.newGraphQL(graphQLSchema).build();
    }

    @Bean
    static TypeRuntimeWiring saksopplysningerWiring(SaksopplysningerDataFetcher saksopplysningerDataFetcher) {
        return TypeRuntimeWiring.newTypeWiring("Query")
            .dataFetcher("hentSaksopplysninger", saksopplysningerDataFetcher)
            .build();
    }

    @Bean
    static TypeRuntimeWiring personopplysningerWiring(PersonopplysningerDataFetcher personopplysningerDataFetcher) {
        return TypeRuntimeWiring.newTypeWiring("Saksopplysninger")
            .dataFetcher("persondata", personopplysningerDataFetcher)
            .build();
    }

    @Bean
    static TypeRuntimeWiring familiemedlemmerWiring(FamiliemedlemmerDataFetcher familiemedlemmerDataFetcher) {
        return TypeRuntimeWiring.newTypeWiring("Personopplysninger")
            .dataFetcher("familiemedlemmer", familiemedlemmerDataFetcher)
            .build();
    }
}
