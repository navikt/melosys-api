package no.nav.melosys.domain.dokument;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;

public interface KonverteringTest {
    default Saksopplysning konverter(InputStream kilde, DokumentFactory factory, SaksopplysningType type, String versjon) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(kilde, Charset.forName(StandardCharsets.UTF_8.name())))) {
            Saksopplysning saksopplysning = new Saksopplysning();
            String xmlStr = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            saksopplysning.leggTilKildesystemOgMottattDokument(null, xmlStr);
            saksopplysning.setType(type);
            saksopplysning.setVersjon(versjon);

            // Setter internXml
            factory.lagDokument(saksopplysning);

            return saksopplysning;
        }
    }

    Saksopplysning getSaksopplysning(String ressurs) throws IOException;
}
