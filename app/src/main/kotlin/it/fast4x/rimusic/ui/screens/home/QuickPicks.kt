package it.fast4x.rimusic.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import it.fast4x.compose.persist.persist
import it.fast4x.compose.persist.persistList
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.NavigationEndpoint
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.discoverPage
import it.fast4x.innertube.requests.relatedPage
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.PlayEventsType
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.models.Artist
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.query
import it.fast4x.rimusic.service.isLocal
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.ShimmerHost
import it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import it.fast4x.rimusic.ui.components.themed.NonQueuedMediaItemMenu
import it.fast4x.rimusic.ui.components.themed.TextPlaceholder
import it.fast4x.rimusic.ui.items.AlbumItem
import it.fast4x.rimusic.ui.items.AlbumItemPlaceholder
import it.fast4x.rimusic.ui.items.ArtistItem
import it.fast4x.rimusic.ui.items.ArtistItemPlaceholder
import it.fast4x.rimusic.ui.items.PlaylistItem
import it.fast4x.rimusic.ui.items.PlaylistItemPlaceholder
import it.fast4x.rimusic.ui.items.SongItem
import it.fast4x.rimusic.ui.items.SongItemPlaceholder
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.ui.styling.px
import it.fast4x.rimusic.utils.UiTypeKey
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.center
import it.fast4x.rimusic.utils.contentWidthKey
import it.fast4x.rimusic.utils.downloadedStateMedia
import it.fast4x.rimusic.utils.forcePlay
import it.fast4x.rimusic.utils.getDownloadState
import it.fast4x.rimusic.utils.isLandscape
import it.fast4x.rimusic.utils.manageDownload
import it.fast4x.rimusic.utils.navigationBarPositionKey
import it.fast4x.rimusic.utils.playEventsTypeKey
import it.fast4x.rimusic.utils.preferences
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.secondary
import it.fast4x.rimusic.utils.semiBold
import it.fast4x.rimusic.utils.showNewAlbumsArtistsKey
import it.fast4x.rimusic.utils.showNewAlbumsKey
import it.fast4x.rimusic.utils.showPlaylistMightLikeKey
import it.fast4x.rimusic.utils.showRelatedAlbumsKey
import it.fast4x.rimusic.utils.showSearchTabKey
import it.fast4x.rimusic.utils.showSimilarArtistsKey
import kotlinx.coroutines.flow.distinctUntilChanged


@ExperimentalMaterialApi
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun QuickPicks(
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onMoodClick: (mood: Innertube.Mood.Item) -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val uiType  by rememberPreference(UiTypeKey, UiType.RiMusic)
    val playEventType  by rememberPreference(playEventsTypeKey, PlayEventsType.MostPlayed)

    var trending by persist<Song?>("home/trending")

    var relatedPageResult by persist<Result<Innertube.RelatedPage?>?>(tag = "home/relatedPageResult")

    var discoverPage by persist<Result<Innertube.DiscoverPage>>("home/discoveryAlbums")

    //var discoverPageAlbums by persist<Result<Innertube.DiscoverPageAlbums>>("home/discoveryAlbums")

    var preferitesArtists by persistList<Artist>("home/artists")

    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    val context = LocalContext.current


    val showRelatedAlbums by rememberPreference(showRelatedAlbumsKey, true)
    val showSimilarArtists by rememberPreference(showSimilarArtistsKey, true)
    val showNewAlbumsArtists by rememberPreference(showNewAlbumsArtistsKey, true)
    val showPlaylistMightLike by rememberPreference(showPlaylistMightLikeKey, true)
    val showNewAlbums by rememberPreference(showNewAlbumsKey, true)

    val navigationBarPosition by rememberPreference(navigationBarPositionKey, NavigationBarPosition.Left)
    val contentWidth = context.preferences.getFloat(contentWidthKey,0.8f)

    /*
    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    fun refresh() {
        refreshScope.launch {
            refreshing = true
            delay(1000)
            // ******** //
            // Do something
            // ******** //
            refreshing = false
        }
    }
    val refreshState = rememberPullRefreshState(refreshing, ::refresh)
     */

    LaunchedEffect(Unit) {
            when (playEventType) {
                PlayEventsType.MostPlayed ->
                    Database.trendingReal().distinctUntilChanged().collect { songs ->
                        val song = songs.firstOrNull()
                        if (relatedPageResult == null || trending?.id != song?.id) {
                            relatedPageResult = Innertube.relatedPage(
                                NextBody(
                                    videoId = (song?.id ?: "HZnNt9nnEhw")
                                )
                            )
                        }
                        trending = song
                    }

                PlayEventsType.LastPlayed ->
                    Database.lastPlayed().distinctUntilChanged().collect { songs ->
                        val song = songs.firstOrNull()
                        if (relatedPageResult == null || trending?.id != song?.id) {
                            relatedPageResult =
                                Innertube.relatedPage(
                                    NextBody(
                                        videoId = (song?.id ?: "HZnNt9nnEhw")
                                    )
                                )
                        }
                        trending = song
                    }
            }
    }

    LaunchedEffect(Unit) {
        //discoverPageAlbums = Innertube.discoverPageNewAlbums()
        discoverPage = Innertube.discoverPage()
    }

    //println("mediaItem newalbums $discoverPageAlbums")

    LaunchedEffect(Unit) {
        Database.preferitesArtistsByName().collect { preferitesArtists = it }
    }

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px
    val albumThumbnailSizeDp = 108.dp
    val albumThumbnailSizePx = albumThumbnailSizeDp.px
    val artistThumbnailSizeDp = 92.dp
    val artistThumbnailSizePx = artistThumbnailSizeDp.px
    val playlistThumbnailSizeDp = 108.dp
    val playlistThumbnailSizePx = playlistThumbnailSizeDp.px

    val scrollState = rememberScrollState()
    val quickPicksLazyGridState = rememberLazyGridState()
    val moodAngGenresLazyGridState = rememberLazyGridState()

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    val showSearchTab by rememberPreference(showSearchTabKey, false)

    BoxWithConstraints (
        modifier = Modifier
            //.pullRefresh(refreshState)
    ) {
        val quickPicksLazyGridItemWidthFactor = if (isLandscape && maxWidth * 0.475f >= 320.dp) {
            0.475f
        } else {
            0.9f
        }
        val itemInHorizontalGridWidth = maxWidth * quickPicksLazyGridItemWidthFactor

        val moodItemWidthFactor = if (isLandscape && maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val itemWidth = maxWidth * moodItemWidthFactor

        Column(
            modifier = Modifier
                .background(colorPalette.background0)
                //.fillMaxSize()
                .fillMaxHeight()
                .fillMaxWidth(if (navigationBarPosition == NavigationBarPosition.Left || navigationBarPosition == NavigationBarPosition.Top) 1f else contentWidth)
                .verticalScroll(scrollState)
                .padding(
                    windowInsets
                        .only(WindowInsetsSides.Vertical)
                        .asPaddingValues()
                )
        ) {

            HeaderWithIcon(
                title = stringResource(R.string.quick_picks),
                iconId = R.drawable.search,
                enabled = true,
                showIcon = !showSearchTab,
                modifier = Modifier,
                onClick = onSearchClick
            )

            BasicText(
                text = stringResource(R.string.tips),
                style = typography.m.semiBold,
                modifier = sectionTextModifier
            )
            BasicText(
                text = when (playEventType) {
                    PlayEventsType.MostPlayed -> stringResource(R.string.by_most_played_song)
                    PlayEventsType.LastPlayed -> stringResource(R.string.by_last_played_song)
                },
                style = typography.xxs.secondary,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
            )

            relatedPageResult?.getOrNull()?.let { related ->
                LazyHorizontalGrid(
                    state = quickPicksLazyGridState,
                    rows = GridCells.Fixed(3),
                    flingBehavior = ScrollableDefaults.flingBehavior(),
                    contentPadding = endPaddingValues,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimensions.itemsVerticalPadding * 3 * 9)
                        //.height((songThumbnailSizeDp + Dimensions.itemsVerticalPadding * 2) * 4)
                ) {
                    trending?.let { song ->
                        item {
                            val isLocal by remember { derivedStateOf { song.asMediaItem.isLocal } }
                            downloadState = getDownloadState(song.asMediaItem.mediaId)
                            val isDownloaded = if (!isLocal) downloadedStateMedia(song.asMediaItem.mediaId) else true

                            SongItem(
                                song = song,
                                isDownloaded = isDownloaded,
                                onDownloadClick = {
                                    binder?.cache?.removeResource(song.asMediaItem.mediaId)
                                    query {
                                        Database.insert(
                                            Song(
                                                id = song.asMediaItem.mediaId,
                                                title = song.asMediaItem.mediaMetadata.title.toString(),
                                                artistsText = song.asMediaItem.mediaMetadata.artist.toString(),
                                                thumbnailUrl = song.thumbnailUrl,
                                                durationText = null
                                            )
                                        )
                                    }

                                    if (!isLocal)
                                    manageDownload(
                                        context = context,
                                        songId = song.id,
                                        songTitle = song.title,
                                        downloadState = isDownloaded
                                    )

                                },
                                downloadState = downloadState,
                                thumbnailSizePx = songThumbnailSizePx,
                                thumbnailSizeDp = songThumbnailSizeDp,
                                trailingContent = {
                                    Image(
                                        painter = painterResource(R.drawable.star),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colorPalette.accent),
                                        modifier = Modifier
                                            .size(16.dp)
                                    )
                                },
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    onDismiss = menuState::hide,
                                                    mediaItem = song.asMediaItem,
                                                    onRemoveFromQuickPicks = {
                                                        query {
                                                            Database.clearEventsFor(song.id)
                                                        }
                                                    },

                                                    onDownload = {
                                                        binder?.cache?.removeResource(song.asMediaItem.mediaId)
                                                        query {
                                                            Database.insert(
                                                                Song(
                                                                    id = song.asMediaItem.mediaId,
                                                                    title = song.asMediaItem.mediaMetadata.title.toString(),
                                                                    artistsText = song.asMediaItem.mediaMetadata.artist.toString(),
                                                                    thumbnailUrl = song.thumbnailUrl,
                                                                    durationText = null
                                                                )
                                                            )
                                                        }
                                                        manageDownload(
                                                            context = context,
                                                            songId = song.id,
                                                            songTitle = song.title,
                                                            downloadState = isDownloaded
                                                        )
                                                    }

                                                )
                                            }
                                        },
                                        onClick = {
                                            val mediaItem = song.asMediaItem
                                            binder?.stopRadio()
                                            binder?.player?.forcePlay(mediaItem)
                                            binder?.setupRadio(
                                                NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                            )
                                        }
                                    )
                                    .animateItemPlacement()
                                    .width(itemInHorizontalGridWidth)
                            )
                        }
                    }

                    //if (!refreshing) {
                        items(
                            items = related.songs?.dropLast(if (trending == null) 0 else 1)
                                ?: emptyList(),
                            key = Innertube.SongItem::key
                        ) { song ->
                            val isLocal by remember { derivedStateOf { song.asMediaItem.isLocal } }
                            downloadState = getDownloadState(song.asMediaItem.mediaId)
                            val isDownloaded =
                                if (!isLocal) downloadedStateMedia(song.asMediaItem.mediaId) else true

                            SongItem(
                                song = song,
                                isDownloaded = isDownloaded,
                                onDownloadClick = {
                                    binder?.cache?.removeResource(song.asMediaItem.mediaId)
                                    query {
                                        Database.insert(
                                            Song(
                                                id = song.asMediaItem.mediaId,
                                                title = song.asMediaItem.mediaMetadata.title.toString(),
                                                artistsText = song.asMediaItem.mediaMetadata.artist.toString(),
                                                thumbnailUrl = song.thumbnail?.url,
                                                durationText = null
                                            )
                                        )
                                    }
                                    if (!isLocal)
                                        manageDownload(
                                            context = context,
                                            songId = song.asMediaItem.mediaId,
                                            songTitle = song.asMediaItem.mediaMetadata.title.toString(),
                                            downloadState = isDownloaded
                                        )

                                },
                                downloadState = downloadState,
                                thumbnailSizePx = songThumbnailSizePx,
                                thumbnailSizeDp = songThumbnailSizeDp,
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    onDismiss = menuState::hide,
                                                    mediaItem = song.asMediaItem,
                                                    onDownload = {
                                                        binder?.cache?.removeResource(song.asMediaItem.mediaId)
                                                        query {
                                                            Database.insert(
                                                                Song(
                                                                    id = song.asMediaItem.mediaId,
                                                                    title = song.asMediaItem.mediaMetadata.title.toString(),
                                                                    artistsText = song.asMediaItem.mediaMetadata.artist.toString(),
                                                                    thumbnailUrl = song.thumbnail?.url,
                                                                    durationText = null
                                                                )
                                                            )
                                                        }
                                                        manageDownload(
                                                            context = context,
                                                            songId = song.asMediaItem.mediaId,
                                                            songTitle = song.asMediaItem.mediaMetadata.title.toString(),
                                                            downloadState = isDownloaded
                                                        )
                                                    },

                                                    )
                                            }
                                        },
                                        onClick = {
                                            val mediaItem = song.asMediaItem
                                            binder?.stopRadio()
                                            binder?.player?.forcePlay(mediaItem)
                                            binder?.setupRadio(
                                                NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                            )
                                        }
                                    )
                                    .animateItemPlacement()
                                    .width(itemInHorizontalGridWidth)
                            )
                        }
                    //}//
                }


                discoverPage?.getOrNull()?.let { page ->
                    var newReleaseAlbumsFiltered by persistList<Innertube.AlbumItem>("discovery/newalbumsartist")
                    page.newReleaseAlbums.forEach { album ->
                        preferitesArtists.forEach { artist ->
                            if (artist.name == album.authors?.first()?.name) {
                                newReleaseAlbumsFiltered += album
                                //Log.d("mediaItem","artst ok")
                            }
                        }
                    }

                    if (showNewAlbumsArtists)
                        if ( newReleaseAlbumsFiltered.isNotEmpty() && preferitesArtists.isNotEmpty() ) {
                            BasicText(
                                text = stringResource(R.string.new_albums_of_your_artists),
                                style = typography.m.semiBold,
                                modifier = sectionTextModifier
                            )

                            LazyRow(contentPadding = endPaddingValues) {
                                items(items = newReleaseAlbumsFiltered.distinct(), key = { it.key }) {
                                    //preferitesArtists.forEach { artist ->
                                    //    if (artist.name == it.authors?.first()?.name)
                                    AlbumItem(
                                        album = it,
                                        thumbnailSizePx = albumThumbnailSizePx,
                                        thumbnailSizeDp = albumThumbnailSizeDp,
                                        alternative = true,
                                        modifier = Modifier.clickable(onClick = {
                                            onAlbumClick( it.key )
                                        })
                                    )
                                    //}

                                }
                            }

                        }

                    if (showNewAlbums) {
                        BasicText(
                            text = stringResource(R.string.new_albums),
                            style = typography.m.semiBold,
                            modifier = sectionTextModifier
                        )

                        LazyRow(contentPadding = endPaddingValues) {
                            items(items = page.newReleaseAlbums, key = { it.key }) {
                                AlbumItem(
                                    album = it,
                                    thumbnailSizePx = albumThumbnailSizePx,
                                    thumbnailSizeDp = albumThumbnailSizeDp,
                                    alternative = true,
                                    modifier = Modifier.clickable(onClick = {
                                        onAlbumClick(it.key)
                                    })
                                )
                            }
                        }
                    }
                }

                if (showRelatedAlbums)
                    related.albums?.let { albums ->
                        BasicText(
                            text = stringResource(R.string.related_albums),
                            style = typography.m.semiBold,
                            modifier = sectionTextModifier
                        )

                        LazyRow(contentPadding = endPaddingValues) {
                            items(
                                items = albums,
                                key = Innertube.AlbumItem::key
                            ) { album ->
                                AlbumItem(
                                    album = album,
                                    thumbnailSizePx = albumThumbnailSizePx,
                                    thumbnailSizeDp = albumThumbnailSizeDp,
                                    alternative = true,
                                    modifier = Modifier
                                        .clickable(onClick = { onAlbumClick(album.key) })
                                )
                            }
                        }
                    }

                if (showSimilarArtists)
                    related.artists?.let { artists ->
                        BasicText(
                            text = stringResource(R.string.similar_artists),
                            style = typography.m.semiBold,
                            modifier = sectionTextModifier
                        )

                        LazyRow(contentPadding = endPaddingValues) {
                            items(
                                items = artists,
                                key = Innertube.ArtistItem::key,
                            ) { artist ->
                                ArtistItem(
                                    artist = artist,
                                    thumbnailSizePx = artistThumbnailSizePx,
                                    thumbnailSizeDp = artistThumbnailSizeDp,
                                    alternative = true,
                                    modifier = Modifier
                                        .clickable(onClick = { onArtistClick(artist.key) })
                                )
                            }
                        }
                    }

                if (showPlaylistMightLike)
                related.playlists?.let { playlists ->
                    BasicText(
                        text = stringResource(R.string.playlists_you_might_like),
                        style = typography.m.semiBold,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 24.dp, bottom = 8.dp)
                    )

                    LazyRow(contentPadding = endPaddingValues) {
                        items(
                            items = playlists,
                            key = Innertube.PlaylistItem::key,
                        ) { playlist ->
                            PlaylistItem(
                                playlist = playlist,
                                thumbnailSizePx = playlistThumbnailSizePx,
                                thumbnailSizeDp = playlistThumbnailSizeDp,
                                alternative = true,
                                showSongsCount = false,
                                modifier = Modifier
                                    .clickable(onClick = { onPlaylistClick(playlist.key) })
                            )
                        }
                    }
                }

                discoverPage?.getOrNull()?.let { page ->
                    if (page.moods.isNotEmpty()) {

                        BasicText(
                            text = stringResource(R.string.moods_and_genres),
                            style = typography.m.semiBold,
                            modifier = sectionTextModifier
                        )

                        LazyHorizontalGrid(
                            state = moodAngGenresLazyGridState,
                            rows = GridCells.Fixed(4),
                            flingBehavior = ScrollableDefaults.flingBehavior(),
                            //flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                            contentPadding = endPaddingValues,
                            modifier = Modifier
                                .fillMaxWidth()
                                //.height((thumbnailSizeDp + Dimensions.itemsVerticalPadding * 8) * 8)
                                .height(Dimensions.itemsVerticalPadding * 4 * 8)
                        ) {
                            items(
                                items = page.moods.sortedBy { it.title },
                                key = { it.endpoint.params ?: it.title }
                            ) {
                                MoodItem(
                                    mood = it,
                                    onClick = { it.endpoint.browseId?.let { _ -> onMoodClick(it) } },
                                    modifier = Modifier
                                        .width(itemWidth)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }

            } ?: relatedPageResult?.exceptionOrNull()?.let {
                BasicText(
                    text = stringResource(R.string.an_error_has_occurred),
                    style = typography.s.secondary.center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(all = 16.dp)
                )
            } ?: ShimmerHost {
                repeat(3) {
                    SongItemPlaceholder(
                        thumbnailSizeDp = songThumbnailSizeDp,
                    )
                }

                TextPlaceholder(modifier = sectionTextModifier)

                Row {
                    repeat(2) {
                        AlbumItemPlaceholder(
                            thumbnailSizeDp = albumThumbnailSizeDp,
                            alternative = true
                        )
                    }
                }

                TextPlaceholder(modifier = sectionTextModifier)

                Row {
                    repeat(2) {
                        ArtistItemPlaceholder(
                            thumbnailSizeDp = albumThumbnailSizeDp,
                            alternative = true
                        )
                    }
                }

                TextPlaceholder(modifier = sectionTextModifier)

                Row {
                    repeat(2) {
                        PlaylistItemPlaceholder(
                            thumbnailSizeDp = albumThumbnailSizeDp,
                            alternative = true
                        )
                    }
                }
            }
        }
        if(uiType == UiType.ViMusic)
        FloatingActionsContainerWithScrollToTop(
            scrollState = scrollState,
            iconId = R.drawable.search,
            onClick = onSearchClick
        )

        /*
        PullRefreshIndicator(
            refreshing, refreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
         */



    }

}


