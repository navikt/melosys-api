package no.nav.melosys.domain.jpa;

import javax.persistence.PostLoad;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;

/**
 * JPA entity listener som setter dokumentobjektet når en saksopplysning loades fra databasen.
 */
public class SaksopplysningListener {
    private DokumentFactory dokumentFactory;

    public SaksopplysningListener() {
        this.dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());
    }

    @PostLoad
    public void postLoad(Saksopplysning saksopplysning) {
        dokumentFactory.lagDokument(saksopplysning);
    }
}
