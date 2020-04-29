package no.nav.melosys.saksflyt.steg.jfr;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;
import no.nav.melosys.service.RegelmodulService;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.dokument.felles.Land.av;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VurderInngangsvilkaarTest {
    @Mock
    private RegelmodulService regelmodulService;
    @Mock
    private FagsakService fagsakService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private VurderInngangsvilkaar agent;

    @Before
    public void setUp() {
        agent = new VurderInngangsvilkaar(regelmodulService, fagsakService);
    }

    @Test
    public void utfoerSteg_funker() throws FunksjonellException, TekniskException {
        // Sett opp input...
        Prosessinstans p = lagProsessinstans();

        // Sett opp regelmodulService til å alltid returnere EØS og ingen feil.
        VurderInngangsvilkaarReply res = new VurderInngangsvilkaarReply();
        res.feilmeldinger = Collections.emptyList();
        res.kvalifisererForEf883_2004 = true;
        when(regelmodulService.vurderInngangsvilkår(anyLong(), any(), any())).thenReturn(res);

        agent.utfør(p);

        verify(fagsakService).oppdaterType(anyString(), eq(true));
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.HENT_ARBF_OPPL);
    }

    @Test
    public void utfoerSteg_feiler() throws FunksjonellException, TekniskException {
        Prosessinstans p = lagProsessinstans();
        when(regelmodulService.vurderInngangsvilkår(anyLong(), any(), any()))
            .thenThrow(new FunksjonellException("Finner ingen informasjon om statsborgerskap"));
        expectedException.expect(FunksjonellException.class);

        agent.utfør(p);

        verify(fagsakService, never()).lagre(any(Fagsak.class));
        assertThat(p.getBehandling().getFagsak().getType()).isNull();
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    public static Prosessinstans lagProsessinstans() {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        p.setBehandling(behandling);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MIN_SAK");
        p.getBehandling().setFagsak(fagsak);

        Saksopplysning sopp = new Saksopplysning();
        sopp.setType(SaksopplysningType.PERSOPL);
        sopp.setDokument(lagPersoppl());
        p.getBehandling().setSaksopplysninger(Collections.singleton(sopp));
        p.setData(ProsessDataKey.SØKNADSLAND, Collections.singletonList(Landkoder.PL.getKode()));
        p.setData(ProsessDataKey.SØKNADSPERIODE, new PeriodeDto(LocalDate.now(), null));
        p.setData(ProsessDataKey.SAKSNUMMER, "1234567");
        return p;
    }

    private static PersonDokument lagPersoppl() {
        PersonDokument pDok = new PersonDokument();
        pDok.statsborgerskap = av("NOR");
        return pDok;
    }
}
