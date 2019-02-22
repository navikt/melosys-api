package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.List;
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
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.domain.util.SoeknadUtils;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataUtils;
import no.nav.melosys.service.dokument.brev.BrevDataVedlegg;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import org.xml.sax.SAXException;

public final class InnvilgelsesbrevMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000108.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevdata) throws JAXBException, SAXException, TekniskException {
        BrevDataVedlegg brevDataVedlegg = ((BrevDataVedlegg) brevdata);

        VedleggMapper vedleggMapper = new VedleggMapper(behandling, resultat);
        vedleggMapper.map(brevDataVedlegg);

        // Bruker A1-vedlegget sine brevdata, da disse er ett supersett av de
        // ordinære brevdata innvilgelsesbrev trenger.
        Fag fag = mapFag(behandling, resultat, brevDataVedlegg.brevDataA1);

        JAXBElement<BrevdataType> brevdataTypeJAXBElement = lagBrevdataType(fellesType, navFelles, fag, vedleggMapper.hent());
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataA1 brevdata) throws TekniskException {
        Fag fag = new Fag();
        fag.setBehandlingstype(BehandlingstypeKode.valueOf(behandling.getType().getKode()));
        fag.setSakstype(SakstypeKode.valueOf(behandling.getFagsak().getType().getKode()));
        Virksomhet arbeidsgiver = brevdata.norskeVirksomheter.iterator().next();
        fag.setArbeidsgiver(arbeidsgiver.navn);

        // Henter ut Landkode fra Arbeidssteder(Fysiske og Maritime) og Oppholdsland
        String arbeidslandSomTekst = hentLandFraArbeidsstederOgOppholdsland(behandling, brevdata);

        Landkoder arbeidsland = Landkoder.valueOf(arbeidslandSomTekst);
        fag.setArbeidsland(arbeidsland.getBeskrivelse());

        Set<Lovvalgsperiode> perioder = resultat.getLovvalgsperioder();
        if (perioder.size() != 1) {
            throw new UnsupportedOperationException(String.format("Antall lovvalgsperioder (%s) ulik 1 støttes ikke i første versjon av Melosys.",
                perioder.size()));
        }
        Lovvalgsperiode periode = perioder.iterator().next();
        fag.setLovvalgsbestemmelse(LovvalgsbestemmelseKode.fromValue(periode.getBestemmelse().getKode()));
        fag.setLovvalgsperiode(LovvalgsperiodeType.builder().withFomDato(lagXmlDato(periode.getFom()))
            .withTomDato(lagXmlDato(periode.getTom()))
            .build());

        // TODO: Ikke avklart/kjent i denne omgang, må fylles ut seinere.
        // Kan utledes ved å se på statsborgerskapet til søkeren i perioden,
        // evt. ta i bruk nytt predikat i regel-modulen (PR som ikke er flettet
        // ennå, http://stash.devillo.no/projects/MELOSYS/repos/melosys-regler/pull-requests/14/overview).
        fag.setTredjelandsborger("false");
        return fag;
    }

    private static String hentLandFraArbeidsstederOgOppholdsland(Behandling behandling, BrevDataA1 brevdata) throws TekniskException {
        String arbeidslandSomTekst;
        if (!brevdata.arbeidssteder.isEmpty()) {
            Arbeidssted arbeidssted = brevdata.arbeidssteder.iterator().next();
            arbeidslandSomTekst = arbeidssted.landKode;
        }
        else {
            SoeknadDokument soeknadDokument = SaksopplysningerUtils.hentSøknadDokument(behandling);
            List<String> land = SoeknadUtils.hentLand(soeknadDokument);
            if (land.isEmpty()) {
                throw new TekniskException("Ingen land funnet");
            }
            arbeidslandSomTekst = land.get(0);
        }
        return arbeidslandSomTekst;
    }

    private static XMLGregorianCalendar lagXmlDato(LocalDate dato) {
        try {
            return BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone(dato);
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Kan ikke lage DatatypeConverterFactory.", e);
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
