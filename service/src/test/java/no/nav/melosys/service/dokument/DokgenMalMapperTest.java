package no.nav.melosys.service.dokument;

import java.time.Instant;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.dokgen.dto.SaksbehandlingstidSoknad;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singleton;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.ATTEST_A1;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DokgenMalMapperTest {

    @Mock
    private KodeverkService mockKodeverkService;

    private DokgenMalMapper dokgenMalMapper;

    @BeforeEach
    void init() {
        dokgenMalMapper = new DokgenMalMapper(mockKodeverkService);
    }

    @Test
    void feilerNårProduserbartDokumentIkkeErStøttet() {
        assertThrows(FunksjonellException.class, () ->
            dokgenMalMapper.mapBehandling(ATTEST_A1, new Behandling(), Instant.now())
        );
    }

    @Test
    void skalMappeTilDto() throws Exception {
        when(mockKodeverkService.dekod(any(), any(), any())).thenReturn("Andeby");

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setSaksopplysninger(singleton(lagPersonopplysning()));
        behandling.setFagsak(lagFagsak());
        DokgenDto dokgenDto = dokgenMalMapper.mapBehandling(MELDING_FORVENTET_SAKSBEHANDLINGSTID, behandling, Instant.now());

        assertTrue(dokgenDto instanceof SaksbehandlingstidSoknad);
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setRegistrertDato(Instant.now());
        return fagsak;
    }

    private Saksopplysning lagPersonopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "99887766554";
        saksopplysning.setDokument(personDokument);
        return saksopplysning;
    }
}