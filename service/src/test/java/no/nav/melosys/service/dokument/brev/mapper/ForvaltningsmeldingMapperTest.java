package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;

import no.nav.dok.melosysbrev._000082.Fag;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.service.dokument.brev.BrevDataMottattDato;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static org.assertj.core.api.Assertions.assertThat;

public class ForvaltningsmeldingMapperTest {

    private ForvaltningsmeldingMapper mapper;

    private EasyRandom easyRandom;

    @BeforeEach
    public void setUp() throws Exception {
        mapper = new ForvaltningsmeldingMapper();
        // Sparer ~= 10 sek. kjøretid i forhold til å skanne hele
        // klassestien (.scanClasspathForConcreteTypes(true))
        easyRandom = EasyRandomConfigurer.randomForDokProd();
    }

    @Test
    public void mapTilBrevXML() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-1");

        MelosysNAVFelles navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.UTSENDT_SELVSTENDIG);
        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingRequest());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();

        String xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, null, brevData);

        assertThat(xml).isNotNull();
    }

    @Test
    public void mapFag() throws Exception {
        BrevDataMottattDato brevData = new BrevDataMottattDato("Z123456", new BrevbestillingRequest());
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();
        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        Fag fag = mapper.mapFag(brevData, behandling);

        assertThat(fag).isNotNull();
        assertThat(fag.getDatoMottatt()).isNotNull();
        assertThat(fag.getAvsender()).isNotNull();
        assertThat(fag.getSaksbehandlingstidDato()).isNotNull();
    }
}
