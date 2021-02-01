package no.nav.melosys.service.dokument.brev.mapper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
import no.nav.melosys.domain.behandlingsgrunnlag.data.MaritimtArbeid;
import no.nav.melosys.domain.familie.AvklarteMedfolgendeBarn;
import no.nav.melosys.domain.familie.IkkeOmfattetBarn;
import no.nav.melosys.domain.familie.OmfattetBarn;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader;
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import org.xml.sax.SAXException;

import static java.util.Map.entry;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART12_1;
import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART12_2;
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
            if (Fartsomrader.INNENRIKS == maritimtArbeid.fartsomradeKode) {
                fag.setArbeidPåTerritorialfarvann(JA);
            }
        }

        if (brevdata.avklartMaritimType == Maritimtyper.SKIP) {
            fag.setArbeidPåSkip(JA);
        }
        if (brevdata.avklartMaritimType == Maritimtyper.SOKKEL) {
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

        if (brevdata.avklarteMedfolgendeBarn.finnes()) {
            fag.setErVurderingLovvalgBarn(JA);
            fag.setAntallBarnOmfattetAvNorskTrygd(
                BigInteger.valueOf(brevdata.avklarteMedfolgendeBarn.barnOmfattetAvNorskTrygd.size())
            );
            fag.setBarnIkkeOmfattetAvNorskTrygdListe(hentBarnIkkeOmfattetAvNorskTrygd(brevdata.avklarteMedfolgendeBarn));
            fag.setBarnOmfattetAvNorskTrygdListe(hentBarnOmfattetAvNorskTrygd(brevdata.avklarteMedfolgendeBarn));
            if (brevdata.avklarteMedfolgendeBarn.hentBegrunnelseFritekst().isPresent()) {
                fag.setMedfoelgendeBarnFritekst(brevdata.avklarteMedfolgendeBarn.hentBegrunnelseFritekst().get());
            }
        } else {
            fag.setAntallBarnOmfattetAvNorskTrygd(BigInteger.valueOf(0));
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

    private BarnOmfattetAvNorskTrygdListeType hentBarnOmfattetAvNorskTrygd(AvklarteMedfolgendeBarn avklarteMedfolgendeBarn) {
        List<BarnInnvilgelseType> barnInnvilgelse = avklarteMedfolgendeBarn.barnOmfattetAvNorskTrygd.stream()
            .map(InnvilgelsesbrevMapper::lagBarnInnvilgelseType)
            .collect(Collectors.toList());
        return BarnOmfattetAvNorskTrygdListeType.builder()
            .withBarnInnvilgelse(barnInnvilgelse).build();
    }

    private BarnIkkeOmfattetAvNorskTrygdListeType hentBarnIkkeOmfattetAvNorskTrygd(AvklarteMedfolgendeBarn avklarteMedfolgendeBarn) throws TekniskException {
        List<BarnAvslagType> barnAvslag = new ArrayList<>();
        for (IkkeOmfattetBarn medfolgendeBarn : avklarteMedfolgendeBarn.barnIkkeOmfattetAvNorskTrygd) {
            barnAvslag.add(lagBarnAvslagType(medfolgendeBarn));
        }
        return BarnIkkeOmfattetAvNorskTrygdListeType.builder()
            .withBarnAvslag(barnAvslag).build();
    }

    private static BarnInnvilgelseType lagBarnInnvilgelseType(OmfattetBarn omfattetBarn) {
        return BarnInnvilgelseType.builder().withBarnOmfattetAvNorskTrygd(omfattetBarn.sammensattNavn).build();
    }

    private BarnAvslagType lagBarnAvslagType(IkkeOmfattetBarn ikkeOmfattetBarn) throws TekniskException {
        return BarnAvslagType.builder()
            .withBarnAvslagBegrunnelse(tilBarnAvslagBegrunnelseKode(ikkeOmfattetBarn.begrunnelse))
            .withBarnIkkeOmfattetAvNorskTrygd(ikkeOmfattetBarn.sammensattNavn).build();
    }

    private BarnAvslagBegrunnelseKode tilBarnAvslagBegrunnelseKode(Medfolgende_barn_begrunnelser begrunnelse) throws TekniskException {
        if (begrunnelse == null) {
            return null;
        } else if (BARN_AVSLAG_BEGRUNNELSE_KODE_MAP.containsKey(begrunnelse)) {
            return BARN_AVSLAG_BEGRUNNELSE_KODE_MAP.get(begrunnelse);
        }
        throw new TekniskException("Ukjent begrunnelse " + begrunnelse + " kan ikke mappes til BarnAvslagBegrunnelseKode");
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
