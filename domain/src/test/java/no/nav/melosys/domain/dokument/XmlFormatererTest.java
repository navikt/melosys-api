package no.nav.melosys.domain.dokument;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class XmlFormatererTest {

    @Test
    public void formaterXml_xmlUtenIdentering_xmlBlirFormatert() {
        final String xmlString = "<d><key>verdi</key></d>";
        final String formatertXml = XmlFormaterer.formaterXml(xmlString);
        assertThat(formatertXml).isEqualToIgnoringNewLines(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
            "<d>\n" +
            "    <key>verdi</key>\n" +
            "</d>\n"
        );
    }
}