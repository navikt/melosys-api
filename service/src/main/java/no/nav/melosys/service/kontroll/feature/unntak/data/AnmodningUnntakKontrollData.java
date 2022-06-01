package no.nav.melosys.service.kontroll.feature.unntak.data;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.person.Persondata;

public record AnmodningUnntakKontrollData(Persondata persondata,
                                          BehandlingsgrunnlagData behandlingsgrunnlagData,
                                          Anmodningsperiode anmodningsperiode,
                                          int antallArbeidsgivere) {
}
