package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;

import no.nav.dok.melosysbrev._000074.Fag;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.service.dokument.brev.BrevDataMottattDato;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class MangelbrevMapperTest {

    private MangelbrevMapper mapper;

    private EasyRandom easyRandom;

    @BeforeEach
    public void setUp() {
        mapper = new MangelbrevMapper();
        easyRandom = EasyRandomConfigurer.randomForDokProd();
    }

    @Test
    void mapTilBrevXML() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-1");

        MelosysNAVFelles navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingDto());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();

        brevData.fritekst = "Test";

        String xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, null, brevData);

        assertThat(xml).isNotNull();
    }

    @Test
    void mapFag() {
        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingDto());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();
        brevData.fritekst = "Test";

        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.ARBEID_FLERE_LAND);

        Fag fag = mapper.mapFag(brevData, behandling);

        assertThat(fag).isNotNull();
        assertThat(fag.getDatoMottatt()).isNotNull();
        assertThat(fag.getAvsender()).isNotNull();

        assertThat(fag.getManglendeOpplysninger()).isNotNull();
        assertThat(fag.getManglendeOpplysninger().getFristDato()).isNotNull();
        assertThat(fag.getManglendeOpplysninger().getManglendeOpplysningerFritekst()).isNotNull();
    }

    @Test
    void mapFag_manglerFritekst() {
        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingDto());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();

        assertThatExceptionOfType(IntegrasjonException.class)
            .isThrownBy(() -> mapper.mapFag(brevData, new Behandling()))
            .withMessageContaining("Mangelbrev mangler informasjon");
    }
}
