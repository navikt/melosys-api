package no.nav.melosys.service.dokument;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaType;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.JuridiskArbeidsgiverNorge;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.doksys.DokSysFasade;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.*;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDtoKonverterer;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.*;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Test;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.springframework.security.authentication.Pac4jAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static no.nav.melosys.domain.avklartefakta.AvklartefaktaType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public final class DokumentServiceTest {

    private static final long BEHANDLINGSID = 13;
    private static final long GSAKSNUMMER = 321L;
    private static final String ORGNR = "123456789";

    private static long idTeller = 1;
    private final DokumentService instans;
    private final DokSysFasade dokSysFasade;

    public DokumentServiceTest() throws Exception {
        this.dokSysFasade = mock(DokSysFasade.class);
        this.instans = lagDokumentService(dokSysFasade);
    }

    @Test
    public final void produserInnvilgelsesbrevFunker() throws Exception {
        BrevData brevData = lagBrevData(RolleType.BRUKER);
        instans.produserDokument(BEHANDLINGSID, ProduserbartDokument.INNVILGELSE_YRKESAKTIV, brevData);
        verify(dokSysFasade).produserIkkeredigerbartDokument(any(DokumentbestillingMetadata.class), any(Object.class));
    }

    @Test
    public final void produserInnvilgelsesbrevutkastFunker() throws Exception {
        OidcProfile oidcProfile = mock(OidcProfile.class);
        when(oidcProfile.getSubject()).thenReturn("testbruker");
        Pac4jAuthenticationToken auth = new Pac4jAuthenticationToken(Collections.singletonList(oidcProfile));
        SecurityContextHolder.getContext().setAuthentication(auth);
        BrevbestillingDto brevbestilling = lagBrevBestillingDto(RolleType.BRUKER);
        byte[] resultat = instans.produserUtkast(BEHANDLINGSID, ProduserbartDokument.INNVILGELSE_YRKESAKTIV, brevbestilling);
        assertThat(resultat).isNull();
        verify(dokSysFasade).produserDokumentutkast(any(DokumentbestillingMetadata.class), any(Object.class));
    }

    @Test
    public final void produserInnvilgelsesbrevMedFullmektigFunker() throws Exception {
        BrevData brevDataDto = lagBrevData(RolleType.REPRESENTANT);
        instans.produserDokument(BEHANDLINGSID, ProduserbartDokument.INNVILGELSE_YRKESAKTIV, brevDataDto);
    }

    @Test
    public final void produserMangelbrevISaksflyt() throws Exception {
        BrevbestillingDto brevbestilling = lagBrevBestillingDto(RolleType.BRUKER);
        instans.produserDokumentISaksflyt(BEHANDLINGSID, ProduserbartDokument.MELDING_MANGLENDE_OPPLYSNINGER, brevbestilling);
    }

    private static BrevbestillingDto lagBrevBestillingDto(RolleType rolle) {
        BrevbestillingDto brevbestilling = new BrevbestillingDto();
        brevbestilling.mottaker = rolle;
        return brevbestilling;
    }

    @Test
    public final void produserMangelbrevISaksflytUtenBrevdata() throws Exception {
        instans.produserDokumentISaksflyt(BEHANDLINGSID, ProduserbartDokument.MELDING_MANGLENDE_OPPLYSNINGER, null);
    }

    @Test
    public final void produserInnvilgelsesbrevISaksflytUtenBehandlingKasterUnntak() throws Exception {
        Throwable unntak = catchThrowable(() -> instans.produserDokumentISaksflyt(~BEHANDLINGSID,
                ProduserbartDokument.INNVILGELSE_YRKESAKTIV, lagBrevBestillingDto(RolleType.BRUKER)));
        assertThat(unntak).isInstanceOfAny(IkkeFunnetException.class).hasNoCause().hasMessageContaining("finnes ikke");
    }

    @Test
    public final void produserUkjentDokumenttypeISaksflytKasterUnntak() throws Exception {
        Throwable unntak = catchThrowable(() -> instans.produserDokumentISaksflyt(BEHANDLINGSID,
                ProduserbartDokument.MELDING_HENLAGT_SAK, lagBrevBestillingDto(RolleType.BRUKER)));
        assertThat(unntak).isInstanceOfAny(FunksjonellException.class).hasNoCause().hasMessageContaining("er ikke støttet");
    }

    @Test
    public final void produserDokumentUtenBehandlingKasterUnntak() throws Exception {
        Throwable unntak = catchThrowable(() -> instans.produserDokument(~BEHANDLINGSID, ProduserbartDokument.ATTEST_A1, lagBrevData(RolleType.ARBEIDSGIVER)));
        assertThat(unntak).isInstanceOf(IkkeFunnetException.class).hasNoCause().hasMessageContaining("finnes ikke");
    }

    @Test
    public final void produserDokumentUtenDokumenttypeKasterUnntak() throws Exception {
        Throwable unntak = catchThrowable(() -> instans.produserDokument(BEHANDLINGSID, null, lagBrevData(RolleType.ARBEIDSGIVER)));
        assertThat(unntak).isInstanceOf(IllegalArgumentException.class).hasNoCause().hasMessageContaining("Ingen gyldig");
    }

    private final BrevData lagBrevData(RolleType mottakerRolle) {
        BrevDataA1 brevData = new BrevDataA1();
        Virksomhet arbeidsgiver = new Virksomhet("Virker av og til", "987654321", null);
        brevData.norskeVirksomheter = Collections.singletonList(arbeidsgiver);

        BrevDataVedlegg vedlegg = new BrevDataVedlegg("Saksbehandler");
        vedlegg.mottaker = mottakerRolle;
        vedlegg.brevDataA1 = brevData;
        return vedlegg;
    }

    private static DokumentService lagDokumentService(DokSysFasade dokSysFasade) throws Exception {
        Aktoer aktør = lagAktør(RolleType.BRUKER);
        Behandling behandling = lagBehandling();
        BehandlingRepository behandlingRepository = mockBehandlingRepository(behandling);
        TpsFasade tpsFasade = mockTpsFasade(aktør);
        Avklartefakta arbeidsgiverFaktum = lagAvklarteFakta(AVKLARTE_ARBEIDSGIVER, ORGNR);
        Avklartefakta yrkesgruppeFaktum = lagAvklarteFakta(YRKESGRUPPE, AvklartYrkesgruppeType.ORDINAER.name(), null);
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat(Arrays.asList(arbeidsgiverFaktum,
                lagAvklarteFakta(AG_FORRETNINGSLAND, "SE"),
                yrkesgruppeFaktum));
        BehandlingsresultatRepository behandlingsresultatRepository = mockBehandlingsresultatRepo(behandlingsresultat);
        AvklarteFaktaRepository avklarteFaktaRepository = mockAvklarteFaktaRepository(arbeidsgiverFaktum, yrkesgruppeFaktum);
        BrevDataByggerVelger brevdatabyggervelger = lagBrevdataByggerVelger(tpsFasade, avklarteFaktaRepository, behandlingsresultatRepository);
        BrevDataService brevDataService = new BrevDataService(tpsFasade, behandlingsresultatRepository);
        return new DokumentService(behandlingRepository, mock(FagsakRepository.class), brevDataService, dokSysFasade, mock(JoarkFasade.class), mock(Binge.class),
                mock(ProsessinstansRepository.class), brevdatabyggervelger);
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(GSAKSNUMMER);
        Set<Aktoer> aktører = new HashSet<Aktoer>(Arrays.asList(lagAktør(RolleType.BRUKER),
                lagAktør(RolleType.REPRESENTANT)));
        fagsak.setAktører(aktører);
        fagsak.setType(Fagsakstype.EU_EØS);
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstype.KLAGE);
        behandling.setId(BEHANDLINGSID);
        SoeknadDokument dok = new SoeknadDokument();
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = "12345678910";
        dok.foretakUtland.add(foretakUtland);
        dok.juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
        dok.juridiskArbeidsgiverNorge.ekstraArbeidsgivere = Collections.singletonList(ORGNR);
        Saksopplysning søknad = lagSaksopplysning(SaksopplysningType.SØKNAD, dok);
        Saksopplysning personopplysninger = lagSaksopplysning(SaksopplysningType.PERSONOPPLYSNING, new PersonDokument());
        behandling.setSaksopplysninger(new HashSet<>(Arrays.asList(søknad, personopplysninger)));
        return behandling;
    }

    private static AvklarteFaktaRepository mockAvklarteFaktaRepository(Avklartefakta arbeidsgiverFaktum, Avklartefakta yrkesgruppeFaktum) {
        AvklarteFaktaRepository avklarteFaktaRepository = mock(AvklarteFaktaRepository.class);
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndType(BEHANDLINGSID, YRKESGRUPPE)).thenReturn(Optional.of(yrkesgruppeFaktum));
        when(avklarteFaktaRepository.findByBehandlingsresultatIdAndTypeAndFakta(BEHANDLINGSID,
                AVKLARTE_ARBEIDSGIVER,
                "TRUE")).thenReturn(Collections.singleton(arbeidsgiverFaktum));
        return avklarteFaktaRepository;
    }

    private static BrevDataByggerVelger lagBrevdataByggerVelger(TpsFasade tpsFasade, AvklarteFaktaRepository avklarteFaktaRepository, BehandlingsresultatRepository behandlingsresultatRepository)
            throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        AvklartefaktaDtoKonverterer faktaKonverterer = new AvklartefaktaDtoKonverterer();
        AvklartefaktaService avklartefaktaService = new AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, faktaKonverterer);
        EregFasade eregFasade = mock(EregFasade.class);
        OrganisasjonDokument orgDok = new OrganisasjonDokument();
        orgDok.setNavn(Collections.singletonList("Virker av og til"));
        OrganisasjonsDetaljer organisasjonDetaljer = new OrganisasjonsDetaljer();
        SemistrukturertAdresse adresse = new SemistrukturertAdresse();
        adresse.setLandkode("NO");
        Periode gyldighetsperiode = new Periode();
        adresse.setGyldighetsperiode(gyldighetsperiode);
        organisasjonDetaljer.forretningsadresse = Arrays.asList(adresse);
        orgDok.setOrganisasjonDetaljer(organisasjonDetaljer);
        when(eregFasade.hentOrganisasjon(ORGNR)).thenReturn(lagSaksopplysning(SaksopplysningType.ORGANISASJON, orgDok));
        RegisterOppslagSystemService registerOppslagService = new RegisterOppslagSystemService(eregFasade, tpsFasade);
        KodeverkRegister kodeverkRegister = mock(KodeverkRegister.class);
        KodeverkService kodeverkService = new KodeverkService(kodeverkRegister);
        LovvalgsperiodeService lovvalgsperiodeService = mock(LovvalgsperiodeService.class);
        VilkaarsresultatRepository vilkaarsresultatRepository = mock(VilkaarsresultatRepository.class);
        UtenlandskMyndighetRepository utenlandskMyndighetRepository = mock(UtenlandskMyndighetRepository.class);
        BrevDataByggerVelger brevdatabyggervelger = new BrevDataByggerVelger(avklartefaktaService, registerOppslagService, kodeverkService, lovvalgsperiodeService, utenlandskMyndighetRepository, vilkaarsresultatRepository);
        return brevdatabyggervelger;
    }

    private static Behandlingsresultat lagBehandlingsresultat(List<Avklartefakta> faktaliste) {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAvklartefakta(new HashSet<>(faktaliste));
        Lovvalgsperiode periode = new Lovvalgsperiode();
        periode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1);
        periode.setFom(LocalDate.now());
        periode.setTom(LocalDate.now());
        List<Lovvalgsperiode> perioder = Arrays.asList(periode);
        behandlingsresultat.setLovvalgsperioder(new HashSet<>(perioder));
        return behandlingsresultat;
    }

    private static BehandlingsresultatRepository mockBehandlingsresultatRepo(Behandlingsresultat behandlingsresultat) {
        BehandlingsresultatRepository behandlingsresultatRepository = mock(BehandlingsresultatRepository.class);
        when(behandlingsresultatRepository.findOne(BEHANDLINGSID)).thenReturn(behandlingsresultat);
        return behandlingsresultatRepository;
    }

    private static TpsFasade mockTpsFasade(Aktoer aktør) throws IkkeFunnetException {
        TpsFasade tpsFasade = mock(TpsFasade.class);
        when(tpsFasade.hentIdentForAktørId(eq(aktør.getAktørId())))
            .thenReturn(String.format("IDENT%s", aktør.getAktørId()));
        return tpsFasade;
    }

    private static BehandlingRepository mockBehandlingRepository(Behandling behandling) {
        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
        when(behandlingRepository.findOne(eq(BEHANDLINGSID))).thenReturn(behandling);
        when(behandlingRepository.findOneWithSaksopplysningerById(eq(BEHANDLINGSID))).thenReturn(behandling);
        return behandlingRepository;
    }

    private static Saksopplysning lagSaksopplysning(SaksopplysningType type, SaksopplysningDokument dokument) {
        Saksopplysning søknad = new Saksopplysning();
        søknad.setType(type);
        søknad.setDokument(dokument);
        return søknad;
    }

    private static Avklartefakta lagAvklarteFakta(AvklartefaktaType type, String subjekt) {
        return lagAvklarteFakta(type, "TRUE", subjekt);
    }

    private static Avklartefakta lagAvklarteFakta(AvklartefaktaType type, String fakta, String subjekt) {
        Avklartefakta arbeidsgiverFaktum = new Avklartefakta();
        arbeidsgiverFaktum.setSubjekt(subjekt);
        arbeidsgiverFaktum.setType(type);
        arbeidsgiverFaktum.setFakta(fakta);
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
