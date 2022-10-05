package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.dok.melosysbrev._000108.SakstypeKode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelseFlereLand;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagStrukturertAdresse;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
import static no.nav.melosys.service.dokument.brev.mapper.felles.FellesBrevtypeMappingTest.hentAlleVerdierFraKodeverk;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysninger;
import static org.assertj.core.api.Assertions.assertThat;

class InnvilgelsesbrevFlereLandMapperTest {
    private final InnvilgelsesbrevFlereLandMapper instans;

    public InnvilgelsesbrevFlereLandMapperTest() {
        instans = new InnvilgelsesbrevFlereLandMapper();
    }

    @Test
    void testSakstypeKode() throws Exception {
        List<String> koderSomIkkeErAktuelleForBrev = Collections.singletonList(
            "UKJENT" // Det er ikke aktuelt med brev for denne
        );

        hentAlleVerdierFraKodeverk(Sakstyper.class)
            .filter(k -> !koderSomIkkeErAktuelleForBrev.contains(k))
            .forEach(SakstypeKode::fromValue);
    }

    @Test
    void mapTilBrevXmlGirIkkeTomXmlStreng() throws Exception {
        var behandling = lagBehandling(lagFagsak());
        var behandlingsresultat = lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()));
        var fellesType = lagFellesType();
        var navFelles = lagNAVFelles();
        var brevdataInnvilgelse = lagBrevdataInnvilgelse();

        String resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevdataInnvilgelse, false);
        assertThat(resultat).matches("(?s)<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }

    private BrevDataInnvilgelseFlereLand lagBrevdataInnvilgelse() {
        List<AvklartVirksomhet> norskeVirksomheter = Collections.singletonList(new AvklartVirksomhet("Telenor", "1234", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID));

        BrevDataInnvilgelseFlereLand brevdataInnvilgelse = new BrevDataInnvilgelseFlereLand(new BrevbestillingRequest(), "SAKSBEHANDLER");
        brevdataInnvilgelse.lovvalgsperiode = lagLovvalgsperiode();
        brevdataInnvilgelse.harAvklartMaritimTypeSkip = true;
        brevdataInnvilgelse.harAvklartMaritimTypeSokkel = false;
        brevdataInnvilgelse.arbeidsgivere = norskeVirksomheter;
        brevdataInnvilgelse.bostedsland = "Norge";
        brevdataInnvilgelse.trydemyndighetsland = Landkoder.DE;
        brevdataInnvilgelse.alleArbeidsland = List.of("Sverige", "Danmark", "Finland", "Spania");
        brevdataInnvilgelse.erMarginaltArbeid = true;
        brevdataInnvilgelse.erBegrensetPeriode = true;
        brevdataInnvilgelse.vedleggA1 = lagBrevdataA1(norskeVirksomheter);
        return brevdataInnvilgelse;
    }

    private static BrevDataA1 lagBrevdataA1(List<AvklartVirksomhet> virksomheter) {
        BrevDataA1 brevdataA1 = new BrevDataA1();
        brevdataA1.person = lagPersonopplysninger();
        brevdataA1.bostedsadresse = lagStrukturertAdresse();
        brevdataA1.yrkesgruppe = Yrkesgrupper.ORDINAER;
        brevdataA1.hovedvirksomhet = virksomheter.get(0);
        ArrayList<AvklartVirksomhet> bivirksomheter = new ArrayList<>(virksomheter);
        bivirksomheter.remove(0);
        brevdataA1.bivirksomheter = bivirksomheter;

        brevdataA1.arbeidssteder = new ArrayList<>();
        brevdataA1.arbeidsland = new ArrayList<>();
        return brevdataA1;
    }

    private static Behandlingsresultat lagBehandlingsresultat(Set<Lovvalgsperiode> perioder) {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(perioder);
        return behandlingsresultat;
    }

    private static Lovvalgsperiode lagLovvalgsperiode() {
        return lagLovvalgsperiode(LocalDate.now());
    }

    private static Lovvalgsperiode lagLovvalgsperiode(LocalDate fom) {
        Lovvalgsperiode periode = new Lovvalgsperiode();
        periode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        periode.setFom(fom);
        periode.setTom(LocalDate.now());
        periode.setLovvalgsland(Landkoder.AT);
        periode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);
        return periode;
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        return fagsak;
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setFagsak(fagsak);
        return behandling;
    }
}
