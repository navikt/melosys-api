package no.nav.melosys.integrasjon.aareg;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import no.nav.melosys.domain.Arbeidsforhold;
import no.nav.melosys.domain.ArbeidsforholdsType;
import no.nav.melosys.domain.Arbeidsgiver;
import no.nav.melosys.domain.PermisjonOgPermittering;
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

    private static final String REGELVERK_ALLE = "ALLE";

    @Autowired
    public AaregService(ArbeidsforholdConsumer arbeidsforholdConsumer) {
        this.arbeidsforholdConsumer = arbeidsforholdConsumer;
    }

    /**
     * Henter en liste av arbeidsforhold for en arbeidstaker.
     * 
     * @param fnr
     *            Fødselsnummer til arbeidstaker.
     * @return
     */
    @Override
    public List<Arbeidsforhold> finnArbeidsforholdPrArbeidstaker(String fnr) throws IntegrasjonException {
        FinnArbeidsforholdPrArbeidstakerRequest request = new FinnArbeidsforholdPrArbeidstakerRequest();
        NorskIdent ident = new NorskIdent();
        ident.setIdent(fnr);
        request.setIdent(ident);
        Regelverker regelverker = new Regelverker();
        // TODO Anders Vi har (foreløpig) sagt at vi kun skal hente arbeidsforhold rapportert på nytt regelverk.
        regelverker.setKodeverksRef(REGELVERK_ALLE);
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
            // TODO Spørre Anders a.getOpprettelsestidspunkt(); ?

            // Dato sist bekreftet
            arbeidsforhold.setSistBekreftet(xmlTilLocalDate(a.getSistBekreftet()));

            // Type arbeidsforhold - "Ordinært", "Maritimt", «Forenklet oppgjørsordning»...
            // Ikke påkrevd i tjenesten.
            Arbeidsforholdstyper type = a.getArbeidsforholdstype();
            if (type != null) {
                arbeidsforhold.setType(ArbeidsforholdsType.getFraKode(type.getValue()));
            }

            // PermisjonOgPermittering. Til å gjøre kontroll etter vedtak er innvilget. Ikke direkte vilkårvurdering.
            // TODO Hentes separat?
            List<no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.PermisjonOgPermittering> permisjoner = a.getPermisjonOgPermittering();
            for (no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.PermisjonOgPermittering p : permisjoner) {
                PermisjonOgPermittering permisjon = new PermisjonOgPermittering();
                permisjon.setPermisjonsId(p.getPermisjonsId());
                permisjon.setStartDato(xmlTilLocalDate(p.getPermisjonsPeriode().getFom()));
                permisjon.setSluttDato(xmlTilLocalDate(p.getPermisjonsPeriode().getTom()));
                // TODO Francois scale?
                permisjon.setProsent(p.getPermisjonsprosent());
                // TODO = Når inmeldt?
                permisjon.setEndringsTidspunkt(xmlTilLocalDateTime(p.getEndringstidspunkt()));
                // TODO Francois Ikke med?
                // p.getPermisjonOgPermittering().getValue(); Permisjonstypen: permisjon eller permittering (kodeverk)

            }

            // Utenlandsopphold
            List<Utenlandsopphold> oppholdListe = a.getUtenlandsopphold();
            for (Utenlandsopphold o : oppholdListe) {
                no.nav.melosys.domain.Utenlandsopphold utenlandsopphold = new no.nav.melosys.domain.Utenlandsopphold();
                utenlandsopphold.setLand(o.getLand().getValue());
                // Rapporteringsperiode Tidsperioden som ble dekket i rapporten
                // TODO rapporteringsperiode eller periode?
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

            // TODO Francois Perioder?

            // Yrkesbetegnelse. Nødvendig for statistikk til EU
            domeneAvtale.setYrke(avtale.getYrke().getValue());

            // Stillingsprosent TODO avventer Anders
            // avtale.getStillingsprosent();
            // avtale.getBeregnetStillingsprosent();

            // Lønnstype. TODO Francois (Ingen vits å ta i bruk?)
            // avtale.getAvloenningstype();

            // For å kunne vurderehvilke gruppen bruker faller under og hvilke artiklene skal vurderes
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

}
