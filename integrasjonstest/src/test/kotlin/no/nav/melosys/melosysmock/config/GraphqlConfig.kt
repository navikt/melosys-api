package no.nav.melosys.melosysmock.config

import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import org.joda.time.DateTime
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate
import java.time.LocalDateTime

@TestConfiguration
class GraphqlConfig {
    @Bean
    fun extendedScalarsLocalDateTime(): GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("LocalDateTime")
        .description("Custom scalar for LocalDateTime")
        .coercing(
            object : Coercing<LocalDateTime?, String?> {

                override fun serialize(dataFetcherResult: Any): String? =
                    when (dataFetcherResult) {
                        is LocalDateTime -> dataFetcherResult.toString()
                        is String -> dataFetcherResult
                        else -> null
                    }

                override fun parseValue(input: Any): LocalDateTime = parse(input)

                override fun parseLiteral(input: Any): LocalDateTime = parse(input)

                private fun parse(input: Any?): LocalDateTime = when (input) {
                    is LocalDateTime -> input
                    is String -> LocalDateTime.parse(input)
                    else -> LocalDateTime.now()
                }
            }
        ).build()

    @Bean
    fun extendedScalarsDateTime(): GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("DateTime")
        .description("Custom scalar for DateTime")
        .coercing(
            object : Coercing<DateTime?, String?> {

                override fun serialize(dataFetcherResult: Any): String? =
                    when (dataFetcherResult) {
                        is LocalDateTime -> dataFetcherResult.toString()
                        is String -> dataFetcherResult
                        else -> null
                    }

                override fun parseValue(input: Any): DateTime = parse(input)

                override fun parseLiteral(input: Any): DateTime = parse(input)

                private fun parse(input: Any?): DateTime = when (input) {
                    is DateTime -> input
                    is String -> DateTime.parse(input)
                    else -> DateTime.now()
                }
            }
        ).build()

    @Bean
    fun extendedScalarsDate(): GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("Date")
        .description("Custom scalar for Date")
        .coercing(
            object : Coercing<LocalDate?, String?> {
                override fun serialize(input: Any): String? =
                    when (input) {
                        is LocalDate -> input.toString()
                        is String -> input
                        else -> null
                    }

                override fun parseValue(input: Any): LocalDate = parse(input)

                override fun parseLiteral(input: Any): LocalDate = parse(input)

                private fun parse(input: Any?) = when (input) {
                    is LocalDate -> input
                    is String -> LocalDate.parse(input)
                    else -> LocalDate.now()
                }
            }
        ).build()

    @Bean
    fun extendedScalarsLong(): GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("Long")
        .description("Custom scalar for Long")
        .coercing(
            object : Coercing<Long?, String?> {
                override fun serialize(input: Any): String =
                    when (input) {
                        is Long -> input.toString()
                        is String -> input
                        else -> input.toString()
                    }

                override fun parseValue(input: Any): Long = parse(input)

                override fun parseLiteral(input: Any): Long = parse(input)

                private fun parse(input: Any?) = when (input) {
                    is Long -> input
                    is String -> input.toLong()
                    else -> 1
                }
            }
        ).build()
}
