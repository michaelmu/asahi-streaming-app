# Exec Plans

This directory is the home for Asahi execution plans.

Execution plans are living implementation documents used to:
- define scoped work clearly
- track progress while changes are in flight
- preserve decisions across sessions
- make completion reliable and auditable

Do not leave active plans scattered across `docs/`.
Put them here and keep their status explicit.

---

## Folder Structure

- `in_progress/` — active plans currently being used for implementation work
- `complete/` — plans that reached a clean stopping point and were finished for their intended pass
- `abandoned/` — plans that were superseded, intentionally dropped, or no longer reflect the path forward

If a plan changes status, move the file rather than keeping stale status in place.

---

# Naming Convention

Use filenames shaped like this:

`YYYY-MM-DD-short-kebab-topic.md`

Examples:
- `2026-04-04-cleanup-hardening-pass.md`
- `2026-04-04-next-pass-ranking-orchestration.md`
- `2026-04-10-player-resume-polish.md`

Rules:
- include the date the plan was created
- keep the topic short and specific
- use kebab-case
- avoid generic names like `EXEC_PLAN_FINAL.md` or `plan2.md`

Why:
- date sorting stays useful
- topics stay human-readable
- multiple plan generations do not collide

---

# Required Plan Structure

Every exec plan should include these sections unless there is a very strong reason not to:

## 1. Header
Must include:
- title
- last updated date
- status
- owner
- optional link to superseded/superseding plans

## 2. Purpose
State:
- what the plan is for
- what kind of pass this is
- what problem it is trying to solve

## 3. How to Use This Plan
Describe:
- what to read before starting
- how to update the plan while working
- what makes a task truly done

## 4. Status Legend
Recommended values:
- `TODO`
- `IN_PROGRESS`
- `BLOCKED`
- `DONE`
- `DEFERRED`
- `OPTIONAL`

## 5. Current Focus
State:
- current phase
- immediate target
- why this is the current priority

This section must be updated whenever the phase or immediate target changes.
A stale `Current Focus` section makes the whole plan misleading.

## 6. Repository Reality Check
Before implementation begins, confirm:
- the actual files/classes involved exist
- the current method/data-class signatures match expectations
- any important mismatch from prior summaries or assumptions is recorded

This section is intentionally lightweight, but it prevents planning against imaginary APIs.

## 7. Locked Decisions
Capture product/architecture decisions that future implementation should treat as settled for this pass.
Examples:
- local storage instead of database
- episode-level instead of show-level history
- duplicate action refreshes recency

## 8. Review Summary or Background
Explain the reasoning behind the plan.
This is where architectural findings, UX observations, or code-review conclusions live.

## 9. Phases / Task Groups
Each task group should include:
- status
- priority
- goal
- why it matters
- proposed sub-steps
- validation expectations

## 10. Recommended Order
List the intended order of execution.
This reduces drift and avoids random work selection.

## 11. Open Questions / Decisions Needed
Capture unresolved questions explicitly instead of hiding them in implementation.

## 12. Risks / Watchouts
Document likely failure modes, churn risks, and areas where regressions are easy.

## 13. Validation Notes / Honesty Check
For major milestones, record:
- what was actually validated
- what was not validated
- known uncertainty or suspicious areas

This is meant to reduce optimistic claims and preserve trust in the plan.

## 14. Progress Log
Append entries as work progresses.
Each meaningful step should record:
- timestamp
- what changed
- what was validated
- what remains
- any new risk or decision

## 15. Scope Changes
Log scope expansions, reductions, intentional deferrals, or newly preserved future hooks.

## 16. Session Start
At the start of a work session, update this with the intended task.

## 17. Definition of Done
State what makes the plan complete for its intended pass.

---

# Execution Rules

## Rule 1: The plan is part of the implementation
Do not treat the plan as paperwork.
If the code changes and the plan does not, the plan is stale.

## Rule 2: Update during the work, not just after
Minimum updates while working:
- task status changes
- progress log entries after meaningful milestones
- scope-change notes when direction shifts

## Rule 3: Mark reality, not optimism
If something is not complete, do not mark it `DONE`.
Use:
- `DEFERRED` if intentionally postponed
- `BLOCKED` if waiting
- `ABANDONED` by moving the plan if it no longer represents the path forward

## Rule 4: Preserve decision history
If behavior changes intentionally, record it.
If a plan item was skipped for good reasons, record that too.

## Rule 5: Validate before claiming completion
A task is only complete when validation happened and was recorded.
Validation can be:
- tests
- build success
- manual flow verification
- explicit rationale if full validation was not possible

## Rule 6: Check the repo before coding against assumptions
Before implementation, confirm the files and APIs you intend to change actually exist in the current branch.
If the real code shape differs from the plan, record that mismatch and adapt before claiming progress.

## Rule 7: Keep commits honest and scoped
Before each commit:
- run `git status --short`
- confirm only intended files are staged
- if unrelated changes are included, either split them, document them, or ask before proceeding

## Rule 8: Close plans promptly when required scope is done
If all required items are `DONE`, `DEFERRED`, `OPTIONAL`, or removed, do not leave the plan lingering in `in_progress/`.
In the same pass:
- update `Current Focus`
- set final plan status truthfully
- move it to `complete/` if no real next required slice remains

---

# When to Create a New Plan vs Reuse One

## Reuse the existing plan if:
- the work is a continuation of the same pass
- the goals are still the same
- the plan is still truthful with reasonable edits

## Create a new plan if:
- the prior pass reached a clean stopping point
- the priorities materially changed
- the old plan would become bloated or misleading
- a new architecture or product direction is being explored

When a new plan supersedes an older one:
- create the new plan
- update the header to note what it supersedes
- move the old plan to `complete/` or `abandoned/` as appropriate

---

# What Makes a Good Exec Plan

Good plans are:
- specific
- honest
- scoped
- ordered
- test-aware
- easy to resume after a session break

Bad plans are:
- vague
- giant wishlists with no prioritization
- stale after implementation starts
- full of tasks marked `DONE` without validation
- missing explicit deferrals for large ideas not chosen yet

---

# Suggested Status Flow

Typical lifecycle:
1. create in `in_progress/`
2. do a repository reality check
3. update while implementing
4. on completion, move to `complete/`
5. if superseded or dropped, move to `abandoned/`

Do not duplicate the same plan across multiple folders.
Move it.

---

# Migration Guidance

When porting older plans into this structure:
- preserve the original content as much as practical
- rename files to the standard format
- update headers/status if necessary
- add a note if the plan predates this structure
- move each plan to the folder matching its real current state

---

# Minimal Checklist for Future Plan Creation

Before calling a new exec plan ready, confirm:
- [ ] correct folder
- [ ] correct filename
- [ ] clear purpose
- [ ] current focus section
- [ ] repository reality check section
- [ ] locked decisions section
- [ ] prioritized phases/tasks
- [ ] validation expectations
- [ ] validation honesty section
- [ ] progress log section
- [ ] scope changes section
- [ ] session start section
- [ ] definition of done

---

# Template

Use:
- `docs/exec-plans/TEMPLATE.md`

Start from the template when creating a new plan, then move the created file into the correct status folder with a properly dated filename.

---

# Current Canonical Plans

See subfolders for current status.
The root `docs/exec-plans/` directory should mainly contain this guide, the template, and the three state folders.
