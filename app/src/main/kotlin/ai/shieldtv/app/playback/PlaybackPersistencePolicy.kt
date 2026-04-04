package ai.shieldtv.app.playback

object PlaybackPersistencePolicy {
    const val SUMMARY = "single-active-resume-record"

    fun describe(): String {
        return "Playback persistence currently stores one active resume record, not a multi-entry continue-watching history. The home-screen continue-watching tile is hydrated from that one record when its progress is in a resume-worthy range."
    }
}
