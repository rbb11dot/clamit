package com.clamit.ui.schedule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// Map string icon names to Material Icons
object ScheduleIcons {
    val all: List<String> = listOf(
        "alarm", "book", "briefcase", "build", "coffee", "directions_run",
        "edit", "emoji_events", "fastfood", "fitness_center", "home",
        "local_dining", "menu_book", "nightlight", "palette", "pets",
        "school", "self_improvement", "shopping_cart", "spa", "sports_esports",
        "star", "sunny", "timer", "train", "work", "shower", "bedtime",
        "cleaning_services", "headphones"
    )

    private val iconMap: Map<String, ImageVector> = mapOf(
        "alarm" to Icons.Default.Alarm,
        "arrow_back" to Icons.AutoMirrored.Filled.ArrowBack,
        "arrow_forward" to Icons.AutoMirrored.Filled.ArrowForward,
        "book" to Icons.Default.Book,
        "briefcase" to Icons.Default.Work,
        "build" to Icons.Default.Build,
        "coffee" to Icons.Default.Coffee,
        "create" to Icons.Default.Create,
        "delete" to Icons.Default.Delete,
        "directions_run" to Icons.AutoMirrored.Filled.DirectionsRun,
        "edit" to Icons.Default.Edit,
        "email" to Icons.Default.Email,
        "emoji_events" to Icons.Default.EmojiEvents,
        "fastfood" to Icons.Default.Fastfood,
        "fitness_center" to Icons.Default.FitnessCenter,
        "home" to Icons.Default.Home,
        "info" to Icons.Default.Info,
        "list" to Icons.AutoMirrored.Filled.List,
        "local_dining" to Icons.Default.LocalDining,
        "menu" to Icons.Default.Menu,
        "menu_book" to Icons.AutoMirrored.Filled.MenuBook,
        "more_vert" to Icons.Default.MoreVert,
        "nightlight" to Icons.Default.Nightlight,
        "notifications" to Icons.Default.Notifications,
        "palette" to Icons.Default.Palette,
        "pets" to Icons.Default.Pets,
        "refresh" to Icons.Default.Refresh,
        "school" to Icons.Default.School,
        "search" to Icons.Default.Search,
        "self_improvement" to Icons.Default.SelfImprovement,
        "settings" to Icons.Default.Settings,
        "shopping_cart" to Icons.Default.ShoppingCart,
        "spa" to Icons.Default.Spa,
        "sports_esports" to Icons.Default.SportsEsports,
        "star" to Icons.Default.Star,
        "sunny" to Icons.Default.WbSunny,
        "timer" to Icons.Default.Timer,
        "today" to Icons.Default.Today,
        "train" to Icons.Default.Train,
        "work" to Icons.Default.Work,
        "shower" to Icons.Default.Shower,
        "bedtime" to Icons.Default.Bedtime,
        "cleaning_services" to Icons.Default.CleaningServices,
        "headphones" to Icons.Default.Headphones,
        "account_circle" to Icons.Default.AccountCircle,
        "add" to Icons.Default.Add,
        "add_circle" to Icons.Default.AddCircle,
        "check" to Icons.Default.Check,
        "close" to Icons.Default.Close,
        "done_all" to Icons.Default.DoneAll,
        "drag_handle" to Icons.Default.DragHandle,
        "event" to Icons.Default.Event,
        "format_list_bulleted" to Icons.AutoMirrored.Filled.FormatListBulleted,
        "keyboard_arrow_down" to Icons.Default.KeyboardArrowDown,
        "keyboard_arrow_up" to Icons.Default.KeyboardArrowUp,
        "play_arrow" to Icons.Default.PlayArrow,
        "schedule" to Icons.Default.Schedule,
        "stop" to Icons.Default.Stop,
        "weekend" to Icons.Default.Weekend,
    )

    fun getIcon(name: String): ImageVector? = iconMap[name]
    fun getIconOrDefault(name: String): ImageVector = iconMap[name] ?: Icons.Default.Star
}
