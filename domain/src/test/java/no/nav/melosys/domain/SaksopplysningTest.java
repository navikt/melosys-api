package no.nav.melosys.domain;

import static no.nav.melosys.domain.SaksopplysningKilde.TPS;
import static no.nav.melosys.domain.SaksopplysningType.PERSONOPPLYSNING;
import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

public class SaksopplysningTest {

    @Ignore //FIXME
    @Test
    public void xmlSerialiseringTest() {
        Saksopplysning so = new Saksopplysning();
        so.setType(PERSONOPPLYSNING);
        so.setKilde(TPS);
        so.setVersjon(1);
        String initDokXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns2:hentPersonResponse xmlns:ns2=\"http://nav.no/tjeneste/virksomhet/person/v3\" xmlns:ns3=\"http://nav.no/tjeneste/virksomhet/person/v3/informasjon\">"
            + "<response><person xsi:type=\"ns3:Bruker\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><sivilstand fomGyldighetsperiode=\"2010-01-30T00:00:00.000+01:00\">"
            + "<sivilstand>GIFT</sivilstand></sivilstand><statsborgerskap><land>NOR</land></statsborgerskap><aktoer xsi:type=\"ns3:PersonIdent\"><ident><ident>88888888888</ident><type>FNR</type>"
            + "</ident></aktoer><kjoenn><kjoenn>K</kjoenn></kjoenn><personnavn><etternavn>NAVNESEN</etternavn><fornavn>FORNAVN LOURDES</fornavn><mellomnavn>NOE</mellomnavn>"
            + "<sammensattNavn>NAVNESEN FORNAVN L NOE</sammensattNavn></personnavn><personstatus><personstatus>BOSA</personstatus></personstatus><foedselsdato><foedselsdato>1969-01-06+01:00"
            + "</foedselsdato></foedselsdato><geografiskTilknytning xsi:type=\"ns3:Bydel\"><geografiskTilknytning>030109</geografiskTilknytning></geografiskTilknytning></person></response>"
            + "</ns2:hentPersonResponse>";
        so.setDokumentXml(initDokXml);
        so.lagDokument();
        so.lagDokumentXml();
        assertEquals(initDokXml, so.getDokumentXml());
    }
    

}
