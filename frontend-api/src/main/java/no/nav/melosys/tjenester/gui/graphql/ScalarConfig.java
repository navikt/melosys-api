package no.nav.melosys.tjenester.gui.graphql;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.time.temporal.ChronoUnit.SECONDS;

@Configuration(proxyBeanMethods = false)
class ScalarConfig {

    private static final Logger log = LoggerFactory.getLogger(ScalarConfig.class);

    @Bean
    public GraphQLScalarType dateScalar() {
        return GraphQLScalarType.newScalar()
            .name("Date")
            .description("Format: YYYY-MM-DD (ISO-8601), example: 2017-11-24")
            .coercing(dateCoercing())
            .build();
    }

    static Coercing<LocalDate, String> dateCoercing() {
        return new Coercing<>() {
            @Override
            public String serialize(Object input) {
                if (input instanceof LocalDate) {
                    return input.toString();
                }

                if (input instanceof String && harGyldigDatoFormat(input)) {
                    return input.toString();
                }

                throw new CoercingSerializeException(
                    "GraphQL serialization from " + input.getClass() + " with value " + input + " to Date scalar not" +
                        " implemented.");
            }

            private boolean harGyldigDatoFormat(Object input) {
                try {
                    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    formatter.setLenient(false);
                    var date = formatter.parse(input.toString());
                    return true;
                } catch (Exception e) {
                    log.warn("Har mottatt en String input med ugyldig GraphQL Date scalar format: {}",
                        input.toString());
                    return false;
                }
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
                    "Graphql serialization from " + input.getClass() + " to DateTime scalar not implemented.");
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
    static GraphQLScalarType longScalar() {
        return GraphQLScalarType.newScalar()
            .name("Long")
            .description("Custom scalar for Long")
            .coercing(longCoercing())
            .build();
    }

    private static Coercing<Long, String> longCoercing() {
        return new Coercing<>() {
            @Override
            public String serialize(Object input) throws CoercingSerializeException {
                if (input instanceof Long l) {
                    return l.toString();
                } else if (input instanceof String s) {
                    return s;
                } else {
                    return null;
                }
            }

            @Override
            public Long parseValue(Object input) throws CoercingParseValueException {
                if (input instanceof Long l) {
                    return l;
                } else if (input instanceof Integer i) {
                    return i.longValue();
                } else if (input instanceof String s) {
                    return Long.parseLong(s);
                } else {
                    return null;
                }
            }

            @Override
            public Long parseLiteral(Object input) throws CoercingParseLiteralException {
                if (input instanceof StringValue stringValue) {
                    try {
                        return Long.parseLong(stringValue.getValue());
                    } catch (NumberFormatException e) {
                        throw new CoercingParseLiteralException(
                            "Expected value to be a Long but it was '" + input + "'"
                        );
                    }
                } else if (input instanceof IntValue intValue) {
                    BigInteger value = intValue.getValue();
                    if (value.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0 || value.compareTo(
                        BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                        throw new CoercingParseLiteralException(
                            "Expected value to be in the Long range but it was '" + value + "'"
                        );
                    }
                    return value.longValue();
                }
                throw new CoercingParseLiteralException(
                    "Expected AST type 'IntValue' or 'StringValue' but was '" + input.getClass().getSimpleName() + "'."
                );
            }
        };
    }

}
