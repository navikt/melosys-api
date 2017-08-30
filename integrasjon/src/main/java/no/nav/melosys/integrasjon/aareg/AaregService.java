package no.nav.melosys.integrasjon.aareg;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Skipsregistre;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Skipstyper;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Utenlandsopphold;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;

public class AaregService implements AaregFasade {

    private static final Logger log = LoggerFactory.getLogger(AaregService.class);

    private ArbeidsforholdConsumer arbeidsforholdConsumer;

    // Kode for  arbeidsforhold basert på nytt regelverk fra 1.1.2015 (a-ordningen)
    private static final String REGELVERK_A_ORDNINGEN = "A_ORDNINGEN";

    @Autowired
    public AaregService(ArbeidsforholdConsumer arbeidsforholdConsumer) {
        this.arbeidsforholdConsumer = arbeidsforholdConsumer;
    }

    /**
     * Etterspør og returnerer en liste av arbeidsforhold fra AA-registeret for en arbeidstaker.
     * 
     * @param ident Fødselsnummer, D-Nummer, SSN... tilhørende en arbeidstaker
     *
     * @return
     */
    /* FIXME
    @Override
    public List<Arbeidsforhold> finnArbeidsforholdPrArbeidstaker(String ident) throws IntegrasjonException {
        FinnArbeidsforholdPrArbeidstakerRequest request = new FinnArbeidsforholdPrArbeidstakerRequest();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);
        request.setIdent(norskIdent);
        Regelverker regelverker = new Regelverker();
        // Vi har (foreløpig) sagt at vi kun skal hente arbeidsforhold rapportert på nytt regelverk.
        regelverker.setKodeverksRef(REGELVERK_A_ORDNINGEN); // Mulige verdier: FOER_A_ORDNINGEN, A_ORDNINGEN, ALLE
        request.setRapportertSomRegelverk(regelverker);

        // Kall til Aa-registret
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

            // Organisasjonsnummer - til den virksomheten hvor arbeidsforholdet er knyttet
            Aktoer arbeidsgiverXml = a.getArbeidsgiver();
            Arbeidsgiver arbeidsgiver = new Arbeidsgiver();
            if (arbeidsgiverXml instanceof Organisasjon) {
                // TODO Francois Test
                arbeidsgiver.setOrgNummer(((Organisasjon) arbeidsgiverXml).getOrgnummer());
            }
            arbeidsgiver.setAktørId(arbeidsgiverXml.getAktoerId());

            // Opplysningspliktig - juridisk enhet (vesentlig virksomhet i Norge?)
            Aktoer opplysningspliktig = a.getOpplysningspliktig();

            // Startdato og slutdato arbeidsforhold
            AnsettelsesPeriode periode = a.getAnsettelsesPeriode();
            XMLGregorianCalendar fraOgMed = periode.getPeriode().getFom();
            XMLGregorianCalendar tilOgMed = periode.getPeriode().getTom();
            arbeidsforhold.setAnsettelseFra(xmlTilLocalDate(fraOgMed));
            arbeidsforhold.setAnsettelseTil(xmlTilLocalDate(tilOgMed));

            // Dato for første gangs registrering - ikke mulig?
            // TODO Spørre Anders dato == a.getOpprettelsestidspunkt(); ?

            // Dato sist bekreftet
            arbeidsforhold.setSistBekreftet(xmlTilLocalDate(a.getSistBekreftet()));

            // Type arbeidsforhold - "Ordinært", "Maritimt", «Forenklet oppgjørsordning»...
            // Ikke påkrevd i tjenesten.
            Arbeidsforholdstyper type = a.getArbeidsforholdstype();
            if (type != null) {
                arbeidsforhold.setType(ArbeidsforholdsType.valueOf(type.getValue()));
            }

            // PermisjonOgPermittering. Til å gjøre kontroll etter vedtak er innvilget. Ikke direkte vilkårvurdering.
            // TODO Hentes med et separat kall?
            List<no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.PermisjonOgPermittering> permisjoner = a.getPermisjonOgPermittering();
            for (no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.PermisjonOgPermittering p : permisjoner) {
                PermisjonOgPermittering permisjon = new PermisjonOgPermittering();
                permisjon.setPermisjonsId(p.getPermisjonsId());
                permisjon.setStartDato(xmlTilLocalDate(p.getPermisjonsPeriode().getFom()));
                permisjon.setSluttDato(xmlTilLocalDate(p.getPermisjonsPeriode().getTom()));
                permisjon.setProsent(p.getPermisjonsprosent());
                permisjon.setEndringsTidspunkt(xmlTilLocalDateTime(p.getEndringstidspunkt())); // TODO = Når inmeldt?
                // TODO Francois typen ikke med?
                // p.getPermisjonOgPermittering().getValue(); Permisjonstypen: permisjon eller permittering (kodeverk)

            }

            // Utenlandsopphold
            List<Utenlandsopphold> oppholdListe = a.getUtenlandsopphold();
            for (Utenlandsopphold o : oppholdListe) {
                no.nav.melosys.domain.Utenlandsopphold utenlandsopphold = new no.nav.melosys.domain.Utenlandsopphold();
                utenlandsopphold.setLand(o.getLand().getValue());

                // TODO rapporteringsperiode eller periode?
                // Rapporteringsperiode: Tidsperioden som ble dekket i rapporten
                utenlandsopphold.setStartdato(xmlTilLocalDate(o.getPeriode().getFom()));
                utenlandsopphold.setSluttdato(xmlTilLocalDate(o.getPeriode().getTom()));
            }

            // Arbeidsavtaler
            List<Arbeidsavtale> avtaler = a.getArbeidsavtale();
            arbeidsforhold.setArbeidsavtaleListe(tilDomeneModell(avtaler));

            resultat.add(arbeidsforhold);
        }

        return resultat;
    }
        
    private List<no.nav.melosys.domain.Arbeidsavtale> tilDomeneModell(List<Arbeidsavtale> avtaler) {
        List<no.nav.melosys.domain.Arbeidsavtale> arbeidsavtaleListe = new ArrayList<>();

        for (Arbeidsavtale avtale : avtaler) {
            no.nav.melosys.domain.Arbeidsavtale domeneAvtale = new no.nav.melosys.domain.Arbeidsavtale();

            // Yrkesbetegnelse. Nødvendig for statistikk til EU
            domeneAvtale.setYrke(avtale.getYrke().getValue());

            domeneAvtale.setTimerPerUke(avtale.getBeregnetAntallTimerPrUke());

            // Stillingsprosent
            // Både når en jobber  i  utlandet, og den periode før. Dette for å avdekke reell utsending
            domeneAvtale.setStillingsprosent(avtale.getStillingsprosent());

            // TODO Yvonne Kanskje til visning (beregnet til   månedlig)?
            // avtale.getBeregnetStillingsprosent();
            // avtale.getAntallTimerGammeltAa();
            // avtale.getEndringsdatoStillingsprosent(); ikke i Informasjonsbehov men i Magic Draw

            // For å kunne vurdere hvilke gruppen bruker faller under og hvilke artiklene skal vurderes
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

            arbeidsavtaleListe.add(domeneAvtale);
        }
        return arbeidsavtaleListe;
    }

    private LocalDate xmlTilLocalDate(XMLGregorianCalendar xmlCal) {
        if (xmlCal == null) {
            return null;
        }
        return xmlCal.toGregorianCalendar().toZonedDateTime().toLocalDate();
    }

    private LocalDateTime xmlTilLocalDateTime(XMLGregorianCalendar tidspunkt) {
        if (tidspunkt == null) {
            return null;
        }
        return tidspunkt.toGregorianCalendar().toZonedDateTime().toLocalDateTime();
    }
    // */

}
