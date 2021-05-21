package no.nav.melosys.tjenester.gui;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.aktoer.AktoerDto;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AktoerTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(AktoerTjenesteTest.class);
    private static final String AKTOER_SCHEMA = "fagsaker-aktoerer-schema.json";
    private static final String AKTOER_POST_SCHEMA = "fagsaker-aktoerer-post-schema.json";

    @Mock
    private TilgangService tilgangService;
    @Mock
    private AktoerService aktoerService;
    @Mock
    private FagsakService fagsakService;

    private AktoerTjeneste aktoerTjeneste;

    @BeforeEach
    public void setUp() {
        aktoerTjeneste = new AktoerTjeneste(tilgangService, aktoerService, fagsakService);
    }

    @Test
    public void aktoerSchemaValidering() throws Exception {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setAktoerID("1234");
        aktoerDto.setRolleKode("BRUKER");
        aktoerDto.setRepresentererKode("BRUKER");
        aktoerDto.setOrgnr("123456789");
        aktoerDto.setDatabaseID(2L);

        validerArray(Collections.singletonList(aktoerDto), AKTOER_SCHEMA, log);

        valider(aktoerDto, AKTOER_POST_SCHEMA, log);
    }

    @Test
    public final void lagOppdaterAktoer() {
        when(fagsakService.hentFagsak("MELTEST-1")).thenReturn(lagFagsak());

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
    public final void hentAktoer_tilAktoerDto() {
        when(fagsakService.hentFagsak("MELTEST-1")).thenReturn(lagFagsak());

        Aktoer aktoerRep = new Aktoer();
        aktoerRep.setId(29L);
        aktoerRep.setAktørId("1234");
        aktoerRep.setInstitusjonId("INST1");
        aktoerRep.setRolle(Aktoersroller.REPRESENTANT);
        aktoerRep.setOrgnr("99");
        aktoerRep.setFagsak(lagFagsak());
        aktoerRep.setUtenlandskPersonId("UTL");
        aktoerRep.setRepresenterer(Representerer.BRUKER);

        Aktoer aktoerMyndighet = new Aktoer();
        aktoerMyndighet.setId(39L);
        aktoerMyndighet.setAktørId("1235");
        aktoerMyndighet.setInstitusjonId("INST2");
        aktoerMyndighet.setRolle(Aktoersroller.MYNDIGHET);
        aktoerMyndighet.setOrgnr("100");
        aktoerMyndighet.setFagsak(lagFagsak());
        aktoerMyndighet.setUtenlandskPersonId("UTL");

        when(aktoerService.hentfagsakAktører(any(), any(), any())).thenReturn(Collections.singletonList(aktoerMyndighet));

        List<AktoerDto> aktører = aktoerTjeneste.hentAktoerer("MELTEST-1", "MYNDIGHET", null);
        AktoerDto aktoerDto = aktører.get(0);

        assertThat(aktoerDto.getAktoerID()).isEqualTo("1235");
        assertThat(aktoerDto.getInstitusjonsID()).isEqualTo("INST2");
        assertThat(aktoerDto.getRolleKode()).isEqualTo("MYNDIGHET");
        assertThat(aktoerDto.getOrgnr()).isEqualTo("100");
        assertThat(aktoerDto.getRepresentererKode()).isNull();
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MELTEST-1");
        return fagsak;
    }
}
