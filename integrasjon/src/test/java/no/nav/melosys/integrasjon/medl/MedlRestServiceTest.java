package no.nav.melosys.integrasjon.medl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.UgyldigInput;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForGet;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class MedlRestServiceTest {

    private MedlRestService medlRestService;

    private String fnr = "77777777773";

    private MedlemskapRestConsumer mockRestConsumer;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws PersonIkkeFunnet, UgyldigInput, Sikkerhetsbegrensning {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockRestConsumer = mock(MedlemskapRestConsumer.class);

        medlRestService = new MedlRestService(mockRestConsumer, objectMapper);
    }

    @Test
    public void getPeriodeListe() throws Exception {
        when(mockRestConsumer.hentPeriodeListe(eq(fnr), any(), any())).thenReturn(
            asList(objectMapper.readValue(
                getClass().getClassLoader().getResource("mock/medlemskap/" + fnr + ".json"),
                MedlemskapsunntakForGet[].class)
            )
        );

        Saksopplysning saksopplysning = medlRestService.hentPeriodeListe(fnr, null, null);
        assertNotNull(saksopplysning);
        assertNotNull(saksopplysning.getKilder());
        assertFalse(saksopplysning.getKilder().isEmpty());
        assertNotNull(saksopplysning.getKilder().iterator().next().getMottattDokument());

        MedlemskapDokument medlemskapDokument = (MedlemskapDokument) saksopplysning.getDokument();

        assertNotNull(medlemskapDokument);
        assertNotNull(medlemskapDokument.getMedlemsperiode());
        assertFalse(medlemskapDokument.getMedlemsperiode().isEmpty());

        for (Medlemsperiode medlemsperiode : medlemskapDokument.getMedlemsperiode()) {
            assertNotNull(medlemsperiode.getType());
            assertNotNull(medlemsperiode.getStatus());
            assertNotNull(medlemsperiode.getLovvalg());
            assertNotNull(medlemsperiode.getKilde());
            assertNotNull(medlemsperiode.getGrunnlagstype());
        }
    }

}
