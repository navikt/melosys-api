package no.nav.melosys.integrasjon.ereg;

import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonConsumer;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonOrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonRequest;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class EregService implements EregFasade {

    private static final Logger log = LoggerFactory.getLogger(EregService.class);

    private static final String ORGANISASJON_VERSJON = "4.0";

    private OrganisasjonConsumer organisasjonConsumer;

    private DokumentFactory dokumentFactory;

    private final JAXBContext jaxbContext;

    @Autowired
    public EregService(OrganisasjonConsumer organisasjonConsumer, DokumentFactory dokumentFactory) {
        this.organisasjonConsumer = organisasjonConsumer;
        this.dokumentFactory = dokumentFactory;

        try {
            jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse.class);
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }

    }
    
    @Override
    public Saksopplysning hentOrganisasjon(String orgnummer) throws IkkeFunnetException, IntegrasjonException {
        HentOrganisasjonRequest request = new HentOrganisasjonRequest();
        request.setOrgnummer(orgnummer);

        // Kall til E-reg
        HentOrganisasjonResponse response = null;
        try {
            response = organisasjonConsumer.hentOrganisasjon(request);
        } catch (HentOrganisasjonOrganisasjonIkkeFunnet hentOrganisasjonOrganisasjonIkkeFunnet) {
            throw new IkkeFunnetException(hentOrganisasjonOrganisasjonIkkeFunnet);
        } catch (HentOrganisasjonUgyldigInput hentOrganisasjonUgyldigInput) {
            throw new IntegrasjonException(hentOrganisasjonUgyldigInput);
        }

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse xmlRoot = new no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse();
            xmlRoot.setResponse(response);
            jaxbContext.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            log.error("", e);
            throw new IntegrasjonException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        saksopplysning.setKilde(SaksopplysningKilde.EREG);
        saksopplysning.setType(SaksopplysningType.ORGANISASJON);
        saksopplysning.setVersjon(ORGANISASJON_VERSJON);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }


}
