package no.nav.melosys.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SoeknadServiceTest {

    private SoeknadService soeknadService;

    @Mock
    private BehandlingRepository behandlingRepo;

    @Mock
    private SaksopplysningRepository saksopplysningRepo;

    private SoeknadDokument soeknadDokument;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp() throws IOException {
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);


        String json = "{\"soknadDokument\":{\"arbeidUtland\":{\"arbeidsland\":[\"ESP\",\"DNK\"],\"arbeidsperiode\":{\"fom\":\"2018-01-01\",\"tom\":\"2018-06-01\"},\"arbeidsandelNorge\":33.3,\"arbeidsandelUtland\":66.6,\"arbeidsstedUtland\":null,\"bostedsland\":\"SWE\",\"erstatterTidligereUtsendt\":false},\"foretakUtland\":{\"foretakUtlandNavn\":\"Volkswagen AG\",\"foretakUtlandOrgnr\":\"1122334444\",\"foretakUtlandAdresse\":null},\"oppholdUtland\":{\"oppholdsland\":\"DEU\",\"oppholdsPeriode\":{\"fom\":\"2018-01-01\",\"tom\":\"2018-06-01\"},\"studentIEOS\":false,\"studentFinansiering\":\"Beskrivelse av hvordan studiene finansieres\",\"studentSemester\":\"2018/2019\",\"studieLand\":\"SWE\"},\"arbeidNorge\":{\"arbeidsforholdOpprettholdIHelePerioden\":true,\"brukerErSelvstendigNaeringsdrivende\":true,\"selvstendigFortsetterEtterArbeidIUtlandet\":true,\"brukerArbeiderIVikarbyra\":\"false\",\"vikarOrgnr\":\"Ola Nordmann 22334455\",\"flyendePersonellHjemmebase\":\"Flybasen Int. Airport, ....\",\"ansattPaSokkelEllerSkip\":\"sokkel | skip\",\"navnSkipEllerSokkel\":\"Trym-sokkelen\",\"sokkelLand\":\"SE\",\"skipFartsomrade\":\"Europeisk fart\",\"skipFlaggLand\":\"SE\"},\"juridiskArbeidsgiverNorge\":{\"antallAnsatte\":350,\"antallAdminAnsatte\":250,\"antallAdminAnsatteEOS\":75,\"andelOmsetningINorge\":78.5,\"andelKontrakterINorge\":50.5,\"erBemanningsbyra\":false,\"hattDriftSiste12Mnd\":true,\"antallUtsendte\":30},\"arbeidsinntekt\":{\"inntektNorskIPerioden\":5500,\"inntektUtenlandskIPerioden\":2000,\"inntektNaeringIPerioden\":0,\"inntektNaturalYtelser\":[\"Fri bolig\",\"Fri bil\"],\"inntektErInnrapporteringspliktig\":true,\"inntektTrygdeavgiftBlirTrukket\":true},\"tilleggsopplysninger\":\"Lang utgreiing om utsendelsen som egentlig ikke er relevant for saksbehandlingen...\"}}";

        soeknadDokument = mapper.readValue(json, SoeknadDokument.class);

        soeknadService = new SoeknadService(behandlingRepo, saksopplysningRepo, dokumentFactory);
    }

    @Test
    public void hentSoeknad() throws Exception {
        Behandling b = new Behandling();
        Set<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning saksopplysning_1 = new Saksopplysning();
        saksopplysning_1.setType(SaksopplysningType.SØKNAD);
        saksopplysning_1.setRegistrertDato(LocalDateTime.now().minusMonths(3));
        saksopplysning_1.setKilde(SaksopplysningKilde.MEDL);

        Saksopplysning saksopplysning_2 = new Saksopplysning();
        saksopplysning_2.setType(SaksopplysningType.PERSONOPPLYSNING);
        saksopplysning_2.setRegistrertDato(LocalDateTime.now().minusMonths(1));
        saksopplysning_2.setKilde(SaksopplysningKilde.TPS);

        Saksopplysning saksopplysning_3 = new Saksopplysning();
        saksopplysning_3.setRegistrertDato(LocalDateTime.now().minusMonths(1));
        saksopplysning_3.setKilde(SaksopplysningKilde.SBH);
        saksopplysning_3.setType(SaksopplysningType.SØKNAD);
        saksopplysning_3.setDokument(soeknadDokument);

        saksopplysninger.add(saksopplysning_1);
        saksopplysninger.add(saksopplysning_2);
        saksopplysninger.add(saksopplysning_3);

        b.setSaksopplysninger(saksopplysninger);

        when(behandlingRepo.findOne(1L)).thenReturn(b);

        SoeknadDokument res = soeknadService.hentSoeknad(1L);

        assertThat(res.arbeidNorge.arbeidsforholdOpprettholdIHelePerioden).isEqualTo(true);
    }

    @Test
    public void registrerSøknad() throws Exception {


        soeknadDokument.arbeidNorge.ansattPaSokkelEllerSkip = "sokkel";
        long behandlingID = 1L;
        Behandling b = new Behandling();

        when(behandlingRepo.findOne(1L)).thenReturn(b);

        soeknadService.registrerSøknad(behandlingID, soeknadDokument);

        verify(saksopplysningRepo, times(1)).save((Saksopplysning) any());
    }

}