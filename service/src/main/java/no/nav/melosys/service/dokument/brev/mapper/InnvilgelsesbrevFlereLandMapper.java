package no.nav.melosys.service.dokument.brev.mapper;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000083.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000083.ObjectFactory;
import no.nav.dok.melosysbrev._000083.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelseFlereLand;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.lagXmlDato;

public final class InnvilgelsesbrevFlereLandMapper implements BrevDataMapper {

    private static final String JA = "true";

    private static final String XSD_LOCATION = "melosysbrev/melosys_000083.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevdata) throws JAXBException, SAXException, TekniskException {
        BrevDataInnvilgelseFlereLand brevDataInnvilgelse = (BrevDataInnvilgelseFlereLand) brevdata;

        VedleggMapper vedleggMapper = new VedleggMapper(behandling, resultat);
        vedleggMapper.map(brevDataInnvilgelse.vedleggA1);

        Fag fag = mapFag(behandling, resultat, brevDataInnvilgelse);

        JAXBElement<BrevdataType> brevdataTypeJAXBElement = lagBrevdataType(fellesType, navFelles, fag, vedleggMapper.hent());
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataInnvilgelseFlereLand brevdata) throws TekniskException {
        Fag fag = new Fag();
        fag.setBehandlingstype(BehandlingstypeKode.valueOf(behandling.getType().getKode()));

        // Logikk i brev benytter antallArbeidsgivere for å aktivere tekst med arbeidsgiver eller arbeidsgiverListe
        int antallArbeidsgivere = brevdata.arbeidsgivere.size();
        fag.setAntallArbeidsgivere(BigInteger.valueOf(antallArbeidsgivere));
        if (antallArbeidsgivere == 1) {
            AvklartVirksomhet avklartVirksomhet = brevdata.arbeidsgivere.iterator().next();
            fag.setArbeidsgiver(avklartVirksomhet.navn);
        }
        fag.setArbeidsgiverListe(mapArbeidsgiverListe(brevdata.arbeidsgivere));

        // AntallArbeidsland avgjør om brevet bruker arbeidsland eller arbeidslandListe
        int antallArbeidsland = brevdata.alleArbeidsland.size();
        fag.setAntallArbeidsland(BigInteger.valueOf(antallArbeidsland));
        if (antallArbeidsland == 1) {
            String arbeidsland = brevdata.alleArbeidsland.iterator().next();
            fag.setArbeidsland(arbeidsland);
        }
        fag.setArbeidslandListe(mapArbeidslandListe(brevdata.alleArbeidsland));

        fag.setBostedsland(brevdata.bostedsland);
        fag.setTrygdemyndighetsland(" "); //TODO: Kun når Norge er utpekt. XSD må tillate EmptyString

        // Virksomhetsland er arbeidsland for selvstendig næringsdrivende
        int antallVirksomhetsland = brevdata.alleArbeidsland.size();
        fag.setAntallVirksomhetsland(BigInteger.valueOf(antallVirksomhetsland));
        if (antallVirksomhetsland == 1) {
            String virksomhetsland = brevdata.alleArbeidsland.iterator().next();
            fag.setVirksomhetsland(virksomhetsland);
        }
        fag.setVirksomhetslandListe(mapVirksomhetsListe(brevdata.alleArbeidsland));

        if (brevdata.avklartMaritimType == Maritimtyper.SKIP) {
            fag.setArbeidPåSkip(JA);
        }
        if (brevdata.avklartMaritimType == Maritimtyper.SOKKEL) {
            fag.setArbeidPåSokkel(JA);
        }

        if (brevdata.erBegrensetPeriode) {
            fag.setBegrensetPeriode(JA);
        }

        if (brevdata.erMarginaltArbeid) {
            fag.setMarginaltArbeid(JA);
        }

        Lovvalgsperiode periode = brevdata.lovvalgsperiode;
        fag.setLovvalgsbestemmelse(LovvalgsbestemmelseKode.fromValue(periode.getBestemmelse().getKode()));
        fag.setLovvalgsperiode(LovvalgsperiodeType.builder()
            .withFomDato(lagXmlDato(periode.getFom()))
            .withTomDato(lagXmlDato(periode.getTom()))
            .build());

        if (periode.getTilleggsbestemmelse() != null) {
            fag.setTilleggsbestemmelse(TilleggsbestemmelseKode.fromValue(periode.getTilleggsbestemmelse().getKode()));
        }

        if (resultat.getVedtakMetadata() != null) {
            fag.setVedtaksType(tilVedtaksTypeKode(resultat.getVedtakMetadata().getVedtakstype()));
        }

        fag.setFritekst(brevdata.fritekst);

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
            default:
                throw new TekniskException("Ukjent vedtakstype " + vedtakstype + " kan ikke mappes til VedtaksTypeKode");
        }
    }

    private VirksomhetslandListeType mapVirksomhetsListe(List<String> næringsdrivendeILand) {
        VirksomhetslandListeType virksomhetslandListeType = new VirksomhetslandListeType();
        for (String land : næringsdrivendeILand) {
            VirksomhetslandType virksomhetslandType = new VirksomhetslandType();
            virksomhetslandType.setVirksomhetsland(land);
            virksomhetslandListeType.getLand().add(virksomhetslandType);
        }
        return virksomhetslandListeType;
    }

    private ArbeidslandListeType mapArbeidslandListe(Collection<String> alleArbeidsland) {
        ArbeidslandListeType arbeidslandListeType = new ArbeidslandListeType();
        for (String arbeidsland : alleArbeidsland) {
            ArbeidslandType arbeidslandType = new ArbeidslandType();
            arbeidslandType.setArbeidsland(arbeidsland);
            arbeidslandListeType.getLand().add(arbeidslandType);
        }
        return arbeidslandListeType;
    }

    private ArbeidsgiverListeType mapArbeidsgiverListe(Collection<AvklartVirksomhet> norskeVirksomheter) {
        ArbeidsgiverListeType arbeidsgiverListeType = new ArbeidsgiverListeType();
        for (AvklartVirksomhet avklartVirksomhet : norskeVirksomheter) {
            ArbeidsgiverType arbeidsgiverType = new ArbeidsgiverType();
            arbeidsgiverType.setArbeidsgiver(avklartVirksomhet.navn);
            arbeidsgiverListeType.getVirksomhet().add(arbeidsgiverType);
        }
        return arbeidsgiverListeType;
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