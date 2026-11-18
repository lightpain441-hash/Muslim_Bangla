package com.example.ui.screens

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.MatrimonialProfileEntity
import com.example.data.local.MatrimonialRequestEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.MatrimonialFirestoreRepository
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodels.MatrimonialViewModel
import com.example.ui.viewmodels.UiState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private val BANGLADESH_DIVISIONS = listOf("সব", "ঢাকা", "চট্টগ্রাম", "সিলেট", "রাজশাহী", "খুলনা", "বরিশাল", "রংপুর", "ময়মনসিংহ")

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context.findActivity() ?: context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

private fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("গুগল সাইন-ইন কনফিগারেশন পাওয়া যায়নি (default_web_client_id অনুপস্থিত)।")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context.findActivity() ?: context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("অপ্রত্যাশিত ক্রেডেনশিয়াল পাওয়া গেছে।")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "গুগল সাইন-ইন ব্যর্থ হয়েছে। আবার চেষ্টা করুন।")
        }
    }
}

private fun signOutMatrimonial(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun MatrimonialGateScreen(
    prefsRepo: UserPreferencesRepository,
    onOpenChat: (String) -> Unit
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val user = currentUser
    if (user == null) {
        var attemptedSilentSignIn by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!attemptedSilentSignIn) {
                attemptedSilentSignIn = true
                attemptAutoSignIn(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {},
                    onUnauthenticated = {},
                    scope = scope
                )
            }
        }
        MatrimonialGoogleSignInGateScreen(
            credentialManager = credentialManager,
            showBack = false,
            onBack = {}
        )
    } else {
        key(user.uid) {
            val viewModel: MatrimonialViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) as Application
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        MatrimonialViewModel(
                            repo = MatrimonialFirestoreRepository(db, auth),
                            currentUserId = user.uid,
                            prefsRepo = prefsRepo
                        )
                    }
                }
            )
            MatrimonialHubScreen(
                viewModel = viewModel,
                userEmail = user.email,
                onSignOut = {
                    signOutMatrimonial(credentialManager, scope)
                },
                onOpenChat = onOpenChat
            )
        }
    }
}

@Composable
fun MatrimonialChatGateScreen(
    requestId: String,
    prefsRepo: UserPreferencesRepository,
    onBack: () -> Unit
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val user = currentUser
    if (user == null) {
        MatrimonialGoogleSignInGateScreen(
            credentialManager = credentialManager,
            showBack = true,
            onBack = onBack
        )
    } else {
        key(user.uid) {
            val viewModel: MatrimonialViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) as Application
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        MatrimonialViewModel(
                            repo = MatrimonialFirestoreRepository(db, auth),
                            currentUserId = user.uid,
                            prefsRepo = prefsRepo
                        )
                    }
                }
            )
            MatrimonialChatScreen(
                requestId = requestId,
                viewModel = viewModel,
                onBack = onBack
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatrimonialGoogleSignInGateScreen(
    credentialManager: CredentialManager,
    showBack: Boolean,
    onBack: () -> Unit
) {
    if (showBack) {
        BackHandler(onBack = onBack)
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("ইসলামিক ম্যাট্রিমোনিয়াল", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                if (showBack) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "পেছনে যান")
                    }
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column {
                        Image(
                            painter = painterResource(id = R.drawable.img_matrimonial_banner),
                            contentDescription = "ইসলামিক ম্যাট্রিমোনিয়াল ব্যানার",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "ম্যাট্রিমোনিয়াল সেকশনে প্রবেশের জন্য গুগল সাইন-ইন প্রয়োজন",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "কুরআন, হাদিস, নামাজের সময়, দুআ, তাসবীহ, কুইজ, যাকাত ও বুকমার্ক কোনো লগইন ছাড়াই ব্যবহার করা যায়। শুধুমাত্র ম্যাট্রিমোনিয়াল (বিবাহ) সেকশনে ভুয়া প্রোফাইল প্রতিরোধ, ওয়ালীর তত্ত্বাবধান ও নিরাপদ ক্লাউড চ্যাটের জন্য গুগল অ্যাকাউন্ট দিয়ে সাইন-ইন বাধ্যতামূলক।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                            Text(
                                text = "প্রাইভেসি ও শালীনতা নিশ্চিতকরণ",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "• আপনার পরিচয় সুরক্ষিত থাকে এবং ডিফল্টভাবে ছবি ঝাপসা থাকে।\n• কেবল আপনার সম্মতিতে অনুমোদিত প্রস্তাবের মাধ্যমেই চ্যাট চালু হয়।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        errorMessage?.let { msg ->
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = { isLoading = false },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isLoading = false }
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("matrimonial_google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("গুগল অ্যাকাউন্ট দিয়ে সাইন-ইন করুন", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrimonialHubScreen(
    viewModel: MatrimonialViewModel,
    userEmail: String? = null,
    onSignOut: () -> Unit = {},
    onOpenChat: (String) -> Unit
) {
    val activeProfilesState by viewModel.activeProfilesState.collectAsStateWithLifecycle()
    val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()
    val requestsState by viewModel.requestsState.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var genderFilter by rememberSaveable { mutableStateOf("ALL") }
    var divisionFilter by rememberSaveable { mutableStateOf("সব") }
    var detailProfile by remember { mutableStateOf<MatrimonialProfileEntity?>(null) }
    var requestTargetProfile by remember { mutableStateOf<MatrimonialProfileEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    val acceptedRevealReceiverIds = remember(requestsState, viewModel.currentUserId) {
        val reqs = (requestsState as? UiState.Success)?.data.orEmpty()
        reqs.filter { it.status == "ACCEPTED" && it.photoRevealApproved }
            .flatMap { it.participantIds }
            .filter { it != viewModel.currentUserId }
            .toSet()
    }

    val pendingOrAcceptedByReceiver = remember(requestsState, viewModel.currentUserId) {
        val reqs = (requestsState as? UiState.Success)?.data.orEmpty()
        reqs.filter { it.senderId == viewModel.currentUserId }
            .associateBy { it.receiverId }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("ইসলামিক ম্যাট্রিমোনিয়াল", fontWeight = FontWeight.Bold)
                    Text(
                        text = userEmail?.takeIf { it.isNotBlank() } ?: "গুগল অ্যাকাউন্ট সংযুক্ত",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("matrimonial_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = "সাইন-আউট করুন"
                    )
                }
            }
        )
        SnackbarHost(hostState = snackbarHostState)

        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("প্রস্তাব খুঁজুন") },
                modifier = Modifier.testTag("matrimonial_tab_discover")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("আগ্রহ ও রিকোয়েস্ট") },
                modifier = Modifier.testTag("matrimonial_tab_requests")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("আমার প্রোফাইল") },
                modifier = Modifier.testTag("matrimonial_tab_profile")
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (selectedTab) {
                0 -> {
                    val profiles = when (val state = activeProfilesState) {
                        is UiState.Success -> state.data.filter {
                            it.userId != viewModel.currentUserId &&
                                (genderFilter == "ALL" || it.gender == genderFilter) &&
                                (divisionFilter == "সব" || it.division == divisionFilter)
                        }
                        else -> emptyList()
                    }
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            MatrimonialModestyBanner()
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        "ALL" to "সবাই",
                                        "PATRO" to "পাত্র",
                                        "PATRI" to "পাত্রী"
                                    ).forEach { (key, label) ->
                                        FilterChip(
                                            selected = genderFilter == key,
                                            onClick = { genderFilter = key },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(BANGLADESH_DIVISIONS) { div ->
                                        FilterChip(
                                            selected = divisionFilter == div,
                                            onClick = { divisionFilter = div },
                                            label = { Text(div) }
                                        )
                                    }
                                }
                            }
                        }
                        when (activeProfilesState) {
                            UiState.Loading -> item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is UiState.Error -> item {
                                Text(
                                    text = (activeProfilesState as UiState.Error).message,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            is UiState.Success -> {
                                if (profiles.isEmpty()) {
                                    item {
                                        Card(shape = RoundedCornerShape(18.dp)) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(20.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "এই ফিল্টারে কোনো সক্রিয় বায়োডাটা পাওয়া যায়নি।",
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "নতুন বায়োডাটা যুক্ত হলে এখানে প্রদর্শিত হবে। আপনি চাইলে নিজের বায়োডাটা প্রকাশ করতে পারেন।",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                FilledTonalButton(
                                                    onClick = { selectedTab = 2 },
                                                    modifier = Modifier
                                                        .height(48.dp)
                                                        .testTag("go_to_publish_biodata_button")
                                                ) {
                                                    Text("নিজের বায়োডাটা প্রকাশ করুন")
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    items(profiles, key = { it.id }) { profile ->
                                        val isPhotoClear = !profile.photoBlurred || (profile.userId in acceptedRevealReceiverIds)
                                        val existingRequest = pendingOrAcceptedByReceiver[profile.userId]
                                        MatrimonialProfileCard(
                                            profile = profile,
                                            isPhotoClear = isPhotoClear,
                                            existingRequestStatus = existingRequest?.status,
                                            onViewDetails = { detailProfile = profile },
                                            onSendInterest = { requestTargetProfile = profile }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (val state = requestsState) {
                            UiState.Loading -> item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is UiState.Error -> item {
                                Text(state.message, color = MaterialTheme.colorScheme.error)
                            }
                            is UiState.Success -> {
                                if (state.data.isEmpty()) {
                                    item {
                                        Card(shape = RoundedCornerShape(18.dp)) {
                                            Text(
                                                text = "এখনো কোনো আগ্রহ বা প্রস্তাব আদান-প্রদান হয়নি। প্রস্তাব পাঠালে ও অপর পক্ষ গ্রহণ করলে এখান থেকে নিরাপদ চ্যাট করতে পারবেন।",
                                                modifier = Modifier.padding(20.dp),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                } else {
                                    items(state.data, key = { it.id }) { req ->
                                        MatrimonialRequestCard(
                                            request = req,
                                            currentUserId = viewModel.currentUserId,
                                            onRespond = { accept, approvePhoto ->
                                                viewModel.respondToRequest(req.id, accept, approvePhoto)
                                            },
                                            onTogglePhotoReveal = { approved ->
                                                viewModel.togglePhotoReveal(req.id, approved)
                                            },
                                            onOpenChat = { onOpenChat(req.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    val existingProfile = (myProfileState as? UiState.Success)?.data
                    MyMatrimonialProfileEditor(
                        currentUserId = viewModel.currentUserId,
                        existingProfile = existingProfile,
                        isLoading = myProfileState is UiState.Loading,
                        onSave = { viewModel.saveMyProfile(it) }
                    )
                }
            }
        }
    }

    detailProfile?.let { profile ->
        val isPhotoClear = !profile.photoBlurred || (profile.userId in acceptedRevealReceiverIds)
        AlertDialog(
            onDismissRequest = { detailProfile = null },
            title = {
                Text(
                    "${profile.profileCode} • ${if (profile.gender == "PATRI") "পাত্রী" else "পাত্র"}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("বয়স: ${profile.age} বছর • বিভাগ: ${profile.division}")
                    Text("শিক্ষাগত যোগ্যতা: ${profile.education}")
                    Text("পেশা: ${profile.profession}")
                    Text("দ্বীনদারিতা: ${profile.practicingNote}")
                    Text("অভিভাবক (ওয়ালী): ${profile.waliRelation}")
                    HorizontalDivider()
                    Text("বিস্তারিত পরিচিতি:", fontWeight = FontWeight.SemiBold)
                    Text(profile.bioBangla)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isPhotoClear) "ছবি প্রদর্শনের অনুমতি সক্রিয় আছে।"
                            else "ইসলামিক পর্দার স্বার্থে ছবি ঝাপসা রাখা হয়েছে। প্রস্তাব গৃহীত হলে এবং সম্মতি দিলে স্পষ্ট ছবি দেখা যাবে।",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                val existingReq = pendingOrAcceptedByReceiver[profile.userId]
                if (existingReq == null) {
                    Button(
                        onClick = {
                            detailProfile = null
                            requestTargetProfile = profile
                        }
                    ) {
                        Text("আগ্রহ পাঠান")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { detailProfile = null }) { Text("বন্ধ করুন") }
            }
        )
    }

    requestTargetProfile?.let { target ->
        var senderLabel by remember(prefs.localDisplayName) { mutableStateOf(prefs.localDisplayName) }
        var introMsg by remember {
            mutableStateOf("আসসালামু আলাইকুম। আপনার বায়োডাটা দেখে পারিবারিক ও অভিভাবকের মাধ্যমে কথা বলতে আগ্রহী।")
        }
        AlertDialog(
            onDismissRequest = { requestTargetProfile = null },
            title = { Text("আগ্রহ ও প্রস্তাব পাঠান (${target.profileCode})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "অপর পক্ষ প্রস্তাব গ্রহণ করার পরই সরাসরি মেসেজিং চালু হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = senderLabel,
                        onValueChange = { senderLabel = it },
                        label = { Text("আপনার নাম / পরিচয়") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = introMsg,
                        onValueChange = { introMsg = it },
                        label = { Text("প্রাথমিক বার্তা ও অভিভাবকের তথ্য") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendInterestRequest(target, senderLabel, introMsg)
                        requestTargetProfile = null
                    },
                    modifier = Modifier.testTag("confirm_send_interest_button")
                ) {
                    Text("প্রস্তাব পাঠান")
                }
            },
            dismissButton = {
                TextButton(onClick = { requestTargetProfile = null }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun MatrimonialModestyBanner() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column {
            Image(
                painter = painterResource(id = R.drawable.img_matrimonial_banner),
                contentDescription = "ইসলামিক ম্যাট্রিমোনিয়াল ব্যানার",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
            )
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        text = "শালীনতা ও ওয়ালীর তত্ত্বাবধান নীতিমালা",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "ডিফল্টভাবে ছবি ঝাপসা থাকে এবং প্রস্তাব অনুমোদনের আগে মেসেজিং বন্ধ থাকে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrimonialProfileCard(
    profile: MatrimonialProfileEntity,
    isPhotoClear: Boolean,
    existingRequestStatus: String?,
    onViewDetails: () -> Unit,
    onSendInterest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = "প্রোফাইল ছবি",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (!isPhotoClear) Modifier.blur(20.dp) else Modifier)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "অবয়ব আইকন",
                            tint = EmeraldPrimary,
                            modifier = Modifier
                                .size(34.dp)
                                .then(if (!isPhotoClear) Modifier.blur(4.dp) else Modifier)
                        )
                    }
                    if (!isPhotoClear) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VisibilityOff,
                                contentDescription = "ঝাপসা ছবি",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = profile.profileCode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (profile.gender == "PATRI") "পাত্রী" else "পাত্র",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "${profile.age} বছর • ${profile.division} • ${profile.profession}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "শিক্ষা: ${profile.education}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "দ্বীনদারিতা: ${profile.practicingNote} • ওয়ালী: ${profile.waliRelation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("বিস্তারিত বায়োডাটা")
                }

                when (existingRequestStatus) {
                    "PENDING" -> {
                        FilledTonalButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("অপেক্ষমাণ")
                        }
                    }
                    "ACCEPTED" -> {
                        FilledTonalButton(
                            onClick = onViewDetails,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("গৃহীত")
                        }
                    }
                    else -> {
                        Button(
                            onClick = onSendInterest,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("send_interest_${profile.id}")
                        ) {
                            Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("আগ্রহ পাঠান")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrimonialRequestCard(
    request: MatrimonialRequestEntity,
    currentUserId: String,
    onRespond: (accept: Boolean, approvePhoto: Boolean) -> Unit,
    onTogglePhotoReveal: (Boolean) -> Unit,
    onOpenChat: () -> Unit
) {
    val isReceiver = request.receiverId == currentUserId
    val otherName = if (isReceiver) request.senderName else request.receiverName
    val roleLabel = if (isReceiver) "আগত প্রস্তাব" else "প্রেরিত প্রস্তাব"

    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(otherName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(roleLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val statusText = when (request.status) {
                    "ACCEPTED" -> "গৃহীত"
                    "DECLINED" -> "প্রত্যাখ্যাত"
                    else -> "অপেক্ষমাণ"
                }
                Surface(
                    color = when (request.status) {
                        "ACCEPTED" -> EmeraldPrimary.copy(alpha = 0.16f)
                        "DECLINED" -> MaterialTheme.colorScheme.errorContainer
                        else -> GoldAccent.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = request.introMessage,
                style = MaterialTheme.typography.bodyMedium
            )

            if (isReceiver && request.status == "PENDING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onRespond(true, false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("গ্রহণ করুন", maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { onRespond(true, true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("ছবিসহ গ্রহণ", maxLines = 1)
                    }
                }
                OutlinedButton(
                    onClick = { onRespond(false, false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("প্রত্যাখ্যান করুন")
                }
            } else if (request.status == "ACCEPTED") {
                if (isReceiver) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (request.photoRevealApproved) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("স্পষ্ট ছবি দেখার অনুমতি", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = request.photoRevealApproved,
                            onCheckedChange = onTogglePhotoReveal
                        )
                    }
                }
                Button(
                    onClick = onOpenChat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_chat_${request.id}")
                ) {
                    Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("শালীন মেসেজিং (চ্যাট করুন)")
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (request.status == "DECLINED") "প্রস্তাবটি প্রত্যাখ্যাত হয়েছে।"
                        else "অপর পক্ষ গ্রহণ করলে চ্যাট অপশন চালু হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MyMatrimonialProfileEditor(
    currentUserId: String,
    existingProfile: MatrimonialProfileEntity?,
    isLoading: Boolean,
    onSave: (MatrimonialProfileEntity) -> Unit
) {
    if (isLoading && existingProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var gender by remember(existingProfile) { mutableStateOf(existingProfile?.gender ?: "PATRO") }
    var ageText by remember(existingProfile) { mutableStateOf((existingProfile?.age ?: 26L).toString()) }
    var division by remember(existingProfile) { mutableStateOf(existingProfile?.division ?: "ঢাকা") }
    var education by remember(existingProfile) { mutableStateOf(existingProfile?.education ?: "") }
    var profession by remember(existingProfile) { mutableStateOf(existingProfile?.profession ?: "") }
    var practicingNote by remember(existingProfile) { mutableStateOf(existingProfile?.practicingNote ?: "পাঁচ ওয়াক্ত নামাজ ও হালাল উপার্জন") }
    var bioBangla by remember(existingProfile) { mutableStateOf(existingProfile?.bioBangla ?: "") }
    var waliRelation by remember(existingProfile) { mutableStateOf(existingProfile?.waliRelation ?: "বাবা") }
    var photoUrl by remember(existingProfile) { mutableStateOf(existingProfile?.photoUrl) }
    var photoBlurred by remember(existingProfile) { mutableStateOf(existingProfile?.photoBlurred ?: true) }
    var active by remember(existingProfile) { mutableStateOf(existingProfile?.active ?: true) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUrl = uri.toString()
        }
    }

    Column(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (existingProfile != null) "বায়োডাটা আপডেট (${existingProfile.profileCode})"
            else "নতুন ম্যাট্রিমোনিয়াল বায়োডাটা তৈরি করুন",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "নির্বাচিত প্রোফাইল ছবি",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (photoBlurred) Modifier.blur(16.dp) else Modifier)
                    )
                } else {
                    Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(34.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Rounded.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("ফটো পিকার দিয়ে ছবি নির্বাচন")
                }
                Text(
                    text = "ডিফল্টভাবে ছবি ঝাপসা (Blur) থাকবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = gender == "PATRO",
                onClick = { gender = "PATRO" },
                label = { Text("পাত্র") }
            )
            FilterChip(
                selected = gender == "PATRI",
                onClick = { gender = "PATRI" },
                label = { Text("পাত্রী") }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = ageText,
                onValueChange = { ageText = it.filter(Char::isDigit) },
                label = { Text("বয়স (১৮-৮০)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = division,
                onValueChange = { division = it },
                label = { Text("বিভাগ / শহর") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = education,
            onValueChange = { education = it },
            label = { Text("শিক্ষাগত যোগ্যতা") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = profession,
            onValueChange = { profession = it },
            label = { Text("পেশা") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = practicingNote,
            onValueChange = { practicingNote = it },
            label = { Text("দ্বীনদারিতা ও ইবাদতের অভ্যাস") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = waliRelation,
            onValueChange = { waliRelation = it },
            label = { Text("অভিভাবক (ওয়ালী)-এর সম্পর্ক") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bioBangla,
            onValueChange = { bioBangla = it },
            label = { Text("সংক্ষিপ্ত পারিবারিক ও ব্যক্তিগত পরিচিতি") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ছবি ঝাপসা রাখুন (প্রস্তাবিত)", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = photoBlurred, onCheckedChange = { photoBlurred = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("অন্যদের জন্য প্রোফাইল সক্রিয় রাখুন", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = active, onCheckedChange = { active = it })
        }

        Button(
            onClick = {
                val parsedAge = (ageText.toLongOrNull() ?: 25L).coerceIn(18L, 80L)
                onSave(
                    MatrimonialProfileEntity(
                        id = existingProfile?.id ?: currentUserId,
                        userId = existingProfile?.userId ?: currentUserId,
                        profileCode = existingProfile?.profileCode ?: "MB-${currentUserId.take(6).uppercase()}",
                        gender = gender,
                        age = parsedAge,
                        division = division,
                        education = education,
                        profession = profession,
                        practicingNote = practicingNote,
                        bioBangla = bioBangla,
                        waliRelation = waliRelation,
                        photoUrl = photoUrl,
                        photoBlurred = photoBlurred,
                        active = active
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_matrimonial_profile_button")
        ) {
            Text("বায়োডাটা সংরক্ষণ ও প্রকাশ করুন")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrimonialChatScreen(
    requestId: String,
    viewModel: MatrimonialViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    LaunchedEffect(requestId) {
        viewModel.openChat(requestId)
    }

    val requestsState by viewModel.requestsState.collectAsStateWithLifecycle()
    val messagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()

    val request = (requestsState as? UiState.Success)?.data?.firstOrNull { it.id == requestId }
    val partnerName = remember(request, viewModel.currentUserId) {
        if (request == null) "মেসেজিং"
        else if (request.senderId == viewModel.currentUserId) request.receiverName else request.senderName
    }

    var messageText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(partnerName, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (request?.photoRevealApproved == true) "শালীন চ্যাট • ছবি অনুমোদিত"
                        else "শালীন চ্যাট • ওয়ালীর তত্ত্বাবধান",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "পেছনে যান")
                }
            }
        )
        SnackbarHost(hostState = snackbarHostState)

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "ইসলামিক আদব বজায় রেখে পারিবারিক ও বিয়ের প্রাসঙ্গিক বিষয়েই আলোচনা করুন।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = messagesState) {
                UiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is UiState.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text(state.message, color = MaterialTheme.colorScheme.error) }

                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.data.isEmpty()) {
                            item {
                                Text(
                                    text = "সালাম জানিয়ে শালীন আলাপ শুরু করুন।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(state.data, key = { it.id }) { msg ->
                                val isMine = msg.senderId == viewModel.currentUserId
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        color = if (isMine) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("শালীন বার্তা লিখুন...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank() && request?.status == "ACCEPTED") {
                            viewModel.sendMessage(requestId, messageText)
                            messageText = ""
                        }
                    },
                    enabled = request?.status == "ACCEPTED" && messageText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_chat_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "বার্তা পাঠান",
                        tint = EmeraldPrimary
                    )
                }
            }
        }
    }
}
