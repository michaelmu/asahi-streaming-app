# Asahi Build Notes

Current state: the repo is still early and may not build cleanly yet, but the goal is to progressively reduce fake scaffolding.

## Immediate priorities
- keep Android app resources/manifest sane
- keep module conventions centralized
- avoid adding more fake UI than necessary
- prefer a minimal debuggable activity over invisible placeholder app code

## Near-term next steps
- add Gradle wrapper / verify build bootstrap if needed
- make module/plugin setup internally consistent
- decide when to switch from placeholder text UI to real Android TV UI host
