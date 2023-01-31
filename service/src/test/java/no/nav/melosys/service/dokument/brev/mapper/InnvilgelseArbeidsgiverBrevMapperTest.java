package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.KjoennKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
import static org.assertj.core.api.Assertions.assertThat;

public class InnvilgelseArbeidsgiverBrevMapperTest {

    private final InnvilgelseArbeidsgiverMapper instans;

    public InnvilgelseArbeidsgiverBrevMapperTest() {
        instans = new InnvilgelseArbeidsgiverMapper();
    }

    @Test
    public void mapArbeidsLandSammensattNavnLovvalgsperiodeFraSøkandTilBrevXmlGirIkkeTomXmlStreng() throws Exception {
        testMapTilBrevXml(lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()),
            Collections.singleton(lagAvklarteFakta())));
    }

    private void testMapTilBrevXml(Behandlingsresultat behandlingsresultat) throws Exception {
        testMapTilBrevXml(lagBehandling(lagFagsak()), behandlingsresultat);
    }

    private void testMapTilBrevXml(Behandling behandling, Behandlingsresultat behandlingsresultat) throws Exception {
        FellesType fellesType = lagFellesType();
        MelosysNAVFelles navFelles = lagNAVFelles();
        BrevDataInnvilgelse brevDataInnvilgelse = new BrevDataInnvilgelse(new BrevbestillingDto(), "Z123456");
        brevDataInnvilgelse.arbeidsland = "Sverige";
        brevDataInnvilgelse.hovedvirksomhet = new AvklartVirksomhet("Equinor", "987654321", null, Yrkesaktivitetstyper.LOENNET_ARBEID);
        brevDataInnvilgelse.lovvalgsperiode = lagLovvalgsperiode();
        brevDataInnvilgelse.personNavn = "For Etter";
        String resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevDataInnvilgelse);
        // TODO: Vurder å bruke XMLUnit e.l. til å sammenlikne XML-strengen
        // grundig mot forventninger.
        assertThat(resultat).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
        assertThat(":navn>For Etter</ns").isSubstringOf(resultat);
    }


    private static Behandlingsresultat lagBehandlingsresultat(Set<Lovvalgsperiode> perioder, Set<Avklartefakta> fakta) {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAvklartefakta(fakta);
        behandlingsresultat.setLovvalgsperioder(perioder);
        return behandlingsresultat;
    }

    private static Lovvalgsperiode lagLovvalgsperiode() {
        return lagLovvalgsperiode(LocalDate.now());
    }

    private static Lovvalgsperiode lagLovvalgsperiode(LocalDate fom) {
        Lovvalgsperiode periode = new Lovvalgsperiode();
        periode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        periode.setFom(fom);
        periode.setTom(LocalDate.now());
        periode.setLovvalgsland(Landkoder.AT);
        return periode;
    }

    private static Avklartefakta lagAvklarteFakta() {
        Avklartefakta faktum = new Avklartefakta();
        faktum.setType(Avklartefaktatyper.VIRKSOMHET);
        faktum.setFakta("TRUE");
        faktum.setSubjekt("123456789");
        return faktum;
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        return fagsak;
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        PersonDokument pdok = new PersonDokument();
        pdok.setKjønn(new KjoennsType(KjoennKode.U.name()));
        pdok.setFornavn("For");
        pdok.setEtternavn("Etter");
        pdok.setSammensattNavn("For Etter");
        pdok.setStatsborgerskap(new Land(Land.BELGIA));
        pdok.setFødselsdato(LocalDate.ofYearDay(1900, 1));
        return lagBehandling(fagsak, Collections.singleton(lagSaksopplysning(SaksopplysningType.PERSOPL, pdok)));
    }

    private static Saksopplysning lagSaksopplysning(SaksopplysningType type, SaksopplysningDokument dokument) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(type);
        saksopplysning.setDokument(dokument);
        return saksopplysning;
    }

    private static Behandling lagBehandling(Fagsak fagsak, Set<Saksopplysning> saksopplysninger) {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setFagsak(fagsak);
        behandling.setSaksopplysninger(saksopplysninger);
        return behandling;
    }
}
