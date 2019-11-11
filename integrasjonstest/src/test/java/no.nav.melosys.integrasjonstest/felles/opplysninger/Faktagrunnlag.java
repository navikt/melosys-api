package no.nav.melosys.integrasjonstest.felles.opplysninger;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;

import static no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_begrunnelser.UTSENDELSE_OVER_24_MN;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_forutgaaende_medl.FOLKEREGISTRERT_IKKE_ARBEIDET_I_NORGE;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_vesentlig_virksomhet.FOR_LITE_OMSETNING_NORGE;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_avslag.SOEKT_FOR_SENT;
import static no.nav.melosys.integrasjonstest.felles.opplysninger.Testsubjekter.AKTØR_ID;
import static no.nav.melosys.integrasjonstest.felles.utils.AvklartefaktaUtils.*;
import static no.nav.melosys.integrasjonstest.felles.utils.SaksflytTestUtils.*;

public class Faktagrunnlag {
    private final long behandlingsid;

    private final Behandlingsdata behandlingsdata;
    public Faktagrunnlag(long behandlingsId, Behandlingsdata behandlingsdata) {
        this.behandlingsid = behandlingsId;
        this.behandlingsdata = behandlingsdata;

        behandlingsdata.nullstill(behandlingsId);
    }

    public void avklartefaktaForArt12(Landkoder soeknadsland, String arbeidsgiversOrgnr) throws FunksjonellException, TekniskException {
        behandlingsdata.opprettAvklartefakta(behandlingsid,
            lagAvklartSoeknadsland(soeknadsland),
            lagAvklartYrkesgruppe(Yrkesgrupper.ORDINAER),
            lagAvklartVirksomhet(arbeidsgiversOrgnr));
    }

    public void vilkaarForArt12Innvilgelse() throws FunksjonellException, TekniskException {
        behandlingsdata.opprettVilkaar(behandlingsid,
            lagVilkaarDto(Vilkaar.FO_883_2004_ART12_1, true),
            lagVilkaarDto(Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET, true),
            lagVilkaarDto(Vilkaar.ART12_1_FORUTGAAENDE_MEDLEMSKAP, true));
    }

    public void vilkaarForArt12Avslag() throws FunksjonellException, TekniskException {
        behandlingsdata.opprettVilkaar(behandlingsid,
            lagVilkaarDto(Vilkaar.FO_883_2004_ART12_1, false, UTSENDELSE_OVER_24_MN),
            lagVilkaarDto(Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET, false, FOR_LITE_OMSETNING_NORGE),
            lagVilkaarDto(Vilkaar.ART12_1_FORUTGAAENDE_MEDLEMSKAP, false, FOLKEREGISTRERT_IKKE_ARBEIDET_I_NORGE),
            lagVilkaarDto(Vilkaar.FO_883_2004_ART16_1, false, SOEKT_FOR_SENT));
    }

    public void avklartefaktaForArt13(Landkoder land1, Landkoder land2, String arbeidsgiverId) throws FunksjonellException, TekniskException {
        behandlingsdata.opprettAvklartefakta(behandlingsid,
            lagAvklartSoeknadsland(land1),
            lagAvklartSoeknadsland(land2),
            lagAvklartYrkesgruppe(Yrkesgrupper.ORDINAER),
            lagAvklartVirksomhet(arbeidsgiverId));
    }

    public void utfyllInnvilgetLovvalgsperiode(LovvalgBestemmelse bestemmelse) throws FunksjonellException, TekniskException {
        behandlingsdata.opprettLovvalgsperiode(behandlingsid,
            lagLovvalgsperiodeDto(bestemmelse, Landkoder.NO, InnvilgelsesResultat.INNVILGET));
    }

    public void utfyllAvslåttLovvalgsperiode(LovvalgBestemmelse bestemmelse) throws FunksjonellException, TekniskException {
        behandlingsdata.opprettLovvalgsperiode(behandlingsid,
            lagLovvalgsperiodeDto(bestemmelse, null, InnvilgelsesResultat.AVSLAATT));
    }

    public void opprettAktørForBrukerOgRepresentant(Representerer representerer, String orgnr) throws FunksjonellException, TekniskException {
        opprettAktørBruker(AKTØR_ID);
        opprettAktørRepresentant(representerer, orgnr);
    }

    public void opprettAktørBruker(String aktørId) throws FunksjonellException, TekniskException {
        behandlingsdata.opprettAktoer(behandlingsid, lagAktørBrukerDto(aktørId));
    }

    public void opprettAktørRepresentant(Representerer representerer, String fullmektigOrg) throws FunksjonellException, TekniskException {
        behandlingsdata.opprettAktoer(behandlingsid, lagAktørRepresentantDto(fullmektigOrg, representerer));
    }

    public long getBehandlingsid() {
        return behandlingsid;
    }
}
