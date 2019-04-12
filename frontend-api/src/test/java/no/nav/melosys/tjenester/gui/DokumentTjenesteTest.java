package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import javax.ws.rs.core.Response;

import com.google.common.collect.Comparators;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.dokument.DokumentService;
import no.nav.melosys.tjenester.gui.dto.dokument.JournalpostInfoDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class DokumentTjenesteTest extends JsonSchemaTestParent {

    private static final Logger log = LoggerFactory.getLogger(DokumentTjenesteTest.class);

    private DokumentTjeneste dokumentTjeneste;

    private String schema;

    @Override
    public String schemaNavn() {
        return schema;
    }

    @Mock
    private DokumentService dokumentService;

    @Mock
    private Tilgang tilgang;

    @Before
    public void setUp() {
        dokumentTjeneste = new DokumentTjeneste(dokumentService, tilgang);
    }

    @Test
    public void hentDokumenter() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException, IOException {
        List<Journalpost> journalposter = EnhancedRandom.randomListOf(3, Journalpost.class);
        given(dokumentService.hentDokumenter(anyString())).willReturn(journalposter);

        Response response = dokumentTjeneste.hentDokumenter("MEL-1873");
        List<JournalpostInfoDto> dtos = (List<JournalpostInfoDto>) response.getEntity();
        boolean inOrder = Comparators.isInOrder(dtos, Comparator.comparing(JournalpostInfoDto::hentGjeldendeTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())));
        assertThat(inOrder).isTrue();


        schema = "dokumenter-oversikt-schema.json";
        validerListe(dtos);
    }
}