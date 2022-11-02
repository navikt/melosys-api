package no.nav.melosys.service.dokument.brev.mapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysninger;
import static org.springframework.test.util.AssertionErrors.assertFalse;

class InnvilgelsesbrevMapperTest {

    private final InnvilgelsesbrevMapper instans;
    private static final LocalDate NOW = LocalDate.parse("2022-02-13");

    public InnvilgelsesbrevMapperTest() {
        instans = new InnvilgelsesbrevMapper();
    }

    @Test
    void mapArbeidslandFraSøknadsTilBrevXmlGirIkkeTomXmlStreng() throws Exception {
        String xmlFraFil = hentBrevXmlFraFil("innvilgelsesbrev/innvilgelsesbrev.xml");
        String testMapTilBrevXml = testMapTilBrevXml(lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()),
            Collections.singleton(lagAvklarteFakta(Avklartefaktatyper.VIRKSOMHET, "123456789"))), false);

        Diff diff = DiffBuilder.compare(xmlFraFil).withTest(testMapTilBrevXml)
            .withNodeFilter(node -> !"ns7:opprettelsesDato".equals(node.getNodeName()))
            .build();
        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    void mapTilBrevXML_maritimtArbeidInnenriks_arbeidslandSettesTilTerritorialfarvannLand() throws Exception {
        String xmlFraFil = hentBrevXmlFraFil("innvilgelsesbrev/innvilgelsesbrev_territorialfarvann.xml");
        String testMapTilBrevXml = testMapTilBrevXml(lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()),
            Collections.singleton(lagAvklarteFakta(Avklartefaktatyper.VIRKSOMHET, "123456789"))), true);

        Diff diff = DiffBuilder.compare(xmlFraFil).withTest(testMapTilBrevXml)
            .withNodeFilter(node -> !"ns7:opprettelsesDato".equals(node.getNodeName()))
            .build();
        assertFalse(diff.toString(), diff.hasDifferences());
    }

    private String testMapTilBrevXml(Behandlingsresultat behandlingsresultat, boolean medFartsområde) throws Exception {
        return testMapTilBrevXml(lagBehandling(lagFagsak(), medFartsområde), behandlingsresultat);
    }

    private String hentBrevXmlFraFil(String filnavn) throws IOException {
        return new String(getClass().getClassLoader().getResourceAsStream(filnavn).readAllBytes());
    }

    private String testMapTilBrevXml(Behandling behandling, Behandlingsresultat behandlingsresultat) throws Exception {
        FellesType fellesType = lagFellesType();
        MelosysNAVFelles navFelles = lagNAVFelles();
        BrevDataA1 brevdataA1 = new BrevDataA1();
        AvklartVirksomhet virksomhet = new AvklartVirksomhet("Virker ikke", "123456789", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID);
        brevdataA1.hovedvirksomhet = virksomhet;
        brevdataA1.bivirksomheter = Collections.singletonList(virksomhet);
        brevdataA1.bostedsadresse = lagStrukturertAdresse();
        brevdataA1.yrkesgruppe = Yrkesgrupper.FLYENDE_PERSONELL;
        brevdataA1.person = lagPersonopplysninger();
        brevdataA1.arbeidssteder = new ArrayList<>();
        brevdataA1.arbeidsland = new ArrayList<>();
        BrevDataInnvilgelse brevdataInnvilgelse = new BrevDataInnvilgelse(new BrevbestillingRequest(), "SAKSBEHANDLER");
        brevdataInnvilgelse.vedleggA1 = brevdataA1;
        brevdataInnvilgelse.lovvalgsperiode = lagLovvalgsperiode();
        brevdataInnvilgelse.avklartMaritimType = Maritimtyper.SKIP;
        brevdataInnvilgelse.erTuristskip = true;
        brevdataInnvilgelse.hovedvirksomhet = virksomhet;
        brevdataInnvilgelse.arbeidsland = "Sverige";
        brevdataInnvilgelse.setAnmodningsperiodesvar(lagAnmodningsperiodeSvarInnvilgelse());
        brevdataInnvilgelse.trygdemyndighetsland = "Sverige";
        brevdataInnvilgelse.avklarteMedfolgendeBarn = lagAvklarteMedfølgendeBarn();

        String resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevdataInnvilgelse, false);
        return resultat;
    }

    private static Behandlingsresultat lagBehandlingsresultat(Set<Lovvalgsperiode> perioder, Set<Avklartefakta> fakta) {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAvklartefakta(fakta);
        behandlingsresultat.setLovvalgsperioder(perioder);
        return behandlingsresultat;
    }

    private static Lovvalgsperiode lagLovvalgsperiode() {
        return lagLovvalgsperiode(NOW);
    }

    private static Lovvalgsperiode lagLovvalgsperiode(LocalDate fom) {
        Lovvalgsperiode periode = new Lovvalgsperiode();
        periode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        periode.setFom(fom);
        periode.setTom(NOW);
        periode.setLovvalgsland(Landkoder.AT);
        periode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);
        return periode;
    }

    private static Avklartefakta lagAvklarteFakta(Avklartefaktatyper type, String verdi) {
        Avklartefakta faktum = new Avklartefakta();
        faktum.setType(type);
        faktum.setFakta("TRUE");
        faktum.setSubjekt(verdi);
        return faktum;
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        return fagsak;
    }

    private static Behandling lagBehandling(Fagsak fagsak, boolean medFartsområde) {
        return lagBehandling(fagsak, lagSoeknadDokument(medFartsområde));
    }

    private static Soeknad lagSoeknadDokument(boolean medFartsområde) {
        Soeknad dokument = new Soeknad();
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.adresse = new StrukturertAdresse();
        fysiskArbeidssted.adresse.setLandkode(Landkoder.AT.getKode());
        dokument.arbeidPaaLand.fysiskeArbeidssteder = Collections.singletonList(fysiskArbeidssted);
        dokument.maritimtArbeid.add(medFartsområde ? lagMaritimtArbeidMedFartsområde() : lagMaritimtArbeidUtenFartsområde());
        return dokument;
    }

    private static MaritimtArbeid lagMaritimtArbeidUtenFartsområde() {
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.enhetNavn = "Dunfjæder";
        maritimtArbeid.innretningLandkode = "NO";
        return maritimtArbeid;
    }

    private static MaritimtArbeid lagMaritimtArbeidMedFartsområde() {
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.enhetNavn = "Dunfjæder";
        maritimtArbeid.innretningLandkode = "NO";
        maritimtArbeid.territorialfarvannLandkode = "GB";
        maritimtArbeid.fartsomradeKode = Fartsomrader.INNENRIKS;
        return maritimtArbeid;
    }

    private static Behandling lagBehandling(Fagsak fagsak, MottatteOpplysningerData mottatteOpplysningerData) {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.KLAGE);
        behandling.setFagsak(fagsak);
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        behandling.getMottatteOpplysninger().setMottatteOpplysningerdata(mottatteOpplysningerData);
        return behandling;
    }
}
