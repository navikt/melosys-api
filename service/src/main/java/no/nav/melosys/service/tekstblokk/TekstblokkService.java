package no.nav.melosys.service.tekstblokk;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.tekstblokk.Tekstblokk;
import no.nav.melosys.domain.tekstblokk.TekstblokkType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.tekstblokk.TekstblokkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TekstblokkService {

    private final TekstblokkRepository tekstblokkRepository;
    private final HtmlSanitizer htmlSanitizer;

    public TekstblokkService(TekstblokkRepository tekstblokkRepository, HtmlSanitizer htmlSanitizer) {
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
    public Tekstblokk opprett(String tittel, String innhold, TekstblokkType type, Set<String> tags) {
        validerTittel(tittel);
        validerInnhold(innhold);

        Tekstblokk tekstblokk = new Tekstblokk();
        tekstblokk.setTittel(tittel.trim());
        tekstblokk.setInnhold(htmlSanitizer.saniter(innhold));
        tekstblokk.setType(type);
        tekstblokk.setTags(normaliserTags(tags));
        return tekstblokkRepository.save(tekstblokk);
    }

    @Transactional
    public Tekstblokk oppdater(Long id, String tittel, String innhold, TekstblokkType type, Set<String> tags) {
        validerTittel(tittel);
        validerInnhold(innhold);

        Tekstblokk tekstblokk = hent(id);
        tekstblokk.setTittel(tittel.trim());
        tekstblokk.setInnhold(htmlSanitizer.saniter(innhold));
        tekstblokk.setType(type);
        tekstblokk.setTags(normaliserTags(tags));
        return tekstblokkRepository.save(tekstblokk);
    }

    @Transactional
    public void slett(Long id) {
        Tekstblokk tekstblokk = hent(id);
        tekstblokkRepository.delete(tekstblokk);
    }

    private void validerTittel(String tittel) {
        if (tittel == null || tittel.trim().isEmpty()) {
            throw new FunksjonellException("Tittel kan ikke være tom");
        }
        if (tittel.length() > 200) {
            throw new FunksjonellException("Tittel kan ikke være lengre enn 200 tegn");
        }
    }

    private void validerInnhold(String innhold) {
        if (innhold == null || innhold.trim().isEmpty()) {
            throw new FunksjonellException("Innhold kan ikke være tomt");
        }
    }

    private Set<String> normaliserTags(Set<String> tags) {
        if (tags == null) {
            return new HashSet<>();
        }
        return tags.stream()
            .filter(t -> t != null && !t.trim().isEmpty())
            .map(t -> t.trim().toLowerCase())
            .filter(t -> t.length() <= 60)
            .collect(Collectors.toSet());
    }
}
