package no.nav.melosys.service.tekstblokk

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jsoup.Jsoup
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
        val html = """<p>Sak <span class="placeholder-utfylt" data-placeholder="saksnummer">MEL-12345</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "{saksnummer}"
        resultat shouldNotContain "MEL-12345"
        resultat shouldNotContain "data-placeholder"
    }

    @Test
    fun `uerstattet placeholder-markering pakkes ut ved lagring`() {
        // Markeringene utledes ved visning og skal aldri bli persistert
        val html = """<p>Sak <span class="placeholder-uerstattet">{saksnummer}</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "<p>Sak {saksnummer}</p>"
        resultat shouldNotContain "span"
    }

    @Test
    fun `ukjent placeholder-markering pakkes ut ved lagring`() {
        val html = """<p>Sak <span class="placeholder-ukjent">{tullenokkel}</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "<p>Sak {tullenokkel}</p>"
        resultat shouldNotContain "span"
    }

    @Test
    fun `betingelsesmarkering pakkes ut, men tokenet bestaar`() {
        val html = """<p><span class="placeholder-betingelse">{#hvis innvilgelse}</span></p>""" +
            """<p>Vedtaket er innvilget.</p><p><span class="placeholder-betingelse">{/hvis}</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "<p>{#hvis innvilgelse}</p>"
        resultat shouldContain "<p>{/hvis}</p>"
        resultat shouldNotContain "span"
    }

    @Test
    fun `markeringsklasse fjernes, men oevrige klasser paa spanen beholdes`() {
        val html = """<p><span class="placeholder-uerstattet annen-klasse">x</span></p>"""

        sanitizer.saniter(html) shouldContain """<span class="annen-klasse">x</span>"""
    }

    @Test
    fun `misformet placeholder-noekkel skrives ikke om, men markeringen pakkes ut`() {
        // Et misformet attributt ville ellers gitt et korrupt token i lagret innhold
        val html = """<p><span class="placeholder-utfylt" data-placeholder="saksnummer}">MEL-12345</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "<p>MEL-12345</p>"
        resultat shouldNotContain "{"
        resultat shouldNotContain "span"
    }

    @Test
    fun `placeholder-noekkel med mellomrom skrives ikke om`() {
        val html = """<p><span class="placeholder-utfylt" data-placeholder="saks nummer">MEL-12345</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "<p>MEL-12345</p>"
        resultat shouldNotContain "{"
        resultat shouldNotContain "span"
    }

    @Test
    fun `gjort valg lagres som raatt valgtoken`() {
        // Et lagret valg ville låst malen til ett alternativ for alle som gjenbruker blokken
        val html = """<p>Land: <span class="placeholder-valgt" data-valg="Bosnia-Hercegovina|Montenegro|Serbia">Montenegro</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "{velg:Bosnia-Hercegovina|Montenegro|Serbia}"
        resultat shouldNotContain "span"
        resultat shouldNotContain "data-valg"
    }

    @Test
    fun `uvalgt valgtoken-markering pakkes ut ved lagring`() {
        val html = """<p>Land: <span class="placeholder-valg">{velg:Norge|Sverige}</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "<p>Land: {velg:Norge|Sverige}</p>"
        resultat shouldNotContain "span"
    }

    @Test
    fun `tomt data-valg skrives ikke om, men markeringen pakkes ut`() {
        val html = """<p><span class="placeholder-valgt" data-valg="">Montenegro</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "<p>Montenegro</p>"
        resultat shouldNotContain "{"
        resultat shouldNotContain "span"
    }

    @Test
    fun `data-valg med klammer skrives ikke om`() {
        // Ville ellers gitt et korrupt token i lagret innhold
        val html = """<p><span class="placeholder-valgt" data-valg="A}|B">A</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "<p>A</p>"
        resultat shouldNotContain "{"
        resultat shouldNotContain "span"
    }

    @Test
    fun `data-valg med vinkelparenteser skrives ikke om`() {
        // Ville ellers gitt et token som blir tolket som markup ved visning
        val html = """<p><span class="placeholder-valgt" data-valg="&lt;A&gt;|B">A</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "<p>A</p>"
        resultat shouldNotContain "{"
        resultat shouldNotContain "data-valg"
    }

    @Test
    fun `escapede tegn i data-valg round-tripper til valgtoken som ren tekst`() {
        val html = """<p><span class="placeholder-valgt" data-valg="A &amp; B|C">A &amp; B</span></p>"""

        val resultat = sanitizer.saniter(html)

        // Tokenet skal være tekst, ikke markup, så vi sammenligner på dekodet tekstinnhold
        Jsoup.parse(resultat!!).text() shouldBe "{velg:A & B|C}"
        resultat shouldNotContain "data-valg"
    }

    @Test
    fun `blanke alternativer filtreres bort slik web gjoer`() {
        // Web (parseValgAlternativer) godtar dette valget, og da skal det ikke forsvinne ved lagring
        val html = """<p><span class="placeholder-valgt" data-valg="A||B|">A</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "{velg:A|B}"
        resultat shouldNotContain "data-valg"
    }

    @Test
    fun `duplikater slaas sammen slik web gjoer`() {
        // Web dedupliserer før tokravet, så data-valg="A|A|B" er gyldig og round-tripper som {velg:A|B}
        val html = """<p><span class="placeholder-valgt" data-valg="A|A|B">A</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "{velg:A|B}"
        resultat shouldNotContain "data-valg"
    }

    @Test
    fun `data-valg med bare duplikater er ugyldig og pakkes ut som tekst`() {
        // Web ville deduplisert til ett alternativ og avvist – da må ikke lagringen skrive
        // et token web etterpå rødmarkerer som ukjent nøkkel
        val html = """<p><span class="placeholder-valgt" data-valg="A|A">A</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldNotContain "{velg:"
        resultat shouldContain "A"
        resultat shouldNotContain "data-valg"
    }

    @Test
    fun `valgt placeholder inni bracketed-text gir raatt token og beholder klamme-spanen`() {
        val html =
            """<p><span class="bracketed-text"><span class="placeholder-valgt" data-valg="Norge|Sverige">Norge</span></span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain """<span class="bracketed-text">{velg:Norge|Sverige}</span>"""
        resultat shouldNotContain "data-valg"
    }

    @Test
    fun `placeholder-valgt uten data-valg mister klassen, men beholder teksten`() {
        val html = """<p><span class="placeholder-valgt">Montenegro</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "<p>Montenegro</p>"
        resultat shouldNotContain "placeholder-valgt"
        resultat shouldNotContain "{"
    }

    @Test
    fun `data-valg med bare ett alternativ skrives ikke om`() {
        val html = """<p><span class="placeholder-valgt" data-valg="Norge">Norge</span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain "<p>Norge</p>"
        resultat shouldNotContain "{"
        resultat shouldNotContain "span"
    }

    @Test
    fun `bracketed-text beholdes ved lagring`() {
        // Klassen kommer fra dagens editor og ligger allerede lagret i biblioteket: å pakke den ut
        // ville endret eksisterende innhold og fjernet klamme-spans forhåndsvisningen trenger
        val html = """<p><span class="bracketed-text"><span class="bracketed-text">[dato]</span></span></p>"""

        val resultat = sanitizer.saniter(html)

        resultat shouldContain """<span class="bracketed-text">"""
        resultat shouldContain "[dato]"
    }

    @Test
    fun `beholder span med kun class`() {
        val html = """<p><span class="ql-indent-1">Tekst</span></p>"""

        sanitizer.saniter(html) shouldContain """<span class="ql-indent-1">Tekst</span>"""
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
