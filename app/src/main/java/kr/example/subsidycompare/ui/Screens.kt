package kr.example.subsidycompare.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.example.subsidycompare.BuildConfig
import kr.example.subsidycompare.R
import kr.example.subsidycompare.data.Carrier
import kr.example.subsidycompare.data.Device
import kr.example.subsidycompare.data.SubsidyData
import kr.example.subsidycompare.domain.Assumptions
import kr.example.subsidycompare.domain.BetterOption
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(vm: AppViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(s.message) {
        s.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }
    BackHandler(enabled = s.screen != Screen.DeviceList) { vm.back() }

    val title = when (val sc = s.screen) {
        Screen.DeviceList -> stringResource(R.string.app_name)
        Screen.Info -> "정보 및 안내"
        is Screen.DeviceDetail -> s.data?.devices?.firstOrNull { it.id == sc.deviceId }?.name ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    if (s.screen != Screen.DeviceList) {
                        TextButton(onClick = { vm.back() }) { Text("←", fontSize = 22.sp) }
                    }
                },
                actions = {
                    if (s.screen == Screen.DeviceList) {
                        TextButton(onClick = { vm.open(Screen.Info) }) { Text("안내") }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val data = s.data
            if (data == null) {
                Text("데이터를 불러오는 중…", Modifier.align(Alignment.Center))
                return@Box
            }
            when (val sc = s.screen) {
                Screen.DeviceList -> DeviceListScreen(vm, s, data)
                Screen.Info -> InfoScreen(data)
                is Screen.DeviceDetail -> {
                    val device = data.devices.firstOrNull { it.id == sc.deviceId }
                    if (device == null) Text("기종 정보를 찾을 수 없어요", Modifier.padding(16.dp))
                    else DeviceDetailScreen(vm, s, device)
                }
            }
        }
    }
}

// ───────────────────────── 기종 목록 ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceListScreen(vm: AppViewModel, s: UiState, data: SubsidyData) {
    val devices = vm.filteredDevices(s)
    PullToRefreshBox(isRefreshing = s.refreshing, onRefresh = { vm.refresh() }) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { DataHeader(data) }
            item {
                OutlinedTextField(
                    value = s.query,
                    onValueChange = vm::setQuery,
                    label = { Text("기종 검색 (예: 갤럭시, 아이폰)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (devices.isEmpty()) {
                item { Text("검색 결과가 없어요", Modifier.padding(vertical = 24.dp)) }
            }
            items(devices, key = { it.id }) { d -> DeviceCard(vm, s, d) }
            item { Disclaimer() }
        }
    }
}

@Composable
private fun DeviceCard(vm: AppViewModel, s: UiState, d: Device) {
    val max = vm.maxSubsidy(s, d)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { vm.open(Screen.DeviceDetail(d.id)) },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(d.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                listOf(d.maker, d.storage).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("출고가 ${d.releasePrice.won()}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                if (max != null) {
                    CarrierBadge(max.second)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "최대 ${max.first.won()}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

// ───────────────────────── 기종 상세 (3사 비교) ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceDetailScreen(vm: AppViewModel, s: UiState, device: Device) {
    val rows = vm.planRows(s, device)
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text("출고가 ${device.releasePrice.won()}", style = MaterialTheme.typography.titleMedium)
            Text(
                "${Assumptions.CONTRACT_MONTHS}개월 실부담 총액이 낮은 순으로 정렬했어요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = s.carrierFilter == null,
                    onClick = { vm.setCarrierFilter(null) },
                    label = { Text("전체") },
                )
                Carrier.entries.forEach { c ->
                    FilterChip(
                        selected = s.carrierFilter == c,
                        onClick = { vm.setCarrierFilter(c) },
                        label = { Text(c.label) },
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = if (s.extraSupport == 0) "" else s.extraSupport.toString(),
                onValueChange = { txt -> vm.setExtraSupport(txt.filter(Char::isDigit).take(7).toIntOrNull() ?: 0) },
                label = { Text("매장 추가지원금 (선택 입력, 원)") },
                supportingText = { Text("매장에서 견적 받은 금액이 있으면 넣어보세요") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (rows.isEmpty()) {
            item { Text("해당 조건의 지원금 정보가 없어요", Modifier.padding(vertical = 24.dp)) }
        }
        items(rows, key = { it.plan.id }) { row -> PlanCard(row) }
        item { Disclaimer() }
    }
}

@Composable
private fun PlanCard(row: PlanRow) {
    val c = row.comparison
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CarrierBadge(row.carrier)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.plan.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        "월 ${row.plan.monthlyFee.won()}" +
                            if (row.plan.dataDesc.isNotBlank()) " · ${row.plan.dataDesc}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "공시(공통)지원금 ${row.subsidyAmount.won()}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Row {
                OptionColumn(
                    title = "지원금 받기",
                    monthly = c.subsidy.totalMonthly,
                    total = c.subsidyTotal,
                    highlight = c.better == BetterOption.SUBSIDY,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                OptionColumn(
                    title = "선택약정 25%",
                    monthly = c.selectDiscount.totalMonthly,
                    total = c.selectTotal,
                    highlight = c.better == BetterOption.SELECT_DISCOUNT,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            val verdict = when (c.better) {
                BetterOption.SUBSIDY -> "지원금 받기가 ${Assumptions.CONTRACT_MONTHS}개월 기준 ${c.saving.won()} 유리"
                BetterOption.SELECT_DISCOUNT -> "선택약정이 ${Assumptions.CONTRACT_MONTHS}개월 기준 ${c.saving.won()} 유리"
                BetterOption.SAME -> "두 방식의 총액이 같아요"
            }
            Text(verdict, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun OptionColumn(title: String, monthly: Long, total: Long, highlight: Boolean, modifier: Modifier) {
    val bg = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(10.dp)
    ) {
        Text(title + if (highlight) "  ✓" else "", style = MaterialTheme.typography.labelMedium)
        Text("월 ${monthly.won()}", fontWeight = FontWeight.Bold)
        Text("총 ${total.won()}", style = MaterialTheme.typography.bodySmall)
    }
}

// ───────────────────────── 정보 화면 ─────────────────────────

@Composable
private fun InfoScreen(data: SubsidyData) {
    val uri = LocalUriHandler.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DataHeader(data)
        Section("계산 방식") {
            Text("• 계약 기간 ${Assumptions.CONTRACT_MONTHS}개월 기준 (단말 할부금 + 월 요금) 총액을 비교합니다.")
            Text("• 단말 할부수수료는 연 ${Assumptions.INSTALLMENT_ANNUAL_RATE_PCT}%, 원리금 균등 방식으로 가정합니다.")
            Text("• 선택약정은 월 요금의 ${(Assumptions.SELECT_DISCOUNT_RATE * 100).toInt()}% 할인으로 계산합니다.")
            Text("• 매장 추가지원금을 입력하면 두 방식 모두 단말 가격에서 차감합니다.")
            Text("• 요금제 의무 유지기간, 부가서비스, 결합·카드 할인, 위약금은 반영하지 않습니다.")
        }
        Section("유의사항") {
            Text(
                "이 앱은 통신사와 관계없는 개인이 만든 참고용 정보 서비스입니다. " +
                    "지원금은 수시로 바뀌며, 실제 구매 조건은 판매처에서 반드시 확인하세요. " +
                    "표시된 정보와 실제 조건이 달라 생긴 손해에 대해 책임지지 않습니다."
            )
        }
        Section("개인정보") {
            Text("이 앱은 이름·연락처·위치 등 개인정보를 수집하지 않습니다.")
            TextButton(onClick = { runCatching { uri.openUri(BuildConfig.PRIVACY_URL) } }) {
                Text("개인정보처리방침 보기")
            }
        }
        if (data.sourceUrl.isNotBlank()) {
            TextButton(onClick = { runCatching { uri.openUri(data.sourceUrl) } }) { Text("데이터 출처 원문 보기") }
        }
        Text(
            "버전 ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        content()
    }
}

// ───────────────────────── 공통 ─────────────────────────

@Composable
private fun DataHeader(data: SubsidyData) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (data.isSample) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3BF)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "⚠️ 샘플 데이터입니다. 화면 확인용 예시이며 실제 지원금과 다릅니다.",
                    color = Color(0xFF5C3D00),
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        Text(
            "기준 ${formatUpdatedAt(data.updatedAt)} · 출처 ${data.source}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CarrierBadge(carrier: Carrier?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(carrier.chipColor()))
        Spacer(Modifier.width(4.dp))
        Text(carrier?.label ?: "?", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Disclaimer() {
    Text(
        "참고용 정보입니다. 지원금은 수시로 바뀌며 실제 조건은 판매처에서 확인하세요. " +
            "이 앱은 통신사와 관계없는 개인이 운영합니다.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}
