# ==============================================================================
# GradPath AI – Autonomous Daily Research Sweeper (Windows Scheduled Task)
# ==============================================================================

$logFile = "D:\University Research\tasks\sweep_log.txt"
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

Add-Content -Path $logFile -Value "[$timestamp] Starting autonomous daily sweep..."

try {
    # 1. Trigger sweep on Ktor server
    $response = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/watchdogs/sweep" -Method Post -TimeoutSec 30 -ErrorAction Stop

    $newCount = $response.newOpportunitiesFound
    $totalCount = $response.totalFeedCount
    Add-Content -Path $logFile -Value "[$timestamp] Sweep successful. New: $newCount, Total: $totalCount"

    # 2. Windows Native Notification Toast if new circulars found
    if ($newCount -gt 0) {
        $title = "🎓 GradPath AI: New Research Opportunities Found!"
        $message = "Autonomous sweeper discovered $newCount new funded circulars & deadlines matching your profile."

        # Show Windows Notification using Windows.UI.Notifications
        [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] > $null
        $template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText02)
        $textNodes = $template.GetElementsByTagName("text")
        $textNodes.Item(0).AppendChild($template.CreateTextNode($title)) > $null
        $textNodes.Item(1).AppendChild($template.CreateTextNode($message)) > $null

        $toast = [Windows.UI.Notifications.ToastNotification]::new($template)
        $notifier = [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier("GradPath AI")
        $notifier.Show($toast)
    }
} catch {
    Add-Content -Path $logFile -Value "[$timestamp] Error during sweep: $($_.Exception.Message)"
}
