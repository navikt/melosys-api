package no.nav.melosys.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SoeknadServiceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private SoeknadService soeknadService;

    @Mock
    private BehandlingRepository behandlingRepo;

    @Mock
    private SaksopplysningRepository saksopplysningRepo;

    private SoeknadDokument soeknadDokument;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.registerModule(new JavaTimeModule());

        URI søknadURI = (getClass().getClassLoader().getResource("soknad.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(søknadURI)));
        soeknadDokument = mapper.readValue(json, SoeknadDokument.class);

        soeknadService = new SoeknadService(behandlingRepo, saksopplysningRepo, dokumentFactory);
    }

    @Test
    public void hentSoeknad() throws Exception {
        Behandling b = new Behandling();
        Set<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning saksopplysning_1 = new Saksopplysning();
        saksopplysning_1.setRegistrertDato(LocalDateTime.now().minusMonths(3).toInstant(ZoneOffset.UTC));
        saksopplysning_1.setEndretDato(LocalDateTime.now().minusMonths(3).toInstant(ZoneOffset.UTC));
        saksopplysning_1.setKilde(SaksopplysningKilde.MEDL);
        saksopplysning_1.setType(SaksopplysningType.SØKNAD);
        saksopplysning_1.setDokument(soeknadDokument);

        Saksopplysning saksopplysning_2 = new Saksopplysning();
        saksopplysning_2.setType(SaksopplysningType.PERSONOPPLYSNING);
        saksopplysning_2.setRegistrertDato(LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.UTC));
        saksopplysning_2.setEndretDato(LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.UTC));
        saksopplysning_2.setKilde(SaksopplysningKilde.TPS);

        saksopplysninger.add(saksopplysning_1);
        saksopplysninger.add(saksopplysning_2);

        b.setSaksopplysninger(saksopplysninger);

        when(behandlingRepo.findOneWithSaksopplysningerById(1L)).thenReturn(b);

        SoeknadDokument res = soeknadService.hentSoeknad(1L);

        assertThat(res.arbeidNorge.arbeidsforholdOpprettholdIHelePerioden).isEqualTo(true);
    }

    @Test
    public void registrerSøknad() throws Exception {
        long behandlingID = 1L;
        Behandling b = new Behandling();
        when(behandlingRepo.findOne(behandlingID)).thenReturn(b);

        soeknadService.registrerSøknad(behandlingID, soeknadDokument);

        verify(saksopplysningRepo, times(1)).save((Saksopplysning) any());
    }
}