package no.nav.melosys.tjenester.gui.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.tjenester.gui.dto.util.DtoUtils;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class MedlemsperiodeDtoTest {

    @Test
    public void tilLocalDate_onJodaTime_returnsJavaTime() {
        final org.joda.time.format.DateTimeFormatter jodaFormatter =
                org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM-dd");
        final org.joda.time.LocalDate jodaLocalDate =
                org.joda.time.LocalDate.parse("2009-10-01", jodaFormatter);

        LocalDate localDate = DtoUtils.tilLocalDate(jodaLocalDate);

        assertNotNull(localDate);
        assertEquals(jodaLocalDate.getDayOfMonth(), localDate.getDayOfMonth());
        assertEquals(jodaLocalDate.getMonthOfYear(), localDate.getMonthValue());
        assertEquals(jodaLocalDate.getYear(), localDate.getYear());
    }

    @Test
    public void tilDto_onMedlemsperiodeFromXml_returnsMedlemsperiodeDto() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<ns2:hentPeriodeListeResponse xmlns:ns2=\"http://nav.no/tjeneste/virksomhet/medlemskap/v2\">\n" +
                "    <response>\n" +
                "        <periodeListe opprinneligOpprettetAv=\"A135683\" sistEndretAv=\"A135683\" tidspunktOpprinneligOpprettet=\"2012-12-05T13:32:41.954+01:00\" tidspunktSistEndret=\"2012-12-05T13:32:42.052+01:00\">\n" +
                "            <fraOgMed>2012-10-09</fraOgMed>\n" +
                "            <tilOgMed>2012-10-25</tilOgMed>\n" +
                "            <id>3207474</id>\n" +
                "            <versjon>1</versjon>\n" +
                "            <datoRegistrert>2012-12-05</datoRegistrert>\n" +
                "            <datoBesluttet>2012-12-05</datoBesluttet>\n" +
                "            <status term=\"Gyldig\">GYLD</status>\n" +
                "            <trygdedekning term=\"Unntatt\">Unntatt</trygdedekning>\n" +
                "            <helsedel>false</helsedel>\n" +
                "            <type term=\"Periode uten medlemskap\">PUMEDSKP</type>\n" +
                "            <land term=\"ØSTERRIKE\">AUT</land>\n" +
                "            <lovvalg term=\"Endelig\">ENDL</lovvalg>\n" +
                "            <kilde term=\"Gosys\">FS22</kilde>\n" +
                "            <kildedokumenttype term=\"A1\">PortBlank_A1</kildedokumenttype>\n" +
                "            <grunnlagstype term=\"EØS 883/04 - 12.1\">FO_12_1</grunnlagstype>\n" +
                "        </periodeListe>\n" +
                "    </response>\n" +
                "</ns2:hentPeriodeListeResponse>";

        JAXBContext jaxbContext = JAXBContext.newInstance(HentPeriodeListeResponse.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        InputStream is = new ByteArrayInputStream(xml.getBytes());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document document = factory.newDocumentBuilder().parse(is);
        NodeList nodeList = document.getElementsByTagName("periodeListe");

        ObjectMapper mapper = new ObjectMapper();

        for (int i = 0; i < nodeList.getLength(); i++) {
            JAXBElement<Medlemsperiode> element = unmarshaller.unmarshal(nodeList.item(i), Medlemsperiode.class);
            Medlemsperiode medlemsperiode = element.getValue();

            assertNotNull(medlemsperiode);

            MedlemsperiodeDto medlemsperiodeDto = MedlemsperiodeDto.toDto(element.getValue());

            assertEquals(medlemsperiode.getFraOgMed().getYear(), medlemsperiodeDto.getPeriode().getFom().getYear());
            assertEquals(medlemsperiode.getFraOgMed().getMonthOfYear(), medlemsperiodeDto.getPeriode().getFom().getMonthValue());
            assertEquals(medlemsperiode.getFraOgMed().getDayOfMonth(), medlemsperiodeDto.getPeriode().getFom().getDayOfMonth());

            assertEquals(medlemsperiode.getTilOgMed().getYear(), medlemsperiodeDto.getPeriode().getTom().getYear());
            assertEquals(medlemsperiode.getTilOgMed().getMonthOfYear(), medlemsperiodeDto.getPeriode().getTom().getMonthValue());
            assertEquals(medlemsperiode.getTilOgMed().getDayOfMonth(), medlemsperiodeDto.getPeriode().getTom().getDayOfMonth());

            assertEquals(medlemsperiode.getType().getValue(), medlemsperiodeDto.getType().getValue());
            assertEquals(medlemsperiode.getType().getTerm(), medlemsperiodeDto.getType().getTerm());

            assertEquals(medlemsperiode.getStatus().getValue(), medlemsperiodeDto.getStatus().getValue());
            assertEquals(medlemsperiode.getStatus().getTerm(), medlemsperiodeDto.getStatus().getTerm());

            assertEquals(medlemsperiode.getGrunnlagstype().getValue(), medlemsperiodeDto.getGrunnlagstype().getValue());
            assertEquals(medlemsperiode.getGrunnlagstype().getTerm(), medlemsperiodeDto.getGrunnlagstype().getTerm());

            String json = mapper.writeValueAsString(medlemsperiodeDto);

            assertNotNull(json);
            assertFalse("".equals(json));
        }
    }
}
