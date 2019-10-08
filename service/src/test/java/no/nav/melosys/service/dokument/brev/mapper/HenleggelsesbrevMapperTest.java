package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;

import no.nav.dok.brevdata.felles.v1.navfelles.NorskPostadresse;
import no.nav.dok.melosysbrev._000072.HenleggelseGrunnKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.service.dokument.brev.BrevDataMottattDato;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.jeasy.random.EasyRandom;
import org.junit.Test;

import static no.nav.melosys.service.dokument.brev.mapper.felles.FellesBrevtypeMappingTest.hentAlleVerdierFraKodeverk;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.*;

public class HenleggelsesbrevMapperTest {

    private static final EasyRandom easyRandom = new EasyRandom(EasyRandomConfigurer.paramForDokProd()
        .collectionSizeRange(0, 2)
        .objectPoolSize(3)
        .excludeField(named("lovvalgsperioder").or(named("avklartefakta").or(named("vilkaarsresultater"))))
        .randomize(named("postnummer").and(ofType(String.class)).and(inClass(NorskPostadresse.class)), () -> "1234"));

    @Test
    public void mapTilBrevXmlGirIkkeTomXmlStreng() throws Exception {
        testMapTilBrevXml();
    }

    private void testMapTilBrevXml() throws Exception {
        testMapTilBrevXml(lagBehandlingsresultat());
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        return easyRandom.nextObject(Behandlingsresultat.class);
    }

    private void testMapTilBrevXml(Behandlingsresultat behandlingsresultat) throws Exception {
        testMapTilBrevXml(lagBehandling(lagFagsak()), behandlingsresultat);
    }

    private Fagsak lagFagsak() {
        return easyRandom.nextObject(Fagsak.class);
    }

    private Behandling lagBehandling(Fagsak fagsak) {
        return easyRandom.nextObject(Behandling.class);
    }

    private void testMapTilBrevXml(Behandling behandling, Behandlingsresultat behandlingsresultat) throws Exception {
        FellesType fellesType = lagFellesType();
        MelosysNAVFelles navFelles = LagMelosysNAVFelles();
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.mottaker = Aktoersroller.BRUKER;
        brevbestillingDto.begrunnelseKode = "ANNET";
        brevbestillingDto.fritekst = "something";

        BrevDataMottattDato brevdata = new BrevDataMottattDato("saksbehandler", brevbestillingDto);
        brevdata.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();

        HenleggelsesbrevMapper instans = new HenleggelsesbrevMapper();
        String resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevdata);
        assertThat(resultat).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }

    private MelosysNAVFelles LagMelosysNAVFelles() {
        return easyRandom.nextObject(MelosysNAVFelles.class);
    }

    private FellesType lagFellesType() {
        return easyRandom.nextObject(FellesType.class);
    }

    @Test
    public void testHenleggelsegrunnKode() throws Exception {
        hentAlleVerdierFraKodeverk(Henleggelsesgrunner.class)
            .forEach(HenleggelseGrunnKode::fromValue);
    }
}