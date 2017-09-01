package no.nav.melosys.tjenester.gui;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.FinnArbeidsforholdPrArbeidstakerResponse;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold;

public class MockTjenesteSvarAareg {

    public static List<Arbeidsforhold> SVAR = null;

    public MockTjenesteSvarAareg() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(FinnArbeidsforholdPrArbeidstakerResponse.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        Reader reader = new StringReader(xml);
        Object xmlBean = unmarshaller.unmarshal(reader);
        FinnArbeidsforholdPrArbeidstakerResponse response = (FinnArbeidsforholdPrArbeidstakerResponse) xmlBean;
        SVAR = response.getParameters().getArbeidsforhold();
    }

    public static String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<ns2:finnArbeidsforholdPrArbeidstakerResponse xmlns:ns2=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3\">\n" +
            "    <parameters>\n" +
            "        <arbeidsforhold opprettetAv=\"srvappserver\" opprettelsestidspunkt=\"2015-02-05T14:39:23.657+01:00\" sistBekreftet=\"2015-10-23T13:37:00.000+02:00\" endretAv=\"srvappserver\" endringstidspunkt=\"2015-10-23T13:41:58.352+02:00\" opphavREF=\"c5773bdb-c80c-4090-ba31-2d0e2693bfa6\" applikasjonsID=\"EDAG\">\n" +
            "            <arbeidsforholdID>GABGPX9O59IZTRD5ILIT60N8</arbeidsforholdID>\n" +
            "            <arbeidsforholdIDnav>35516487</arbeidsforholdIDnav>\n" +
            "            <ansettelsesPeriode fomBruksperiode=\"2015-10-23+02:00\" endretAv=\"srvappserver\" endringstidspunkt=\"2015-10-23T13:39:03.046+02:00\" opphavREF=\"1f51a95e-295b-4e5b-ba84-a019e2b73541\" applikasjonsID=\"EDAG\">\n" +
            "                <periode>\n" +
            "                    <fom>2013-08-19T00:00:00.000+02:00</fom>\n" +
            "                    <tom>2015-05-31T00:00:00.000+02:00</tom>\n" +
            "                </periode>\n" +
            "            </ansettelsesPeriode>\n" +
            "            <arbeidsforholdstype kodeRef=\"ordinaertArbeidsforhold\">Ordinært arbeidsforhold</arbeidsforholdstype>\n" +
            "            <arbeidsavtale endretAv=\"srvappserver\" endringstidspunkt=\"2015-02-05T14:39:23.657+01:00\" opphavREF=\"fea67a1e-6a58-43fc-89d1-a2ffd0dfb676\" applikasjonsID=\"EDAG\" fomGyldighetsperiode=\"2015-01-01T00:00:00.000+01:00\" fomBruksperiode=\"2015-02-05+01:00\">\n" +
            "                <arbeidstidsordning kodeRef=\"ikkeSkift\">Ikke skift</arbeidstidsordning>\n" +
            "                <avloenningstype kodeRef=\"fast\">Fastlønn</avloenningstype>\n" +
            "                <yrke kodeRef=\"2142104\">SIVILINGENIØR (BYGG OG ANLEGG)</yrke>\n" +
            "                <avtaltArbeidstimerPerUke>40.0</avtaltArbeidstimerPerUke>\n" +
            "                <stillingsprosent>100.0</stillingsprosent>\n" +
            "                <sisteLoennsendringsdato>2014-07-01+02:00</sisteLoennsendringsdato>\n" +
            "                <beregnetAntallTimerPrUke>40.0</beregnetAntallTimerPrUke>\n" +
            "                <endringsdatoStillingsprosent>2013-08-19+02:00</endringsdatoStillingsprosent>\n" +
            "            </arbeidsavtale>\n" +
            "            <permisjonOgPermittering endretAv=\"srvappserver\" endringstidspunkt=\"2015-06-30T14:39:32.612+02:00\" opphavREF=\"7e62741e-85de-4061-a88c-c4174c03932f\" applikasjonsID=\"EDAG\">\n" +
            "                <permisjonsId>RHFZHKEHZQ3N4F39ZVK1LURO</permisjonsId>\n" +
            "                <permisjonsPeriode>\n" +
            "                    <fom>2014-08-21T00:00:00.000+02:00</fom>\n" +
            "                    <tom>2014-08-21T00:00:00.000+02:00</tom>\n" +
            "                </permisjonsPeriode>\n" +
            "                <permisjonsprosent>100.0</permisjonsprosent>\n" +
            "                <permisjonOgPermittering kodeRef=\"permisjon\">Permisjon</permisjonOgPermittering>\n" +
            "            </permisjonOgPermittering>\n" +
            "            <permisjonOgPermittering endretAv=\"srvappserver\" endringstidspunkt=\"2015-06-30T14:39:32.612+02:00\" opphavREF=\"7e62741e-85de-4061-a88c-c4174c03932f\" applikasjonsID=\"EDAG\">\n" +
            "                <permisjonsId>24MHXNSR0KYP118IQLFWK0TR</permisjonsId>\n" +
            "                <permisjonsPeriode>\n" +
            "                    <fom>2014-08-18T00:00:00.000+02:00</fom>\n" +
            "                    <tom>2014-08-18T00:00:00.000+02:00</tom>\n" +
            "                </permisjonsPeriode>\n" +
            "                <permisjonsprosent>100.0</permisjonsprosent>\n" +
            "                <permisjonOgPermittering kodeRef=\"permisjon\">Permisjon</permisjonOgPermittering>\n" +
            "            </permisjonOgPermittering>\n" +
            "            <arbeidsgiver xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>873102322</orgnummer>\n" +
            "            </arbeidsgiver>\n" +
            "            <arbeidstaker>\n" +
            "                <ident>\n" +
            "                    <ident>88888888884</ident>\n" +
            "                </ident>\n" +
            "            </arbeidstaker>\n" +
            "            <opplysningspliktig xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>910253158</orgnummer>\n" +
            "            </opplysningspliktig>\n" +
            "            <arbeidsforholdInnrapportertEtterAOrdningen>true</arbeidsforholdInnrapportertEtterAOrdningen>\n" +
            "        </arbeidsforhold>\n" +
            "        <arbeidsforhold opprettetAv=\"BAAREG002\" opprettelsestidspunkt=\"2014-12-13T13:49:22.639+01:00\" sistBekreftet=\"2010-09-30T00:00:00.000+02:00\" endretAv=\"EDAG-1456\" endringstidspunkt=\"2015-06-01T03:43:54.020+02:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "            <arbeidsforholdID>konvertert_b5e5e15e-5d8a-4f1c-a525-17f3bacd5ea6</arbeidsforholdID>\n" +
            "            <arbeidsforholdIDnav>6768425</arbeidsforholdIDnav>\n" +
            "            <ansettelsesPeriode fomBruksperiode=\"2014-12-13+01:00\" endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.640+01:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "                <periode>\n" +
            "                    <fom>2010-09-01T00:00:00.000+02:00</fom>\n" +
            "                    <tom>2010-09-30T00:00:00.000+02:00</tom>\n" +
            "                </periode>\n" +
            "            </ansettelsesPeriode>\n" +
            "            <arbeidsforholdstype kodeRef=\"ordinaertArbeidsforhold\">Ordinært arbeidsforhold</arbeidsforholdstype>\n" +
            "            <arbeidsavtale endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.639+01:00\" applikasjonsID=\"KONVERTERING\" fomGyldighetsperiode=\"2010-09-01T00:00:00.000+02:00\" fomBruksperiode=\"2014-12-13+01:00\">\n" +
            "                <yrke kodeRef=\"5131101\">BARNEHAGEASSISTENT</yrke>\n" +
            "                <beregnetAntallTimerPrUke>8.87</beregnetAntallTimerPrUke>\n" +
            "                <endringsdatoStillingsprosent>2010-09-01+02:00</endringsdatoStillingsprosent>\n" +
            "            </arbeidsavtale>\n" +
            "            <arbeidsgiver xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>991895299</orgnummer>\n" +
            "            </arbeidsgiver>\n" +
            "            <arbeidstaker>\n" +
            "                <ident>\n" +
            "                    <ident>88888888884</ident>\n" +
            "                </ident>\n" +
            "            </arbeidstaker>\n" +
            "            <opplysningspliktig xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>976994434</orgnummer>\n" +
            "            </opplysningspliktig>\n" +
            "            <arbeidsforholdInnrapportertEtterAOrdningen>false</arbeidsforholdInnrapportertEtterAOrdningen>\n" +
            "        </arbeidsforhold>\n" +
            "        <arbeidsforhold opprettetAv=\"BAAREG002\" opprettelsestidspunkt=\"2014-12-13T13:49:22.067+01:00\" sistBekreftet=\"2013-08-19T00:00:00.000+02:00\" endretAv=\"EDAG-1456\" endringstidspunkt=\"2015-06-01T03:46:54.395+02:00\" applikasjonsID=\"AVSLUTNING\">\n" +
            "            <arbeidsforholdID>konvertert_d1fbb862-9605-48fb-9be3-6d66b193b00a</arbeidsforholdID>\n" +
            "            <arbeidsforholdIDnav>6767325</arbeidsforholdIDnav>\n" +
            "            <ansettelsesPeriode fomBruksperiode=\"2015-02-22+01:00\" endretAv=\"BAAREG006\" endringstidspunkt=\"2015-02-22T11:59:37.952+01:00\" applikasjonsID=\"AVSLUTNING\">\n" +
            "                <periode>\n" +
            "                    <fom>2013-08-19T00:00:00.000+02:00</fom>\n" +
            "                    <tom>2014-12-31T23:59:59.000+01:00</tom>\n" +
            "                </periode>\n" +
            "            </ansettelsesPeriode>\n" +
            "            <arbeidsforholdstype kodeRef=\"ordinaertArbeidsforhold\">Ordinært arbeidsforhold</arbeidsforholdstype>\n" +
            "            <arbeidsavtale endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.067+01:00\" applikasjonsID=\"KONVERTERING\" fomGyldighetsperiode=\"2013-08-01T00:00:00.000+02:00\" fomBruksperiode=\"2014-12-13+01:00\">\n" +
            "                <yrke kodeRef=\"2142104\">SIVILINGENIØR (BYGG OG ANLEGG)</yrke>\n" +
            "                <beregnetAntallTimerPrUke>40.0</beregnetAntallTimerPrUke>\n" +
            "                <endringsdatoStillingsprosent>2013-08-19+02:00</endringsdatoStillingsprosent>\n" +
            "            </arbeidsavtale>\n" +
            "            <arbeidsgiver xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>873102322</orgnummer>\n" +
            "            </arbeidsgiver>\n" +
            "            <arbeidstaker>\n" +
            "                <ident>\n" +
            "                    <ident>88888888884</ident>\n" +
            "                </ident>\n" +
            "            </arbeidstaker>\n" +
            "            <opplysningspliktig xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>910253158</orgnummer>\n" +
            "            </opplysningspliktig>\n" +
            "            <arbeidsforholdInnrapportertEtterAOrdningen>false</arbeidsforholdInnrapportertEtterAOrdningen>\n" +
            "        </arbeidsforhold>\n" +
            "        <arbeidsforhold opprettetAv=\"BAAREG002\" opprettelsestidspunkt=\"2014-12-13T13:49:22.067+01:00\" sistBekreftet=\"2010-01-31T00:00:00.000+01:00\" endretAv=\"EDAG-1456\" endringstidspunkt=\"2015-06-01T03:46:54.395+02:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "            <arbeidsforholdID>konvertert_3ca4d061-a2a3-4b31-a3f7-7ef310e8608c</arbeidsforholdID>\n" +
            "            <arbeidsforholdIDnav>6767328</arbeidsforholdIDnav>\n" +
            "            <ansettelsesPeriode fomBruksperiode=\"2014-12-13+01:00\" endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.067+01:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "                <periode>\n" +
            "                    <fom>2010-01-05T00:00:00.000+01:00</fom>\n" +
            "                    <tom>2010-01-31T00:00:00.000+01:00</tom>\n" +
            "                </periode>\n" +
            "            </ansettelsesPeriode>\n" +
            "            <arbeidsforholdstype kodeRef=\"ordinaertArbeidsforhold\">Ordinært arbeidsforhold</arbeidsforholdstype>\n" +
            "            <arbeidsavtale endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.067+01:00\" applikasjonsID=\"KONVERTERING\" fomGyldighetsperiode=\"2010-01-01T00:00:00.000+01:00\" fomBruksperiode=\"2014-12-13+01:00\">\n" +
            "                <yrke kodeRef=\"5139113\">PLEIEMEDHJELPER</yrke>\n" +
            "                <beregnetAntallTimerPrUke>6.89</beregnetAntallTimerPrUke>\n" +
            "                <endringsdatoStillingsprosent>2010-01-05+01:00</endringsdatoStillingsprosent>\n" +
            "            </arbeidsavtale>\n" +
            "            <arbeidsgiver xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>973982427</orgnummer>\n" +
            "            </arbeidsgiver>\n" +
            "            <arbeidstaker>\n" +
            "                <ident>\n" +
            "                    <ident>88888888884</ident>\n" +
            "                </ident>\n" +
            "            </arbeidstaker>\n" +
            "            <opplysningspliktig xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>942952880</orgnummer>\n" +
            "            </opplysningspliktig>\n" +
            "            <arbeidsforholdInnrapportertEtterAOrdningen>false</arbeidsforholdInnrapportertEtterAOrdningen>\n" +
            "        </arbeidsforhold>\n" +
            "        <arbeidsforhold opprettetAv=\"BAAREG002\" opprettelsestidspunkt=\"2014-12-13T13:49:22.067+01:00\" sistBekreftet=\"2009-08-31T00:00:00.000+02:00\" endretAv=\"EDAG-1456\" endringstidspunkt=\"2015-06-01T03:46:54.395+02:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "            <arbeidsforholdID>konvertert_83b47566-253d-433b-9d80-851a8cc9c8cc</arbeidsforholdID>\n" +
            "            <arbeidsforholdIDnav>6767327</arbeidsforholdIDnav>\n" +
            "            <ansettelsesPeriode fomBruksperiode=\"2014-12-13+01:00\" endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.067+01:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "                <periode>\n" +
            "                    <fom>2009-07-01T00:00:00.000+02:00</fom>\n" +
            "                    <tom>2009-08-31T00:00:00.000+02:00</tom>\n" +
            "                </periode>\n" +
            "            </ansettelsesPeriode>\n" +
            "            <arbeidsforholdstype kodeRef=\"ordinaertArbeidsforhold\">Ordinært arbeidsforhold</arbeidsforholdstype>\n" +
            "            <arbeidsavtale endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.067+01:00\" applikasjonsID=\"KONVERTERING\" fomGyldighetsperiode=\"2009-07-01T00:00:00.000+02:00\" fomBruksperiode=\"2014-12-13+01:00\">\n" +
            "                <yrke kodeRef=\"5139113\">PLEIEMEDHJELPER</yrke>\n" +
            "                <beregnetAntallTimerPrUke>17.27</beregnetAntallTimerPrUke>\n" +
            "                <endringsdatoStillingsprosent>2009-07-01+02:00</endringsdatoStillingsprosent>\n" +
            "            </arbeidsavtale>\n" +
            "            <arbeidsgiver xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>973982427</orgnummer>\n" +
            "            </arbeidsgiver>\n" +
            "            <arbeidstaker>\n" +
            "                <ident>\n" +
            "                    <ident>88888888884</ident>\n" +
            "                </ident>\n" +
            "            </arbeidstaker>\n" +
            "            <opplysningspliktig xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>942952880</orgnummer>\n" +
            "            </opplysningspliktig>\n" +
            "            <arbeidsforholdInnrapportertEtterAOrdningen>false</arbeidsforholdInnrapportertEtterAOrdningen>\n" +
            "        </arbeidsforhold>\n" +
            "        <arbeidsforhold opprettetAv=\"BAAREG002\" opprettelsestidspunkt=\"2014-12-13T13:49:22.067+01:00\" sistBekreftet=\"2007-08-05T00:00:00.000+02:00\" endretAv=\"EDAG-1456\" endringstidspunkt=\"2015-06-01T03:46:54.395+02:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "            <arbeidsforholdID>konvertert_cac9778e-dd0d-4228-b4da-01f0a73b66d6</arbeidsforholdID>\n" +
            "            <arbeidsforholdIDnav>6767326</arbeidsforholdIDnav>\n" +
            "            <ansettelsesPeriode fomBruksperiode=\"2014-12-13+01:00\" endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.067+01:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "                <periode>\n" +
            "                    <fom>2007-05-18T00:00:00.000+02:00</fom>\n" +
            "                    <tom>2007-08-05T00:00:00.000+02:00</tom>\n" +
            "                </periode>\n" +
            "            </ansettelsesPeriode>\n" +
            "            <arbeidsforholdstype kodeRef=\"ordinaertArbeidsforhold\">Ordinært arbeidsforhold</arbeidsforholdstype>\n" +
            "            <arbeidsavtale endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.067+01:00\" applikasjonsID=\"KONVERTERING\" fomGyldighetsperiode=\"2007-05-01T00:00:00.000+02:00\" fomBruksperiode=\"2014-12-13+01:00\">\n" +
            "                <yrke kodeRef=\"5123115\">BARISTA</yrke>\n" +
            "                <beregnetAntallTimerPrUke>37.5</beregnetAntallTimerPrUke>\n" +
            "                <endringsdatoStillingsprosent>2007-05-18+02:00</endringsdatoStillingsprosent>\n" +
            "            </arbeidsavtale>\n" +
            "            <arbeidsgiver xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>981541677</orgnummer>\n" +
            "            </arbeidsgiver>\n" +
            "            <arbeidstaker>\n" +
            "                <ident>\n" +
            "                    <ident>88888888884</ident>\n" +
            "                </ident>\n" +
            "            </arbeidstaker>\n" +
            "            <opplysningspliktig xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>981527615</orgnummer>\n" +
            "            </opplysningspliktig>\n" +
            "            <arbeidsforholdInnrapportertEtterAOrdningen>false</arbeidsforholdInnrapportertEtterAOrdningen>\n" +
            "        </arbeidsforhold>\n" +
            "        <arbeidsforhold opprettetAv=\"BAAREG002\" opprettelsestidspunkt=\"2014-12-13T13:49:22.639+01:00\" sistBekreftet=\"2010-08-31T00:00:00.000+02:00\" endretAv=\"EDAG-1456\" endringstidspunkt=\"2015-06-01T03:43:54.020+02:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "            <arbeidsforholdID>konvertert_0aa2654e-0c10-406f-933d-52ab7f5a20a4</arbeidsforholdID>\n" +
            "            <arbeidsforholdIDnav>6768424</arbeidsforholdIDnav>\n" +
            "            <ansettelsesPeriode fomBruksperiode=\"2014-12-13+01:00\" endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.639+01:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "                <periode>\n" +
            "                    <fom>2010-06-01T00:00:00.000+02:00</fom>\n" +
            "                    <tom>2010-08-31T00:00:00.000+02:00</tom>\n" +
            "                </periode>\n" +
            "            </ansettelsesPeriode>\n" +
            "            <arbeidsforholdstype kodeRef=\"ordinaertArbeidsforhold\">Ordinært arbeidsforhold</arbeidsforholdstype>\n" +
            "            <arbeidsavtale endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.639+01:00\" applikasjonsID=\"KONVERTERING\" fomGyldighetsperiode=\"2010-08-01T00:00:00.000+02:00\" fomBruksperiode=\"2014-12-13+01:00\">\n" +
            "                <yrke kodeRef=\"5139113\">PLEIEMEDHJELPER</yrke>\n" +
            "                <beregnetAntallTimerPrUke>8.98</beregnetAntallTimerPrUke>\n" +
            "                <endringsdatoStillingsprosent>2010-08-01+02:00</endringsdatoStillingsprosent>\n" +
            "            </arbeidsavtale>\n" +
            "            <arbeidsgiver xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>973982427</orgnummer>\n" +
            "            </arbeidsgiver>\n" +
            "            <arbeidstaker>\n" +
            "                <ident>\n" +
            "                    <ident>88888888884</ident>\n" +
            "                </ident>\n" +
            "            </arbeidstaker>\n" +
            "            <opplysningspliktig xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>942952880</orgnummer>\n" +
            "            </opplysningspliktig>\n" +
            "            <arbeidsforholdInnrapportertEtterAOrdningen>false</arbeidsforholdInnrapportertEtterAOrdningen>\n" +
            "        </arbeidsforhold>\n" +
            "        <arbeidsforhold opprettetAv=\"BAAREG002\" opprettelsestidspunkt=\"2014-12-13T13:49:22.640+01:00\" sistBekreftet=\"2013-08-02T00:00:00.000+02:00\" endretAv=\"EDAG-1456\" endringstidspunkt=\"2015-06-01T03:43:54.020+02:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "            <arbeidsforholdID>konvertert_d0aac8f0-9011-4e11-b3d2-a37b624e1787</arbeidsforholdID>\n" +
            "            <arbeidsforholdIDnav>6768427</arbeidsforholdIDnav>\n" +
            "            <ansettelsesPeriode fomBruksperiode=\"2014-12-13+01:00\" endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.640+01:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "                <periode>\n" +
            "                    <fom>2011-02-07T00:00:00.000+01:00</fom>\n" +
            "                    <tom>2013-08-02T00:00:00.000+02:00</tom>\n" +
            "                </periode>\n" +
            "            </ansettelsesPeriode>\n" +
            "            <arbeidsforholdstype kodeRef=\"ordinaertArbeidsforhold\">Ordinært arbeidsforhold</arbeidsforholdstype>\n" +
            "            <arbeidsavtale endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.640+01:00\" applikasjonsID=\"KONVERTERING\" fomGyldighetsperiode=\"2011-02-01T00:00:00.000+01:00\" fomBruksperiode=\"2014-12-13+01:00\">\n" +
            "                <yrke kodeRef=\"2212122\">AVDELINGSINGENIØR (MATFORSKNING)</yrke>\n" +
            "                <beregnetAntallTimerPrUke>37.5</beregnetAntallTimerPrUke>\n" +
            "                <endringsdatoStillingsprosent>2011-02-07+01:00</endringsdatoStillingsprosent>\n" +
            "            </arbeidsavtale>\n" +
            "            <arbeidsgiver xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>974600951</orgnummer>\n" +
            "            </arbeidsgiver>\n" +
            "            <arbeidstaker>\n" +
            "                <ident>\n" +
            "                    <ident>88888888884</ident>\n" +
            "                </ident>\n" +
            "            </arbeidstaker>\n" +
            "            <opplysningspliktig xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>976821580</orgnummer>\n" +
            "            </opplysningspliktig>\n" +
            "            <arbeidsforholdInnrapportertEtterAOrdningen>false</arbeidsforholdInnrapportertEtterAOrdningen>\n" +
            "        </arbeidsforhold>\n" +
            "        <arbeidsforhold opprettetAv=\"srvappserver\" opprettelsestidspunkt=\"2015-06-10T14:47:05.933+02:00\" sistBekreftet=\"2017-06-28T14:01:30.000+02:00\" endretAv=\"srvappserver\" endringstidspunkt=\"2017-06-28T14:30:29.468+02:00\" opphavREF=\"eda00000-0000-0000-0000-000641358209\" applikasjonsID=\"EDAG\">\n" +
            "            <arbeidsforholdID>V974600951R100636S1001L0001</arbeidsforholdID>\n" +
            "            <arbeidsforholdIDnav>36958123</arbeidsforholdIDnav>\n" +
            "            <ansettelsesPeriode fomBruksperiode=\"2015-06-10+02:00\" endretAv=\"srvappserver\" endringstidspunkt=\"2015-06-10T14:47:05.934+02:00\" opphavREF=\"a0c4c670-8751-45bd-a8c9-5d67d3a4b929\" applikasjonsID=\"EDAG\">\n" +
            "                <periode>\n" +
            "                    <fom>2015-06-01T00:00:00.000+02:00</fom>\n" +
            "                </periode>\n" +
            "            </ansettelsesPeriode>\n" +
            "            <arbeidsforholdstype kodeRef=\"ordinaertArbeidsforhold\">Ordinært arbeidsforhold</arbeidsforholdstype>\n" +
            "            <arbeidsavtale endretAv=\"srvappserver\" endringstidspunkt=\"2016-01-12T16:33:35.054+01:00\" opphavREF=\"7cb24f71-5cdb-445e-bb00-4b6bdbeb772e\" applikasjonsID=\"EDAG\" fomGyldighetsperiode=\"2015-12-01T00:00:00.000+01:00\" fomBruksperiode=\"2016-01-12+01:00\">\n" +
            "                <arbeidstidsordning kodeRef=\"ikkeSkift\">Ikke skift</arbeidstidsordning>\n" +
            "                <avloenningstype kodeRef=\"fast\">Fastlønn</avloenningstype>\n" +
            "                <yrke kodeRef=\"3119136\">INGENIØR (ØVRIG TEKNISK VIRKSOMHET)</yrke>\n" +
            "                <avtaltArbeidstimerPerUke>37.5</avtaltArbeidstimerPerUke>\n" +
            "                <stillingsprosent>100.0</stillingsprosent>\n" +
            "                <sisteLoennsendringsdato>2015-06-01+02:00</sisteLoennsendringsdato>\n" +
            "                <beregnetAntallTimerPrUke>37.5</beregnetAntallTimerPrUke>\n" +
            "                <endringsdatoStillingsprosent>2015-06-01+02:00</endringsdatoStillingsprosent>\n" +
            "            </arbeidsavtale>\n" +
            "            <permisjonOgPermittering endretAv=\"srvappserver\" endringstidspunkt=\"2017-01-24T11:16:32.277+01:00\" opphavREF=\"eda00000-0000-0000-0000-000400508547\" applikasjonsID=\"EDAG\">\n" +
            "                <permisjonsId>P00006</permisjonsId>\n" +
            "                <permisjonsPeriode>\n" +
            "                    <fom>2017-02-15T00:00:00.000+01:00</fom>\n" +
            "                    <tom>2017-02-20T00:00:00.000+01:00</tom>\n" +
            "                </permisjonsPeriode>\n" +
            "                <permisjonsprosent>100.0</permisjonsprosent>\n" +
            "                <permisjonOgPermittering kodeRef=\"permisjon\">Permisjon</permisjonOgPermittering>\n" +
            "            </permisjonOgPermittering>\n" +
            "            <permisjonOgPermittering endretAv=\"srvappserver\" endringstidspunkt=\"2016-09-15T10:04:13.763+02:00\" opphavREF=\"eda00000-0000-0000-0000-000176251690\" applikasjonsID=\"EDAG\">\n" +
            "                <permisjonsId>P00004</permisjonsId>\n" +
            "                <permisjonsPeriode>\n" +
            "                    <fom>2016-09-01T00:00:00.000+02:00</fom>\n" +
            "                    <tom>2016-09-02T00:00:00.000+02:00</tom>\n" +
            "                </permisjonsPeriode>\n" +
            "                <permisjonsprosent>100.0</permisjonsprosent>\n" +
            "                <permisjonOgPermittering kodeRef=\"permisjon\">Permisjon</permisjonOgPermittering>\n" +
            "            </permisjonOgPermittering>\n" +
            "            <permisjonOgPermittering endretAv=\"srvappserver\" endringstidspunkt=\"2016-09-15T10:04:13.763+02:00\" opphavREF=\"eda00000-0000-0000-0000-000176251690\" applikasjonsID=\"EDAG\">\n" +
            "                <permisjonsId>P00005</permisjonsId>\n" +
            "                <permisjonsPeriode>\n" +
            "                    <fom>2016-08-22T00:00:00.000+02:00</fom>\n" +
            "                    <tom>2016-08-26T00:00:00.000+02:00</tom>\n" +
            "                </permisjonsPeriode>\n" +
            "                <permisjonsprosent>100.0</permisjonsprosent>\n" +
            "                <permisjonOgPermittering kodeRef=\"permisjon\">Permisjon</permisjonOgPermittering>\n" +
            "            </permisjonOgPermittering>\n" +
            "            <permisjonOgPermittering endretAv=\"srvappserver\" endringstidspunkt=\"2017-05-15T13:38:45.685+02:00\" opphavREF=\"eda00000-0000-0000-0000-000580327886\" applikasjonsID=\"EDAG\">\n" +
            "                <permisjonsId>P00007</permisjonsId>\n" +
            "                <permisjonsPeriode>\n" +
            "                    <fom>2017-05-05T00:00:00.000+02:00</fom>\n" +
            "                    <tom>2017-05-05T00:00:00.000+02:00</tom>\n" +
            "                </permisjonsPeriode>\n" +
            "                <permisjonsprosent>100.0</permisjonsprosent>\n" +
            "                <permisjonOgPermittering kodeRef=\"permisjon\">Permisjon</permisjonOgPermittering>\n" +
            "            </permisjonOgPermittering>\n" +
            "            <permisjonOgPermittering endretAv=\"srvappserver\" endringstidspunkt=\"2015-09-23T08:00:31.048+02:00\" opphavREF=\"d452d59d-5358-4233-bb2e-0e0ccdc4e286\" applikasjonsID=\"EDAG\">\n" +
            "                <permisjonsId>P00001</permisjonsId>\n" +
            "                <permisjonsPeriode>\n" +
            "                    <fom>2015-09-02T00:00:00.000+02:00</fom>\n" +
            "                    <tom>2015-09-04T00:00:00.000+02:00</tom>\n" +
            "                </permisjonsPeriode>\n" +
            "                <permisjonsprosent>100.0</permisjonsprosent>\n" +
            "                <permisjonOgPermittering kodeRef=\"permisjon\">Permisjon</permisjonOgPermittering>\n" +
            "            </permisjonOgPermittering>\n" +
            "            <permisjonOgPermittering endretAv=\"srvappserver\" endringstidspunkt=\"2016-03-02T13:52:56.104+01:00\" opphavREF=\"a2a1fae8-28e2-4de1-8d7d-4b74c5a0dee2\" applikasjonsID=\"EDAG\">\n" +
            "                <permisjonsId>P00002</permisjonsId>\n" +
            "                <permisjonsPeriode>\n" +
            "                    <fom>2016-02-18T00:00:00.000+01:00</fom>\n" +
            "                    <tom>2016-02-22T00:00:00.000+01:00</tom>\n" +
            "                </permisjonsPeriode>\n" +
            "                <permisjonsprosent>100.0</permisjonsprosent>\n" +
            "                <permisjonOgPermittering kodeRef=\"permisjon\">Permisjon</permisjonOgPermittering>\n" +
            "            </permisjonOgPermittering>\n" +
            "            <permisjonOgPermittering endretAv=\"srvappserver\" endringstidspunkt=\"2016-05-13T09:28:39.543+02:00\" opphavREF=\"eda00000-0000-0000-0000-000008729117\" applikasjonsID=\"EDAG\">\n" +
            "                <permisjonsId>P00003</permisjonsId>\n" +
            "                <permisjonsPeriode>\n" +
            "                    <fom>2016-03-31T00:00:00.000+02:00</fom>\n" +
            "                    <tom>2016-04-01T00:00:00.000+02:00</tom>\n" +
            "                </permisjonsPeriode>\n" +
            "                <permisjonsprosent>100.0</permisjonsprosent>\n" +
            "                <permisjonOgPermittering kodeRef=\"permisjon\">Permisjon</permisjonOgPermittering>\n" +
            "            </permisjonOgPermittering>\n" +
            "            <arbeidsgiver xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>974600951</orgnummer>\n" +
            "            </arbeidsgiver>\n" +
            "            <arbeidstaker>\n" +
            "                <ident>\n" +
            "                    <ident>88888888884</ident>\n" +
            "                </ident>\n" +
            "            </arbeidstaker>\n" +
            "            <opplysningspliktig xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>964338531</orgnummer>\n" +
            "            </opplysningspliktig>\n" +
            "            <arbeidsforholdInnrapportertEtterAOrdningen>true</arbeidsforholdInnrapportertEtterAOrdningen>\n" +
            "        </arbeidsforhold>\n" +
            "        <arbeidsforhold opprettetAv=\"BAAREG002\" opprettelsestidspunkt=\"2014-12-13T13:49:22.640+01:00\" sistBekreftet=\"2011-01-31T00:00:00.000+01:00\" endretAv=\"EDAG-1456\" endringstidspunkt=\"2015-06-01T03:43:54.020+02:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "            <arbeidsforholdID>konvertert_f6af66d1-55e3-409a-86b9-27c9af8ea7f7</arbeidsforholdID>\n" +
            "            <arbeidsforholdIDnav>6768426</arbeidsforholdIDnav>\n" +
            "            <ansettelsesPeriode fomBruksperiode=\"2014-12-13+01:00\" endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.640+01:00\" applikasjonsID=\"KONVERTERING\">\n" +
            "                <periode>\n" +
            "                    <fom>2010-11-01T00:00:00.000+01:00</fom>\n" +
            "                    <tom>2011-01-31T00:00:00.000+01:00</tom>\n" +
            "                </periode>\n" +
            "            </ansettelsesPeriode>\n" +
            "            <arbeidsforholdstype kodeRef=\"ordinaertArbeidsforhold\">Ordinært arbeidsforhold</arbeidsforholdstype>\n" +
            "            <arbeidsavtale endretAv=\"BAAREG002\" endringstidspunkt=\"2014-12-13T13:49:22.640+01:00\" applikasjonsID=\"KONVERTERING\" fomGyldighetsperiode=\"2011-01-01T00:00:00.000+01:00\" fomBruksperiode=\"2014-12-13+01:00\">\n" +
            "                <yrke kodeRef=\"5131101\">BARNEHAGEASSISTENT</yrke>\n" +
            "                <beregnetAntallTimerPrUke>28.56</beregnetAntallTimerPrUke>\n" +
            "                <endringsdatoStillingsprosent>2011-01-01+01:00</endringsdatoStillingsprosent>\n" +
            "            </arbeidsavtale>\n" +
            "            <arbeidsgiver xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>991895299</orgnummer>\n" +
            "            </arbeidsgiver>\n" +
            "            <arbeidstaker>\n" +
            "                <ident>\n" +
            "                    <ident>88888888884</ident>\n" +
            "                </ident>\n" +
            "            </arbeidstaker>\n" +
            "            <opplysningspliktig xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/informasjon/arbeidsforhold\" xsi:type=\"ns4:Organisasjon\">\n" +
            "                <orgnummer>976994434</orgnummer>\n" +
            "            </opplysningspliktig>\n" +
            "            <arbeidsforholdInnrapportertEtterAOrdningen>false</arbeidsforholdInnrapportertEtterAOrdningen>\n" +
            "        </arbeidsforhold>\n" +
            "    </parameters>\n" +
            "</ns2:finnArbeidsforholdPrArbeidstakerResponse>";

}
