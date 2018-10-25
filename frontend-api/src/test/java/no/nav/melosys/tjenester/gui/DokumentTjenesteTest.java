package no.nav.melosys.tjenester.gui;

import no.nav.melosys.domain.DokumentType;
import org.junit.Test;

import static no.nav.melosys.tjenester.gui.dto.Dokumenttype.MELDING_MANGLENDE_OPPLYSNINGER;
import static org.assertj.core.api.Assertions.assertThat;

public class DokumentTjenesteTest {

    @Test
    public void mapDokumenttype() {

        DokumentType dokumentType = DokumentType.valueOf(MELDING_MANGLENDE_OPPLYSNINGER.getKode());

        assertThat(dokumentType).isNotNull();
        assertThat(dokumentType).isEqualTo(DokumentType.MELDING_MANGLENDE_OPPLYSNINGER);
    }
}
