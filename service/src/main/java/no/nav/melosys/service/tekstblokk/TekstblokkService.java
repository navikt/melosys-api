package no.nav.melosys.service.tekstblokk;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.tekstblokk.Tekstblokk;
import no.nav.melosys.domain.tekstblokk.TekstblokkType;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.tekstblokk.TekstblokkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TekstblokkService {

    public record Input(String tittel, String innhold, TekstblokkType type, Collection<String> tags) {}

    private final TekstblokkRepository tekstblokkRepository;
    private final TekstblokkHtmlSanitizer htmlSanitizer;

    public TekstblokkService(TekstblokkRepository tekstblokkRepository, TekstblokkHtmlSanitizer htmlSanitizer) {
        this.tekstblokkRepository = tekstblokkRepository;
        this.htmlSanitizer = htmlSanitizer;
    }

    @Transactional(readOnly = true)
    public List<Tekstblokk> hentAlle(TekstblokkType type) {
        if (type == null) {
            return tekstblokkRepository.findAllByOrderByTittelAsc();
        }
        return tekstblokkRepository.findAllByTypeOrderByTittelAsc(type);
    }

    @Transactional(readOnly = true)
    public Tekstblokk hent(Long id) {
        return tekstblokkRepository.findById(id)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke tekstblokk med id " + id));
    }

    @Transactional
    public Tekstblokk opprett(Input input) {
        Tekstblokk tekstblokk = new Tekstblokk();
        populerFraInput(tekstblokk, input);
        return tekstblokkRepository.save(tekstblokk);
    }

    @Transactional
    public Tekstblokk oppdater(Long id, Input input) {
        Tekstblokk tekstblokk = hent(id);
        populerFraInput(tekstblokk, input);
        return tekstblokkRepository.save(tekstblokk);
    }

    @Transactional
    public void slett(Long id) {
        tekstblokkRepository.delete(hent(id));
    }

    private void populerFraInput(Tekstblokk tekstblokk, Input input) {
        tekstblokk.setTittel(input.tittel().trim());
        tekstblokk.setInnhold(htmlSanitizer.saniter(input.innhold()));
        tekstblokk.setType(input.type());
        tekstblokk.setTags(normaliserTags(input.tags()));
    }

    private Set<String> normaliserTags(Collection<String> tags) {
        if (tags == null) {
            return new HashSet<>();
        }
        return tags.stream()
            .filter(t -> t != null && !t.trim().isEmpty())
            .map(t -> t.trim().toLowerCase(Locale.ROOT))
            .filter(t -> t.length() <= 60)
            .collect(Collectors.toSet());
    }
}
