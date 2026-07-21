package no.nav.melosys.domain

/**
 * Publiseres når en fagsak endrer status. Publiseres sentralt fra FagsakService.oppdaterStatus,
 * som alle fagsak-status-mutasjoner går gjennom — også stier uten aktiv behandling (henleggelse
 * som bortfalt, annullering av sak med kun inaktive behandlinger).
 */
class FagsakStatusEndretEvent(val fagsak: Fagsak)
