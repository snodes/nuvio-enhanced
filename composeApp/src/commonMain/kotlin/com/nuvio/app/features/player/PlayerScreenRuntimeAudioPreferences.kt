package com.nuvio.app.features.player

internal val PlayerScreenRuntime.contentLanguage: String?
    get() {
        val metadata = listOfNotNull(metaUiState.meta, playerMeta).filter {
            it.id == parentMetaId && it.type == parentMetaType
        }
        return metadata.firstNotNullOfOrNull { resolveContentLanguage(it.language, null) }
            ?: resolveContentLanguage(args.contentLanguage, null)
            ?: metadata.firstNotNullOfOrNull { countryToLanguageCode(it.country) }
            ?: "ja".takeIf { metadata.any { it.isAnime } }
            ?: inferAnimeOriginalLanguage(
                ids = listOfNotNull(parentMetaId, args.videoId) + metadata.map { it.id },
                types = listOf(parentMetaType, args.contentType.orEmpty()) + metadata.map { it.type },
                genres = metadata.flatMap { it.genres },
            )
    }

internal val PlayerScreenRuntime.preferredAudioLanguageTargets: List<String>
    get() = resolvePreferredAudioLanguageTargets(
        preferredAudioLanguage = playerSettingsUiState.preferredAudioLanguage,
        secondaryPreferredAudioLanguage = playerSettingsUiState.secondaryPreferredAudioLanguage,
        deviceLanguages = DeviceLanguagePreferences.preferredLanguageCodes(),
        contentOriginalLanguage = contentLanguage,
    )

internal data class AppliedAudioPreferences(
    val controller: PlayerEngineController,
    val languages: List<String>,
    val tracks: List<AudioTrack>,
)

internal fun PlayerScreenRuntime.applyPreferredAudioTrack(targets: List<String>) {
    if (isUserExplicitAudioSelection) return
    val controller = playerController ?: return
    val preferences = AppliedAudioPreferences(
        controller = controller,
        languages = targets,
        tracks = audioTracks.map { it.copy(isSelected = false) },
    )
    val hasCommentary = audioTracks.any(::isCommentaryAudioTrack)
    if (appliedAudioPreferences != preferences) {
        // Native language selection cannot distinguish main audio from commentary in that language.
        // With commentary present, clear native language ranking and select only safe candidates.
        controller.applyAudioLanguagePreferences(if (hasCommentary) emptyList() else targets)
        appliedAudioPreferences = preferences
        preferredAudioSelectionApplied = false
    } else {
        preferredAudioSelectionApplied = true
    }
    if (hasCommentary) {
        val safeTracks = audioTracks.filterNot(::isCommentaryAudioTrack)
        val preferred = findPreferredAudioTrackIndex(safeTracks, targets)
        val track = safeTracks.getOrNull(preferred)
            ?: safeTracks.firstOrNull { it.isSelected }
            ?: safeTracks.firstOrNull()
        if (track != null && !track.isSelected) {
            controller.selectAudioTrack(track.index)
            selectedAudioIndex = track.index
        }
    }
}

internal fun PlayerScreenRuntime.restorePersistedAudioPreference(preference: PersistedPlayerTrackPreference) {
    if (audioTracks.isEmpty()) return
    val restoredIndex = findPersistedAudioTrackIndex(audioTracks, preference)
    if (restoredIndex < 0) return
    playerController?.selectAudioTrack(restoredIndex)
    selectedAudioIndex = restoredIndex
    isUserExplicitAudioSelection = true
    preferredAudioSelectionApplied = true
}

internal fun PlayerScreenRuntime.refreshAudioTracksIfChanged() {
    if (playbackSnapshot.isLoading) return
    val controller = playerController ?: return
    if (!preferredAudioSelectionApplied || controller.getAudioTracks() != audioTracks) {
        refreshTracks()
    }
}
