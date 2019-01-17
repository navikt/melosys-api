package no.nav.melosys.service.dokument.sed.mapper;

import java.util.Collection;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.eux.model.SedType;
import no.nav.melosys.eux.model.medlemskap.impl.MedlemskapA009;
import no.nav.melosys.eux.model.nav.Fastperiode;
import no.nav.melosys.eux.model.nav.Gjelderperiode;
import no.nav.melosys.eux.model.nav.Utsendingsland;
import no.nav.melosys.eux.model.nav.Vedtak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.sed.A009Data;

public class A009Mapper extends AbstraktSedMapper<MedlemskapA009, A009Data> {

    @Override
    protected MedlemskapA009 hentMedlemskap(A009Data sedData) throws TekniskException, FunksjonellException {

        final MedlemskapA009 medlemskap = new MedlemskapA009();

        Collection<Lovvalgsperiode> lovvalgsperioder = sedData.getLovvalgsperioder();

        if (lovvalgsperioder.size() != 1) {
            throw new TekniskException("SED A009 skal kun ha én lovvalgsperiode");
        }

        medlemskap.setVedtak(hentVedtak(lovvalgsperioder.iterator().next()));

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

        String bestemmelse = null;

        if (lovvalgsperiode.getBestemmelse() instanceof LovvalgBestemmelse_883_2004) {
            if(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1.getKode().equals(lovvalgsperiode.getBestemmelse().getKode())) {
                bestemmelse = "12_1";
            } else if (LovvalgBestemmelse_883_2004.FO_883_2004_ART12_2.getKode().equals(lovvalgsperiode.getBestemmelse().getKode())) {
                bestemmelse = "12_2";
            }
        }

        if (bestemmelse == null) throw new FunksjonellException("Lovvalgsbestemmelse er ikke av artikkel 12!");

        vedtak.setArtikkelforordning(bestemmelse);

        Gjelderperiode gjelderperiode = new Gjelderperiode();

        //Vil alltid være fast periode
        Fastperiode fastperiode = new Fastperiode();
        fastperiode.setStartdato(dateTimeFormatter.format(lovvalgsperiode.getFom()));
        fastperiode.setSluttdato(dateTimeFormatter.format(lovvalgsperiode.getTom()));
        gjelderperiode.setFastperiode(fastperiode);

        vedtak.setGjelderperiode(gjelderperiode);

        return vedtak;
    }

    private Utsendingsland hentUtsendingsland(A009Data sedData) {
        Utsendingsland utsendingsland = new Utsendingsland();
        utsendingsland.setArbeidsgiver(hentArbeidsGiver(sedData.getArbeidsgivendeVirkomsheter()));
        return utsendingsland;
    }

    protected SedType getSedType() {
        return SedType.A009;
    }
}
