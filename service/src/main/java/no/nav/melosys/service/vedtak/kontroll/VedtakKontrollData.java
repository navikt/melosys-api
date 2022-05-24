package no.nav.melosys.service.vedtak.kontroll;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.person.Persondata;

final class VedtakKontrollData {

    private final MedlemskapDokument medlemskapDokument;
    private final Persondata persondata;
    private final BehandlingsgrunnlagData behandlingsgrunnlagData;
    private final Lovvalgsperiode lovvalgsperiode;
    private final Lovvalgsperiode opprinneligLovvalgsperiode;

    VedtakKontrollData(MedlemskapDokument medlemskapDokument,
                       Persondata persondata,
                       BehandlingsgrunnlagData behandlingsgrunnlagData,
                       Lovvalgsperiode lovvalgsperiode,
                       Lovvalgsperiode opprinneligLovvalgsperiode) {
        this.medlemskapDokument = medlemskapDokument;
        this.behandlingsgrunnlagData = behandlingsgrunnlagData;
        this.persondata = persondata;
        this.lovvalgsperiode = lovvalgsperiode;
        this.opprinneligLovvalgsperiode = opprinneligLovvalgsperiode;
    }

    static VedtakKontrollData lagKontrollDataForAvslag(Persondata persondata,
                                                       BehandlingsgrunnlagData behandlingsgrunnlagData) {
        return new VedtakKontrollData(null, persondata, behandlingsgrunnlagData, null, null);
    }

    MedlemskapDokument getMedlemskapDokument() {
        return medlemskapDokument;
    }

    BehandlingsgrunnlagData getBehandlingsgrunnlagData() {
        return behandlingsgrunnlagData;
    }

    Persondata getPersondata() {
        return persondata;
    }

    Lovvalgsperiode getLovvalgsperiode() {
        return lovvalgsperiode;
    }

    Lovvalgsperiode getOpprinneligLovvalgsperiode() {
        return opprinneligLovvalgsperiode;
    }
}
