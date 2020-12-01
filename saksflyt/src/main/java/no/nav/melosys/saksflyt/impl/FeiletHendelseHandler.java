package no.nav.melosys.saksflyt.impl;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.hendelser.FeiletHendelse;

public interface FeiletHendelseHandler {
    void behandleFeiletHendelse(FeiletHendelse feiletHendelse) throws IkkeFunnetException;
}
