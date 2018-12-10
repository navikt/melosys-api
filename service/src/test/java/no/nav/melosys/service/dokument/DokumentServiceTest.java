package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaType;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DokSysFasade;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.*;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDtoKonverterer;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.BrevDataService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.kodeverk.KodeverkService;

import org.junit.Test;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static no.nav.melosys.domain.avklartefakta.AvklartefaktaType.AG_FORRETNINGSLAND;
import static no.nav.melosys.domain.avklartefakta.AvklartefaktaType.AVKLARTE_ARBEIDSGIVER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public final class DokumentServiceTest {

    private static final long BEHANDLINGSID = 13;
    private static final long GSAKSNUMMER = 321L;

    private static long idTeller = 1;
    private final DokumentService instans;
    private final DokSysFasade dokSysFasade;

    public DokumentServiceTest() throws Exception {
        this.dokSysFasade = mock(DokSysFasade.class);
        this.instans = lagDokumentService(dokSysFasade);
    }

    @Test
    public final void produserInnvilgelsesbrevFunker() throws Exception {
        BrevData brevData = lagBrevDataDto(RolleType.BRUKER);
        instans.produserDokument(BEHANDLINGSID, Dokumenttype.INNVILGELSE_YRKESAKTIV, brevData);
        verify(dokSysFasade).produserIkkeredigerbartDokument(any(DokumentbestillingMetadata.class), any(Object.class));
    }

    @Test
    public final void produserInnvilgelsesbrevutkastFunker() throws Exception {
        OIDCAuthenticationToken auth = new OIDCAuthenticationToken("bruker ikke",
                "Issuer Not", null, null, null, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        BrevbestillingDto brevbestilling = lagBrevBestillingDto(RolleType.BRUKER);
        byte[] resultat = instans.produserUtkast(BEHANDLINGSID, Dokumenttype.INNVILGELSE_YRKESAKTIV, brevbestilling);
        assertThat(resultat).isNull();
        verify(dokSysFasade).produserDokumentutkast(any(DokumentbestillingMetadata.class), any(Object.class));
    }

    @Test
    public final void produserInnvilgelsesbrevMedFullmektigFunker() throws Exception {
        BrevData brevDataDto = lagBrevDataDto(RolleType.REPRESENTANT);
        instans.produserDokument(BEHANDLINGSID, Dokumenttype.INNVILGELSE_YRKESAKTIV, brevDataDto);
    }

    @Test
    public final void produserInnvilgelsesbrevISaksflyt() throws Exception {
        BrevbestillingDto brevbestilling = lagBrevBestillingDto(RolleType.BRUKER);
        instans.produserDokumentISaksflyt(BEHANDLINGSID, Dokumenttype.INNVILGELSE_YRKESAKTIV, brevbestilling);
    }

    private static BrevbestillingDto lagBrevBestillingDto(RolleType rolle) {
        BrevbestillingDto brevbestilling = new BrevbestillingDto();
        brevbestilling.mottaker = rolle;
        return brevbestilling;
    }

    @Test
    public final void produserInnvilgelsesbrevISaksflytUtenBrevdata() throws Exception {
        instans.produserDokumentISaksflyt(BEHANDLINGSID, Dokumenttype.INNVILGELSE_YRKESAKTIV, null);
    }

    @Test
    public final void produserInnvilgelsesbrevISaksflytUtenBehandlingKasterUnntak() throws Exception {
        Throwable unntak = catchThrowable(() -> instans.produserDokumentISaksflyt(~BEHANDLINGSID,
                Dokumenttype.INNVILGELSE_YRKESAKTIV, lagBrevBestillingDto(RolleType.BRUKER)));
        assertThat(unntak).isInstanceOfAny(IkkeFunnetException.class).hasNoCause().hasMessageContaining("finnes ikke");
    }

    @Test
    public final void produserUkjentDokumenttypeISaksflytKasterUnntak() throws Exception {
        Throwable unntak = catchThrowable(() -> instans.produserDokumentISaksflyt(BEHANDLINGSID,
                Dokumenttype.MELDING_HENLAGT_SAK, lagBrevBestillingDto(RolleType.BRUKER)));
        assertThat(unntak).isInstanceOfAny(FunksjonellException.class).hasNoCause().hasMessageContaining("er ikke støttet");
    }

    @Test
    public final void produserDokumentUtenBehandlingKasterUnntak() throws Exception {
        Throwable unntak = catchThrowable(() -> instans.produserDokument(~BEHANDLINGSID, Dokumenttype.ATTEST_A1, lagBrevDataDto(RolleType.ARBEIDSGIVER)));
        assertThat(unntak).isInstanceOf(IkkeFunnetException.class).hasNoCause().hasMessageContaining("finnes ikke");
    }

    @Test
    public final void produserDokumentUtenDokumenttypeKasterUnntak() throws Exception {
        Throwable unntak = catchThrowable(() -> instans.produserDokument(BEHANDLINGSID, null, lagBrevDataDto(RolleType.ARBEIDSGIVER)));
        assertThat(unntak).isInstanceOf(TekniskException.class).hasNoCause().hasMessageContaining("Ingen gyldig");
    }

    @Test
    public final void produserDokumentMedDokumenttypeUtenIdKasterUnntak() throws Exception {
        Throwable unntak = catchThrowable(() -> instans.produserDokument(BEHANDLINGSID, Dokumenttype.MELDING_HENLAGT_SAK,
                lagBrevDataDto(RolleType.ARBEIDSGIVER)));
        assertThat(unntak).isInstanceOf(TekniskException.class)
            .hasNoCause()
            .hasMessageContaining("Fant ikke dokumenttypeId");
    }

    private final BrevData lagBrevDataDto(RolleType mottakerRolle) {
        BrevData brevDataDto = new BrevData();
        brevDataDto.saksbehandler = "Behandler Ei";
        brevDataDto.mottaker = mottakerRolle;
        return brevDataDto;
    }

    private static DokumentService lagDokumentService(DokSysFasade dokSysFasade) throws Exception {
        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(GSAKSNUMMER);
        Aktoer aktør = lagAktør(RolleType.BRUKER);
        Set<Aktoer> aktører = new HashSet<Aktoer>(Arrays.asList(lagAktør(RolleType.BRUKER),
                lagAktør(RolleType.REPRESENTANT)));
        fagsak.setAktører(aktører);
        fagsak.setType(Fagsakstype.EU_EØS);
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstype.KLAGE);
        behandling.setId(BEHANDLINGSID);
        when(behandlingRepository.findOne(eq(BEHANDLINGSID))).thenReturn(behandling);
        when(behandlingRepository.findOneWithSaksopplysningerById(eq(BEHANDLINGSID))).thenReturn(behandling);
        FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        TpsFasade tpsFasade = mock(TpsFasade.class);
        when(tpsFasade.hentIdentForAktørId(eq(aktør.getAktørId())))
            .thenReturn(String.format("IDENT%s", aktør.getAktørId()));
        BehandlingsresultatRepository behandlingsresultatRepository = mock(BehandlingsresultatRepository.class);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        List<Avklartefakta> faktaliste = Arrays.asList(lagAvklarteFakta(AVKLARTE_ARBEIDSGIVER, "Lorum"),
                lagAvklarteFakta(AG_FORRETNINGSLAND, "SE"));
        behandlingsresultat.setAvklartefakta(new HashSet<>(faktaliste));
        Lovvalgsperiode periode = new Lovvalgsperiode();
        periode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1);
        periode.setFom(LocalDate.now());
        periode.setTom(LocalDate.now());
        List<Lovvalgsperiode> perioder = Arrays.asList(periode);
        behandlingsresultat.setLovvalgsperioder(new HashSet<>(perioder));
        when(behandlingsresultatRepository.findOne(BEHANDLINGSID)).thenReturn(behandlingsresultat);
        BrevDataService brevDataService = new BrevDataService(tpsFasade, behandlingsresultatRepository);
        JoarkFasade joarkFasade = mock(JoarkFasade.class);
        Binge binge = mock(Binge.class);
        ProsessinstansRepository prosessinstansRepo = mock(ProsessinstansRepository.class);
        AvklarteFaktaRepository avklarteFaktaRepository = mock(AvklarteFaktaRepository.class);
        AvklartefaktaDtoKonverterer faktaKonverterer = new AvklartefaktaDtoKonverterer();
        AvklartefaktaService avklartefaktaService = new AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, faktaKonverterer);
        EregFasade eregFasade = mock(EregFasade.class);
        RegisterOppslagSystemService registerOppslagService = new RegisterOppslagSystemService(eregFasade, tpsFasade);
        KodeverkRegister kodeverkRegister = mock(KodeverkRegister.class);
        KodeverkService kodeverkService = new KodeverkService(kodeverkRegister);
        BrevDataByggerVelger brevdatabyggervelger = new BrevDataByggerVelger(avklartefaktaService, registerOppslagService, kodeverkService);
        return new DokumentService(behandlingRepository, fagsakRepository,
                brevDataService, dokSysFasade, joarkFasade, binge,
                prosessinstansRepo, brevdatabyggervelger);
    }

    private static Avklartefakta lagAvklarteFakta(AvklartefaktaType type, String subjekt) {
        Avklartefakta arbeidsgiverFaktum = new Avklartefakta();
        arbeidsgiverFaktum.setSubjekt(subjekt);
        arbeidsgiverFaktum.setType(type);
        arbeidsgiverFaktum.setFakta("TRUE");
        return arbeidsgiverFaktum;
    }

    private static Aktoer lagAktør(RolleType type) {
        Aktoer aktør = new Aktoer();
        aktør.setAktørId(type.name() + idTeller++);
        aktør.setAktørId("123");
        aktør.setRolle(type);
        return aktør;
    }

}
