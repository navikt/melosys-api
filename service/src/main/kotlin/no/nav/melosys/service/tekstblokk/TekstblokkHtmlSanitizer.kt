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
     * en utfylt placeholder (PlaceholderBlot i web) skrives tilbake til rå {nøkkel}, slik at én
     * behandlings verdier ikke blir liggende i det delte biblioteket, og rene markerings-spans
     * pakkes ut. Én parse dekker begge transformene.
     */
    private fun tilLagretForm(html: String): String {
        val dokument = Jsoup.parseBodyFragment(html)
        val spans = dokument.body().select("span")
        val utfylte = spans.filter { it.erUtfyltPlaceholder() }
        val markeringer = spans.filter { !it.erUtfyltPlaceholder() && it.classNames().any(MARKERINGSKLASSER::contains) }
        if (utfylte.isEmpty() && markeringer.isEmpty()) return html

        utfylte.forEach { it.replaceWith(TextNode("{${it.attr("data-placeholder").trim()}}")) }
        // Dokumentrekkefølge: ytterste span pakkes ut først, nøstede markeringer henger fortsatt med
        markeringer.forEach { it.unwrap() }
        return dokument.body().html()
    }

    private fun Element.erUtfyltPlaceholder(): Boolean = attr("data-placeholder").isNotBlank()

    private companion object {
        private val MARKERINGSKLASSER = setOf("placeholder-uerstattet", "placeholder-utfylt", "bracketed-text")
    }
}
