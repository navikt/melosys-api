package no.nav.melosys.saksflyt.agent.jfr;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.regler.api.lovvalg.rep.Feilmelding;
import no.nav.melosys.regler.api.lovvalg.rep.Kategori;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.service.RegelmodulService;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VurderInngangsvilkaarTest {

    @Mock
    private Binge binge;

    @Mock
    private ProsessinstansRepository repo;

    @Mock
    private RegelmodulService regelmodulService;
    
    @Mock
    private FagsakService fagsakService;

    private VurderInngangsvilkaar agent;

    @Before
    public void setUp() {
        agent = new VurderInngangsvilkaar(binge, repo, regelmodulService, fagsakService);
    }

    @Test
    public void utfoerStegUtenFeil() {
        // Sett opp input...
        Prosessinstans p = lagProsessinstans();
        
        // Sett opp regelmodulService til å alltid returnere EØS og ingen feil.
        VurderInngangsvilkaarReply res = new VurderInngangsvilkaarReply();
        res.feilmeldinger = Collections.emptyList();
        res.kvalifisererForEf883_2004 = true;
        when(regelmodulService.vurderInngangsvilkår(any(), any(), any())).thenReturn(res);
        
        agent.utførSteg(p);

        verify(fagsakService, times(1)).lagre(any());
        
        assertEquals(null, p.getHendelser());
        assertEquals(FagsakType.EU_EØS, p.getBehandling().getFagsak().getType());
        assertEquals(ProsessSteg.JFR_OPPRETT_OPPGAVE, p.getSteg());
    }

    @Test
    public void utfoerStegMedFeil() {
        // Sett opp input...
        Prosessinstans p = lagProsessinstans();

        // Sett opp regelmodulService til å alltid returnere feil.
        Feilmelding fm = new Feilmelding();
        fm.kategori = Kategori.TEKNISK_FEIL;
        fm.melding = "YOU SHALL NOT PASS!";
        VurderInngangsvilkaarReply res = new VurderInngangsvilkaarReply();
        res.feilmeldinger = Collections.singletonList(fm);
        when(regelmodulService.vurderInngangsvilkår(any(), any(), any())).thenReturn(res);
        
        agent.utførSteg(p);

        verify(repo, times(1)).save(any(Prosessinstans.class));
        verify(fagsakService, times(0)).lagre(any());
        
        assertEquals(1, p.getHendelser().size());
        assertEquals(null, p.getBehandling().getFagsak().getType());
        assertEquals(ProsessSteg.FEILET_MASKINELT, p.getSteg());
    }

    public static Prosessinstans lagProsessinstans() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setFagsak(new Fagsak());
        PersonDokument pDok = new PersonDokument();
        pDok.statsborgerskap = new Land("NOR");
        Saksopplysning sopp = new Saksopplysning();
        sopp.setDokument(pDok);
        p.getBehandling().setSaksopplysninger(Collections.singleton(sopp));
        p.setData(ProsessDataKey.LAND, Collections.singletonList("POL"));
        p.setData(ProsessDataKey.SØKNADSPERIODE, new PeriodeDto(LocalDate.now(), null));
        return p;
    }

}
