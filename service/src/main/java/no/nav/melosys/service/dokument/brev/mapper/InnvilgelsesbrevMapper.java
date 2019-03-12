package no.nav.melosys.service.dokument.brev.mapper;

import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000108.*;
import no.nav.dok.melosysbrev._000108.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000108.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartInnstallasjonsType;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.domain.util.SoeknadUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.lagXmlDato;

public final class InnvilgelsesbrevMapper implements BrevDataMapper {

    static final String JA = "true";

    private static final String XSD_LOCATION = "melosysbrev/melosys_000108.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevdata) throws JAXBException, SAXException, TekniskException {
        BrevDataInnvilgelse brevDataInnvilgelse = (BrevDataInnvilgelse) brevdata;

        VedleggMapper vedleggMapper = new VedleggMapper(behandling, resultat);
        vedleggMapper.map(brevDataInnvilgelse.vedleggA1);

        Fag fag = mapFag(behandling, brevDataInnvilgelse);

        JAXBElement<BrevdataType> brevdataTypeJAXBElement = lagBrevdataType(fellesType, navFelles, fag, vedleggMapper.hent());
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private Fag mapFag(Behandling behandling, BrevDataInnvilgelse brevdataInnvilgelse) throws TekniskException {
        BrevDataA1 brevdataA1 = brevdataInnvilgelse.vedleggA1;

        Fag fag = new Fag();
        fag.setBehandlingstype(BehandlingstypeKode.valueOf(behandling.getType().getKode()));
        fag.setSakstype(SakstypeKode.valueOf(behandling.getFagsak().getType().getKode()));

        AvklartVirksomhet avklartVirksomhet = brevdataA1.norskeVirksomheter.iterator().next();
        fag.setArbeidsgiver(avklartVirksomhet.navn);
        fag.setYrkesaktivitet(YrkesaktivitetsKode.fromValue(avklartVirksomhet.yrkesaktivitet.getKode()));

        fag.setInngangsvilkårbegrunnelse(InngangsvilkaarBegrunnelseKode.EOS_BORGER);
        fag.setTrygdemyndighetsland("TRYGDEMYNDIGHETSLAND");

        SoeknadDokument søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);

        // Henter ut Landkode fra Arbeidssteder(Fysiske og Maritime) og Oppholdsland
        String arbeidslandSomTekst = hentArbeidslandFraArbeidsstederOgOppholdsland(søknad, brevdataA1);
        Landkoder arbeidslandKode = Landkoder.valueOf(arbeidslandSomTekst);
        fag.setArbeidsland(arbeidslandKode.getBeskrivelse());

        if (!søknad.maritimtArbeid.isEmpty()) {
            MaritimtArbeid maritimtArbeid = søknad.maritimtArbeid.iterator().next();
            fag.setFlaggland(maritimtArbeid.flaggLandKode);
            fag.setArbeidPåTerritorialfarvann(maritimtArbeid.territorialfarvann);
        }

        if (brevdataInnvilgelse.avklartSokkelEllerSkip == AvklartInnstallasjonsType.SKIP) {
            fag.setArbeidPåSkip(JA);
        }
        if (brevdataInnvilgelse.avklartSokkelEllerSkip == AvklartInnstallasjonsType.SOKKEL) {
            fag.setArbeidPåSokkel(JA);
        }

        Lovvalgsperiode periode = brevdataInnvilgelse.lovvalgsperiode;
        fag.setLovvalgsbestemmelse(LovvalgsbestemmelseKode.fromValue(periode.getBestemmelse().getKode()));
        fag.setLovvalgsperiode(LovvalgsperiodeType.builder()
            .withFomDato(lagXmlDato(periode.getFom()))
            .withTomDato(lagXmlDato(periode.getTom()))
            .build());

        if (periode.getTilleggsbestemmelse() != null) {
            fag.setTilleggsbestemmelse(TilleggsbestemmelseKode.fromValue(periode.getTilleggsbestemmelse().getKode()));
        }

        return fag;
    }

    private static String hentArbeidslandFraArbeidsstederOgOppholdsland(SoeknadDokument søknad, BrevDataA1 brevdata) throws TekniskException {
        String arbeidslandSomTekst;
        if (!brevdata.arbeidssteder.isEmpty()) {
            Arbeidssted arbeidssted = brevdata.arbeidssteder.iterator().next();
            arbeidslandSomTekst = arbeidssted.landKode;
        }
        else {
            List<String> land = SoeknadUtils.hentLand(søknad);
            if (land.isEmpty()) {
                throw new TekniskException("Ingen land funnet");
            }
            arbeidslandSomTekst = land.get(0);
        }
        return arbeidslandSomTekst;
    }

    private static JAXBElement<BrevdataType> lagBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag, VedleggType vedlegg) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = BrevdataType.builder()
            .withFelles(fellesType)
            .withNAVFelles(navFelles)
            .withFag(fag)
            .withVedlegg(vedlegg)
            .build();
        return factory.createBrevdata(brevdataType);
    }

}
