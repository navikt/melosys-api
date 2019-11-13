package no.nav.melosys.saksflyt.steg;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstraktDistribuerJournalpost extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(AbstraktDistribuerJournalpost.class);

    private final DoksysFasade doksysFasade;

    @Autowired
    public AbstraktDistribuerJournalpost(DoksysFasade doksysFasade) {
        this.doksysFasade = doksysFasade;
    }

    protected void bestillDistribuering(String journalpostId, UtenlandskMyndighet utenlandskMyndighet) {
        String bestillingsId = doksysFasade.distribuerJournalpost(journalpostId, tilStrukturertAdresse(utenlandskMyndighet));
        log.info("Distribuering av journalpost {} bestilt med bestillingsId {}", journalpostId, bestillingsId);
    }

    private static StrukturertAdresse tilStrukturertAdresse(UtenlandskMyndighet utenlandskMyndighet) {
        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.poststed = utenlandskMyndighet.poststed;
        strukturertAdresse.postnummer = utenlandskMyndighet.postnummer;
        strukturertAdresse.gatenavn = utenlandskMyndighet.gateadresse;
        strukturertAdresse.landkode = utenlandskMyndighet.landkode.getKode();
        return strukturertAdresse;
    }
}
