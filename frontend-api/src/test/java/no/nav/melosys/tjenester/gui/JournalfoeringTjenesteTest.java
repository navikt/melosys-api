package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import javax.ws.rs.core.Response;

import org.jeasy.random.EasyRandom;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.JournalpostDto;
import org.jeasy.random.EasyRandomParameters;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("resource")
public class JournalfoeringTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(JournalfoeringTjenesteTest.class);
    private static final String JOURNALFOERING_SCHEMA = "journalforing-schema.json";
    private static final String JOURNALFOERING_TILORDNE_SCHEMA = "journalforing-tilordne-post-schema.json";
    private static final String JOURNALFOERING_OPPRETT_SCHEMA = "journalforing-opprett-post-schema.json";
    private static final String SAMPLE_ORGNR = "899655123";
    private static final String SAMPLE_FNR = "77777777772";

    private EasyRandom random;

    private JournalfoeringTjeneste tjeneste;
    @Mock
    private JournalfoeringService journalføringService;

    @Before
    public void setUp() {
        tjeneste = new JournalfoeringTjeneste(journalføringService);

        random = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));
    }

    @Test
    public void hentJournalpostValidering() throws IOException, MelosysException, JSONException {
        Journalpost journalpost = random.nextObject(Journalpost.class);
        journalpost.setBrukerId(SAMPLE_FNR);
        journalpost.setAvsenderId(SAMPLE_ORGNR);
        when(journalføringService.hentJournalpost(anyString())).thenReturn(journalpost);

        Response response = tjeneste.hentJournalpostOpplysninger(anyString());
        JournalpostDto journalpostDto = (JournalpostDto) response.getEntity();

        valider(journalpostDto, JOURNALFOERING_SCHEMA, log);
    }

    @Test
    public void journalføringTilordneSchemaValidering() throws IOException, JSONException {
        JournalfoeringTilordneDto journalfoeringDto = random.nextObject(JournalfoeringTilordneDto.class);
        valider(journalfoeringDto, JOURNALFOERING_TILORDNE_SCHEMA, log);
    }

    @Test
    public void journalføringOpprettSchemaValidering() throws IOException, JSONException {
        JournalfoeringOpprettDto journalfoeringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalfoeringDto.setArbeidsgiverID(SAMPLE_ORGNR);
        journalfoeringDto.setRepresentantID(SAMPLE_ORGNR);
        valider(journalfoeringDto, JOURNALFOERING_OPPRETT_SCHEMA, log);
    }

    @Test
    public void journalføringOpprettSchemaValideringMedRepresentantIDNull() throws IOException, JSONException {
        JournalfoeringOpprettDto journalfoeringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalfoeringDto.setArbeidsgiverID(SAMPLE_ORGNR);
        journalfoeringDto.setRepresentantID(null);
        valider(journalfoeringDto, JOURNALFOERING_OPPRETT_SCHEMA, log);
    }

}

