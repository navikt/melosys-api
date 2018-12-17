package no.nav.melosys.service.dokument.brev.mapper;

import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.dokument.brev.BrevDataA1;

public class VedleggMapper  {

    private Behandling behandling;
    private Behandlingsresultat resultat;
    private VedleggType vedlegg;

    public VedleggMapper(Behandling behandling, Behandlingsresultat resultat) {
        this.behandling = behandling;
        this.resultat = resultat;
        this.vedlegg = new VedleggType();
    }

    public void map(BrevDataA1 brevDataA1, BrevDataA001 brevDataA001) throws TekniskException {
        if (brevDataA1 != null) {
            map(brevDataA1);
        }
        if (brevDataA001 != null) {
            map(brevDataA001);
        }
    }

    public void map(BrevDataA1 brevDataA1) throws TekniskException {
        A1Mapper mapper = new A1Mapper();
        vedlegg.setA1(mapper.mapA1(behandling, resultat, brevDataA1));
    }

    public void map(BrevDataA001 brevDataA001) throws TekniskException {
        A001Mapper mapper = new A001Mapper();
        vedlegg.setSEDA001(mapper.mapSEDA001(brevDataA001));
    }

    public VedleggType hent() {
        return vedlegg;
    }
}