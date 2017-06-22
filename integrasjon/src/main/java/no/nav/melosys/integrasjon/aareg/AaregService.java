package no.nav.melosys.integrasjon.aareg;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import no.nav.melosys.domain.Arbeidsforhold;
import no.nav.melosys.domain.ArbeidsforholdsType;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdConsumer;
import no.nav.melosys.integrasjon.felles.IntegrasjonException;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Aktoer;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.AnsettelsesPeriode;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsavtale;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforholdstyper;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidstidsordninger;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Fartsomraader;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.MaritimArbeidsavtale;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.PermisjonOgPermittering;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Skipsregistre;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Skipstyper;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Utenlandsopphold;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;

public class AaregService implements AaregFasade {

    private static final Logger log = LoggerFactory.getLogger(AaregService.class);

    private ArbeidsforholdConsumer arbeidsforholdConsumer;

    private static final String REGELVERK_ALLE = "ALLE";

    @Autowired
    public AaregService(ArbeidsforholdConsumer arbeidsforholdConsumer) {
        this.arbeidsforholdConsumer = arbeidsforholdConsumer;
    }

    /**
     *  Henter en liste av arbeidsforhold for en arbeidstaker.
     * @param fnr Fødselsnummer til arbeidstaker.
     * @return
     */
    @Override
    public List<Arbeidsforhold> finnArbeidsforholdPrArbeidstaker(String fnr) throws IntegrasjonException {
        FinnArbeidsforholdPrArbeidstakerRequest request = new FinnArbeidsforholdPrArbeidstakerRequest();
        NorskIdent ident = new NorskIdent();
        ident.setIdent(fnr);
        request.setIdent(ident);
        Regelverker regelverker = new Regelverker();
        regelverker.setKodeverksRef(REGELVERK_ALLE);
        request.setRapportertSomRegelverk(regelverker);

        FinnArbeidsforholdPrArbeidstakerResponse response = null;
        try {
            response = arbeidsforholdConsumer.finnArbeidsforholdPrArbeidstaker(request);
        } catch (FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning begrensning) {
            throw new IntegrasjonException(begrensning);
        } catch (FinnArbeidsforholdPrArbeidstakerUgyldigInput ugyldigInput) { // NOSONAR
            throw new IntegrasjonException(ugyldigInput);
        }

        List<Arbeidsforhold> resultat = new ArrayList<>();

        List<no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold> liste;
        liste = response.getArbeidsforhold();

        for (no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold a : liste) {
            Arbeidsforhold arbeidsforhold = new Arbeidsforhold();

            // Organisasjonsnummer -  til den virksomheten hvor arbeidsforholdet er knyttet
            // TODO Francois hvordan finner vi ut av nummeret ut fra aktoer
            Aktoer arbeidsgiver = a.getArbeidsgiver();

            // Opplysningspliktig -  juridisk enhet (vesentlig virksomhet i  Norge?)
            Aktoer opplysningspliktig = a.getOpplysningspliktig();

            // Startdato og slutdato arbeidsforhold
            AnsettelsesPeriode periode = a.getAnsettelsesPeriode();
            XMLGregorianCalendar fraOgMed = periode.getPeriode().getFom();
            XMLGregorianCalendar tilOgMed = periode.getPeriode().getTom();
            arbeidsforhold.setAnsettelseFra(xmlTilLocalDate(fraOgMed));
            arbeidsforhold.setAnsettelseTil(xmlTilLocalDate(tilOgMed));

            // Dato for første gangs registrering - ikke mulig
            // Dato sist bekreftet
            // TODO

            // Type arbeidsforhold -  "Ordinært", "Maritimt", «Forenklet oppgjørsordning»...
            Arbeidsforholdstyper type = a.getArbeidsforholdstype();
            if (type != null) {
                arbeidsforhold.setType(ArbeidsforholdsType.getFraKode(type.getValue()));
            } else {
                throw new IntegrasjonException("Arbeidsforholdstyper er null for arbeidsforhold " + a.getArbeidsforholdID());
            }


            // TODO

            // Permisjon. Til å gjøre kontroll etter vedtak er innvilget. Ikke direkte vilkårvurdering.
            List<PermisjonOgPermittering> permisjoner = a.getPermisjonOgPermittering();

            // Utenlandsopphold
            // startdato + sluttdato
            List<Utenlandsopphold> oppholdListe = a.getUtenlandsopphold();

            // Arbeidsavtaler
            List<Arbeidsavtale> avtaler = a.getArbeidsavtale();
            arbeidsforhold.setArbeidsavtaleListe(tilDomeneModell(avtaler));






            resultat.add(arbeidsforhold);
        }


        return resultat;
    }

    private LocalDate xmlTilLocalDate(XMLGregorianCalendar xmlCal) {
        if (xmlCal == null) {
            return null;
        }
        return xmlCal.toGregorianCalendar().toZonedDateTime().toLocalDate();
    }

    private List<no.nav.melosys.domain.Arbeidsavtale> tilDomeneModell(List<Arbeidsavtale> avtaler) {
        List<no.nav.melosys.domain.Arbeidsavtale> arbeidsavtaleListe = new ArrayList<>();
        for (Arbeidsavtale avtale : avtaler) {
            no.nav.melosys.domain.Arbeidsavtale domeneAvtale = new no.nav.melosys.domain.Arbeidsavtale();

            // TODO Perioder?

            // Yrkesbetegnelse. Nødvendig for statistikk til EU
            domeneAvtale.setYrke(avtale.getYrke().getValue());

            // Stillingsprosent TODO avventer Anders
            //avtale.getStillingsprosent();
            //avtale.getBeregnetStillingsprosent();


            // Lønnstype. TODO Francois (Ingen vits å ta i bruk?)
            //avtale.getAvloenningstype();

            // For å   kunne vurderehvilke gruppen bruker faller under og hvilke artiklene skal vurderes
            Arbeidstidsordninger arbeidstidsordning = avtale.getArbeidstidsordning();
            if (arbeidstidsordning != null) {
                arbeidstidsordning.getValue();
            }

            // Maritimt arbeidsavtale
            if (avtale instanceof MaritimArbeidsavtale) {
                MaritimArbeidsavtale maritimArbeidsavtale = (MaritimArbeidsavtale) avtale;

                Fartsomraader fartsomraadeXml = maritimArbeidsavtale.getFartsomraade();
                String fartsomraade = (fartsomraadeXml == null) ? null : fartsomraadeXml.getValue();
                domeneAvtale.setFartsområde(fartsomraade);

                Skipsregistre skipsregisterXml = maritimArbeidsavtale.getSkipsregister();
                String skipregister = (skipsregisterXml == null) ? null : skipsregisterXml.getValue();
                domeneAvtale.setSkipsregister(skipregister);

                Skipstyper skipstypeXml = maritimArbeidsavtale.getSkipstype();
                String skipstype = (skipstypeXml == null) ? null : skipstypeXml.getValue();
                domeneAvtale.setSkipstype(skipstype);



            }


        }
        return arbeidsavtaleListe;
    }

}
