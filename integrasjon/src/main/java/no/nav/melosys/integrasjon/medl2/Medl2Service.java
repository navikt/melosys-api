package no.nav.melosys.integrasjon.medl2;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.integrasjon.medl2.medlemskap.MedlemskapConsumer;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Foedselsnummer;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;
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
import java.util.List;

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
    public List<Medlemsperiode> hentPeriodeListe(String fnr) throws PersonIkkeFunnet, Sikkerhetsbegrensning {
        HentPeriodeListeResponse res = hentPeriodeListeResponse(fnr);

        return res.getPeriodeListe();
    }

    @Override
    public Saksopplysning getPeriodeListe(String fnr) throws PersonIkkeFunnet, Sikkerhetsbegrensning {
        HentPeriodeListeResponse response = hentPeriodeListeResponse(fnr);

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            String namespace = "http://nav.no/tjeneste/virksomhet/medlemskap/v2";
            QName qName = new QName(namespace,"hentPeriodeListeResponse");
            JAXBElement<HentPeriodeListeResponse> root = new JAXBElement<>(qName, HentPeriodeListeResponse.class, response);

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(root, xmlWriter);
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        saksopplysning.setKilde(SaksopplysningKilde.MEDL2);
        saksopplysning.setType(SaksopplysningType.MEDLEMSKAP);
        saksopplysning.setVersjon(MEDLEMSKAP_VERSJON);

        return saksopplysning;
    }

    private HentPeriodeListeResponse hentPeriodeListeResponse(String fnr) throws PersonIkkeFunnet, Sikkerhetsbegrensning {
        Foedselsnummer ident = new Foedselsnummer();
        ident.setValue(fnr);

        HentPeriodeListeRequest req = new HentPeriodeListeRequest();
        req.setIdent(ident);

        return medlemskapConsumer.hentPeriodeListe(req);
    }
}
