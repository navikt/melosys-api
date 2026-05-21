package no.nav.melosys.service.tekstblokk;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Sanitering av HTML lagret som en tekstblokk eller brevmal.
 * Skal ikke gjenbrukes for annen funksjonalitet – safelisten er tilpasset Quill-editoren
 * i Send brev og admin-siden for tekstblokker.
 */
@Component
public class TekstblokkHtmlSanitizer {

    // Tillatte tagger matcher Quill-toolbarens output. Hold synkronisert med
    // src/felleskomponenter/htmlEditor/htmlEditor.tsx (formats-listen) i melosys-web.
    private static final Safelist SAFELIST = new Safelist()
        .addTags("p", "br", "strong", "em", "u", "h2", "ul", "ol", "li", "span", "table", "thead", "tbody", "tr", "th", "td")
        .addAttributes("span", "class")
        .addAttributes("p", "class")
        .addAttributes("th", "colspan", "rowspan")
        .addAttributes("td", "colspan", "rowspan");

    public String saniter(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, SAFELIST);
    }
}
