package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.dok.melosysbrev._000108.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.BehandlingstypeKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.LovvalgsbestemmelseKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaType;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataUtils;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

import org.xml.sax.SAXException;

import static no.nav.melosys.domain.avklartefakta.AvklartefaktaType.AG_FORRETNINGSLAND;

public final class InnvilgelsesbrevMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000108.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevdata) throws JAXBException, SAXException, TekniskException {
        Fag fag = mapFag(behandling, resultat, (BrevDataA1) brevdata);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = lagBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private final Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataA1 brevdata) {
        Fag fag = new Fag();
        fag.setBehandlingstype(BehandlingstypeKode.valueOf(behandling.getType().getKode()));
        fag.setSakstype(SakstypeKode.valueOf(behandling.getFagsak().getType().getKode()));
        Virksomhet arbeidsgiver = brevdata.norskeVirksomheter.iterator().next();
        fag.setArbeidsgiver(arbeidsgiver.navn);
        // Slå opp arbeidsland i avklartefakte, fall tilbake på søknaden (kan overkjøres av saksbehandler for sokkel/skip).
        String arbeidsland = finnAvklartFaktum(resultat, AG_FORRETNINGSLAND).map(Avklartefakta::getSubjekt)
            .orElseGet(() -> hentArbeidslandFraSøknaden(behandling));
        fag.setArbeidsland(arbeidsland);
        Set<Lovvalgsperiode> perioder = resultat.getLovvalgsperioder();
        if (perioder.size() != 1) {
            throw new UnsupportedOperationException(String.format("Antall lovvalgsperioder (%s) ulik 1 støttes ikke i første versjon av Melosys.",
                    perioder.size()));
        }
        Lovvalgsperiode periode = perioder.iterator().next();
        fag.setLovvalgsbestemmelse(LovvalgsbestemmelseKode.fromValue(periode.getBestemmelse().getKode()));
        fag.setPeriode(PeriodeType.builder().withFomDato(lagXmlDato(periode.getFom()))
            .withTomDato(lagXmlDato(periode.getTom()))
            .build());
        // TODO: Ikke avklart/kjent i denne omgang, må fylles ut seinere.
        // Kan utledes ved å se på statsborgerskapet til søkeren i perioden,
        // evt. ta i bruk nytt predikat i regel-modulen (PR som ikke er flettet
        // ennå, http://stash.devillo.no/projects/MELOSYS/repos/melosys-regler/pull-requests/14/overview).
        fag.setTredjelandsborger("false");
        // Er alltid lik arbeidsland for 12.x-brevene i leveranse 1.
        // Feltet kan bli slått sammen med førstnevnte dersom avklaring ikke
        // avdekker noe behov for to separate felt.
        fag.setTrygdemyndighetsland(arbeidsland);
        return fag;
    }

    private static String hentArbeidslandFraSøknaden(Behandling behandling) {
        try {
            return SaksopplysningerUtils.hentSøknadDokument(behandling).arbeidUtland.iterator().next().adresse.landKode;
        } catch (TekniskException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Optional<Avklartefakta> finnAvklartFaktum(Behandlingsresultat resultat, AvklartefaktaType type) {
        return resultat.getAvklartefakta().stream()
            .filter(f -> f.getType() == type && f.getFakta().equals("TRUE"))
            .findFirst();
    }

    private static XMLGregorianCalendar lagXmlDato(LocalDate dato) {
        try {
            return BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone(dato);
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Kan ikke lage DatatypeConverterFactory.", e);
        }
    }

    private static JAXBElement<BrevdataType> lagBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = BrevdataType.builder()
            .withFelles(fellesType)
            .withNAVFelles(navFelles)
            .withFag(fag)
            .build();
        return factory.createBrevdata(brevdataType);
    }

}
