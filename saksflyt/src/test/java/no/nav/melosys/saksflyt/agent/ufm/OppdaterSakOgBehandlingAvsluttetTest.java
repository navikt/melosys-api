package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterSakOgBehandlingAvsluttetTest {

    @Mock
    private SakOgBehandlingFasade sakOgBehandlingFasade;
    @Mock
    private TpsService tpsService;
    @Mock
    private SaksopplysningRepository saksopplysningRepository;

    private OppdaterSakOgBehandlingAvsluttet oppdaterSakOgBehandlingAvsluttet;

    @Before
    public void setup() {
        oppdaterSakOgBehandlingAvsluttet = new OppdaterSakOgBehandlingAvsluttet(sakOgBehandlingFasade, tpsService, saksopplysningRepository);
    }

    @Test
    public void utfør_medAktørId_ingenKallMotTps() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);

        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "22");

        oppdaterSakOgBehandlingAvsluttet.utfør(prosessinstans);

        verify(sakOgBehandlingFasade).sendBehandlingAvsluttet(any());
    }

    @Test
    public void utfør_utenAktørId_forventKallMotTpsForAktørId() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);

        prosessinstans.setBehandling(behandling);

        Saksopplysning saksopplysning = new Saksopplysning();
        PersonDokument personDokument = new PersonDokument();
        personDokument.fnr = "99";
        saksopplysning.setDokument(personDokument);

        when(saksopplysningRepository.findByBehandlingAndType(any(), eq(SaksopplysningType.PERSOPL)))
            .thenReturn(Optional.of(saksopplysning));

        when(tpsService.hentAktørIdForIdent(anyString())).thenReturn("333");
        oppdaterSakOgBehandlingAvsluttet.utfør(prosessinstans);

        verify(sakOgBehandlingFasade).sendBehandlingAvsluttet(any());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }
}