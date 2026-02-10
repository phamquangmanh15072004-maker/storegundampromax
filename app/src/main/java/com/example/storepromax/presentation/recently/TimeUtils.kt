import java.util.concurrent.TimeUnit

fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Vừa xong"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} phút trước"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} giờ trước"
        diff < TimeUnit.DAYS.toMillis(2) -> "Hôm qua"
        else -> "${TimeUnit.MILLISECONDS.toDays(diff)} ngày trước"
    }
}
fun getDateHeader(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < TimeUnit.DAYS.toMillis(1) -> "Hôm nay"
        diff < TimeUnit.DAYS.toMillis(2) -> "Hôm qua"
        else -> "Cũ hơn"
    }
}