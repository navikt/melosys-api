package no.nav.melosys.regler.service.lovvalg;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;

import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsavtale;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periodetype;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.Organisasjonsnavn;
import no.nav.melosys.domain.dokument.person.*;
import no.nav.melosys.domain.dokument.soeknad.*;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.regler.api.lovvalg.req.FastsettLovvalgRequest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ManuellTest {
    
    private LovvalgTjenesteImpl lovvalgTjeneste;

    @Before
    public void setUp() {
        lovvalgTjeneste = new LovvalgTjenesteImpl();
    }

    @Test
    public void fastsettLovvalgTest() throws Exception {
        FastsettLovvalgRequest request = byggRequest();
        FastsettLovvalgReply reply = lovvalgTjeneste.fastsettLovvalg(request);
        assertNotNull(reply);
    }

    private FastsettLovvalgRequest byggRequest() {
        String fnr = "FJERNET";
        Periode perioden = new Periode(LocalDate.of(2017, 1, 1), LocalDate.of(2018, 12, 31));
        
        FastsettLovvalgRequest req = new FastsettLovvalgRequest();
        SoeknadDokument sd = new SoeknadDokument();
        req.søknadDokument = sd;
        sd.fnr = fnr;
        sd.sammensattNavn = "Nananananananana Batman";
        sd.arbeidNorge = new ArbeidNorge();
        sd.arbeidNorge.brukerErSelvstendigNæringsdrivende = false;
        sd.arbeidUtland = new ArbeidUtland();
        sd.arbeidUtland.arbeidsperiode = perioden;
        sd.arbeidUtland.arbeidsland = Arrays.asList(new Land("SWE"));
        sd.oppholdUtland = new OppholdUtland();
        sd.oppholdUtland.oppholdsPeriode = perioden;
        sd.oppholdUtland.oppholdsland = new Land("SWE");
        
        PersonDokument pd = new PersonDokument();
        req.personopplysningDokument = pd;
        pd.fnr = fnr;
        pd.sivilstand = Sivilstand.GIFT;
        pd.statsborgerskap = new Land("NOR");
        pd.sammensattNavn = sd.sammensattNavn;
        pd.kjønn = "M";
        pd.fødselsdato = LocalDate.of(1972, 11, 19);
        pd.personstatus = Personstatus.ADNR;
        Familiemedlem ektefelle = new Familiemedlem();
        pd.familiemedlemmer = Arrays.asList(ektefelle);
        ektefelle.fnr = "FJERNET";
        ektefelle.navn = "Damatil Batman";
        ektefelle.familierelasjon = Familierelasjon.EKTE;
        
        ArbeidsforholdDokument ad = new ArbeidsforholdDokument();
        req.arbeidsforholdDokumenter = Arrays.asList(ad);
        Arbeidsforhold af = new Arbeidsforhold();
        ad.arbeidsforhold = Arrays.asList(af);
        af.arbeidsforholdID = "";
        af.arbeidsforholdIDnav = 44137899;
        af.ansettelsesPeriode = new no.nav.melosys.domain.dokument.felles.Periode(LocalDate.of(2004, 1, 1), null);
        af.arbeidsforholdstype = "Forenklet oppgjørsordning";
        af.permisjonOgPermittering = Collections.emptyList();
        af.utenlandsopphold = Collections.emptyList();
        af.arbeidsgivertype = Aktoertype.Person;
        af.arbeidsgiverID = "FJERNET";
        af.arbeidstakerID = fnr;
        af.opplysningspliktigtype = Aktoertype.Person;
        af.opplysningspliktigID = "FJERNET";
        af.opprettelsestidspunkt = OffsetDateTime.now();
        af.sistBekreftet = OffsetDateTime.now();
        af.arbeidsforholdInnrapportertEtterAOrdningen = true;
        Arbeidsavtale avt = new Arbeidsavtale();
        af.arbeidsavtaler = Arrays.asList(avt);
        avt.arbeidstidsordning = "Ikke skift";
        avt.avloenningstype = "Fastlønn";
        avt.yrke = "ALTMULIGMANN (PRIVATHJEM)";
        avt.avtaltArbeidstimerPerUke = BigDecimal.valueOf(37.5d);
        avt.stillingsprosent = BigDecimal.valueOf(100);
        avt.beregnetAntallTimerPrUke = BigDecimal.valueOf(37.5d);
        avt.maritimArbeidsavtale = false;
        
        InntektDokument id = new InntektDokument();
        req.inntektDokumenter = Arrays.asList(id);
        ArbeidsInntektMaaned aim = new ArbeidsInntektMaaned();
        id.arbeidsInntektMaanedListe = Arrays.asList(aim);
        aim.aarMaaned = YearMonth.of(2018, 01);
        ArbeidsInntektInformasjon aii = new ArbeidsInntektInformasjon();
        aim.arbeidsInntektInformasjon = aii;
        Inntekt innt = new Inntekt();
        aii.inntektListe = Arrays.asList(innt);
        innt.beloep = BigDecimal.valueOf(25000);
        innt.fordel = "kontantytelse";
        innt.inntektskilde = "A-ordningen";
        innt.inntektsperiodetype = "Maaned";
        innt.inntektsstatus = "LoependeInnrapportert";
        innt.levereringstidspunkt = LocalDateTime.of(2018, 1, 1, 0, 0, 0);
        innt.utbetaltIPeriode = YearMonth.of(2018, 1);
        innt.opplysningspliktigID = "873152362";
        innt.virksomhetID = "873152362";
        innt.inntektsmottakerID = fnr;
        innt.inngaarIGrunnlagForTrekk = true;
        innt.utloeserArbeidsgiveravgift = true;

        MedlemskapDokument md = new MedlemskapDokument();
        req.medlemskapDokumenter = Arrays.asList(md);
        Medlemsperiode mp = new Medlemsperiode();
        md.medlemsperiode = Arrays.asList(mp);
        mp.periode = new no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.of(2018, 1, 1), LocalDate.of(9999, 12, 31));
        mp.type = Periodetype.PMMEDSKP;
        mp.status = "GYLD";
        mp.grunnlagstype = "MEDEOS";
        mp.land = "Nor";
        mp.lovvalg = "ENDL";
        mp.trygdedekning = "Full";
        mp.kildedokumenttype = "E101";
        mp.kilde = "FS22";

        OrganisasjonDokument org = new OrganisasjonDokument();
        req.organisasjonDokumenter = Arrays.asList(org);
        OrganisasjonsDetaljer det = new OrganisasjonsDetaljer();
        org.organisasjonDetaljer = det;
        det.orgnummer = "873152362";
        Organisasjonsnavn orgnavn = new Organisasjonsnavn();
        det.navn = Arrays.asList(orgnavn);
        orgnavn.navn = Arrays.asList("Dc Comics");

        return req;
    }

}
