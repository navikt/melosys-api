package no.nav.melosys.domain

/**
 * Publiseres når en fagsak endrer status. Publiseres sentralt fra FagsakService.oppdaterStatus,
 * som alle fagsak-status-mutasjoner går gjennom — også stier uten aktiv behandling (henleggelse
 * som bortfalt, annullering av sak med kun inaktive behandlinger).
 *
 * Bærer kun saksnummer (ikke Fagsak-entiteten): konsumenten trenger bare saksnummer, og et slankt
 * event unngår å eksponere en managed JPA-entitet utenfor transaksjonskonteksten den ble lastet i.
 */
class FagsakStatusEndretEvent(val saksnummer: String)
