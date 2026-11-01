package com.nuvio.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_player_episode_code_episode_only
import nuvio.composeapp.generated.resources.compose_player_episode_code_full
import org.jetbrains.compose.resources.stringResource

/**
 * Kitsu gives every season or sequel its own entry and the app files its episodes under season 1,
 * so "S1 E4" on "Cyberpunk: Edgerunners 2" is made up. Kitsu shows read "E4" instead.
 * The player sets [LocalEpisodeOnlyCodes] for its show; callers that know the id pass it directly.
 */
internal val LocalEpisodeOnlyCodes = compositionLocalOf { false }

internal fun usesEpisodeOnlyCodes(contentId: String?): Boolean =
    contentId?.startsWith("kitsu:", ignoreCase = true) == true

@Composable
internal fun episodeCodeLabel(season: Int, episode: Int, contentId: String? = null): String =
    if (LocalEpisodeOnlyCodes.current || usesEpisodeOnlyCodes(contentId)) {
        stringResource(Res.string.compose_player_episode_code_episode_only, episode)
    } else {
        stringResource(Res.string.compose_player_episode_code_full, season, episode)
    }
