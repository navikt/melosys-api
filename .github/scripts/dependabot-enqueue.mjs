// Legger Dependabot-PR-er i merge queue. dependabot-automerge.yml kopierer fila inn i en
// utsjekk av navikt/automerge-dependabot og kjører den der.
//
// Utvalget er det samme som actionen bruker: findMergeablePRs og applyFilters importeres
// fra actionens src/, så reglene ikke kopieres. Actionen merger med REST, og det avviser
// merge queue. Her legges PR-en i kø med GraphQL enqueuePullRequest.
//
// Inputene har samme navn og tolkning som actionens (action.yml), som miljøvariabler.
// DRY_RUN=true viser utvalget uten å godkjenne eller legge i kø.

import * as core from '@actions/core';
import * as github from '@actions/github';
import { findMergeablePRs, approvePullRequest } from './src/pullRequests.js';
import { applyFilters, getFilterReasons } from './src/filters.js';
import { shouldRunAtCurrentTime } from './src/timeUtils.js';

const liste = (navn) => (process.env[navn] ? process.env[navn].split(',').map((v) => v.trim()) : []);
const tall = (navn, standard) => {
  const verdi = parseInt(process.env[navn] ?? '', 10);
  return Number.isNaN(verdi) ? standard : verdi;
};

const token = process.env.TOKEN;
const dryRun = process.env.DRY_RUN === 'true';
const autoApprove = process.env.AUTO_APPROVE === 'true';
const minimumAgeInDays = tall('MINIMUM_AGE_OF_PR', 0);
const retryDelayMs = tall('RETRY_DELAY_MS', 2000);
const filterOptions = {
  ignoredDependencies: liste('IGNORED_DEPENDENCIES'),
  alwaysAllow: liste('ALWAYS_ALLOW'),
  alwaysAllowLabels: liste('ALWAYS_ALLOW_LABELS'),
  ignoredVersions: liste('IGNORED_VERSIONS'),
  semverFilter: process.env.SEMVER_FILTER ? liste('SEMVER_FILTER') : ['patch', 'minor'],
};

// Tidslinjen gir siste uttak av køen og siste push i én spørring. Er siste hendelse et
// uttak på grunn av feilet sjekk eller timeout, eller manual av noe annet enn en bot, legges
// PR-en ikke inn igjen før en ny commit. GitHub bruker også manual for uttak den gjør selv
// (actor github-merge-queue, typen Bot). Andre grunner, som merged og merge_conflict,
// hindrer ikke ny innlegging.
const blokkerer = (uttak) =>
  ['failed_checks', 'checks_timed_out'].includes(uttak.reason) ||
  (uttak.reason === 'manual' && uttak.actor?.__typename !== 'Bot');
const PR_STATUS = `
  query($owner: String!, $repo: String!, $number: Int!) {
    repository(owner: $owner, name: $repo) {
      pullRequest(number: $number) {
        id
        isInMergeQueue
        timelineItems(last: 20, itemTypes: [REMOVED_FROM_MERGE_QUEUE_EVENT, HEAD_REF_FORCE_PUSHED_EVENT, PULL_REQUEST_COMMIT]) {
          nodes {
            __typename
            ... on RemovedFromMergeQueueEvent { createdAt reason actor { __typename login } }
          }
        }
      }
    }
  }`;

const ENQUEUE = `
  mutation($id: ID!, $headOid: GitObjectID!) {
    enqueuePullRequest(input: { pullRequestId: $id, expectedHeadOid: $headOid }) {
      mergeQueueEntry { position }
    }
  }`;

async function run() {
  if (!token) throw new Error('TOKEN mangler');
  if (!shouldRunAtCurrentTime(process.env.BLACKOUT_PERIODS ?? '')) {
    core.info('Blackout-periode. Ingenting legges i kø.');
    return;
  }

  const octokit = github.getOctokit(token);
  const { owner, repo } = github.context.repo;
  const { data: repoData } = await octokit.rest.repos.get({ owner, repo });
  const defaultBranch = repoData.default_branch;

  // Som actionen: bare fra default-branchen, så en branch ikke kan endre utvalget.
  if (!dryRun && process.env.GITHUB_REF !== `refs/heads/${defaultBranch}`) {
    core.warning(`Kjører ikke fra ${defaultBranch} (ref ${process.env.GITHUB_REF}). Ingenting legges i kø.`);
    return;
  }

  const regler = await octokit.paginate('GET /repos/{owner}/{repo}/rules/branches/{branch}', {
    owner, repo, branch: defaultBranch,
  });
  if (!regler.some((regel) => regel.type === 'merge_queue')) {
    core.warning(`${owner}/${repo} har ingen aktiv merge queue på ${defaultBranch}. Ingenting legges i kø.`);
    if (!dryRun) return;
  }

  const { eligiblePRs, initialPRs } = await findMergeablePRs(octokit, owner, repo, minimumAgeInDays, retryDelayMs);
  const utvalgt = eligiblePRs.length > 0 ? applyFilters(eligiblePRs, filterOptions) : [];
  const resultat = new Map();
  let feilet = 0;

  for (const pr of utvalgt) {
    try {
      const { repository } = await octokit.graphql(PR_STATUS, { owner, repo, number: pr.number });
      const status = repository.pullRequest;
      if (status.isInMergeQueue) {
        resultat.set(pr.number, 'står alt i køen');
        continue;
      }
      const siste = status.timelineItems.nodes.at(-1);
      if (siste?.__typename === 'RemovedFromMergeQueueEvent' && blokkerer(siste)) {
        const grunn = `tatt ut av køen ${siste.createdAt} (reason: ${siste.reason ?? 'ukjent'}, actor: ${siste.actor?.login ?? 'ukjent'})`;
        core.info(`PR #${pr.number}: ${grunn}, legges ikke inn igjen før ny commit.`);
        resultat.set(pr.number, grunn);
        continue;
      }
      if (dryRun) {
        resultat.set(pr.number, 'ville blitt lagt i kø (dry run)');
        continue;
      }
      if (autoApprove && !(await approvePullRequest(octokit, owner, repo, pr.number))) {
        resultat.set(pr.number, 'godkjenning feilet');
        feilet++;
        continue;
      }
      const svar = await octokit.graphql(ENQUEUE, { id: status.id, headOid: pr.head.sha });
      const plass = svar.enqueuePullRequest.mergeQueueEntry?.position;
      core.info(`PR #${pr.number} lagt i kø${plass != null ? ` (plass ${plass})` : ''}: ${pr.title}`);
      resultat.set(pr.number, 'lagt i kø');
    } catch (error) {
      core.warning(`Klarte ikke legge PR #${pr.number} i kø: ${error.message}`);
      resultat.set(pr.number, `feilet: ${error.message}`);
      feilet++;
    }
  }

  const rader = initialPRs.map((pr) => {
    const filtrert = (getFilterReasons(pr.number) ?? []).map((r) => r.reason).join('; ');
    const tekst = resultat.get(pr.number) ?? (filtrert || 'ikke valgt');
    return [`#${pr.number}`, pr.title, tekst];
  });
  await core.summary
    .addHeading('Dependabot i merge queue', 2)
    .addRaw(dryRun ? 'Dry run: ingenting er godkjent eller lagt i kø.\n\n' : '')
    .addTable([[{ data: 'PR', header: true }, { data: 'Tittel', header: true }, { data: 'Resultat', header: true }], ...rader])
    .write()
    .catch(() => rader.forEach((rad) => core.info(rad.join('  '))));

  // Resten av PR-ene behandles først, men kjøringen blir rød, så feilen blir sett.
  if (feilet > 0) core.setFailed(`${feilet} PR-er ble ikke lagt i kø; se advarslene over.`);
}

run().catch((error) => core.setFailed(`Feilet: ${error.message}`));
