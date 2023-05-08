package no.nav.melosys.service.oppgave.migrering

import org.junit.jupiter.api.Test
import java.io.File


class DslTest {

    @Test
    fun test() {
        val text = document {
            table {
                th { item(fc = "red", bc= "#eee0b3") { "header 1" }; item(font = Font.STRONG) { "header 2" }; item { "header 3" } }

                (0..10).forEach {
                    tr { item(fc = "brown", bc = "#fff0b3") { it }; item(font = Font.ITALIC) { it }; item { it } }
                }

                tr { item(fc = "blue") { "nede 1" }; item { "nede 2" }; item { "nede 2" } }
            }
        }.export(HtmlExporter(true))

        println(text)
        File("/Users/rune/div/test-dsl.html").writeText(text)
    }
}
