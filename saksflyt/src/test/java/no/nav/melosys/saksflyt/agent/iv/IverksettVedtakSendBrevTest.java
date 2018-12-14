package no.nav.melosys.saksflyt.agent.iv;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.integrasjon.doksys.DokSysFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.*;
import org.junit.Test;

import static no.nav.melosys.domain.ProsessSteg.IV_AVSLUTT_BEHANDLING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IverksettVedtakSendBrevTest {

    private final IverksettVedtakSendBrev agent;
    private static final long BEHANDLINGSID = 42L;
    private static final long BEHANDLINGSID_MED_FLERE_PERIODER = 43L;
    private static final long INNVILGET_BEHANDLINGSID = 44L;
    private static final long BEHANDLINGSID_UTENLANDSK_LOVVALG = 45L;
    private static final long BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE = 46L;
    private static final long INNVILGET_BEHANDLINGSID_12_1 = 47L;
    private static final long INNVILGET_BEHANDLINGSID_12_2 = 48L;

    public IverksettVedtakSendBrevTest() throws Exception {
        agent = lagStegbehandler(lagBehandling(INNVILGET_BEHANDLINGSID));
    }

    private static IverksettVedtakSendBrev lagStegbehandler(Behandling behandling) throws Exception {
        BehandlingsresultatRepository behandlingsresultatRepo = mockBehandlingsresultatRepository();
        BrevData brevdata = new BrevDataA1("Z123456");
        BrevDataByggerA1 brevDataByggerA1 = mock(BrevDataByggerA1.class);
        when(brevDataByggerA1.lag(any(), any())).thenReturn(brevdata);

        BrevDataByggerVelger byggerVelger = mock(BrevDataByggerVelger.class);
        when(byggerVelger.hent(any())).thenReturn(brevDataByggerA1);

        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
        when(behandlingRepository.findOneWithSaksopplysningerById(eq(behandling.getId()))).thenReturn(behandling);

        DokumentSystemService dokService = lagDokumentService(byggerVelger);
        return new IverksettVedtakSendBrev(dokService, byggerVelger, behandlingRepository, behandlingsresultatRepo);
    }

    private static BehandlingRepository mockBehandlingRepository() {
        Fagsak fagsak = lagFagsak();
        Behandling behandling = lagBehandling(fagsak);
        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
        when(behandlingRepository.findOne(or(or(eq(INNVILGET_BEHANDLINGSID),
                eq(INNVILGET_BEHANDLINGSID_12_1)),
                eq(INNVILGET_BEHANDLINGSID_12_2))))
                    .thenReturn(behandling);
        return behandlingRepository;
    }

    private static DokumentSystemService lagDokumentService(BrevDataByggerVelger brevDataByggerVelger) {
        BehandlingRepository behandlingRepository = mockBehandlingRepository();
        FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        BrevDataService brevDataService = mock(BrevDataService.class);
        DokSysFasade dokSysFasade = mock(DokSysFasade.class);
        JoarkFasade joarkFasade = mock(JoarkFasade.class);
        return new DokumentSystemService(behandlingRepository, fagsakRepository,
                brevDataService, dokSysFasade, joarkFasade, brevDataByggerVelger);
    }

    private static BehandlingsresultatRepository mockBehandlingsresultatRepository() {
        BehandlingsresultatRepository behandlingsresultatRepo = mock(BehandlingsresultatRepository.class);
        Lovvalgsperiode periode = lagLovvalgsperiode();
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat(periode, Landkoder.AT, BehandlingsresultatType.IKKE_FASTSATT);
        when(behandlingsresultatRepo.findOne(BEHANDLINGSID)).thenReturn(behandlingsresultat);
        Lovvalgsperiode periode2 = lagLovvalgsperiode(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1, LocalDate.now().plusDays(30));
        Behandlingsresultat behandlingsresultatMedFlerePerioder = lagBehandlingsresultat(new HashSet<Lovvalgsperiode>(Arrays.asList(periode, periode2)), null, BehandlingsresultatType.FASTSATT_LOVVALGSLAND);
        assertThat(behandlingsresultatMedFlerePerioder.getLovvalgsperioder().size()).isGreaterThan(1);
        when(behandlingsresultatRepo.findOne(BEHANDLINGSID_MED_FLERE_PERIODER)).thenReturn(behandlingsresultatMedFlerePerioder);
        Behandlingsresultat innvilgetBehandlingsResultat = lagBehandlingsresultat(periode, Landkoder.NO);
        when(behandlingsresultatRepo.findOne(INNVILGET_BEHANDLINGSID)).thenReturn(innvilgetBehandlingsResultat);
        Behandlingsresultat innvilgetResultat12_1 = lagBehandlingsresultat(lagLovvalgsperiode(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1), Landkoder.NO);
        when(behandlingsresultatRepo.findOne(INNVILGET_BEHANDLINGSID_12_1)).thenReturn(innvilgetResultat12_1);
        Behandlingsresultat innvilgetResultat12_2 = lagBehandlingsresultat(lagLovvalgsperiode(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_2), Landkoder.NO);
        when(behandlingsresultatRepo.findOne(INNVILGET_BEHANDLINGSID_12_2)).thenReturn(innvilgetResultat12_2);
        Behandlingsresultat utenlandskLovvalgResultat = lagBehandlingsresultat(periode, Landkoder.BE);
        when(behandlingsresultatRepo.findOne(BEHANDLINGSID_UTENLANDSK_LOVVALG)).thenReturn(utenlandskLovvalgResultat);
        Behandlingsresultat norskLovvalgUtenInnvilgetBestemmelse = lagBehandlingsresultat(lagLovvalgsperiode(LovvalgBestemmelse_883_2004.FO_883_2004_ANNET), Landkoder.NO);
        when(behandlingsresultatRepo.findOne(BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE)).thenReturn(norskLovvalgUtenInnvilgetBestemmelse);
        return behandlingsresultatRepo;
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstype.SØKNAD);
        return behandling;
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(1234L);
        fagsak.setType(Fagsakstype.EU_EØS);
        Aktoer aktør = new Aktoer();
        aktør.setAktørId("1");
        aktør.setRolle(RolleType.BRUKER);
        fagsak.setAktører(Collections.singleton(aktør));
        return fagsak;
    }

    private static Lovvalgsperiode lagLovvalgsperiode() {
        return lagLovvalgsperiode(LovvalgBestemmelse_883_2004.FO_883_2004_ART16_1, LocalDate.now());
    }

    private static Lovvalgsperiode lagLovvalgsperiode(LovvalgBestemmelse_883_2004 bestemmelse) {
        return lagLovvalgsperiode(bestemmelse, LocalDate.now());
    }

    private static Lovvalgsperiode lagLovvalgsperiode(LovvalgBestemmelse_883_2004 bestemmelse, LocalDate fom) {
        Lovvalgsperiode periode = new Lovvalgsperiode();
        periode.setFom(fom);
        periode.setTom(fom.plusDays(1));
        periode.setLovvalgsland(Landkoder.AT);
        periode.setBestemmelse(bestemmelse);
        return periode;
    }

    private static Behandlingsresultat lagBehandlingsresultat(Lovvalgsperiode periode, Landkoder land) {
        return lagBehandlingsresultat(Collections.singleton(periode), land, BehandlingsresultatType.FASTSATT_LOVVALGSLAND);
    }

    private static Behandlingsresultat lagBehandlingsresultat(Lovvalgsperiode periode, Landkoder land, BehandlingsresultatType type) {
        return lagBehandlingsresultat(Collections.singleton(periode), land, type);
    }

    private static Behandlingsresultat lagBehandlingsresultat(Set<Lovvalgsperiode> perioder, Landkoder land, BehandlingsresultatType type) {
        Behandlingsresultat utenlandskLovvalgResultat = new Behandlingsresultat();
        utenlandskLovvalgResultat.setLovvalgsperioder(perioder);
        utenlandskLovvalgResultat.setType(type);
        utenlandskLovvalgResultat.setFastsattAvLand(land);
        return utenlandskLovvalgResultat;
    }

    @Test
    public void utfoerSteg() throws Exception {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setId(BEHANDLINGSID);
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);
        Properties properties = new Properties();
        p.addData(properties);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(BEHANDLINGSID));
        instans.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(IV_AVSLUTT_BEHANDLING);
    }

    @Test
    public final void utførStegIIverksettVedtakMedFlereLovvalgsperioderGirUnntak() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_MED_FLERE_PERIODER);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(BEHANDLINGSID_MED_FLERE_PERIODER));
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public final void utførStegPåInnvilgelsesBrevBestemtAv16_1GårTilGsakAvsluttOppgave() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(INNVILGET_BEHANDLINGSID);
        agent.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_AVSLUTT_BEHANDLING);
    }

    @Test
    public final void utførStegPåInnvilgelsesBrevBestemtAv12_1GårTilGsakAvsluttOppgave() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(INNVILGET_BEHANDLINGSID_12_1);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(INNVILGET_BEHANDLINGSID_12_1));
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_AVSLUTT_BEHANDLING);
    }

    @Test
    public final void utførStegPåInnvilgelsesBrevBestemtAv12_2GårTilGsakAvsluttOppgave() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(INNVILGET_BEHANDLINGSID_12_2);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(INNVILGET_BEHANDLINGSID_12_2));
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_AVSLUTT_BEHANDLING);
    }

    @Test
    public final void utførStegPåFastsattLovvalgIUtlandetGårTilGsakAvsluttOppgave() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_UTENLANDSK_LOVVALG);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(BEHANDLINGSID_UTENLANDSK_LOVVALG));
        instans.utførSteg(prosessinstans);        
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_AVSLUTT_BEHANDLING);
    }

    @Test
    public final void utførStegPåFastsattLovvalgINorgeUtenInnvilgetBestemmelseGårTilGsakAvsluttOppgave() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE));
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_AVSLUTT_BEHANDLING);
    }

    @Test
    public final void utførStegPåUkjentProsesstypeFeiler() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(INNVILGET_BEHANDLINGSID, ProsessType.JFR_KNYTT);
        agent.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public final void utførStegPåIkkeEksisterendeBehandlingFeiler() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(~INNVILGET_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE));
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);

    }

    private static Prosessinstans lagProsessinstans(long behandlingsid) {
        return lagProsessinstans(behandlingsid, ProsessType.IVERKSETT_VEDTAK);
    }

    private static Prosessinstans lagProsessinstans(long behandlingsid, ProsessType type) {
        Prosessinstans resultat = new Prosessinstans();
        Behandling behandling = lagBehandling(behandlingsid);
        resultat.setBehandling(behandling);
        resultat.setType(type);
        BrevData brevdata = new BrevData();
        brevdata.mottaker = RolleType.BRUKER;
        resultat.setData(ProsessDataKey.BREVDATA, brevdata);
        return resultat;
    }

    private static Behandling lagBehandling(long behandlingsid) {
        Behandling behandling = new Behandling();
        behandling.setId(behandlingsid);
        behandling.setType(Behandlingstype.SØKNAD);
        return behandling;
    }
}