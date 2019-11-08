package no.nav.melosys.saksflyt.steg.jfr;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.domain.dokument.person.StatsborgerskapPeriode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.regler.api.lovvalg.rep.Feilmelding;
import no.nav.melosys.regler.api.lovvalg.rep.Kategori;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.RegelmodulService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VurderInngangsvilkaarTest {
    @Mock
    private RegelmodulService regelmodulService;
    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private SaksopplysningerService saksopplysningerService;

    private VurderInngangsvilkaar agent;

    @Before
    public void setUp() {
        agent = new VurderInngangsvilkaar(regelmodulService, fagsakRepository, saksopplysningerService);
    }

    @Test
    public void utfoerSteg_funker() throws IkkeFunnetException {
        // Sett opp input...
        Prosessinstans p = lagProsessinstans();

        // Sett opp regelmodulService til å alltid returnere EØS og ingen feil.
        VurderInngangsvilkaarReply res = new VurderInngangsvilkaarReply();
        res.feilmeldinger = Collections.emptyList();
        res.kvalifisererForEf883_2004 = true;
        when(regelmodulService.vurderInngangsvilkår(any(), any(), any())).thenReturn(res);
        when(saksopplysningerService.hentPersonOpplysninger(anyLong())).thenReturn(lagPersoppl());

        agent.utførSteg(p);

        verify(fagsakRepository).save(any(Fagsak.class));

        assertNull(p.getHendelser());
        assertEquals(Sakstyper.EU_EOS, p.getBehandling().getFagsak().getType());
        assertEquals(ProsessSteg.HENT_ARBF_OPPL, p.getSteg());
    }

    @Test
    public void utfoerSteg_feiler() throws IkkeFunnetException {
        // Sett opp input...
        Prosessinstans p = lagProsessinstans();

        // Sett opp regelmodulService til å alltid returnere feil.
        Feilmelding fm = new Feilmelding();
        fm.kategori = Kategori.TEKNISK_FEIL;
        fm.melding = "YOU SHALL NOT PASS!";
        VurderInngangsvilkaarReply res = new VurderInngangsvilkaarReply();
        res.feilmeldinger = Collections.singletonList(fm);
        when(regelmodulService.vurderInngangsvilkår(any(), any(), any())).thenReturn(res);
        when(saksopplysningerService.hentPersonOpplysninger(anyLong())).thenReturn(lagPersoppl());

        agent.utførSteg(p);

        verify(fagsakRepository, never()).save(any(Fagsak.class));

        assertEquals(2, p.getHendelser().size());
        assertNull(p.getBehandling().getFagsak().getType());
        assertEquals(ProsessSteg.FEILET_MASKINELT, p.getSteg());
    }

    @Test
    public void utfoerStegMedHistorikkStatsborgerskap() throws IkkeFunnetException {
        // Sett opp input...
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(2L);
        p.setBehandling(behandling);
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.UKJENT);
        p.getBehandling().setFagsak(fagsak);
        PersonDokument pDok = new PersonDokument();
        pDok.statsborgerskap = new Land("NOR");
        Saksopplysning sopp = new Saksopplysning();
        sopp.setType(SaksopplysningType.PERSOPL);
        sopp.setDokument(pDok);

        Saksopplysning historiskSopp = new Saksopplysning();
        historiskSopp.setType(SaksopplysningType.PERSHIST);
        PersonhistorikkDokument personhistorikkDokument = new PersonhistorikkDokument();
        historiskSopp.setDokument(personhistorikkDokument);
        StatsborgerskapPeriode statsborgerskapPeriode = new StatsborgerskapPeriode();
        statsborgerskapPeriode.statsborgerskap = new Land("IRL");
        personhistorikkDokument.statsborgerskapListe.add(statsborgerskapPeriode);

        Set<Saksopplysning> saksopplysninger = new HashSet<>();
        saksopplysninger.add(sopp);
        saksopplysninger.add(historiskSopp);

        p.getBehandling().setSaksopplysninger(saksopplysninger);
        p.setData(ProsessDataKey.SØKNADSLAND, Collections.singletonList(Landkoder.CH.getKode()));
        p.setData(ProsessDataKey.SØKNADSPERIODE, new PeriodeDto(LocalDate.now().minusMonths(6), null));

        // Sett opp regelmodulService til å alltid returnere EØS og ingen feil.
        VurderInngangsvilkaarReply res = new VurderInngangsvilkaarReply();
        res.feilmeldinger = Collections.emptyList();
        res.kvalifisererForEf883_2004 = true;
        when(regelmodulService.vurderInngangsvilkår(any(), any(), any())).thenReturn(res);
        when(saksopplysningerService.hentPersonhistorikk(anyLong())).thenReturn(personhistorikkDokument);

        agent.utførSteg(p);

        verify(fagsakRepository).save(any(Fagsak.class));

        assertNull(p.getHendelser());
        assertEquals(Sakstyper.EU_EOS, p.getBehandling().getFagsak().getType());
        assertEquals(ProsessSteg.HENT_ARBF_OPPL, p.getSteg());
    }

    public static Prosessinstans lagProsessinstans() {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        p.setBehandling(behandling);
        p.getBehandling().setFagsak(new Fagsak());

        Saksopplysning sopp = new Saksopplysning();
        sopp.setType(SaksopplysningType.PERSOPL);
        sopp.setDokument(lagPersoppl());
        p.getBehandling().setSaksopplysninger(Collections.singleton(sopp));
        p.setData(ProsessDataKey.SØKNADSLAND, Collections.singletonList(Landkoder.PL.getKode()));
        p.setData(ProsessDataKey.SØKNADSPERIODE, new PeriodeDto(LocalDate.now(), null));
        return p;
    }

    private static PersonDokument lagPersoppl() {
        PersonDokument pDok = new PersonDokument();
        pDok.statsborgerskap = new Land("NOR");
        return pDok;
    }

}
