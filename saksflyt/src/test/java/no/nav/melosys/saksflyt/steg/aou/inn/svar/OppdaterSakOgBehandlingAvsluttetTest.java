package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sob.SobService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterSakOgBehandlingAvsluttetTest {

    @Mock
    private SobService sobService;
    @Mock
    private FagsakService fagsakService;

    private OppdaterSakOgBehandlingAvsluttet oppdaterSakOgBehandlingAvsluttet;

    @Before
    public void setup() {
        oppdaterSakOgBehandlingAvsluttet = new OppdaterSakOgBehandlingAvsluttet(fagsakService, sobService);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "22");

        Saksopplysning saksopplysning = new Saksopplysning();
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "99";
        saksopplysning.setDokument(personDokument);
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));

        oppdaterSakOgBehandlingAvsluttet.utfør(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        verify(fagsakService).avsluttFagsakOgBehandling(eq(behandling.getFagsak()), eq(Saksstatuser.LOVVALG_AVKLART));
        verify(sobService).sakOgBehandlingAvsluttet(eq("123"), eq(1L), eq("22"));
    }
}