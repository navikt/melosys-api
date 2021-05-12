package no.nav.melosys.service.dokument.brev.mapper;

import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataVedlegg;

public class VedleggMapper  {

    private Behandling behandling;
    private Behandlingsresultat resultat;
    private VedleggType vedlegg;

    public VedleggMapper(Behandling behandling, Behandlingsresultat resultat) {
        this.behandling = behandling;
        this.resultat = resultat;
        this.vedlegg = new VedleggType();
    }

    public void map(BrevDataVedlegg vedleggData) {
        if (vedleggData.brevDataA1 != null) {
            map(vedleggData.brevDataA1);
        }
        if (vedleggData.brevDataA001 != null) {
            map(vedleggData.brevDataA001);
        }
    }

    public void map(BrevDataA1 brevDataA1) {
        A1Mapper mapper = new A1Mapper();
        vedlegg.setA1(mapper.mapA1(behandling, resultat, brevDataA1));
    }

    public void map(BrevDataA001 brevDataA001) {
        A001Mapper mapper = new A001Mapper();
        vedlegg.setSEDA001(mapper.mapSEDA001(brevDataA001));
    }

    public VedleggType hent() {
        return vedlegg;
    }
}
