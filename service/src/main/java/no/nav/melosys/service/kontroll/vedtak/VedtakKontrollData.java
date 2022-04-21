package no.nav.melosys.service.kontroll.vedtak;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.kontroll.KontrollData;

final class VedtakKontrollData extends KontrollData {
    private final Persondata persondata;
    private final BehandlingsgrunnlagData behandlingsgrunnlagData;
    private final Lovvalgsperiode lovvalgsperiode;
    private final Lovvalgsperiode opprinneligLovvalgsperiode;

    VedtakKontrollData(MedlemskapDokument medlemskapDokument,
                       Persondata persondata,
                       BehandlingsgrunnlagData behandlingsgrunnlagData,
                       Lovvalgsperiode lovvalgsperiode,
                       Lovvalgsperiode opprinneligLovvalgsperiode) {
        super(medlemskapDokument);
        this.behandlingsgrunnlagData = behandlingsgrunnlagData;
        this.persondata = persondata;
        this.lovvalgsperiode = lovvalgsperiode;
        this.opprinneligLovvalgsperiode = opprinneligLovvalgsperiode;
    }

    VedtakKontrollData(Persondata persondata,
                       BehandlingsgrunnlagData behandlingsgrunnlagData) {
        super(null);
        this.behandlingsgrunnlagData = behandlingsgrunnlagData;
        this.persondata = persondata;
        this.lovvalgsperiode = null;
        this.opprinneligLovvalgsperiode = null;
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

    public Lovvalgsperiode getOpprinneligLovvalgsperiode() {
        return opprinneligLovvalgsperiode;
    }
}
