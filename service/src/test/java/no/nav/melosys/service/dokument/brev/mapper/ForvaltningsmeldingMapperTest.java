package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.dok.melosysbrev._000082.Fag;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import org.junit.Before;
import org.junit.Test;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static org.assertj.core.api.Assertions.assertThat;

public class ForvaltningsmeldingMapperTest {

    private ForvaltningsmeldingMapper mapper;

    private EnhancedRandom enhancedRandom;

    @Before
    public void setUp() throws Exception {
        mapper = new ForvaltningsmeldingMapper();
        enhancedRandom = EnhancedRandomBuilder
            .aNewEnhancedRandomBuilder()
            .scanClasspathForConcreteTypes(true)
            .build();
    }

    @Test
    public void mapTilBrevXML() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-1");

        MelosysNAVFelles navFelles = enhancedRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        Behandling behandling = new Behandling();
        behandling.setRegistrertDato(Instant.now());

        String xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling,null, null);

        assertThat(xml).isNotNull();
    }

    @Test
    public void mapFag() throws Exception {
        Behandling behandling = new Behandling();
        behandling.setRegistrertDato(Instant.now());

        Fag fag = mapper.mapFag(behandling);

        assertThat(fag).isNotNull();
        assertThat(fag.getDatoMottatt()).isNotNull();
        assertThat(fag.getAvsender()).isNotNull();
        assertThat(fag.getSaksbehandlingstidDato()).isNotNull();
    }
}