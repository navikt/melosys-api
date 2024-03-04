package no.nav.melosys.service.dokument.brev.mapper;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000083.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000083.ObjectFactory;
import no.nav.dok.melosysbrev._000083.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
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
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevdata) throws JAXBException, SAXException {
        BrevDataInnvilgelseFlereLand brevDataInnvilgelse = (BrevDataInnvilgelseFlereLand) brevdata;

        VedleggMapper vedleggMapper = new VedleggMapper(behandling, resultat);
        vedleggMapper.map(brevDataInnvilgelse.getVedleggA1());

        Fag fag = mapFag(behandling, resultat, brevDataInnvilgelse);

        JAXBElement<BrevdataType> brevdataTypeJAXBElement = lagBrevdataType(fellesType, navFelles, fag, vedleggMapper.hent());
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataInnvilgelseFlereLand brevdata) {
        Fag fag = new Fag();

        fag.setBehandlingstype(BehandlingstypeKodeMapper.hentBehandlingstypeKode(behandling));

        // Logikk i brev benytter antallArbeidsgivere for å aktivere tekst med arbeidsgiver eller arbeidsgiverListe
        int antallArbeidsgivere = brevdata.getArbeidsgivere().size();
        fag.setAntallArbeidsgivere(BigInteger.valueOf(antallArbeidsgivere));
        if (antallArbeidsgivere == 1) {
            AvklartVirksomhet avklartVirksomhet = brevdata.getArbeidsgivere().iterator().next();
            fag.setArbeidsgiver(avklartVirksomhet.navn);
        }
        fag.setArbeidsgiverListe(mapArbeidsgiverListe(brevdata.getArbeidsgivere()));

        // AntallArbeidsland avgjør om brevet bruker arbeidsland eller arbeidslandListe
        int antallArbeidsland = brevdata.getAlleArbeidsland().size();
        fag.setAntallArbeidsland(BigInteger.valueOf(antallArbeidsland));
        if (antallArbeidsland == 1) {
            String arbeidsland = brevdata.getAlleArbeidsland().iterator().next();
            fag.setArbeidsland(arbeidsland);
        }
        fag.setArbeidslandListe(mapArbeidslandListe(brevdata.getAlleArbeidsland()));
        fag.setErUkjenteEllerAlleEosLand(Boolean.toString(brevdata.getUkjenteEllerAlleEosLand()));

        fag.setBostedsland(brevdata.getBostedsland());
        if (brevdata.getTrydemyndighetsland() != null) {
            fag.setTrygdemyndighetsland(brevdata.getTrydemyndighetsland().getBeskrivelse());
        } else {
            fag.setTrygdemyndighetsland(" ");
        }

        // Virksomhetsland er arbeidsland for selvstendig næringsdrivende
        int antallVirksomhetsland = brevdata.getAlleArbeidsland().size();
        fag.setAntallVirksomhetsland(BigInteger.valueOf(antallVirksomhetsland));
        if (antallVirksomhetsland == 1) {
            String virksomhetsland = brevdata.getAlleArbeidsland().iterator().next();
            fag.setVirksomhetsland(virksomhetsland);
        }
        fag.setVirksomhetslandListe(mapVirksomhetsListe(brevdata.getAlleArbeidsland()));

        if (brevdata.getAvklartMaritimTypeSkip()) {
            fag.setArbeidPåSkip(JA);
        }
        if (brevdata.getAvklartMaritimTypeSokkel()) {
            fag.setArbeidPåSokkel(JA);
        }

        if (brevdata.getBegrensetPeriode()) {
            fag.setBegrensetPeriode(JA);
        }

        Lovvalgsperiode periode = brevdata.getLovvalgsperiode();
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

        fag.setFritekst(brevdata.getFritekst());

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
            default -> throw new TekniskException("Ukjent vedtakstype " + vedtakstype + " kan ikke mappes til VedtaksTypeKode");
        };
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
        arbeidslandListeType.getLand().addAll(
            alleArbeidsland.stream()
                .sorted()
                .map(this::tilArbeidslandType)
                .collect(Collectors.toList())
        );

        return arbeidslandListeType;
    }

    private ArbeidslandType tilArbeidslandType(String arbeidsland) {
        ArbeidslandType arbeidslandType = new ArbeidslandType();
        arbeidslandType.setArbeidsland(arbeidsland);
        return arbeidslandType;
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
