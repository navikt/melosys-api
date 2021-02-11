package no.nav.melosys.service.brev;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper.LOENNET_ARBEID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrevmalInnholdServiceTest {

    @Mock
    private BehandlingService mockBehandlingService;

    @Mock
    private AvklarteVirksomheterService mockAvklarteVirksomheterService;

    private BrevmalInnholdService brevmalInnholdService;

    @BeforeEach
    void init() {
        brevmalInnholdService = new BrevmalInnholdService(mockBehandlingService, mockAvklarteVirksomheterService);
    }

    @Test
    void gittIkkeAvsluttetBehandlingSkalTilgjengeligeBrevmalerReturneres() throws Exception {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(new Behandling());

        List<Produserbaredokumenter> brevMaler = brevmalInnholdService.hentBrevMaler(123L);

        assertEquals(2, brevMaler.size());
        assertTrue(brevMaler.containsAll(asList(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, MANGELBREV_BRUKER)));
    }

    @Test
    void gittRegistrertSkalArbeidsgiverReturneres() throws Exception {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(new Behandling());
        AvklartVirksomhet avklartVirksomhet = new AvklartVirksomhet("Advokatene AS", "123456789", new StrukturertAdresse(), LOENNET_ARBEID);
        when(mockAvklarteVirksomheterService.hentAlleNorskeVirksomheter(any())).thenReturn(singletonList(avklartVirksomhet));

        List<AvklartVirksomhet> arbeidsgivere = brevmalInnholdService.hentArbeidsgivere(123L);

        assertEquals(1, arbeidsgivere.size());
        AvklartVirksomhet virksomhet = arbeidsgivere.get(0);
        assertEquals("Advokatene AS", virksomhet.navn);
        assertEquals("123456789", virksomhet.orgnr);
    }

}