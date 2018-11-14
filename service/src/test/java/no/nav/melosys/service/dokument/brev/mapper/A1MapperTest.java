package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.service.dokument.brev.BrevDataDto;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static org.assertj.core.api.Assertions.assertThat;

public class A1MapperTest {

    private A1Mapper mapper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private EnhancedRandom enhancedRandom;

    private String fritekst = "";
    private Behandlingsresultat behandlingsresultat;

    @Before
    public void setUp() {
        mapper = new A1Mapper();
        enhancedRandom = EnhancedRandomBuilder
            .aNewEnhancedRandomBuilder()
            .scanClasspathForConcreteTypes(true)
            .build();

        behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setRegistrertDato(Instant.now());
    }

    @SuppressWarnings("Duplicates")
    @Test
    @Ignore
    public void mapTilBrevXML() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-1");

        MelosysNAVFelles navFelles = enhancedRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        Behandling behandling = new Behandling();
        behandling.setRegistrertDato(Instant.now());

        BrevDataDto brevDataDto = new BrevDataDto();
        String xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevDataDto);

        assertThat(xml).isNotNull();
    }
}