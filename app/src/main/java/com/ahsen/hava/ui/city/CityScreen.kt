package com.ahsen.hava.ui.city

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahsen.hava.R
import com.ahsen.hava.domain.model.City
import com.ahsen.hava.domain.model.PopularCities
import com.ahsen.hava.domain.model.WeatherSnapshot
import com.ahsen.hava.ui.BottomBarSpace
import com.ahsen.hava.ui.LoadState
import com.ahsen.hava.ui.common.UiText
import com.ahsen.hava.ui.component.EmptyView
import com.ahsen.hava.ui.component.ErrorView
import com.ahsen.hava.ui.component.ShimmerList
import com.ahsen.hava.ui.theme.GlassBorder
import com.ahsen.hava.ui.theme.GlassFill
import com.ahsen.hava.ui.theme.HavaTheme
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.SkyBackground
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import com.ahsen.hava.ui.theme.rememberTimeOfDaySky

/**
 * Bağlayıcı katman: ViewModel'i edinir, state'i toplar, [CityScreen]'e verir.
 * Konum izni de burada sorulur; çünkü izin penceresi Activity'ye bağlı bir iştir.
 */
@Composable
fun CityRoute(
    onCityClick: (City) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CityViewModel = viewModel { CityViewModel(createSavedStateHandle()) },
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCityId by viewModel.selectedCityId.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val localWeather by viewModel.localWeather.collectAsStateWithLifecycle()
    val favoriteWeather by viewModel.favoriteWeather.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Sistem izin penceresi. Kullanıcı yaklaşık ya da tam konumdan birine izin verirse yeter.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> viewModel.onLocationPermissionResult(result.values.any { it }) }

    val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    fun hasPermission() = permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    // İzin zaten verilmişse açılışta konumun havasını sessizce getir. (İlk açılışta izin sormayız:
    // kullanıcı karta dokunduğunda sorarız.)
    LaunchedEffect(Unit) { if (hasPermission()) viewModel.onLocationPermissionResult(true) }

    CityScreen(
        query = query,
        uiState = uiState,
        selectedCityId = selectedCityId,
        favorites = favorites,
        favoriteWeather = favoriteWeather,
        localWeather = localWeather,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::search,
        onCityClick = { city ->
            viewModel.onCityClick(city) // Logcat + seçili vurgusu
            onCityClick(city) // tahmin ekranına geçiş
        },
        onToggleFavorite = viewModel::onToggleFavorite,
        onLocalWeatherClick = {
            when (val state = localWeather) {
                is CityViewModel.LocalWeather.Ready -> onCityClick(state.city)
                CityViewModel.LocalWeather.Loading -> Unit
                else -> if (hasPermission()) viewModel.onLocationPermissionResult(true) else permissionLauncher.launch(permissions)
            }
        },
        modifier = modifier,
    )
}

/** Şehir ekranı: state tutmaz, aldığını çizer ve olayları yukarı bildirir. */
@Composable
fun CityScreen(
    query: String,
    uiState: LoadState<List<City>>,
    selectedCityId: Long?,
    favorites: List<City>,
    localWeather: CityViewModel.LocalWeather,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onCityClick: (City) -> Unit,
    onToggleFavorite: (City) -> Unit,
    onLocalWeatherClick: () -> Unit,
    modifier: Modifier = Modifier,
    favoriteWeather: Map<Long, WeatherSnapshot> = emptyMap(),
) {
    // Dinamik arka plan: gündüz mavi, akşam lacivert gökyüzü.
    val sky = rememberTimeOfDaySky()
    val favoriteIds = favorites.map { it.id }.toSet()

    SkyBackground(sky = sky, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Header(modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp))
            SearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                // Dört durumun hepsi burada. sealed olduğu için biri unutulamaz.
                when (uiState) {
                    LoadState.Loading -> ShimmerList()
                    is LoadState.Success -> CityList(
                        // Arama kutusu boşken listenin başında: konumun havası ve favoriler.
                        home = if (query.isBlank()) {
                            HomeSection(localWeather, favorites, favoriteWeather, onLocalWeatherClick)
                        } else {
                            null
                        },
                        cities = uiState.data,
                        listTitle = if (query.isBlank()) {
                            stringResource(R.string.popular_title)
                        } else {
                            stringResource(R.string.result_count, uiState.data.size)
                        },
                        favoriteIds = favoriteIds,
                        selectedCityId = selectedCityId,
                        onCityClick = onCityClick,
                        onToggleFavorite = onToggleFavorite,
                    )
                    LoadState.Empty -> EmptyView(
                        title = stringResource(R.string.empty_title),
                        message = stringResource(R.string.empty_message),
                    )
                    is LoadState.Error -> ErrorView(message = uiState.message.asString(), onRetry = onSearch)
                }
            }
        }
    }
}

/** "Hava+" başlığı: artı işareti sarı. */
@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = TextPrimary)) { append(stringResource(R.string.brand_name)) }
                withStyle(SpanStyle(color = HavaYellow)) { append("+") }
            },
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.header_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
    }
}

/**
 * Arama kutusu. Değeri yukarıdan alır, her harfi yukarı bildirir.
 * Klavyedeki Enter tuşu "Ara" olur (Bonus: klavyedeki ara tuşu).
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            // Bonus: kutuyu temizle. Sadece kutu doluyken görünür.
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = GlassFill,
            unfocusedContainerColor = GlassFill,
            focusedBorderColor = HavaYellow,
            unfocusedBorderColor = GlassBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = HavaYellow,
            focusedPlaceholderColor = TextSecondary,
            unfocusedPlaceholderColor = TextSecondary,
            focusedLeadingIconColor = HavaYellow,
            unfocusedLeadingIconColor = TextSecondary,
            focusedTrailingIconColor = TextPrimary,
            unfocusedTrailingIconColor = TextSecondary,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                onSearch()
            },
        ),
    )
}

/** Ana sayfanın üst kısmı: konum kartı ve favoriler. Sadece arama kutusu boşken gösterilir. */
private data class HomeSection(
    val localWeather: CityViewModel.LocalWeather,
    val favorites: List<City>,
    val favoriteWeather: Map<Long, WeatherSnapshot>,
    val onLocalWeatherClick: () -> Unit,
)

/**
 * Kaydırılabilir tek liste: (varsa) konum kartı ve favoriler, ardından şehirler.
 * Hepsi aynı LazyColumn'da: favoriler çoğalınca ekran sıkışmaz, birlikte kayar.
 * LazyColumn sadece ekranda görünen satırları çizer; `key` sayesinde Compose hangi satırın
 * hangisi olduğunu bilir. Yıldız dolu olan şehirler favorilerdedir.
 */
@Composable
private fun CityList(
    home: HomeSection?,
    cities: List<City>,
    listTitle: String,
    favoriteIds: Set<Long>,
    selectedCityId: Long?,
    onCityClick: (City) -> Unit,
    onToggleFavorite: (City) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Başlıklar burada çözülür: LazyColumn'un içi @Composable değil, stringResource orada çağrılamaz.
    val myLocationTitle = stringResource(R.string.section_my_location)
    val favoritesTitle = stringResource(R.string.section_favorites)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = BottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (home != null) {
            sectionTitle(myLocationTitle)
            item(key = "local-weather") {
                LocalWeatherCard(state = home.localWeather, onClick = home.onLocalWeatherClick)
            }
            if (home.favorites.isNotEmpty()) {
                sectionTitle(favoritesTitle)
                items(items = home.favorites, key = { "favorite-${it.id}" }) { city ->
                    FavoriteCard(city = city, snapshot = home.favoriteWeather[city.id], onClick = { onCityClick(city) })
                }
            }
        }
        sectionTitle(listTitle)
        items(items = cities, key = { "city-${it.id}" }) { city ->
            CityCard(
                city = city,
                isSelected = city.id == selectedCityId,
                onClick = { onCityClick(city) },
                isFavorite = city.id in favoriteIds,
                onToggleFavorite = { onToggleFavorite(city) },
            )
        }
    }
}

private fun LazyListScope.sectionTitle(text: String) {
    item(key = "title-$text") {
        Text(
            text = text,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = HavaYellow,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun CityScreenPreview() {
    HavaTheme {
        CityScreen(
            query = "",
            uiState = LoadState.Success(PopularCities.all),
            selectedCityId = PopularCities.all[2].id,
            favorites = listOf(PopularCities.all[2]),
            localWeather = CityViewModel.LocalWeather.NeedsPermission,
            onQueryChange = {},
            onSearch = {},
            onCityClick = {},
            onToggleFavorite = {},
            onLocalWeatherClick = {},
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun CityScreenEmptyPreview() {
    HavaTheme {
        CityScreen(
            query = "xyzq",
            uiState = LoadState.Empty,
            selectedCityId = null,
            favorites = emptyList(),
            localWeather = CityViewModel.LocalWeather.NeedsPermission,
            onQueryChange = {},
            onSearch = {},
            onCityClick = {},
            onToggleFavorite = {},
            onLocalWeatherClick = {},
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun CityScreenErrorPreview() {
    HavaTheme {
        CityScreen(
            query = "Ankara",
            uiState = LoadState.Error(UiText(R.string.error_network)),
            selectedCityId = null,
            favorites = emptyList(),
            localWeather = CityViewModel.LocalWeather.Loading,
            onQueryChange = {},
            onSearch = {},
            onCityClick = {},
            onToggleFavorite = {},
            onLocalWeatherClick = {},
        )
    }
}
