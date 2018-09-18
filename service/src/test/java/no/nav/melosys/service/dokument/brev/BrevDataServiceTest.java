package no.nav.melosys.service.dokument.brev;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;

import no.nav.dok.brevdata.felles.v1.navfelles.Saksbehandler;
import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Element;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNavAnsatt;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNavEnhet;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataServiceTest {

    BrevDataService service;

    @Before
    public void setUp() throws IkkeFunnetException {
        TpsFasade tpsFasade = mock(TpsFasade.class);
        service = spy(new BrevDataService(tpsFasade));

        when(tpsFasade.hentIdentForAktørId(any())).thenReturn("Fnr");

        Saksbehandler saksbehandler = new Saksbehandler();
        saksbehandler.setNavEnhet(lagNavEnhet());
        saksbehandler.setNavAnsatt(lagNavAnsatt("test"));

        doReturn(saksbehandler).when(service).lagSaksbehandler();
    }

    @Test
    public void mapTilXml() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MOCK-1");

        Aktoer aktør = new Aktoer();
        aktør.setAktørId("Aktør-Id");
        aktør.setRolle(RolleType.BRUKER);
        fagsak.setAktører(new HashSet<>(Collections.singleton(aktør)));

        Behandling behandling = new Behandling();
        behandling.setRegistrertDato(Instant.now());
        behandling.setType(Behandlingstype.SØKNAD);
        behandling.setFagsak(fagsak);

        Element element = service.lagBrevXML(DokumentType.FORVALTNINGSMELDING, behandling);

        assertThat(element).isNotNull();
    }
}
