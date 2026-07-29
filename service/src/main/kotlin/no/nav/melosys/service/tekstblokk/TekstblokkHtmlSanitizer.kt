package no.nav.melosys.service.tekstblokk

import org.jsoup.Jsoup
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

    fun saniter(html: String?): String? = html?.let { Jsoup.clean(it, safelist) }
}
