package no.nav.melosys.integrasjonstest.felles.opplysninger;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;

import static no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_begrunnelser.UTSENDELSE_OVER_24_MN;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_forutgaaende_medl.FOLKEREGISTRERT_IKKE_ARBEIDET_I_NORGE;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_vesentlig_virksomhet.FOR_LITE_OMSETNING_NORGE;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_avslag.SOEKT_FOR_SENT;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1;
import static no.nav.melosys.integrasjonstest.felles.opplysninger.Testbehandlinger.TOM_BEHANDLING;
import static no.nav.melosys.integrasjonstest.felles.opplysninger.Testsubjekter.AKTØR_ID;
import static no.nav.melosys.integrasjonstest.felles.opplysninger.Testsubjekter.AVKLART_ARBEIDSGIVER_ORGNR;
import static no.nav.melosys.integrasjonstest.felles.utils.AktoerTestUtils.lagAktørBrukerDto;
import static no.nav.melosys.integrasjonstest.felles.utils.AktoerTestUtils.lagAktørRepresentantDto;
import static no.nav.melosys.integrasjonstest.felles.utils.AvklartefaktaTestUtils.*;
import static no.nav.melosys.integrasjonstest.felles.utils.LovvalgsperiodeTestUtils.lagLovvalgsperiodeDto;
import static no.nav.melosys.integrasjonstest.felles.utils.VilkaarTestUtils.lagVilkaarDto;

public class TestdataUtfyller {
    private final long behandlingsid;
    private final MelosysTjenesteGrensesnitt behandlingsUtfyller;

    public static TestdataUtfyller til(MelosysTjenesteGrensesnitt grensesnitt, LovvalgBestemmelse bestemmelse, InnvilgelsesResultat resultat) throws FunksjonellException, TekniskException {
        if (bestemmelse == Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1) {
            return lagTestdataUtfyllerArt12(TOM_BEHANDLING, grensesnitt, resultat);
        }

        throw new TekniskException("Forhåndsdefinert mal eksisterer ikke for valgt kombinasjon");
    }

    private static TestdataUtfyller lagTestdataUtfyllerArt12(long behandlingId, MelosysTjenesteGrensesnitt grensesnitt, InnvilgelsesResultat resultat) throws FunksjonellException, TekniskException {
        TestdataUtfyller utfyllerArt12 = new TestdataUtfyller(behandlingId, grensesnitt)
        .utfyllAvklartefaktaForArt12(Landkoder.AT, AVKLART_ARBEIDSGIVER_ORGNR)
        .opprettInnvilgetLovvalgsperiode(FO_883_2004_ART12_1);
        if (resultat == InnvilgelsesResultat.INNVILGET) {
            utfyllerArt12.utfyllVilkaarForArt12Innvilgelse();
        } else {
            utfyllerArt12.utfyllVilkaarForArt12Avslag();
        }
        return utfyllerArt12;
    }

    public static TestdataUtfyller til(long behandlingId, MelosysTjenesteGrensesnitt melosysGrensesnitt) throws FunksjonellException, TekniskException {
        return new TestdataUtfyller(behandlingId, melosysGrensesnitt);
    }

    public TestdataUtfyller(long behandlingsId, MelosysTjenesteGrensesnitt behandlingsUtfyller) throws FunksjonellException, TekniskException {
        this.behandlingsid = behandlingsId;
        this.behandlingsUtfyller = behandlingsUtfyller;

        behandlingsUtfyller.nullstill(behandlingsId);
        opprettAktørBruker(AKTØR_ID);
    }

    public TestdataUtfyller utfyllAvklartefaktaForArt12(Landkoder soeknadsland, String arbeidsgiversOrgnr) throws FunksjonellException, TekniskException {
        behandlingsUtfyller.opprettAvklartefakta(behandlingsid,
            lagAvklartSoeknadsland(soeknadsland),
            lagAvklartYrkesgruppe(Yrkesgrupper.ORDINAER),
            lagAvklartVirksomhet(arbeidsgiversOrgnr));
        return this;
    }

    public TestdataUtfyller utfyllAvklartefaktaArbeidPåSkip(Landkoder soeknadsland, Landkoder arbeidsland, String skipsnavn, String arbeidsgiversOrgnr) throws FunksjonellException, TekniskException {
        behandlingsUtfyller.opprettAvklartefakta(behandlingsid,
            lagAvklartSoeknadsland(soeknadsland),
            lagAvklartBostedsland(soeknadsland),
            lagAvklartYrkesgruppe(Yrkesgrupper.SOKKEL_ELLER_SKIP),
            lagAvklartMaritimtArbeid(Maritimtyper.SKIP, skipsnavn),
            lagAvklartArbeidsland(arbeidsland, skipsnavn),
            lagAvklartVirksomhet(arbeidsgiversOrgnr));
        return this;
    }

    public TestdataUtfyller utfyllVilkaarForArt12Innvilgelse() throws FunksjonellException, TekniskException {
        behandlingsUtfyller.opprettVilkaar(behandlingsid,
            lagVilkaarDto(Vilkaar.FO_883_2004_ART12_1, true),
            lagVilkaarDto(Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET, true),
            lagVilkaarDto(Vilkaar.ART12_1_FORUTGAAENDE_MEDLEMSKAP, true));
        return this;
    }

    public TestdataUtfyller utfyllVilkaarForArt12Avslag() throws FunksjonellException, TekniskException {
        behandlingsUtfyller.opprettVilkaar(behandlingsid,
            lagVilkaarDto(Vilkaar.FO_883_2004_ART12_1, false, UTSENDELSE_OVER_24_MN),
            lagVilkaarDto(Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET, false, FOR_LITE_OMSETNING_NORGE),
            lagVilkaarDto(Vilkaar.ART12_1_FORUTGAAENDE_MEDLEMSKAP, false, FOLKEREGISTRERT_IKKE_ARBEIDET_I_NORGE),
            lagVilkaarDto(Vilkaar.FO_883_2004_ART16_1, false, SOEKT_FOR_SENT));
        return this;
    }

    public TestdataUtfyller utfyllVilkaarForArt113A() throws FunksjonellException, TekniskException {
        behandlingsUtfyller.opprettVilkaar(behandlingsid,
            lagVilkaarDto(Vilkaar.FO_883_2004_ART11_3A, true),
            lagVilkaarDto(Vilkaar.FO_883_2004_ART11_4_1, true),
            lagVilkaarDto(Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP, false));
        return this;
    }

    public TestdataUtfyller utfyllAvklartefaktaForArt13(Landkoder land1, Landkoder land2, String arbeidsgiverId, Landkoder bostedsland) throws FunksjonellException, TekniskException {
        behandlingsUtfyller.opprettAvklartefakta(behandlingsid,
            lagAvklartSoeknadsland(land1),
            lagAvklartSoeknadsland(land2),
            lagAvklartYrkesgruppe(Yrkesgrupper.ORDINAER),
            lagAvklartBostedsland(bostedsland),
            lagAvklartVirksomhet(arbeidsgiverId));
        return this;
    }

    public TestdataUtfyller utfyllAvklartefaktaForArt13BostedNorge(Landkoder land1, Landkoder land2, String arbeidsgiverId) throws FunksjonellException, TekniskException {
        return utfyllAvklartefaktaForArt13(land1, land2, arbeidsgiverId, Landkoder.NO);
    }

    public TestdataUtfyller opprettInnvilgetLovvalgsperiode(LovvalgBestemmelse bestemmelse) throws FunksjonellException, TekniskException {
        behandlingsUtfyller.opprettLovvalgsperiode(behandlingsid,
            lagLovvalgsperiodeDto(bestemmelse, Landkoder.NO, InnvilgelsesResultat.INNVILGET));
        return this;
    }

    public TestdataUtfyller opprettAvslåttLovvalgsperiode(LovvalgBestemmelse bestemmelse) throws FunksjonellException, TekniskException {
        behandlingsUtfyller.opprettLovvalgsperiode(behandlingsid,
            lagLovvalgsperiodeDto(bestemmelse, null, InnvilgelsesResultat.AVSLAATT));
        return this;
    }

    private TestdataUtfyller opprettAktørBruker(String aktørId) throws FunksjonellException, TekniskException {
        behandlingsUtfyller.opprettAktoer(behandlingsid, lagAktørBrukerDto(aktørId));
        return this;
    }

    public TestdataUtfyller opprettAktørRepresentant(Representerer representerer, String fullmektigOrg) throws FunksjonellException, TekniskException {
        behandlingsUtfyller.opprettAktoer(behandlingsid, lagAktørRepresentantDto(fullmektigOrg, representerer));
        return this;
    }

    public long getBehandlingsid() {
        return behandlingsid;
    }
}
