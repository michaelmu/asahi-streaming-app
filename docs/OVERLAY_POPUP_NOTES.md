# Overlay Popup Notes

## Purpose

Asahi has several flows where inline status text or full-screen navigation is a poor fit.
A reusable overlay popup component provides a better TV UX for short-lived, high-focus interactions.

Implemented uses:
- Real-Debrid device linking flow
- APK update/install flow
- Resume prompt flow
- Playback failure recovery

---

## Good Uses for Overlay Popups

### 1. Real-Debrid device auth
Strong overlay case.

### 2. Update install flow
Strong overlay case.

### 3. Resume prompt
Now implemented as an overlay instead of a dedicated screen.

Why this works better:
- small modal decision
- no extra destination needed
- keeps user anchored to source-selection flow

### 4. Playback failure recovery
Now implemented as an overlay.

Current direction:
- Back to Sources
- Try Again

This is a much better fit than silently failing or dumping users into awkward states.

### 5. Confirm destructive or disruptive account actions
Still a good future candidate.

### 6. External player handoff confirmation (future)
Still a natural overlay use.

### 7. Important one-step informational states
Examples:
- link successful
- update downloaded
- install unavailable
- next-episode available (if implemented lightly)

---

## Places Where Overlay Popups Are Probably NOT Ideal

### Search results
Should remain browse-first, not modal.

### Source browsing itself
The source list should remain a normal screen/list, not a popup.

### General settings navigation
Overlay should not become a substitute for ordinary screens.

### Rich player controls
Player transport should rely mostly on Media3 controller behavior, not generic popups.

---

## Recommended Next Uses

If overlay popups are expanded further, the next best candidates are:

1. destructive account/settings confirmations
2. next-episode prompt
3. external-player handoff confirmation

---

## Design Guidance

Overlay popups should:
- be concise
- have 1–2 strong actions, not 6 buttons
- focus attention without replacing whole app screens unnecessarily
- feel like a TV modal, not a mobile dialog dumped onto a TV screen

---

## Bottom Line

Overlay popups are best used in Asahi for:
- short modal decisions
- auth/install/update flows
- recoverable error handling
- small resume/continuation choices

They should improve focus and clarity, not replace core navigation.
