package no.nav.melosys.integrasjon.dokgen;

import no.nav.melosys.integrasjon.dokgen.dto.Flettedata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DokgenService {

    private final DokgenConsumer dokgenConsumer;

    @Autowired
    public DokgenService(DokgenConsumer dokgenConsumer) {
        this.dokgenConsumer = dokgenConsumer;
    }

    public byte[] lagPdf(String mal,  Flettedata flettedata) {
        return dokgenConsumer.lagPdf(mal, flettedata);
    }

}
