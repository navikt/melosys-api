package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000108.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000108.ObjectFactory;
import no.nav.dok.melosysbrev._000108.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.arbeidsforhold.Fartsomraade;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import org.xml.sax.SAXException;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART12_1;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART12_2;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.lagXmlDato;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory.mapArt121BegrunnelseType;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory.mapArt122BegrunnelseType;

public final class InnvilgelsesbrevMapper implements BrevDataMapper {

    private static final String JA = "true";

    private static final String XSD_LOCATION = "melosysbrev/melosys_000108.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevdata) throws JAXBException, SAXException, TekniskException {
        BrevDataInnvilgelse brevDataInnvilgelse = (BrevDataInnvilgelse) brevdata;

        VedleggMapper vedleggMapper = new VedleggMapper(behandling, resultat);
        vedleggMapper.map(brevDataInnvilgelse.vedleggA1);

        Fag fag = mapFag(behandling, resultat, brevDataInnvilgelse);

        JAXBElement<BrevdataType> brevdataTypeJAXBElement = lagBrevdataType(fellesType, navFelles, fag, vedleggMapper.hent());
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataInnvilgelse brevdata) throws TekniskException {
        Fag fag = new Fag();

        fag.setBehandlingstype(BehandlingstypeKodeMapper.hentBehandlingstypeKode(behandling));
        fag.setSakstype(SakstypeKode.valueOf(behandling.getFagsak().getType().getKode()));

        AvklartVirksomhet hovedvirksomhet = brevdata.hovedvirksomhet;
        if (hovedvirksomhet.erArbeidsgiver()) {
            fag.setArbeidsgiver(hovedvirksomhet.navn);
        }
        fag.setYrkesaktivitet(YrkesaktivitetsKode.fromValue(hovedvirksomhet.yrkesaktivitet.getKode()));

        fag.setInngangsvilkårbegrunnelse(InngangsvilkaarBegrunnelseKode.EOS_BORGER);

        fag.setArbeidsland(brevdata.arbeidsland);
        fag.setBostedsland(brevdata.bostedsland);
        fag.setTrygdemyndighetsland(brevdata.trygdemyndighetsland);

        BehandlingsgrunnlagData grunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        fag.setFlaggland(brevdata.arbeidsland);
        if (!grunnlagData.maritimtArbeid.isEmpty()) {
            MaritimtArbeid maritimtArbeid = grunnlagData.maritimtArbeid.iterator().next();
            if (Fartsomraade.INNENRIKS.getKode().equalsIgnoreCase(maritimtArbeid.fartsomradeKode)) {
                fag.setArbeidPåTerritorialfarvann(JA);
            }
        }

        if (brevdata.harAvklartMaritimTypeSkip) {
            fag.setArbeidPåSkip(JA);
        }
        if (brevdata.harAvklartMaritimTypeSokkel) {
            fag.setArbeidPåSokkel(JA);
        }

        brevdata.getAnmodningsperiodesvar().map(AnmodningsperiodeSvar::getBegrunnelseFritekst)
            .ifPresent(fag::setBegrunnelseFritekst);

        brevdata.getAnmodningsperiodesvar()
            .map(AnmodningsperiodeSvar::getAnmodningsperiodeSvarType)
            .map(Anmodningsperiodesvartyper::getKode)
            .map(AnmodningsPeriodeSvarTypeKode::valueOf)
            .ifPresent(fag::setAnmodningsPeriodeSvarType);

        Lovvalgsperiode periode = brevdata.lovvalgsperiode;
        fag.setLovvalgsbestemmelse(LovvalgsbestemmelseKode.fromValue(periode.getBestemmelse().getKode()));
        fag.setLovvalgsperiode(LovvalgsperiodeType.builder()
            .withFomDato(lagXmlDato(periode.getFom()))
            .withTomDato(lagXmlDato(periode.getTom()))
            .build());

        if (periode.getTilleggsbestemmelse() != null) {
            fag.setTilleggsbestemmelse(TilleggsbestemmelseKode.fromValue(periode.getTilleggsbestemmelse().getKode()));
        }
        if (brevdata.begrunnelseKode != null) {
            fag.setEndretPeriodeBegrunnelse(EndretPeriodeBegrunnelseKode.fromValue(brevdata.begrunnelseKode));
        }

        Set<VilkaarBegrunnelse> art121Begrunnelser = resultat.hentVilkaarbegrunnelser(FO_883_2004_ART12_1);
        fag.setArt121Begrunnelse(mapArt121BegrunnelseType(art121Begrunnelser));

        if (brevdata.erTuristskip) {
            fag.setVilkår(VilkaarKode.FTRL_2_12_UNNTAK_TURISTSKIP);
        }

        Set<VilkaarBegrunnelse> art122Begrunnelser = resultat.hentVilkaarbegrunnelser(FO_883_2004_ART12_2);
        fag.setArt122Begrunnelse(mapArt122BegrunnelseType(art122Begrunnelser));

        fag.setFritekst(brevdata.fritekst);

        if (resultat.getVedtakMetadata() != null) {
            fag.setVedtaksType(tilVedtaksTypeKode(resultat.getVedtakMetadata().getVedtakstype()));
        }

        if (brevdata.erArt16UtenArt12) {
            fag.setArt16UtenArt12(JA);
        }

        return fag;
    }

    private VedtaksTypeKode tilVedtaksTypeKode(Vedtakstyper vedtakstype) throws TekniskException {
        if (vedtakstype == null) {
            return null;
        }

        switch (vedtakstype) {
            case FØRSTEGANGSVEDTAK:
                return VedtaksTypeKode.FOERSTEGANGSVEDTAK;
            case KORRIGERT_VEDTAK:
                return VedtaksTypeKode.KORRIGERT_VEDTAK;
            case OMGJØRINGSVEDTAK:
                return VedtaksTypeKode.OMGJOERINGSVEDTAK;
            case ENDRINGSVEDTAK:
                return null; //Brev har ikke koder for ENDRINGSVEDTAK
            default:
                throw new TekniskException("Ukjent vedtakstype " + vedtakstype + " kan ikke mappes til VedtaksTypeKode");
        }
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
