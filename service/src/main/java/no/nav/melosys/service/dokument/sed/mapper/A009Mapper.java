package no.nav.melosys.service.dokument.sed.mapper;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.eux.model.SedType;
import no.nav.melosys.eux.model.medlemskap.impl.MedlemskapA009;
import no.nav.melosys.eux.model.nav.Fastperiode;
import no.nav.melosys.eux.model.nav.GjelderPeriode;
import no.nav.melosys.eux.model.nav.Utsendingsland;
import no.nav.melosys.eux.model.nav.Vedtak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.sed.A009Data;

public class A009Mapper implements SedMapper<MedlemskapA009, A009Data> {

    @Override
    public MedlemskapA009 hentMedlemskap(A009Data sedData) throws TekniskException, FunksjonellException {

        final MedlemskapA009 medlemskap = new MedlemskapA009();

        Lovvalgsperiode lovvalgsperiode = sedData.getLovvalgsperiode();

        medlemskap.setVedtak(hentVedtak(lovvalgsperiode));

        if (!sedData.getPersonDokument().erEgenAnsatt) {
            medlemskap.setUtsendingsland(hentUtsendingsland(sedData));
        }

        return medlemskap;
    }

    private Vedtak hentVedtak(Lovvalgsperiode lovvalgsperiode) throws FunksjonellException {
        Vedtak vedtak = new Vedtak();

        vedtak.setEropprinneligvedtak("ja"); //Confluence: "I første omgang støttes kun IntionDecision = Ja". Setter derfor ikke datoforrigevedtak eller erendringsvedtak
        vedtak.setLand(lovvalgsperiode.getLovvalgsland().getKode());
        vedtak.setGjeldervarighetyrkesaktivitet("nei"); //Vil være 'ja' om det er åpen periode. Melosys støtter ikke åpen periode.

        if (!erKorrektLovvalgbestemmelse( lovvalgsperiode.getBestemmelse())) {
            throw new FunksjonellException("Lovvalgsbestemmelse er ikke av artikkel 12!");
        }

        vedtak.setArtikkelforordning(LovvalgTilEuxMapper.mapMelosysLovvalgTilEux(lovvalgsperiode.getBestemmelse()));

        GjelderPeriode gjelderperiode = new GjelderPeriode();

        //Vil alltid være fast periode
        Fastperiode fastperiode = new Fastperiode();
        fastperiode.setStartdato(dateTimeFormatter.format(lovvalgsperiode.getFom()));
        fastperiode.setSluttdato(dateTimeFormatter.format(lovvalgsperiode.getTom()));
        gjelderperiode.setFastperiode(fastperiode);

        vedtak.setGjelderperiode(gjelderperiode);

        return vedtak;
    }

    private boolean erKorrektLovvalgbestemmelse(LovvalgBestemmelse lovvalgBestemmelse) {
        return lovvalgBestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1
            || lovvalgBestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_2;
    }

    private Utsendingsland hentUtsendingsland(A009Data sedData) {
        Utsendingsland utsendingsland = new Utsendingsland();
        utsendingsland.setArbeidsgiver(hentArbeidsGiver(sedData.getArbeidsgivendeVirksomheter()));
        return utsendingsland;
    }

    public SedType getSedType() {
        return SedType.A009;
    }
}
