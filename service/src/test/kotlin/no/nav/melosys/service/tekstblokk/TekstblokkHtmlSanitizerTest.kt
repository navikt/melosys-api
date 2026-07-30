package no.nav.melosys.service.tekstblokk

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

class TekstblokkHtmlSanitizerTest {

    private val sanitizer = TekstblokkHtmlSanitizer()

    @Test
    fun `beholder data-list slik at punktliste ikke blir nummerliste`() {
        // Quill 2 lagrer begge listetyper som <ol>; data-list er det eneste som skiller dem.
        val html = """<ol><li data-list="bullet">Første</li><li data-list="bullet">Andre</li></ol>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain """data-list="bullet""""
    }

    @Test
    fun `beholder data-list for nummerliste`() {
        val html = """<ol><li data-list="ordered">Første</li></ol>"""

        sanitizer.saniter(html) shouldContain """data-list="ordered""""
    }

    @Test
    fun `beholder class for innrykk paa listeelement`() {
        val html = """<ol><li data-list="bullet" class="ql-indent-1">Innrykket</li></ol>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "ql-indent-1"
        resultat shouldContain """data-list="bullet""""
    }

    @Test
    fun `utfylt placeholder lagres som raa noekkel`() {
        // Biblioteket er delt: behandlingens verdi skal aldri bli liggende i tekstblokken
        val html = """<p>Sak <span class="placeholder-utfylt" data-placeholder="saksnummer">2024/123456</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "{saksnummer}"
        resultat shouldNotContain "2024/123456"
        resultat shouldNotContain "data-placeholder"
    }

    @Test
    fun `beholder span med kun class`() {
        val html = """<p><span class="ql-cursor">Tekst</span></p>"""

        sanitizer.saniter(html) shouldContain """<span class="ql-cursor">Tekst</span>"""
    }

    @Test
    fun `fjerner script og andre tagger utenfor safelisten`() {
        val html = "<p>Tekst</p><script>alert('x')</script><iframe src='//example.com'></iframe>"

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "<p>Tekst</p>"
        resultat shouldNotContain "script"
        resultat shouldNotContain "iframe"
    }

    @Test
    fun `fjerner event-handlere og style`() {
        val html = """<p onclick="stjelData()" style="color:red">Tekst</p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldNotContain "onclick"
        resultat shouldNotContain "style"
    }

    @Test
    fun `beholder formatering fra Quill-toolbaren`() {
        // Jsoup pretty-printer output, så vi sammenligner tagg for tagg framfor hele strengen.
        val html = "<h2>Overskrift</h2><p><strong>fet</strong> <em>kursiv</em> <u>understreket</u></p>"

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "<h2>Overskrift</h2>"
        resultat shouldContain "<strong>fet</strong>"
        resultat shouldContain "<em>kursiv</em>"
        resultat shouldContain "<u>understreket</u>"
    }

    @Test
    fun `beholder tabell med colspan og rowspan`() {
        val html = """<table><tbody><tr><td colspan="2" rowspan="1">Celle</td></tr></tbody></table>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain """colspan="2""""
        resultat shouldContain """rowspan="1""""
    }

    @Test
    fun `null gir null`() {
        sanitizer.saniter(null) shouldBe null
    }
}
