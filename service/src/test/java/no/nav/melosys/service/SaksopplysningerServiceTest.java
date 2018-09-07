package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.aareg.AaregService;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdMock;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.ereg.EregService;
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonMock;
import no.nav.melosys.integrasjon.inntk.InntektFasade;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.integrasjon.inntk.inntekt.InntektMock;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.MedlService;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapMock;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class SaksopplysningerServiceTest {

    private SaksopplysningerService saksopplysningerService;

    private TpsFasade tpsFasade;

    private ProsessinstansRepository prosessinstansRepository;

    private BehandlingRepository behandlingRepo;

    private Binge binge;


    @Before
    public void setUp() {
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());

        AaregFasade aareg = new AaregService(new ArbeidsforholdMock(), dokumentFactory);
        EregFasade ereg = new EregService(new OrganisasjonMock(), dokumentFactory);
        MedlFasade medl = new MedlService(new MedlemskapMock(), dokumentFactory);
        InntektFasade inntekt = new InntektService(new InntektMock(), dokumentFactory);
        behandlingRepo = mock(BehandlingRepository.class);
        binge = mock(Binge.class);
        prosessinstansRepository = mock(ProsessinstansRepository.class);
        tpsFasade = mock(TpsFasade.class);

        saksopplysningerService = new SaksopplysningerService(tpsFasade, aareg, ereg, medl, inntekt, prosessinstansRepository, binge, behandlingRepo);

        ReflectionTestUtils.setField(saksopplysningerService, "arbeidsforholdhistorikkAntallÅr", 5);
        ReflectionTestUtils.setField(saksopplysningerService, "inntektshistorikkAntallMåneder", 12);
        ReflectionTestUtils.setField(saksopplysningerService, "medlemskaphistorikkAntallÅr", 5);
    }

    @Test
    public void hentArbeidsforholdHistorikk() throws SikkerhetsbegrensningException {
        final Long arbeidsforholdsID = 12608035L;
        ArbeidsforholdDokument dokument = saksopplysningerService.hentArbeidsforholdHistorikk(arbeidsforholdsID);
        assertFalse(dokument.getArbeidsforhold().isEmpty());
        assertTrue(dokument.getArbeidsforhold().get(0).getArbeidsavtaler().size() > 1);
    }

    @Test
    public void hentSaksopplysninger() throws Exception {
        // Skru av logging for denne testen siden den skaper mye forventet støy
        final Logger log = (Logger) LoggerFactory.getLogger(SaksopplysningerService.class);
        Level opprinneligLevel = log.getLevel();
        log.setLevel(Level.OFF);

        final String[] identer = new String[]{"88888888884", "77777777779"};

        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn(String.valueOf(returnsFirstArg()));
        when(tpsFasade.hentPersonMedAdresse(anyString())).thenReturn(new Saksopplysning());

        for (String fnr : identer) {
            Set<Saksopplysning> saksopplysninger = saksopplysningerService.hentSaksopplysninger(fnr);

            assertFalse(saksopplysninger.isEmpty());
        }

        // Skru på logging igjen
        log.setLevel(opprinneligLevel);
    }

    @Test
    public void oppfriskSaksopplysning() throws IkkeFunnetException {

        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        Aktoer aktør = new Aktoer();
        aktør.setAktørId("123");
        aktør.setRolle(RolleType.BRUKER);
        HashSet<Aktoer> aktører = new HashSet<>();
        aktører.add(aktør);
        fagsak.setAktører(aktører);
        behandling.setFagsak(fagsak);

        HashSet<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning saksopplysningPerson = new Saksopplysning();
        saksopplysningPerson.setType(SaksopplysningType.PERSONOPPLYSNING);
        saksopplysninger.add(saksopplysningPerson);

        SoeknadDokument soeknadDokument = new SoeknadDokument();

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.arbeidsland = new ArrayList<>();
        arbeidUtland.arbeidsland.add(new Land(Land.NORGE));
        arbeidUtland.arbeidsperiode = new Periode(LocalDate.now(),LocalDate.of(2018, 9, 18));
        soeknadDokument.arbeidUtland = arbeidUtland;

        Saksopplysning saksopplysningSøknad = new Saksopplysning();
        saksopplysningSøknad.setType(SaksopplysningType.SØKNAD);
        saksopplysningSøknad.setDokument(soeknadDokument);
        saksopplysninger.add(saksopplysningSøknad);

        behandling.setSaksopplysninger(saksopplysninger);

        when(prosessinstansRepository.findByStegIsNotNullAndBehandling_Id(anyLong())).thenReturn(Optional.empty());
        when(behandlingRepo.findOne(anyLong())).thenReturn(behandling);
        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn("12345");

        saksopplysningerService.oppfriskSaksopplysning(anyLong());

        assertThat(behandling.getSaksopplysninger().size()).isEqualTo(1);
        assertThat(behandling.getSaksopplysninger().stream().findFirst().get().getType()).isEqualTo(SaksopplysningType.SØKNAD);
        verify(binge, times(1)).leggTil(any());
    }
}