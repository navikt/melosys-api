package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;

import io.github.benas.randombeans.FieldDefinition;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import no.nav.dok.brevdata.felles.v1.navfelles.NorskPostadresse;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.service.dokument.brev.BrevDataHenleggelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class HenleggelsesbrevMapperTest {

    private static final EnhancedRandom enhancedRandom = EnhancedRandomConfigurer.builderForDokProd()
        .collectionSizeRange(0, 2)
        .objectPoolSize(3)
        .randomize(new FieldDefinition<>("postnummer", String.class, NorskPostadresse.class),
                (Randomizer<String>) (() -> "1234"))
        .build();

    @Test
    public void mapTilBrevXmlGirIkkeTomXmlStreng() throws Exception {
        testMapTilBrevXml();
    }

    @Test
    public void mapTilBrevUtenHenleggelsesgrunnGirUnntak() throws Exception {
        Behandlingsresultat resultat = lagBehandlingsresultat();
        resultat.setHenleggelsesgrunn(null);
        Throwable unntak = catchThrowable(() -> testMapTilBrevXml(resultat));
        assertThat(unntak).isInstanceOf(NullPointerException.class)
            .hasNoCause();
    }

    private void testMapTilBrevXml() throws Exception {
        testMapTilBrevXml(lagBehandlingsresultat());
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        return enhancedRandom.nextObject(Behandlingsresultat.class, "lovvalgsperioder", "avklartefakta", "vilkaarsresultater");
    }

    private void testMapTilBrevXml(Behandlingsresultat behandlingsresultat) throws Exception {
        testMapTilBrevXml(lagBehandling(lagFagsak()), behandlingsresultat);
    }

    private Fagsak lagFagsak() {
        return enhancedRandom.nextObject(Fagsak.class);
    }

    private Behandling lagBehandling(Fagsak fagsak) {
        return enhancedRandom.nextObject(Behandling.class);
    }

    private void testMapTilBrevXml(Behandling behandling, Behandlingsresultat behandlingsresultat) throws Exception {
        FellesType fellesType = lagFellesType();
        MelosysNAVFelles navFelles = LagMelosysNAVFelles();
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.mottaker = RolleType.BRUKER;
        brevbestillingDto.begrunnelseKode = "ANNET";
        brevbestillingDto.fritekst = "something";

        BrevDataHenleggelse brevdata = new BrevDataHenleggelse("saksbehandler", brevbestillingDto);
        brevdata.initierendeJournalpostForsendelseMottattTidspunkt = Instant.now();

        HenleggelsesbrevMapper instans = new HenleggelsesbrevMapper();
        String resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevdata);
        assertThat(resultat).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }

    private MelosysNAVFelles LagMelosysNAVFelles() {
        return enhancedRandom.nextObject(MelosysNAVFelles.class);
    }

    private FellesType lagFellesType() {
        return enhancedRandom.nextObject(FellesType.class);
    }

}
