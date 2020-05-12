package no.nav.melosys.service.kontroll.vedtak;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.service.kontroll.KontrollData;

final class VedtakKontrollData extends KontrollData {
    private final PersonDokument personDokument;
    private final BehandlingsgrunnlagData behandlingsgrunnlagData;
    private final Lovvalgsperiode lovvalgsperiode;

    VedtakKontrollData(MedlemskapDokument medlemskapDokument,
                       PersonDokument personDokument,
                       BehandlingsgrunnlagData behandlingsgrunnlagData,
                       Lovvalgsperiode lovvalgsperiode) {
        super(medlemskapDokument);
        this.behandlingsgrunnlagData = behandlingsgrunnlagData;
        this.personDokument = personDokument;
        this.lovvalgsperiode = lovvalgsperiode;
    }

    public BehandlingsgrunnlagData getBehandlingsgrunnlagData() {
        return behandlingsgrunnlagData;
    }

    PersonDokument getPersonDokument() {
        return personDokument;
    }

    Lovvalgsperiode getLovvalgsperiode() {
        return lovvalgsperiode;
    }
}
