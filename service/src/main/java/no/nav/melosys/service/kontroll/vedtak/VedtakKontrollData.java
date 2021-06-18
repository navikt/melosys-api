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

    VedtakKontrollData(MedlemskapDokument medlemskapDokument,
                       Persondata persondata,
                       BehandlingsgrunnlagData behandlingsgrunnlagData,
                       Lovvalgsperiode lovvalgsperiode) {
        super(medlemskapDokument);
        this.behandlingsgrunnlagData = behandlingsgrunnlagData;
        this.persondata = persondata;
        this.lovvalgsperiode = lovvalgsperiode;
    }

    BehandlingsgrunnlagData getBehandlingsgrunnlagData() {
        return behandlingsgrunnlagData;
    }

    Persondata getPersonDokument() {
        return persondata;
    }

    Lovvalgsperiode getLovvalgsperiode() {
        return lovvalgsperiode;
    }
}
