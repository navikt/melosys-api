package no.nav.melosys.domain.dokument.person;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

/**
 * Representerer svar fra personregisteret (TPS)
 */
@XmlRootElement
public class PersonopplysningDokument extends SaksopplysningDokument {

    public String fnr;

    public Sivilstand sivilstand;

    /** Kodeverk: Landkoder */
    public String statsborgerskap;

    /** Kodeverk: Kjønnstyper */
    public String kjønn;

    // FIXME: Trenger vi fornavn, mellomnavn og etternavn også?
    public String sammensattNavn;

    public LocalDate fødselsdato;

    public LocalDate dødsdato;

    public Diskresjonskode diskresjonskode;

    public Personstatus personstatus;
    
    //FIXME: Trenger vi postadresse?
    
    public static void main(String[] args) throws Exception {
        // Transform...
        Transformer t = TransformerFactory.newInstance().newTransformer(new StreamSource(new File("C:/ws/melosys-app/domain/src/main/resources/tps_person_3.0.xslt")));
        StreamSource xmlSource = new StreamSource(new File("C:/ws/melosys-app/integrasjon/src/main/resources/mock/person/88888888884.xml"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Result outputTarget = new StreamResult(System.out);
        Result outputTarget = new StreamResult(baos);
        t.transform(xmlSource, outputTarget);
        byte[] xmlBytes = baos.toByteArray();
        // Unmarshal...
        ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes);
        JAXBContext ctx = JAXBContext.newInstance(PersonopplysningDokument.class);
        Unmarshaller um = ctx.createUnmarshaller();
        
        PersonopplysningDokument p2 = (PersonopplysningDokument) um.unmarshal(bais);
        
        System.err.println("dd");

        
    }
    
}
