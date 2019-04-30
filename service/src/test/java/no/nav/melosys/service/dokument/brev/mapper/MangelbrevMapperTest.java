package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;

import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.dok.melosysbrev._000074.Fag;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataHenleggelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
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

        BrevDataHenleggelse brevData = new BrevDataHenleggelse("Z123456", new BrevbestillingDto());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();

        brevData.fritekst = "Test";

        String xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, null, brevData);

        assertThat(xml).isNotNull();
    }

    @Test
    public void mapFag() throws Exception {
        BrevDataHenleggelse brevData = new BrevDataHenleggelse("Z123456", new BrevbestillingDto());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();

        brevData.fritekst = "Test";

        Fag fag = mapper.mapFag(brevData);

        assertThat(fag).isNotNull();
        assertThat(fag.getDatoMottatt()).isNotNull();
        assertThat(fag.getAvsender()).isNotNull();

        assertThat(fag.getManglendeOpplysninger()).isNotNull();
        assertThat(fag.getManglendeOpplysninger().getFristDato()).isNotNull();
        assertThat(fag.getManglendeOpplysninger().getManglendeOpplysningerFritekst()).isNotNull();
    }

    @Test
    public void mapFag_manglerFritekst() throws Exception {
        BrevDataHenleggelse brevData = new BrevDataHenleggelse("Z123456", new BrevbestillingDto());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();


        expectedException.expect(IntegrasjonException.class);

        mapper.mapFag(brevData);
    }
}