package no.nav.melosys.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.SaksopplysningRepository;

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

        // FIXME: Last json fra fil
        String json = "{\n" +
                "  \"soknadDokument\": {\n" +
                "    \"arbeidUtland\": {\n" +
                "      \"arbeidsland\": [\n" +
                "        \"ESP\",\n" +
                "        \"DNK\"\n" +
                "      ],\n" +
                "      \"arbeidsperiode\": {\n" +
                "        \"fom\": \"2018-01-01\",\n" +
                "        \"tom\": \"2018-06-01\"\n" +
                "      },\n" +
                "      \"arbeidsandelNorge\": 33.3,\n" +
                "      \"arbeidsandelUtland\": 66.6,\n" +
                "      \"bostedsland\": \"SWE\",\n" +
                "      \"erstatterTidligereUtsendt\": false\n" +
                "    },\n" +
                "    \"foretakUtland\": {\n" +
                "      \"foretakUtlandNavn\": \"Volkswagen AG\",\n" +
                "      \"foretakUtlandOrgnr\": \"1122334444\"\n" +
                "    },\n" +
                "    \"oppholdUtland\": {\n" +
                "      \"oppholdsland\": [\n" +
                "        \"GB\"\n" +
                "      ],\n" +
                "      \"oppholdsPeriode\": {\n" +
                "        \"fom\": \"2018-01-01\",\n" +
                "        \"tom\": \"2019-01-01\"\n" +
                "      },\n" +
                "      \"studentIEOS\": false,\n" +
                "      \"studentFinansiering\": \"Beskrivelse av hvordan studiene finansieres\",\n" +
                "      \"studentSemester\": \"2018/2019\",\n" +
                "      \"studieLand\": \"SWE\"\n" +
                "    },\n" +
                "    \"arbeidNorge\": {\n" +
                "      \"arbeidsforholdOpprettholdIHelePerioden\": true,\n" +
                "      \"selvstendigFortsetterEtterArbeidIUtlandet\": true,\n" +
                "      \"vikarOrgnr\": \"Ola Nordmann 22334455\",\n" +
                "      \"flyendePersonellHjemmebase\": \"Flybasen Int. Airport, ....\",\n" +
                "      \"navnSkipEllerSokkel\": \"Trym-sokkelen\",\n" +
                "      \"sokkelLand\": \"SE\",\n" +
                "      \"skipFlaggLand\": \"SE\",\n" +
                "      \"brukerErSelvstendigNaeringsdrivende\": true,\n" +
                "      \"brukerArbeiderIVikarbyra\": false,\n" +
                "      \"ansattPaSokkelEllerSkip\": \"sokkel | skip\",\n" +
                "      \"skipFartsomrade\": \"Europeisk fart\",\n" +
                "      \"valgteArbeidsforhold\": []\n" +
                "    },\n" +
                "    \"juridiskArbeidsgiverNorge\": {\n" +
                "      \"antallAnsatte\": 350,\n" +
                "      \"antallAdminAnsatte\": 250,\n" +
                "      \"andelOmsetningINorge\": 78.5,\n" +
                "      \"andelKontrakterINorge\": 50.5,\n" +
                "      \"erBemanningsbyra\": false,\n" +
                "      \"hattDriftSiste12Mnd\": true,\n" +
                "      \"antallUtsendte\": 30,\n" +
                "      \"antallAdminAnsatteEOS\": 75\n" +
                "    },\n" +
                "    \"arbeidsinntekt\": {\n" +
                "      \"inntektNorskIPerioden\": 5500,\n" +
                "      \"inntektUtenlandskIPerioden\": 2000,\n" +
                "      \"inntektNaeringIPerioden\": 0,\n" +
                "      \"inntektNaturalYtelser\": [\n" +
                "        \"Fri bolig\",\n" +
                "        \"Fri bil\"\n" +
                "      ],\n" +
                "      \"inntektErInnrapporteringspliktig\": true,\n" +
                "      \"inntektTrygdeavgiftBlirTrukket\": true\n" +
                "    },\n" +
                "    \"tilleggsopplysninger\": \"Lang utgreiing om utsendelsen som egentlig ikke er relevant for saksbehandlingen...\",\n" +
                "    \"arbeidsgiversBekreftelse\": {\n" +
                "      \"arbeidsgiverBekrefterUtsendelse\": true,\n" +
                "      \"arbeidstakerAnsattUnderUtsendelsen\": true,\n" +
                "      \"erstatterArbeidstakerenUtsendte\": true,\n" +
                "      \"arbeidstakerTidligereUtsendt24Mnd\": true,\n" +
                "      \"arbeidsgiverBetalerArbeidsgiveravgift\": true,\n" +
                "      \"trygdeavgiftTrukketGjennomSkatt\": true,\n" +
                "      \"trygdeavgiftTrukketGjennomSkattDato\": \"2020-02-02\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

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