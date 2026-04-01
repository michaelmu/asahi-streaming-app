# Asahi Build Notes

Current state: the repo is still early and may not build cleanly yet, but the goal is to progressively reduce fake scaffolding.

Update: the real Gradle wrapper has now been generated successfully. The next step is verifying actual project/module build behavior.

## Immediate priorities
- keep Android app resources/manifest sane
- keep module conventions centralized
- avoid adding more fake UI than necessary
- prefer a minimal debuggable activity over invisible placeholder app code
- make bootstrap expectations explicit even before the real wrapper/build verification exists

## Near-term next steps
- add Gradle wrapper / verify build bootstrap
- make module/plugin setup internally consistent
- fix buildSrc/root plugin resolution and versioning issues surfaced by first bootstrap attempts
- decide when to switch from placeholder text UI to real Android TV UI host
- avoid adding more feature complexity until bootstrap gaps are reduced
