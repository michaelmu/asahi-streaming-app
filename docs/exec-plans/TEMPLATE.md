# <Project / Feature / Pass Name>

Last updated: YYYY-MM-DD UTC
Status: TODO
Owner: <owner>
Location: `docs/exec-plans/<in_progress|complete|abandoned>/YYYY-MM-DD-short-kebab-topic.md`
Supersedes: <optional>
Superseded by: <optional>

## Purpose

Describe:
- what this plan is for
- why it exists
- what kind of pass this is
- what problem it is solving

---

## How to Use This Plan

### Before starting a session
1. Read this file top-to-bottom.
2. Check `Current Focus`.
3. Check `Open Questions / Decisions Needed`.
4. Check the latest `Progress Log` entry.
5. Update `Session Start` before coding.

### While implementing
Update this file during the work, not only after the work.

Minimum required updates:
- task status changes
- progress log entries after meaningful milestones
- scope-change notes when direction shifts
- validation notes after completed steps

### Completion rule
A task is only `DONE` when:
- the code landed
- relevant tests/build/manual validation happened
- follow-up work is captured here if needed

---

## Status Legend

- `TODO` = not started
- `IN_PROGRESS` = actively being worked
- `BLOCKED` = waiting on a prerequisite or decision
- `DONE` = implemented and validated
- `DEFERRED` = intentionally postponed
- `OPTIONAL` = not required unless chosen

---

## Current Focus

**Current phase:** <phase>

**Immediate target:** <what is being worked right now>

**Why this now:**
<why this is the current priority>

> Update this section whenever the active phase or immediate target changes.

---

## Repository Reality Check

Before implementation begins, confirm:
- <file/class/method actually exists>
- <signature/state shape confirmed>
- <important mismatch from assumptions, if any>

---

## Locked Decisions

- <decision that should be treated as settled>
- <decision that should be treated as settled>

---

## Background / Review Summary

Summarize the reasoning behind the plan:
- review findings
- architectural concerns
- UX concerns
- product rationale
- prior work this builds on

---

# Phase A — <name>

## A1. <task name>
Status: TODO
Priority: High

### Goal
<what success looks like>

### Why this matters
<why this task is worth doing>

### Proposed sub-steps
- [TODO] <step>
- [TODO] <step>
- [TODO] <step>

### Validation
- <test/build/manual validation requirement>
- <test/build/manual validation requirement>

---

## A2. <task name>
Status: TODO
Priority: Medium

### Goal
<goal>

### Why this matters
<why>

### Proposed sub-steps
- [TODO] <step>
- [TODO] <step>

### Validation
- <validation>

---

# Phase B — <name>

## B1. <task name>
Status: TODO
Priority: Medium

### Goal
<goal>

### Why this matters
<why>

### Proposed sub-steps
- [TODO] <step>
- [TODO] <step>

### Validation
- <validation>

---

# Optional Work

## O1. <optional task name>
Status: OPTIONAL
Priority: Low

### Notes
<why it is optional and when it should be pulled in>

---

## Recommended Order

1. <task>
2. <task>
3. <task>

---

## Open Questions / Decisions Needed

### Q1. <question>
Current recommendation:
<recommendation>

### Q2. <question>
Current recommendation:
<recommendation>

---

## Risks / Watchouts

- <risk>
- <risk>
- <risk>

---

## Validation Notes / Honesty Check

### <milestone or phase>
- Validated by: <tests/build/manual flow>
- Not validated: <what remains unverified>
- Known uncertainty: <anything suspicious or partially inferred>

---

## Progress Log

### YYYY-MM-DD HH:MM UTC
- Created the plan.
- No implementation work completed yet.

---

## Scope Changes

### YYYY-MM-DD
- Initial scope established.
- Future hooks to preserve: <data/model requirement that should shape implementation now even if UI comes later>

---

## Session Start

### YYYY-MM-DD HH:MM UTC
Intended task: <what this session is starting with>

---

## Definition of Done

This plan is complete for its intended pass when:
- all accepted items are marked `DONE`, `DEFERRED`, `OPTIONAL`, or removed
- validation is recorded for completed work
- major follow-up work is explicitly captured
- `Current Focus` no longer implies unfinished required work
- the file is ready to move out of `in_progress/` without misleading anyone
