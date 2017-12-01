package no.nav.melosys.domain.jpa;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.PostLoad;

/**
 * JPA entity listener som setter dokumentobjektet når en saksopplysning loades fra databasen.
 */
public class SaksopplysningListener {

    private static final Logger log = LoggerFactory.getLogger(SaksopplysningListener.class);

    private DokumentFactory dokumentFactory;

    public SaksopplysningListener() {
        this.dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());
    }

    @PostLoad
    public void postLoad(Saksopplysning saksopplysning) {
        log.debug("Post Load av saksopplysning med ID: {}", saksopplysning.getId());
        dokumentFactory.lagDokument(saksopplysning);
        log.debug("Dokument av type {} opprettet", saksopplysning.getType());
    }

}
