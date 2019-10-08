package no.nav.melosys.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
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
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
        when(saksopplysningRepo.findByBehandling_IdAndType(1L, SaksopplysningType.SØKNAD)).thenReturn(Optional.of(lagSøknadssaksopplysning(SaksopplysningType.SØKNAD)));

        SoeknadDokument res = soeknadService.hentSøknad(1L);

        assertThat(res.arbeidNorge.arbeidsforholdOpprettholdIHelePerioden).isEqualTo(true);
    }

    @Test
    public void hentBehandlingUtenSøknadKasterUnntak() throws Exception {
        when(behandlingRepo.findWithSaksopplysningerById(1L)).thenReturn(lagBehandling(Collections.emptySet()));
        Throwable unntak = catchThrowable(() -> soeknadService.hentSøknad(1L));
        assertThat(unntak).isInstanceOf(IkkeFunnetException.class)
                .hasMessageContaining("ikke funnet for behandlingsid 1");
    }

    private Behandling lagBehandling() {
        Set<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning saksopplysning_1 = lagSøknadssaksopplysning(SaksopplysningType.SØKNAD);

        Saksopplysning saksopplysning_2 = new Saksopplysning();
        saksopplysning_2.setType(SaksopplysningType.PERSOPL);
        saksopplysning_2.setRegistrertDato(LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.UTC));
        saksopplysning_2.setEndretDato(LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.UTC));
        saksopplysning_2.setKilde(SaksopplysningKilde.TPS);

        saksopplysninger.add(saksopplysning_1);
        saksopplysninger.add(saksopplysning_2);

        return lagBehandling(saksopplysninger);
    }

    private Saksopplysning lagSøknadssaksopplysning(SaksopplysningType type) {
        Saksopplysning saksopplysning_1 = new Saksopplysning();
        saksopplysning_1.setRegistrertDato(LocalDateTime.now().minusMonths(3).toInstant(ZoneOffset.UTC));
        saksopplysning_1.setEndretDato(LocalDateTime.now().minusMonths(3).toInstant(ZoneOffset.UTC));
        saksopplysning_1.setKilde(SaksopplysningKilde.MEDL);
        saksopplysning_1.setType(type);
        saksopplysning_1.setDokument(soeknadDokument);
        return saksopplysning_1;
    }

    private Behandling lagBehandling(Set<Saksopplysning> saksopplysninger) {
        Behandling b = new Behandling();
        b.setSaksopplysninger(saksopplysninger);
        return b;
    }

    @Test
    public void registrerSøknad() throws Exception {
        long behandlingID = 1L;
        Behandling b = new Behandling();
        when(behandlingRepo.findById(behandlingID)).thenReturn(Optional.of(b));

        soeknadService.registrerSøknad(behandlingID, soeknadDokument);

        verify(saksopplysningRepo, times(1)).save((Saksopplysning) any());
    }
}