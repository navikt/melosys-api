package no.nav.melosys.service.aktoer;

import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.KontaktopplysningID;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.KontaktopplysningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

@Service
public class KontaktopplysningService {
    private final KontaktopplysningRepository kontaktopplysningRepository;

    @Autowired
    public KontaktopplysningService(KontaktopplysningRepository kontaktopplysningRepository) {
        this.kontaktopplysningRepository = kontaktopplysningRepository;
    }

    public java.util.Optional<Kontaktopplysning> hentKontaktopplysning(String saksnummer, String orgnr) {
        return kontaktopplysningRepository.findById(new KontaktopplysningID(saksnummer, orgnr));
    }

    public Kontaktopplysning lagEllerOppdaterKontaktopplysning(String saksnummer, String orgnr, String kontaktOrgnr, String kontaktNavn) {
        Kontaktopplysning kontaktopplysning = kontaktopplysningRepository.findById(new KontaktopplysningID(saksnummer, orgnr))
            .orElseGet(() -> {
                Kontaktopplysning lokalKontaktopplysning = new Kontaktopplysning();
                lokalKontaktopplysning.setKontaktopplysningID(new KontaktopplysningID(saksnummer, orgnr));
                return lokalKontaktopplysning;
            });
        kontaktopplysning.setKontaktOrgnr(kontaktOrgnr);
        kontaktopplysning.setKontaktNavn(kontaktNavn);
        kontaktopplysningRepository.save(kontaktopplysning);
        return kontaktopplysning;
    }

    public void slettKontaktopplysning(String saksnummer, String orgnr) throws FunksjonellException {
        try {
            kontaktopplysningRepository.deleteById(new KontaktopplysningID(saksnummer, orgnr));
        } catch (EmptyResultDataAccessException e) {
            throw new FunksjonellException("Finner ingen kontaktopplysninger med gitt saksnummer/orgnummer");
        }
    }
}
