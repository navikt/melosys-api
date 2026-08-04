package no.nav.melosys.service.tekstblokk

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.safety.Safelist
import org.springframework.stereotype.Component

/**
 * Sanitering av HTML lagret som en tekstblokk eller brevmal.
 * Skal ikke gjenbrukes for annen funksjonalitet – safelisten er tilpasset Quill-editoren
 * i Send brev og admin-siden for tekstblokker.
 */
@Component
class TekstblokkHtmlSanitizer {

    // Tillatte tagger matcher Quill-toolbarens output. Hold synkronisert med
    // src/felleskomponenter/htmlEditor/htmlEditor.tsx (formats-listen) i melosys-web.
    private val safelist: Safelist = Safelist()
        .addTags("p", "br", "strong", "em", "u", "h2", "ul", "ol", "li", "span", "table", "thead", "tbody", "tr", "th", "td")
        .addAttributes("span", "class")
        .addAttributes("p", "class")
        // Quill 2 lagrer både punkt- og nummerliste som <ol> og skiller dem via
        // <li data-list="bullet"> / <li data-list="ordered">. class brukes til innrykk
        // (ql-indent-N). Uten disse ville punktlister bli vist som nummerliste.
        .addAttributes("li", "data-list", "class")
        .addAttributes("ol", "class")
        .addAttributes("ul", "class")
        .addAttributes("th", "colspan", "rowspan")
        .addAttributes("td", "colspan", "rowspan")

    fun saniter(html: String?): String? = html?.let { Jsoup.clean(tilLagretForm(it), safelist) }

    /**
     * Placeholder-markeringene er rendering-artefakter som utledes ved visning og skal aldri lagres:
     * en utfylt placeholder (PlaceholderBlot i web) skrives tilbake til rå {nøkkel} og et gjort valg
     * (PlaceholderValgtBlot) til rå {velg:…}, slik at verken én behandlings verdier eller ett låst
     * alternativ blir liggende i det delte biblioteket. Rene markerings-spans pakkes ut.
     * Én parse dekker transformene.
     */
    private fun tilLagretForm(html: String): String {
        val dokument = Jsoup.parseBodyFragment(html)
        val spans = dokument.body().select("span")
        val tokener = spans.filter { it.erRåttToken() }
        val markeringer = spans.filter { !it.erRåttToken() && it.classNames().any(MARKERINGSKLASSER::contains) }
        if (tokener.isEmpty() && markeringer.isEmpty()) return html

        // Innholdet forkastes bevisst: den utfylte verdien ville persistert behandlingsspesifikke
        // persondata i det delte biblioteket, og det gjorte valget ville låst blokken til ett alternativ
        tokener.forEach { it.replaceWith(TextNode(it.råttToken())) }
        // Dokumentrekkefølge: ytterste span pakkes ut først, nøstede markeringer henger fortsatt med.
        // En utfylt span uten gyldig nøkkel havner her og beholder teksten sin: den er ikke skillbar
        // fra manuelt skrevet tekst, og å slette innhold admin ser i editoren ville vært stille datatap.
        markeringer.forEach { it.fjernMarkeringsklasser() }
        return dokument.body().html()
    }

    private fun Element.erRåttToken(): Boolean = erUtfyltPlaceholder() || erValgtPlaceholder()

    // Valgtokenet bygges av de filtrerte alternativene, ikke av rå-attributtet, så et normalisert
    // valg (data-valg="A||B|") lagres på samme form som web ville skrevet det: {velg:A|B}
    private fun Element.råttToken(): String =
        if (erUtfyltPlaceholder()) "{${attr("data-placeholder").trim()}}"
        else "{$VELG_PREFIKS${attr("data-valg").tilValgAlternativer().joinToString("|")}}"

    // Ugyldig nøkkel skrives ikke om – et misformet attributt ville gitt et korrupt token i lagret innhold
    private fun Element.erUtfyltPlaceholder(): Boolean = NØKKELMØNSTER.matches(attr("data-placeholder").trim())

    // Egen validering enn for nøkler: | og : hører hjemme i en alternativliste, men ikke i NØKKELMØNSTER
    private fun Element.erValgtPlaceholder(): Boolean =
        hasClass(VALGT_KLASSE) && attr("data-valg").tilValgAlternativer().isNotEmpty()

    // Speiler parseValgAlternativer i melosys-web src/services/modules/placeholdere.ts: blanke deler
    // filtreres bort før kravet om minst to alternativer, slik at et valg web regner som gyldig ikke
    // forsvinner stille ved lagring. Tom liste betyr ugyldig.
    private fun String.tilValgAlternativer(): List<String> =
        if (any(UGYLDIGE_VALGTEGN::contains)) emptyList()
        else split("|").map(String::trim).filter(String::isNotBlank).takeIf { it.size >= 2 }.orEmpty()

    /** Andre klasser kan bære formatering, så spanen beholdes hvis det er noen igjen. */
    private fun Element.fjernMarkeringsklasser() {
        val øvrige = classNames().filterNot(MARKERINGSKLASSER::contains)
        if (øvrige.isEmpty()) unwrap() else classNames(LinkedHashSet(øvrige))
    }

    private companion object {
        private const val VALGT_KLASSE = "placeholder-valgt"

        // Må speile placeholder-klassene i PLACEHOLDER_MARKERINGSKLASSER i melosys-web
        // src/services/modules/placeholdere.ts. Web-listen har i tillegg bracketed-text for
        // opprydding ved innsetting – den er master-editorens egen klasse og skal ikke fjernes
        // ved lagring her, ellers endres innhold som finnes i biblioteket fra før.
        // placeholder-valgt står her for fallbacken: uten gyldig data-valg er spanen ren markering.
        // placeholder-betingelse markerer {#hvis}/{/hvis}-tokenene og er rent boolsk: teksten er
        // selve tokenet og består ved unwrap, som de øvrige markeringene uten eget dataattributt.
        private val MARKERINGSKLASSER = setOf(
            "placeholder-uerstattet", "placeholder-ukjent", "placeholder-utfylt", "placeholder-valg",
            "placeholder-betingelse", VALGT_KLASSE,
        )

        // Konservativt mønster som dekker alle nøklene i PlaceholderRegister
        private val NØKKELMØNSTER = Regex("^[a-z0-9-]+\$")

        private const val VELG_PREFIKS = "velg:"
        private val UGYLDIGE_VALGTEGN = setOf('{', '}', '<', '>')
    }
}
