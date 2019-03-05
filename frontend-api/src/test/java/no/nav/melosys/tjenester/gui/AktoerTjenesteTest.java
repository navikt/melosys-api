package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Collections;
import javax.ws.rs.core.Response;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.aktoer.AktoerService;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AktoerTjenesteTest extends JsonSchemaTest {

    private static final Logger log = LoggerFactory.getLogger(AktoerTjenesteTest.class);

    private static final String AKTOER_SCHEMA = "aktoer-schema.json";

    private static final String AKTOER_POST_SCHEMA = "aktoer-post-schema.json";

    private String schemaType;

    private AktoerTjeneste aktoerTjeneste;

    private AktoerService aktoerService;

    @Before
    public void setUp() {
        FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        Tilgang tilgang = mock(Tilgang.class);

        aktoerService = mock(AktoerService.class);
        aktoerTjeneste = new AktoerTjeneste(tilgang, aktoerService, fagsakRepository);
        when(fagsakRepository.findBySaksnummer("MELTEST-1")).thenReturn(lagFagsak());
    }

    @Override
    public String schemaNavn() {
        return schemaType;
    }

    @Test
    public void aktoerSchemaValidering() throws IOException, JSONException {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setAktoerID("1234");
        aktoerDto.setRolleKode("BRUKER");
        aktoerDto.setRepresentererKode("BRUKER");
        aktoerDto.setOrgnr("123456789");

        schemaType = AKTOER_SCHEMA;
        validerListe(Collections.singletonList(aktoerDto), log);

        schemaType =  AKTOER_POST_SCHEMA;
        valider(aktoerDto, log);

    }

    @Test
    public final void lagOppdaterAktoer() throws FunksjonellException, TekniskException {

        Aktoer aktoerBruker = new Aktoer();
        aktoerBruker.setAktørId("1234");
        aktoerBruker.setRolle(Aktoersroller.BRUKER);
        aktoerBruker.setFagsak(lagFagsak());


        AktoerDto aktoerDtoBruker = new AktoerDto();
        aktoerDtoBruker.setAktoerID("1235");
        aktoerDtoBruker.setRolleKode("BRUKER");

        aktoerTjeneste.lagAktoerer("MELTEST-1", aktoerDtoBruker);
        verify(aktoerService).lagEllerOppdaterAktoer(lagFagsak(), aktoerDtoBruker);

    }

    @Test
    public final void hentAktoer_tilAktoerDto() throws SikkerhetsbegrensningException, TekniskException, IkkeFunnetException {

        Aktoer aktoerRep = new Aktoer();
        aktoerRep.setAktørId("1234");
        aktoerRep.setInstitusjonId("INST1");
        aktoerRep.setRolle(Aktoersroller.REPRESENTANT);
        aktoerRep.setOrgnr("99");
        aktoerRep.setFagsak(lagFagsak());
        aktoerRep.setUtenlandskPersonId("UTL");
        aktoerRep.setRepresenterer(Representerer.BRUKER);

        Aktoer aktoerMyd = new Aktoer();
        aktoerMyd.setAktørId("1235");
        aktoerMyd.setInstitusjonId("INST2");
        aktoerMyd.setRolle(Aktoersroller.MYNDIGHET);
        aktoerMyd.setOrgnr("100");
        aktoerMyd.setFagsak(lagFagsak());
        aktoerMyd.setUtenlandskPersonId("UTL");

        when(aktoerService.hentfagsakAktoerer(any(), any(), any())).thenReturn(aktoerMyd);

        Response response = aktoerTjeneste.hentAktoerer("MELTEST-1", "MYNDIGHET", null);
        AktoerDto aktoerDto = (AktoerDto) response.getEntity();

        assertThat(aktoerDto.getAktoerID()).isEqualTo("1235");
        assertThat(aktoerDto.getInstitusjonsID()).isEqualTo("INST2");
        assertThat(aktoerDto.getRolleKode()).isEqualTo("MYNDIGHET");
        assertThat(aktoerDto.getOrgnr()).isEqualTo("100");
        assertThat(aktoerDto.getRepresentererKode()).isNull();

        when(aktoerService.hentfagsakAktoerer(any(), any(), any())).thenReturn(aktoerRep);

        response = aktoerTjeneste.hentAktoerer("MELTEST-1", "REPRESENTANT", "BRUKER");
        aktoerDto = (AktoerDto) response.getEntity();

        assertThat(aktoerDto.getAktoerID()).isEqualTo("1234");
        assertThat(aktoerDto.getInstitusjonsID()).isEqualTo("INST1");
        assertThat(aktoerDto.getRolleKode()).isEqualTo("REPRESENTANT");
        assertThat(aktoerDto.getOrgnr()).isEqualTo("99");
        assertThat(aktoerDto.getRepresentererKode()).isEqualTo("BRUKER");

    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-1");
        return fagsak;
    }
}