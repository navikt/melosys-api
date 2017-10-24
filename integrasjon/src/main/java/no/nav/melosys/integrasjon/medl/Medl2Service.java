package no.nav.melosys.integrasjon.medl;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.medl.medlemskap.HentPeriodeListeResponseWrapper;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapConsumer;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapConsumerConfig;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Foedselsnummer;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

@Service
public class Medl2Service implements Medl2Fasade {

    private static final Logger log = LoggerFactory.getLogger(Medl2Service.class);

    private static final String MEDLEMSKAP_VERSJON = "2.0";

    private final MedlemskapConsumer medlemskapConsumer;

    private DokumentFactory dokumentFactory;

    private final Marshaller marshaller;

    @Autowired
    public Medl2Service(MedlemskapConsumer medlemskapConsumer, DokumentFactory dokumentFactory) {
        this.medlemskapConsumer = medlemskapConsumer;
        this.dokumentFactory = dokumentFactory;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(HentPeriodeListeResponseWrapper.class);
            marshaller = jaxbContext.createMarshaller();
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Saksopplysning getPeriodeListe(String fnr) throws IntegrasjonException, SikkerhetsbegrensningException {
        HentPeriodeListeResponse response = hentPeriodeListeResponse(fnr);

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            HentPeriodeListeResponseWrapper wrapper
                    = new HentPeriodeListeResponseWrapper().withPeriodeListe(response.getPeriodeListe());
            JAXBElement<HentPeriodeListeResponseWrapper> xmlRoot
                    = new JAXBElement<>(MedlemskapConsumerConfig.getResponse(), HentPeriodeListeResponseWrapper.class, wrapper);

            marshaller.marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        saksopplysning.setKilde(SaksopplysningKilde.MEDL);
        saksopplysning.setType(SaksopplysningType.MEDLEMSKAP);
        saksopplysning.setVersjon(MEDLEMSKAP_VERSJON);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }

    private HentPeriodeListeResponse hentPeriodeListeResponse(String fnr) throws SikkerhetsbegrensningException {
        Foedselsnummer ident = new Foedselsnummer();
        ident.setValue(fnr);

        HentPeriodeListeRequest req = new HentPeriodeListeRequest();
        req.setIdent(ident);

        try {
            return medlemskapConsumer.hentPeriodeListe(req);
        } catch (Sikkerhetsbegrensning sikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(sikkerhetsbegrensning);
        } catch (PersonIkkeFunnet personIkkeFunnet) {
            throw new IntegrasjonException(personIkkeFunnet);
        }
    }
}
