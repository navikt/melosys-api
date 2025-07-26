package no.nav.melosys.saksflyt.steg.brev;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestBuilder;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.ForvaltningsmeldingMottaker;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Mottakerroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendForvaltningsmeldingTest {

    @Mock
    private BrevBestiller brevBestiller;
    @Mock
    private BehandlingService behandlingService;

    private SendForvaltningsmelding sendForvaltningsmelding;

    private static final String ANNEN_PERSON_IDENT = "21075114491";
    private static final String ANNEN_ORG_NR = "999999999";

    @BeforeEach
    public void setUp() {
        sendForvaltningsmelding = new SendForvaltningsmelding(brevBestiller, behandlingService);
    }

    @Test
    void utfør_forvaltningsMeldingMottakerErBruker_bestillerForvaltningsmelding() {
        final long behandlingID = 21432L;
        var behandling = BehandlingTestBuilder.builderWithDefaults()
            .medId(behandlingID)
            .build();
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        var prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER, ForvaltningsmeldingMottaker.BRUKER);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, "TEST");


        sendForvaltningsmelding.utfør(prosessinstans);


        verify(behandlingService).hentBehandlingMedSaksopplysninger(behandlingID);
        verify(brevBestiller).bestill(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, List.of(Mottaker.medRolle(BRUKER)), null, "TEST", null, behandling);
    }

    @Test
    void utfør_forvaltningsMeldingMottakerErAvsenderAvsenderErAnnenPerson_bestillerForvaltningsmelding() {
        final long behandlingID = 21432L;
        var behandling = BehandlingTestBuilder.builderWithDefaults()
            .medId(behandlingID)
            .build();
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        var prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER, ForvaltningsmeldingMottaker.AVSENDER);
        prosessinstans.setData(ProsessDataKey.AVSENDER_ID, ANNEN_PERSON_IDENT);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, "TEST");


        sendForvaltningsmelding.utfør(prosessinstans);

        Mottaker forventetMottaker = Mottaker.medRolle(ANNEN_PERSON);
        forventetMottaker.setPersonIdent(ANNEN_PERSON_IDENT);
        verify(behandlingService).hentBehandlingMedSaksopplysninger(behandlingID);
        verify(brevBestiller).bestill(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, List.of(forventetMottaker), null, "TEST", null, behandling);
    }

    @Test
    void utfør_forvaltningsMeldingMottakerErAvsenderAvsenderErAnnenOrganisasjon_bestillerForvaltningsmelding() {
        final long behandlingID = 21432L;
        var behandling = BehandlingTestBuilder.builderWithDefaults()
            .medId(behandlingID)
            .build();
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);
        var prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER, ForvaltningsmeldingMottaker.AVSENDER);
        prosessinstans.setData(ProsessDataKey.AVSENDER_ID, ANNEN_ORG_NR);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, "TEST");


        sendForvaltningsmelding.utfør(prosessinstans);

        Mottaker forventetMottaker = Mottaker.medRolle(ANNEN_ORGANISASJON);
        forventetMottaker.setOrgnr(ANNEN_ORG_NR);
        verify(behandlingService).hentBehandlingMedSaksopplysninger(behandlingID);
        verify(brevBestiller).bestill(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, List.of(forventetMottaker), null, "TEST", null, behandling);
    }

    @Test
    void utfør_skalIkkeSendeForvaltningsmelding_senderIkke() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        prosessinstans.setData(ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER, ForvaltningsmeldingMottaker.INGEN);

        sendForvaltningsmelding.utfør(prosessinstans);
        verify(brevBestiller, never()).bestill(any());
    }
}
