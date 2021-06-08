package no.nav.melosys.tjenester.gui.graphql;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import graphql.Scalars;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.time.temporal.ChronoUnit.SECONDS;

@Configuration(proxyBeanMethods = false)
class ScalarConfig {
    @Bean
    static GraphQLScalarType dateScalar() {
        return GraphQLScalarType.newScalar()
            .name("Date")
            .description("Format: YYYY-MM-DD (ISO-8601), example: 2017-11-24")
            .coercing(dateCoercing())
            .build();
    }

    private static Coercing<LocalDate, String> dateCoercing() {
        return new Coercing<>() {
            @Override
            public String serialize(Object input) {
                if (input instanceof LocalDate) {
                    return input.toString();
                }
                throw new CoercingSerializeException(
                    "Serialization from " + input.getClass() + " to Date not implemented.");
            }

            @Override
            public LocalDate parseValue(Object input) {
                throw new CoercingSerializeException("Parsing from " + input.getClass() + " to Date not implemented.");
            }

            @Override
            public LocalDate parseLiteral(Object input) {
                if (input instanceof StringValue) {
                    try {
                        return LocalDate.parse(((StringValue) input).getValue());
                    } catch (DateTimeParseException e) {
                        throw new CoercingParseLiteralException(
                            "Value not a valid date. Provided value: " + input + ". Expected format: YYYY-MM-DD", e);
                    }
                }
                throw new CoercingParseLiteralException(
                    "Value not a valid date. Provided value: " + input + ". Expected format: YYYY-MM-DD");
            }
        };
    }

    @Bean
    static GraphQLScalarType dateTimeScalar() {
        return GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("Format: YYYY-MM-DDTHH:mm:SS (ISO-8601), example: 2011-12-03T10:15:30")
            .coercing(dateTimeCoercing())
            .build();
    }

    private static Coercing<LocalDateTime, String> dateTimeCoercing() {
        return new Coercing<>() {
            @Override
            public String serialize(Object input) {
                if (input instanceof LocalDateTime) {
                    return ((LocalDateTime) input).truncatedTo(SECONDS).toString();
                }
                throw new CoercingSerializeException(
                    "Serialization from " + input.getClass() + " to DateTime not implemented.");
            }

            @Override
            public LocalDateTime parseValue(Object input) {
                throw new CoercingSerializeException(
                    "Parsing from " + input.getClass() + " to DateTime not implemented.");
            }

            @Override
            public LocalDateTime parseLiteral(Object input) {
                throw new CoercingParseLiteralException(
                    "Parsing of literal " + input.getClass() + " to DateTime not implemented.");
            }
        };
    }

    @Bean
    static GraphQLScalarType GraphQLLong() {
        return Scalars.GraphQLLong;
    }
}
