package no.nav.melosys.service.dokument.brev.mapper;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000108.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000108.ObjectFactory;
import no.nav.dok.melosysbrev._000108.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader;
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.brev.felles.LovvalgsbestemmelseKodeMapper;
import no.nav.melosys.service.brev.felles.TilleggsbestemmelseKodeMapper;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.mapper.felles.KonvEftaStorbritanniaLovvalgbestemmelser;
import org.xml.sax.SAXException;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static no.nav.melosys.domain.kodeverk.Vilkaar.*;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.lagXmlDato;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory.mapArt121BegrunnelseType;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory.mapArt122BegrunnelseType;

public final class InnvilgelsesbrevMapper implements BrevDataMapper {
    private static final String JA = "true";

    private static final String XSD_LOCATION = "melosysbrev/melosys_000108.xsd";

    private static final Map<Medfolgende_barn_begrunnelser, BarnAvslagBegrunnelseKode> BARN_AVSLAG_BEGRUNNELSE_KODE_MAP
        = Map.ofEntries(
        entry(Medfolgende_barn_begrunnelser.OVER_18_AR, BarnAvslagBegrunnelseKode.OVER_18_AAR),
        entry(Medfolgende_barn_begrunnelser.IKKE_SOEKERS_BARN, BarnAvslagBegrunnelseKode.IKKE_SOEKERS_BARN),
        entry(Medfolgende_barn_begrunnelser.IKKE_BOSATT_I_NORGE, BarnAvslagBegrunnelseKode.IKKE_BOSATT_I_NORGE),
        entry(Medfolgende_barn_begrunnelser.MANGLER_OPPLYSNINGER, BarnAvslagBegrunnelseKode.MANGLER_OPPLYSNINGER));

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevdata) throws JAXBException, SAXException {
        BrevDataInnvilgelse brevDataInnvilgelse = (BrevDataInnvilgelse) brevdata;

        VedleggMapper vedleggMapper = new VedleggMapper(behandling, resultat);
        vedleggMapper.map(brevDataInnvilgelse.getVedleggA1());

        Fag fag = mapFag(behandling, resultat, brevDataInnvilgelse);

        JAXBElement<BrevdataType> brevdataTypeJAXBElement = lagBrevdataType(fellesType, navFelles, fag, vedleggMapper.hent());
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataInnvilgelse brevdata) {
        Fag fag = new Fag();

        fag.setBehandlingstype(BehandlingstypeKodeMapper.hentBehandlingstypeKode(behandling));
        fag.setSakstype(SakstypeKode.valueOf(behandling.getFagsak().getType().getKode()));

        AvklartVirksomhet hovedvirksomhet = brevdata.getHovedvirksomhet();
        if (hovedvirksomhet.erArbeidsgiver()) {
            fag.setArbeidsgiver(hovedvirksomhet.navn);
        }
        fag.setYrkesaktivitet(YrkesaktivitetsKode.fromValue(hovedvirksomhet.yrkesaktivitet.getKode()));

        fag.setInngangsvilkårbegrunnelse(InngangsvilkaarBegrunnelseKode.EOS_BORGER);
        fag.setBostedsland(brevdata.getBostedsland());
        fag.setTrygdemyndighetsland(brevdata.getTrygdemyndighetsland());

        MottatteOpplysningerData grunnlagData = behandling.getMottatteOpplysninger().getMottatteOpplysningerData();

        fag.setArbeidsland(brevdata.getArbeidsland());
        fag.setFlaggland(brevdata.getArbeidsland());

        if (!grunnlagData.maritimtArbeid.isEmpty()) {
            MaritimtArbeid maritimtArbeid = grunnlagData.maritimtArbeid.iterator().next();
            fag.setFlaggland(maritimtArbeid.getFlaggLandkode());
            if (Fartsomrader.INNENRIKS == maritimtArbeid.getFartsomradeKode()) {
                fag.setArbeidPåTerritorialfarvann(JA);
                fag.setArbeidsland(Landkoder.valueOf(maritimtArbeid.getTerritorialfarvannLandkode()).getBeskrivelse());
            }
        }

        if (brevdata.getAvklartMaritimType() == Maritimtyper.SKIP) {
            fag.setArbeidPåSkip(JA);
        }
        if (brevdata.getAvklartMaritimType() == Maritimtyper.SOKKEL) {
            fag.setArbeidPåSokkel(JA);
        }

        brevdata.getAnmodningsperiodesvar().map(AnmodningsperiodeSvar::getBegrunnelseFritekst)
            .ifPresent(fag::setBegrunnelseFritekst);

        brevdata.getAnmodningsperiodesvar()
            .map(AnmodningsperiodeSvar::getAnmodningsperiodeSvarType)
            .map(Anmodningsperiodesvartyper::getKode)
            .map(AnmodningsPeriodeSvarTypeKode::valueOf)
            .ifPresent(fag::setAnmodningsPeriodeSvarType);

        Lovvalgsperiode periode = brevdata.getLovvalgsperiode();
        boolean erStorbritannia = Arrays.stream(Lovvalgbestemmelser_konv_efta_storbritannia.values()).anyMatch(bestemmelse -> bestemmelse == periode.getBestemmelse());

        fag.setLovvalgsbestemmelse(LovvalgsbestemmelseKodeMapper.map(
            erStorbritannia ? KonvEftaStorbritanniaLovvalgbestemmelser.GB_KONV_LOVVALGBESTEMMELSE_MAP.get(periode.getBestemmelse()) : periode.getBestemmelse())
        );

        fag.setLovvalgsperiode(new LovvalgsperiodeType()
            .withFomDato(lagXmlDato(periode.getFom()))
            .withTomDato(lagXmlDato(periode.getTom()))
        );

        if (periode.getTilleggsbestemmelse() != null) {
            fag.setTilleggsbestemmelse(TilleggsbestemmelseKodeMapper.map(
                erStorbritannia ? KonvEftaStorbritanniaLovvalgbestemmelser.GB_KONV_TILLEGGBESTEMMELSE_MAP.get(periode.getTilleggsbestemmelse()) : periode.getTilleggsbestemmelse())
            );
        }
        if (brevdata.getBegrunnelseKode() != null) {
            fag.setEndretPeriodeBegrunnelse(EndretPeriodeBegrunnelseKode.fromValue(brevdata.getBegrunnelseKode()));
        }

        Set<VilkaarBegrunnelse> art121Begrunnelser = resultat.hentVilkaarbegrunnelser(FO_883_2004_ART12_1, KONV_EFTA_STORBRITANNIA_ART14_1, KONV_EFTA_STORBRITANNIA_ART16_1);
        fag.setArt121Begrunnelse(mapArt121BegrunnelseType(art121Begrunnelser));

        if (brevdata.getTuristskip()) {
            fag.setVilkår(VilkaarKode.FTRL_2_12_UNNTAK_TURISTSKIP);
        }

        Set<VilkaarBegrunnelse> art122Begrunnelser = resultat.hentVilkaarbegrunnelser(FO_883_2004_ART12_2, KONV_EFTA_STORBRITANNIA_ART14_2, KONV_EFTA_STORBRITANNIA_ART16_3);
        fag.setArt122Begrunnelse(mapArt122BegrunnelseType(art122Begrunnelser));

        fag.setFritekst(brevdata.getFritekst());

        if (resultat.getVedtakMetadata() != null) {
            fag.setVedtaksType(tilVedtaksTypeKode(resultat.getVedtakMetadata().getVedtakstype()));
        }

        if (brevdata.getArt16UtenArt12()) {
            fag.setArt16UtenArt12(JA);
        }

        if (brevdata.getAvklarteMedfolgendeBarn().finnes()) {
            fag.setErVurderingLovvalgBarn(JA);
            fag.setAntallBarnOmfattetAvNorskTrygd(
                BigInteger.valueOf(brevdata.getAvklarteMedfolgendeBarn().getFamilieOmfattetAvNorskTrygd().size())
            );
            fag.setBarnIkkeOmfattetAvNorskTrygdListe(hentBarnIkkeOmfattetAvNorskTrygd(brevdata.getAvklarteMedfolgendeBarn()));
            fag.setBarnOmfattetAvNorskTrygdListe(hentBarnOmfattetAvNorskTrygd(brevdata.getAvklarteMedfolgendeBarn()));
            brevdata.getAvklarteMedfolgendeBarn().hentBegrunnelseFritekst().ifPresent(fag::setMedfoelgendeBarnFritekst);
        } else {
            fag.setAntallBarnOmfattetAvNorskTrygd(BigInteger.valueOf(0));
        }

        return fag;
    }

    private VedtaksTypeKode tilVedtaksTypeKode(Vedtakstyper vedtakstype) {
        if (vedtakstype == null) {
            return null;
        }

        return switch (vedtakstype) {
            case FØRSTEGANGSVEDTAK -> VedtaksTypeKode.FOERSTEGANGSVEDTAK;
            case KORRIGERT_VEDTAK -> VedtaksTypeKode.KORRIGERT_VEDTAK;
            case OMGJØRINGSVEDTAK -> VedtaksTypeKode.OMGJOERINGSVEDTAK;
            case ENDRINGSVEDTAK -> null; //Brev har ikke koder for ENDRINGSVEDTAK
            default -> throw new TekniskException("Vedtakstype " + vedtakstype + " kan ikke mappes til VedtaksTypeKode");
        };
    }

    private BarnOmfattetAvNorskTrygdListeType hentBarnOmfattetAvNorskTrygd(AvklarteMedfolgendeFamilie avklarteMedfolgendeBarn) {
        List<BarnInnvilgelseType> barnInnvilgelse = avklarteMedfolgendeBarn.getFamilieOmfattetAvNorskTrygd().stream()
            .map(this::lagBarnInnvilgelseType)
            .collect(Collectors.toList());
        return new BarnOmfattetAvNorskTrygdListeType()
            .withBarnInnvilgelse(barnInnvilgelse);
    }

    private BarnIkkeOmfattetAvNorskTrygdListeType hentBarnIkkeOmfattetAvNorskTrygd(AvklarteMedfolgendeFamilie avklarteMedfolgendeBarn) {
        List<BarnAvslagType> barnAvslag = new ArrayList<>();
        for (IkkeOmfattetFamilie medfolgendeBarn : avklarteMedfolgendeBarn.getFamilieIkkeOmfattetAvNorskTrygd()) {
            barnAvslag.add(lagBarnAvslagType(medfolgendeBarn));
        }
        return new BarnIkkeOmfattetAvNorskTrygdListeType()
            .withBarnAvslag(barnAvslag);
    }

    private BarnInnvilgelseType lagBarnInnvilgelseType(OmfattetFamilie omfattetBarn) {
        return new BarnInnvilgelseType()
            .withBarnOmfattetAvNorskTrygd(omfattetBarn.getSammensattNavn())
            .withBarnFodselsnummer(omfattetBarn.getIdent());
    }

    private BarnAvslagType lagBarnAvslagType(IkkeOmfattetFamilie ikkeOmfattetBarn) {
        return new BarnAvslagType()
            .withBarnAvslagBegrunnelse(tilBarnAvslagBegrunnelseKode(ikkeOmfattetBarn.getBegrunnelse()))
            .withBarnFodselsnummer(ikkeOmfattetBarn.getIdent())
            .withBarnIkkeOmfattetAvNorskTrygd(ikkeOmfattetBarn.getSammensattNavn());
    }

    private BarnAvslagBegrunnelseKode tilBarnAvslagBegrunnelseKode(String begrunnelse) {
        if (begrunnelse == null) {
            return null;
        } else if (BARN_AVSLAG_BEGRUNNELSE_KODE_MAP.containsKey(Medfolgende_barn_begrunnelser.valueOf(begrunnelse))) {
            return BARN_AVSLAG_BEGRUNNELSE_KODE_MAP.get(Medfolgende_barn_begrunnelser.valueOf(begrunnelse));
        }
        throw new TekniskException("Ukjent begrunnelse " + begrunnelse + " kan ikke mappes til BarnAvslagBegrunnelseKode");
    }

    private static JAXBElement<BrevdataType> lagBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag, VedleggType vedlegg) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = new BrevdataType()
            .withFelles(fellesType)
            .withNAVFelles(navFelles)
            .withFag(fag)
            .withVedlegg(vedlegg);
        return factory.createBrevdata(brevdataType);
    }
}
