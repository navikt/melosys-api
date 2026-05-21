package no.nav.melosys.service.tekstblokk;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Saniterer HTML levert av Quill-editoren. Tillater de samme formatene som editoren
 * eksponerer i toolbaren, og fjerner alle script-, style- og event-attributter.
 */
@Component
public class HtmlSanitizer {

    private final Safelist safelist;

    public HtmlSanitizer() {
        this.safelist = new Safelist()
            .addTags("p", "br", "strong", "em", "u", "h2", "ul", "ol", "li", "span", "table", "thead", "tbody", "tr", "th", "td")
            .addAttributes("span", "class")
            .addAttributes("p", "class")
            .addAttributes("th", "colspan", "rowspan")
            .addAttributes("td", "colspan", "rowspan");
    }

    public String saniter(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, safelist);
    }
}
