package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;

import io.github.benas.randombeans.api.EnhancedRandom;

import no.nav.dok.melosysbrev._000074.Fag;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.service.dokument.brev.BrevData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;

import static org.assertj.core.api.Assertions.assertThat;

public class MangelbrevMapperTest {

    private MangelbrevMapper mapper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private EnhancedRandom enhancedRandom;

    @Before
    public void setUp() {
        mapper = new MangelbrevMapper();
        enhancedRandom = EnhancedRandomConfigurer.randomForDokProd();
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

        BrevData BrevData = new BrevData("Z123456");
        BrevData.mottaker = Aktoersroller.BRUKER;
        BrevData.fritekst = "Test";

        String xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, null, BrevData);

        assertThat(xml).isNotNull();
    }

    @Test
    public void mapFag() throws Exception {
        Behandling behandling = new Behandling();
        behandling.setRegistrertDato(Instant.now());
        BrevData BrevData = new BrevData("Z123456");
        BrevData.fritekst = "Test";

        Fag fag = mapper.mapFag(behandling, BrevData);

        assertThat(fag).isNotNull();
        assertThat(fag.getDatoMottatt()).isNotNull();
        assertThat(fag.getAvsender()).isNotNull();

        assertThat(fag.getManglendeOpplysninger()).isNotNull();
        assertThat(fag.getManglendeOpplysninger().getFristDato()).isNotNull();
        assertThat(fag.getManglendeOpplysninger().getManglendeOpplysningerFritekst()).isNotNull();
    }

    @Test
    public void mapFag_manglerFritekst() throws Exception {
        Behandling behandling = new Behandling();
        behandling.setRegistrertDato(Instant.now());
        BrevData BrevData = new BrevData("Z123456");

        expectedException.expect(IntegrasjonException.class);

        mapper.mapFag(behandling, BrevData);
    }
}