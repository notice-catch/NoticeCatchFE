package com.umc.catchandroid.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.umc.catchandroid.presentation.component.NotificationBellIcon
import com.umc.catchandroid.presentation.home.NoticeItem
import com.umc.catchandroid.ui.theme.CatchDeadlineSoon
import com.umc.catchandroid.ui.theme.CatchDivider
import com.umc.catchandroid.ui.theme.CatchPrimary
import com.umc.catchandroid.ui.theme.CatchTextBody
import com.umc.catchandroid.ui.theme.CatchTextCaption
import com.umc.catchandroid.ui.theme.CatchTextTitle
import java.time.LocalDate
import java.time.YearMonth

data class CalendarDay(
    val day: Int,
    val isCurrentMonth: Boolean,
    val dateStr: String?
)

private val weekDays = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
private val CalendarGridBorder = Color(0xFFE5E7EB)

@Composable
fun CalendarScreen(
    onNoticeClick: (Long) -> Unit,
    onNotificationClick: () -> Unit = {},
    hasUnreadNotification: Boolean = false,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val yearMonth by viewModel.yearMonth.collectAsState()
    val deadlineDates by viewModel.deadlineDates.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val noticesForDate by viewModel.noticesForDate.collectAsState()
    val upcomingNotices by viewModel.upcomingNotices.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val days = buildCalendarDays(yearMonth)
    val selectedDay = selectedDate.substringAfterLast("-").toIntOrNull() ?: 0
    val todayStr = LocalDate.now().toString()

    // 오늘(D-Day) 마감인 공지인지 판별 - 홈 화면과 동일한 빨간 배지 표시용
    fun isDueToday(notice: com.umc.catchandroid.domain.model.Notice): Boolean {
        val deadline = notice.deadlineAt ?: return false
        return try {
            deadline.substring(0, 10) == todayStr
        } catch (e: Exception) {
            false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 상단 앱바
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "캘린더",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CatchTextTitle,
                modifier = Modifier.align(Alignment.Center)
            )
            NotificationBellIcon(
                hasUnread = hasUnreadNotification,
                onClick = onNotificationClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // 에러 배너
        if (errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(CatchDeadlineSoon.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = errorMessage ?: "",
                    fontSize = 12.sp,
                    color = CatchTextCaption
                )
                Text(
                    text = "다시 시도",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CatchPrimary,
                    modifier = Modifier.clickable { viewModel.retry() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            // 월 이동
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.prevMonth() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달", tint = CatchTextTitle)
                    }
                    Text(
                        text = "${yearMonth.year}년 ${yearMonth.monthValue}월",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CatchTextTitle,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "다음 달", tint = CatchTextTitle)
                    }
                }
            }

            // 요일 헤더
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDays.forEachIndexed { index, day ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = when (index) {
                                    0 -> Color(0xFFE05353)
                                    else -> CatchTextCaption
                                }
                            )
                        }
                    }
                }
            }

            // 날짜 그리드
            item {
                val weeks = days.chunked(7)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, CalendarGridBorder, RoundedCornerShape(16.dp))
                ) {
                    weeks.forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { calDay ->
                                CalendarDayCell(
                                    calDay = calDay,
                                    isSelected = calDay.isCurrentMonth && calDay.dateStr == selectedDate,
                                    hasDeadline = calDay.dateStr != null && deadlineDates.contains(calDay.dateStr),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(0.5.dp, CalendarGridBorder),
                                    onClick = {
                                        calDay.dateStr?.let { viewModel.onDateSelected(it) }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 선택한 날짜 일정
            item {
                val title = if (selectedDate == todayStr) {
                    "오늘 일정 ${noticesForDate.size}개"
                } else {
                    "${yearMonth.monthValue}월 ${selectedDay}일 일정 ${noticesForDate.size}개"
                }
                Text(
                    text = "📅 $title",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CatchTextTitle,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            if (noticesForDate.isEmpty()) {
                item {
                    Text(
                        text = "이 날짜에는 마감 일정이 없어요.",
                        fontSize = 13.sp,
                        color = CatchTextCaption,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }
            } else {
                items(noticesForDate) { notice ->
                    NoticeItem(
                        notice = notice,
                        isDueToday = isDueToday(notice),
                        onClick = { onNoticeClick(notice.noticeId) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // 다가오는 일정
            item {
                Text(
                    text = "🕐 다가오는 일정 ${upcomingNotices.size}개",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CatchTextTitle,
                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
                )
            }
            if (upcomingNotices.isEmpty()) {
                item {
                    Text(
                        text = "다가오는 일정이 없어요.",
                        fontSize = 13.sp,
                        color = CatchTextCaption
                    )
                }
            } else {
                items(upcomingNotices) { notice ->
                    NoticeItem(
                        notice = notice,
                        isDueToday = isDueToday(notice),
                        onClick = { onNoticeClick(notice.noticeId) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun CalendarDayCell(
    calDay: CalendarDay,
    isSelected: Boolean,
    hasDeadline: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .aspectRatio(0.8f)
            .clickable(enabled = calDay.isCurrentMonth) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .then(
                    if (isSelected) Modifier.background(CatchPrimary, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = calDay.day.toString(),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    !calDay.isCurrentMonth -> CatchTextCaption
                    else -> CatchTextTitle
                }
            )
        }
        if (hasDeadline && !isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(4.dp)
                    .background(CatchDeadlineSoon, CircleShape)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

private fun buildCalendarDays(yearMonth: YearMonth): List<CalendarDay> {
    val days = mutableListOf<CalendarDay>()
    val firstDay = yearMonth.atDay(1)
    val startOffset = firstDay.dayOfWeek.value % 7 // 일=0 ~ 토=6
    val prevMonth = yearMonth.minusMonths(1)
    val prevLength = prevMonth.lengthOfMonth()
    for (i in startOffset downTo 1) {
        days.add(CalendarDay(prevLength - i + 1, false, null))
    }
    for (d in 1..yearMonth.lengthOfMonth()) {
        val dateStr = "%04d-%02d-%02d".format(yearMonth.year, yearMonth.monthValue, d)
        days.add(CalendarDay(d, true, dateStr))
    }
    val remaining = (7 - days.size % 7) % 7
    for (d in 1..remaining) {
        days.add(CalendarDay(d, false, null))
    }
    return days
}