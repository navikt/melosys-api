package no.nav.melosys.service.journalforing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

// FIXME Fjernes når vi kan hente dokumenter fra joark.
@Service
@Profile("mocking")
public class DokumentMockService extends DokumentService {

    DokumentMockService() {
        super(null);
    }

    @Override
    public byte[] hentDokument(String journalpostID, String dokumentID) {
        String pdfNavn = "dokumenttest.pdf";

        InputStream is = getClass().getClassLoader().getResourceAsStream(pdfNavn);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        try {
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer.toByteArray();
    }
}
