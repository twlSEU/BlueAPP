package com.example.blue.feature.time

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blue.data.repository.TimeRepository
import com.example.blue.feature.common.AppBackButton
import com.example.blue.feature.common.AppDatePickerDialog
import com.example.blue.feature.common.appScaffoldContentWindowInsets
import com.example.blue.ui.theme.BlueTheme
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.launch

private val TraceBackground = Color(0xFFF5F9FC) // 页面背景：接近白色的冰蓝
private val TraceSurface = Color(0xFFFCFEFF)    // 卡片背景：轻微冷白

private val TraceTitle = Color(0xFF294458)      // 标题：深灰蓝，保证清晰度
private val TraceBody = Color(0xFF718797)       // 正文：柔和蓝灰
private val TraceEmpty = Color(0xFFE8F0F6)      // 空状态：极浅冰蓝

private val TraceAccent = Color(0xFF6F97B5)     // 强调色：低饱和中灰蓝
private val TraceBorder = Color(0xFFDCE8F1)     // 边框：淡冰蓝

private val TraceRowColors = listOf(
    Color(0xFFDAE8F5), // 01 极浅冰蓝
    Color(0xFFD0DFEC), // 02
    Color(0xFFC0D3E2), // 03
    Color(0xFFB0C7D9), // 04
    Color(0xFFA1BBCF), // 05
    Color(0xFF91AFC6), // 06
    Color(0xFF81A3BD), // 07
    Color(0xFF7198B4), // 08 深灰蓝
)

@Composable
fun LifeTraceScreen(
    repository: TimeRepository,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    val birthdayFlow = remember(repository) { repository.observeBirthday() }
    val storedBirthday by birthdayFlow.collectAsStateWithLifecycle(initialValue = null)
    val today = remember { LocalDate.now() }
    val scope = rememberCoroutineScope()
    var draftBirthday by remember { mutableStateOf<LocalDate?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(storedBirthday) {
        storedBirthday?.let { draftBirthday = it }
    }
    val displayedBirthday = draftBirthday ?: storedBirthday
    val age = displayedBirthday?.let { lifeAge(it, today) } ?: 0
    val elapsedDays = displayedBirthday?.let { lifeElapsedDays(it, today) } ?: 0L

    LifeTraceContent(
        age = age,
        birthday = displayedBirthday,
        elapsedDays = elapsedDays,
        isSaved = displayedBirthday != null && displayedBirthday == storedBirthday,
        onChooseBirthday = { showPicker = true },
        onSaveBirthday = {
            displayedBirthday?.let { birthday -> scope.launch { repository.saveBirthday(birthday) } }
        },
        onBack = onBack,
        showTopBar = showTopBar,
    )

    if (showPicker) {
        AppDatePickerDialog(
            selectedDate = displayedBirthday ?: today.minusYears(25),
            latestDate = today,
            helperText = "选择你的出生日期",
            onDismiss = { showPicker = false },
            onDateSelected = { date ->
                draftBirthday = date
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LifeTraceContent(
    age: Int,
    birthday: LocalDate?,
    elapsedDays: Long,
    isSaved: Boolean,
    onChooseBirthday: () -> Unit,
    onSaveBirthday: () -> Unit,
    onBack: () -> Unit,
    showTopBar: Boolean = true,
) {
    Scaffold(
        containerColor = TraceBackground,
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = { Text("岁痕", fontWeight = FontWeight.SemiBold, color = TraceTitle) },
                    navigationIcon = { AppBackButton(onClick = onBack) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = TraceBackground,
                        scrolledContainerColor = TraceBackground,
                    ),
                )
            }
        },
        contentWindowInsets = appScaffoldContentWindowInsets(showTopBar),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = TraceSurface,
                    shadowElevation = 7.dp,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = TraceTitle)) {
                                    append("已点亮 ")
                                }
                                withStyle(SpanStyle(color = Color(0xFF4F8FBD))) {
                                    append("${lifeProgressPercent(birthday, elapsedDays)}%")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                            fontWeight = FontWeight.Bold,
                            color = TraceTitle,
                            textAlign = TextAlign.Left,
                        )
                        LifeGrid(age = age)
                    }
                }
            }
            item {
                BirthdayCard(
                    birthday = birthday,
                    elapsedDays = elapsedDays,
                    isSaved = isSaved,
                    onChooseBirthday = onChooseBirthday,
                    onSaveBirthday = onSaveBirthday,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                )
            }
        }
    }
}

@Composable
private fun LifeGrid(age: Int) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = ((maxWidth - 32.dp - 45.dp) / 10f).coerceAtMost(30.dp)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(8) { row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(10) { column ->
                            val index = row * 10 + column
                            val isLit = index < age
                            val isCurrent = age > 0 && index == age - 1
                            val shape = RoundedCornerShape(cellSize * 0.27f)
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(shape)
                                    .background(if (isLit) TraceRowColors[row] else TraceEmpty)
                                    .then(
                                        if (isCurrent) Modifier.border(1.7.dp, Color(0xFFB0B8BE), shape)
                                        else Modifier,
                                    ),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = ((row + 1) * 10).toString(),
                        modifier = Modifier.width(28.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TraceBody.copy(alpha = 0.8f),
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
    }
}

@Composable
private fun BirthdayCard(
    birthday: LocalDate?,
    elapsedDays: Long,
    isSaved: Boolean,
    onChooseBirthday: () -> Unit,
    onSaveBirthday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(32.dp)
    Surface(
        modifier = modifier.shadow(
            elevation = 14.dp,
            shape = cardShape,
            ambientColor = TraceAccent.copy(alpha = 0.10f),
            spotColor = TraceAccent.copy(alpha = 0.14f),
        ),
        shape = cardShape,
        color = TraceSurface,
        border = BorderStroke(1.dp, TraceBorder.copy(alpha = 0.72f)),
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.82f), TraceSurface),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.74f), cardShape),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DecoratedCardLabel(text = "出生日期", color = TraceTitle)
                Surface(
                    onClick = onChooseBirthday,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                    shape = CircleShape,
                    color = Color(0xFFF2F7FA),
                    border = BorderStroke(1.dp, TraceBorder.copy(alpha = 0.82f)),
                    shadowElevation = 1.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.72f), Color.Transparent),
                                ),
                            )
                            .padding(horizontal = 24.dp, vertical = 17.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            birthday?.toString() ?: "点击设置生日",
                            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.6.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = TraceTitle,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (!isSaved && birthday != null) {
                    Button(
                        onClick = onSaveBirthday,
                        modifier = Modifier.padding(top = 12.dp).widthIn(min = 156.dp).height(44.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF476D88),
                            contentColor = Color.White,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 1.dp),
                    ) {
                        Text("保存日期", fontWeight = FontWeight.SemiBold)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp)
                        .height(1.dp)
                        .background(TraceBorder.copy(alpha = 0.46f)),
                )
                DecoratedCardLabel(text = "已过去", color = TraceBody)
                Text(
                    text = if (birthday == null) "—" else elapsedDays.toString(),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        lineHeight = 66.sp,
                        letterSpacing = (-1.4).sp,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF8DCAFA), Color(0xFF1976E9)),
                        ),
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Surface(
                    modifier = Modifier.padding(top = 8.dp),
                    shape = CircleShape,
                    color = Color(0xFF2688EC).copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, Color(0xFF5EAEF5).copy(alpha = 0.16f)),
                ) {
                    Text(
                        "天",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2186EA),
                    )
                }
            }
        }
    }
}

@Composable
private fun DecoratedCardLabel(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(Modifier.width(30.dp).height(1.dp).background(TraceBorder.copy(alpha = 0.78f)))
        Box(Modifier.size(4.dp).clip(CircleShape).background(TraceAccent.copy(alpha = 0.62f)))
        Text(
            text,
            modifier = Modifier.padding(horizontal = 3.dp),
            style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 0.8.sp),
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Box(Modifier.size(4.dp).clip(CircleShape).background(TraceAccent.copy(alpha = 0.62f)))
        Box(Modifier.width(30.dp).height(1.dp).background(TraceBorder.copy(alpha = 0.78f)))
    }
}

private fun formatElapsedDays(days: Long): String =
    NumberFormat.getIntegerInstance(Locale.US).format(days)

internal fun lifeProgressPercent(birthday: LocalDate?, elapsedDays: Long): String {
    if (birthday == null) return "0.0"
    val eightyYearDays = ChronoUnit.DAYS.between(birthday, birthday.plusYears(80)).coerceAtLeast(1)
    val percentage = elapsedDays.coerceIn(0, eightyYearDays).toDouble() / eightyYearDays * 100.0
    return String.format(Locale.ROOT, "%.1f", percentage)
}

internal fun lifeAge(birthday: LocalDate, today: LocalDate = LocalDate.now()): Int =
    Period.between(birthday, today).years.coerceIn(0, 80)

internal fun lifeElapsedDays(birthday: LocalDate, today: LocalDate = LocalDate.now()): Long =
    ChronoUnit.DAYS.between(birthday, today).coerceAtLeast(0)

@Preview(name = "岁痕 · 生日已设置", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LifeTracePreview() {
    BlueTheme(dynamicColor = false) {
        LifeTraceContent(
            age = 27,
            birthday = LocalDate.of(1999, 3, 18),
            elapsedDays = 9_983,
            isSaved = true,
            onChooseBirthday = {},
            onSaveBirthday = {},
            onBack = {},
        )
    }
}
