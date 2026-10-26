package com.ahsen.hava.ui.trip

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahsen.hava.R
import com.ahsen.hava.domain.model.Place
import com.ahsen.hava.domain.model.Routine
import com.ahsen.hava.domain.model.TravelMode
import com.ahsen.hava.domain.model.TripRoutes
import com.ahsen.hava.domain.usecase.TripPlan
import com.ahsen.hava.ui.LoadState
import com.ahsen.hava.ui.theme.GlassFill
import com.ahsen.hava.ui.theme.HavaYellow
import com.ahsen.hava.ui.theme.SkyBackground
import com.ahsen.hava.ui.theme.TextPrimary
import com.ahsen.hava.ui.theme.TextSecondary
import com.ahsen.hava.ui.theme.rememberTimeOfDaySky

/**
 * Bağlayıcı katman: ViewModel + konum ve bildirim izni.
 *
 * @param openRoutineRequest bildirime her dokunuşta artan sayaç; yeni bir değer gelince rutin yolculuğu açılır.
 */
@Composable
fun TripRoute(
    onBack: (() -> Unit)?,
    openRoutineRequest: Int = 0,
    viewModel: TripViewModel = viewModel { TripViewModel() },
) {
    val origin by viewModel.origin.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val myLocation by viewModel.myLocation.collectAsStateWithLifecycle()
    val isLocating by viewModel.isLocating.collectAsStateWithLifecycle()
    val activeField by viewModel.activeField.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val isWeatherLoading by viewModel.isWeatherLoading.collectAsStateWithLifecycle()
    val departureOffset by viewModel.departureOffset.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val routine by viewModel.routine.collectAsStateWithLifecycle()
    val routineEditor by viewModel.routineEditor.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Sekme değişip geri gelince aynı istek ikinci kez işlenmesin.
    var handledRoutineRequest by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(openRoutineRequest) {
        if (openRoutineRequest > handledRoutineRequest) {
            handledRoutineRequest = openRoutineRequest
            viewModel.onOpenRoutine()
        }
    }

    // Android 13+: bildirim izni ilk rutin kaydında sorulur (izin olmadan rutin yine kaydedilir, sadece bildirim gelmez).
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val saveRoutine: (Routine) -> Unit = { newRoutine ->
        viewModel.onSaveRoutine(newRoutine)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> viewModel.onLocationPermissionResult(result.values.any { it }) }

    // Ekran açılınca: izin zaten varsa konumu sessizce bul, yoksa bir kez sor.
    LaunchedEffect(Unit) {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val granted = permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (granted) viewModel.onLocationPermissionResult(true) else permissionLauncher.launch(permissions)
    }

    TripScreen(
        origin = origin,
        destination = destination,
        myLocation = myLocation,
        isLocating = isLocating,
        activeField = activeField,
        query = query,
        results = results,
        routes = routes,
        plan = plan,
        isWeatherLoading = isWeatherLoading,
        departureOffset = departureOffset,
        selectedMode = selectedMode,
        onModeSelect = viewModel::onModeSelect,
        onDepartureChange = viewModel::onDepartureChange,
        onRetryRoutes = viewModel::onRetryRoutes,
        onBack = onBack,
        onFieldClick = viewModel::onFieldClick,
        onQueryChange = viewModel::onQueryChange,
        onPlaceSelected = viewModel::onPlaceSelected,
        onUseMyLocation = viewModel::onUseMyLocation,
        onSwap = viewModel::onSwap,
        routine = routine,
        onOpenRoutine = viewModel::onOpenRoutine,
        onEditRoutine = viewModel::onEditRoutine,
        onRoutineEnabledChange = viewModel::onRoutineEnabledChange,
    )

    routineEditor?.let { editor ->
        RoutineSheet(
            editor = editor,
            onSave = saveRoutine,
            onDelete = viewModel::onDeleteRoutine,
            onDismiss = viewModel::onDismissRoutineEditor,
        )
    }
}

@Composable
fun TripScreen(
    origin: Place?,
    destination: Place?,
    myLocation: Place?,
    isLocating: Boolean,
    activeField: TripField?,
    query: String,
    results: LoadState<List<Place>>?,
    routes: LoadState<TripRoutes>?,
    plan: TripPlan?,
    isWeatherLoading: Boolean,
    departureOffset: Int,
    selectedMode: TravelMode?,
    onModeSelect: (TravelMode) -> Unit,
    onDepartureChange: (Int) -> Unit,
    onRetryRoutes: () -> Unit,
    onBack: (() -> Unit)?,
    onFieldClick: (TripField) -> Unit,
    onQueryChange: (String) -> Unit,
    onPlaceSelected: (Place) -> Unit,
    onUseMyLocation: () -> Unit,
    onSwap: () -> Unit,
    routine: Routine?,
    onOpenRoutine: () -> Unit,
    onEditRoutine: () -> Unit,
    onRoutineEnabledChange: (Boolean) -> Unit,
) {
    val from = origin ?: myLocation
    SkyBackground(sky = rememberTimeOfDaySky()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            TopBar(onBack = onBack)
            RouteCard(
                originText = origin?.name ?: stringResource(if (isLocating) R.string.locating else R.string.my_location),
                originIsMyLocation = origin == null,
                destinationText = destination?.name,
                activeField = activeField,
                query = query,
                onFieldClick = onFieldClick,
                onQueryChange = onQueryChange,
                onSwap = onSwap,
                canSwap = from != null && destination != null,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            // Rota kutusunun hemen altı: ekrandaki yolculuk rutin değilse "rutinim yap",
            // rutinse (ya da henüz yolculuk seçilmediyse) kayıtlı rutin kartı.
            val hasTrip = activeField == null && from != null && destination != null
            val isRoutineTrip = routine != null && routine.from == from && routine.to == destination
            when {
                hasTrip && !isRoutineTrip -> {
                    Spacer(Modifier.height(10.dp))
                    SaveRoutineCard(onClick = onEditRoutine, modifier = Modifier.padding(horizontal = 16.dp))
                }
                // Arama yazılırken sonuçlara yer açmak için gizlenir.
                routine != null && query.isEmpty() -> {
                    Spacer(Modifier.height(10.dp))
                    RoutineCard(
                        routine = routine,
                        onOpen = onOpenRoutine,
                        onEdit = onEditRoutine,
                        onEnabledChange = onRoutineEnabledChange,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (activeField != null) {
                    SearchResults(
                        field = activeField,
                        results = results,
                        query = query,
                        onPlaceSelected = onPlaceSelected,
                        onUseMyLocation = onUseMyLocation,
                    )
                } else if (from != null && destination != null) {
                    RouteOptions(
                        routes = routes,
                        plan = plan,
                        selectedMode = selectedMode,
                        isWeatherLoading = isWeatherLoading,
                        departureOffset = departureOffset,
                        onModeSelect = onModeSelect,
                        onDepartureChange = onDepartureChange,
                        onRetry = onRetryRoutes,
                    )
                }
            }
        }
    }
}

/** Başlık. Alt menüden açıldığında (ana sekme) geri düğmesi yoktur. */
@Composable
private fun TopBar(onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (onBack == null) 20.dp else 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(CircleShape)
                    .background(GlassFill),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = TextPrimary)
            }
        }
        Column {
            // Hava sekmesindeki "Hava+" başlığıyla aynı dil: artı işareti sarı.
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = TextPrimary)) { append(stringResource(R.string.trip_title)) }
                    withStyle(SpanStyle(color = HavaYellow)) { append("+") }
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        }
    }
}
