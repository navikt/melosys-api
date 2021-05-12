package no.nav.melosys.service.vedtak.publisering.dto;


import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Bestemmelse;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public record LovvalgOgMedlemskapsperiode(Bestemmelse bestemmelse, LovvalgBestemmelse tilleggsbestemmelse,
                                          String lovvalgslandKode, Periode periode,
                                          InnvilgelsesResultat innvilgelsesResultat, Trygdedekninger dekning,
                                          Medlemskapstyper medlemskapstype, FastsattTrygdeavgift fastsattTrygdeavgift) {
}
