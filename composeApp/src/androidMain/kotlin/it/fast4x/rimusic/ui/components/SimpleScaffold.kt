package it.fast4x.rimusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavController
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.ui.components.themed.AppBar
import it.fast4x.rimusic.ui.styling.Dimensions
import me.knighthat.colorPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleScaffold(
    navController: NavController,
    content: @Composable () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val customModifier = if( UiType.RiMusic.isCurrent())
        Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    else Modifier


    androidx.compose.material3.Scaffold(
        modifier = customModifier,
        containerColor = colorPalette().background0,
        topBar = {
            if( UiType.RiMusic.isCurrent()) {
                AppBar(navController)
            }
        },
        bottomBar = {}
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .padding(paddingValues)
                .background(colorPalette().background0)
                .fillMaxSize()
        ) {

            Surface(
                modifier = Modifier
                    .fillMaxWidth(
                        if( NavigationBarPosition.Right.isCurrent() )
                            Dimensions.contentWidthRightBar
                        else
                            1f
                    ),
                content = content
            )
        }
    }
}