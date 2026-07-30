package no.nav.melosys.service.tekstblokk

import org.jsoup.Jsoup
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

    fun saniter(html: String?): String? = html?.let { Jsoup.clean(tilRåPlaceholder(it), safelist) }

    /**
     * En utfylt placeholder (PlaceholderBlot i web) skrives tilbake til rå {nøkkel} før lagring.
     * Slik blir aldri én behandlings verdier liggende i det delte biblioteket, og neste
     * innsetting resolver verdien på nytt.
     */
    private fun tilRåPlaceholder(html: String): String {
        val dokument = Jsoup.parseBodyFragment(html)
        val utfylte = dokument.body().select("span[data-placeholder]")
            .filter { it.attr("data-placeholder").isNotBlank() }
        if (utfylte.isEmpty()) return html

        utfylte.forEach { it.replaceWith(TextNode("{${it.attr("data-placeholder").trim()}}")) }
        return dokument.body().html()
    }
}
