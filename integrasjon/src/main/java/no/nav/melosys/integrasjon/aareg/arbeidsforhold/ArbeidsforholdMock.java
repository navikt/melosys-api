package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.HentArbeidsforholdHistorikkSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.HentArbeidsforholdHistorikkResponse;

public class ArbeidsforholdMock implements ArbeidsforholdConsumer {
    @SuppressWarnings("resource")
    @Override
    public FinnArbeidsforholdPrArbeidstakerResponse finnArbeidsforholdPrArbeidstaker(FinnArbeidsforholdPrArbeidstakerRequest request) throws FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning, FinnArbeidsforholdPrArbeidstakerUgyldigInput {
        String ident = request.getIdent().getIdent();

        List<String> støttet = Arrays.asList("88888888884", "88888888885", "88888888886", "99999999999", "FJERNET");
        if (!støttet.contains(ident)) {
            throw new IntegrasjonException("ident " + ident + " er ikke støttet.");
        }

        no.nav.tjeneste.virksomhet.arbeidsforhold.v3.FinnArbeidsforholdPrArbeidstakerResponse response = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.arbeidsforhold.v3.FinnArbeidsforholdPrArbeidstakerResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            InputStream is = getClass().getClassLoader().getResourceAsStream("mock/arbeidsforhold/" + ident + ".xml");
            Object xmlBean = unmarshaller.unmarshal(is);
            response = (no.nav.tjeneste.virksomhet.arbeidsforhold.v3.FinnArbeidsforholdPrArbeidstakerResponse) xmlBean;
            return response.getParameters();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public HentArbeidsforholdHistorikkResponse hentArbeidsforholdHistorikk(HentArbeidsforholdHistorikkRequest request) throws HentArbeidsforholdHistorikkSikkerhetsbegrensning, HentArbeidsforholdHistorikkArbeidsforholdIkkeFunnet {
        String arbeidsforholdId = String.valueOf(request.getArbeidsforholdId());

        if (!arbeidsforholdId.equals("12608035")) {
            throw new IntegrasjonException("arbeidsforholdsID " + arbeidsforholdId + " er ikke støttet.");
        }

        no.nav.tjeneste.virksomhet.arbeidsforhold.v3.HentArbeidsforholdHistorikkResponse response = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.arbeidsforhold.v3.HentArbeidsforholdHistorikkResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            InputStream is = getClass().getClassLoader().getResourceAsStream("mock/arbeidsforhold/" + arbeidsforholdId + ".xml");
            Object xmlBean = unmarshaller.unmarshal(is);
            response = (no.nav.tjeneste.virksomhet.arbeidsforhold.v3.HentArbeidsforholdHistorikkResponse) xmlBean;
            return response.getParameters();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
