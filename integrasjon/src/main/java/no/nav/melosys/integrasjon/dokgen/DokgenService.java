package no.nav.melosys.integrasjon.dokgen;

import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DokgenService {

    private final DokgenConsumer dokgenConsumer;

    @Autowired
    public DokgenService(DokgenConsumer dokgenConsumer) {
        this.dokgenConsumer = dokgenConsumer;
    }

    public byte[] lagPdf(String malNavn, DokgenDto dokgenDto) {
        return dokgenConsumer.lagPdf(malNavn, dokgenDto);
    }

}
