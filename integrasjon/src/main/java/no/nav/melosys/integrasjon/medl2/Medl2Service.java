package no.nav.melosys.integrasjon.medl2;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.medl2.medlemskap.MedlemskapConsumer;
import no.nav.melosys.integrasjon.medl2.medlemskap.MedlemskapConsumerConfig;
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
import javax.xml.namespace.QName;
import java.io.StringWriter;

/*
* FIXME: Bør flyttes til src/test, men krever større overordnede endringer.
*/
@Service
public class Medl2Service implements Medl2Fasade {

    private static final Logger log = LoggerFactory.getLogger(Medl2Service.class);

    private static final String MEDLEMSKAP_VERSJON = "2.0";

    private final MedlemskapConsumer medlemskapConsumer;

    private final Marshaller marshaller;

    @Autowired
    public Medl2Service(MedlemskapConsumer medlemskapConsumer) {
        this.medlemskapConsumer = medlemskapConsumer;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(HentPeriodeListeResponse.class);
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
            JAXBElement<HentPeriodeListeResponse> xmlRoot
                    = new JAXBElement<>(MedlemskapConsumerConfig.getResponse(), HentPeriodeListeResponse.class, response);

            marshaller.marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        // TODO: Implementeres av EESSI2-335
        //saksopplysning.setKilde(SaksopplysningKilde.MEDL2);
        //saksopplysning.setType(SaksopplysningType.MEDLEMSKAP);
        saksopplysning.setVersjon(MEDLEMSKAP_VERSJON);

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
