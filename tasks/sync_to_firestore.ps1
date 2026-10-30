# ==============================================================================
# GradPath AI – 4-Pillar Silent Firestore REST Synchronizer
# Automatically invoked by Antigravity AI after scheduled web research
# ==============================================================================

param (
    [string]$Collection = "feed",
    [string]$FilePath = "D:\University Research\db\feed.json"
)

$configFile = "D:\University Research\db\firebase_config.json"
$logFile = "D:\University Research\tasks\sync_log.txt"
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

if (-not (Test-Path $configFile)) {
    Add-Content -Path $logFile -Value "[$timestamp] Skip: firebase_config.json not found."
    exit 0
}

if (-not (Test-Path $FilePath)) {
    Add-Content -Path $logFile -Value "[$timestamp] Error: Data file $FilePath does not exist."
    exit 1
}

try {
    $config = Get-Content -Path $configFile -Raw -Encoding UTF8 | ConvertFrom-Json
    $projectId = $config.projectId
    $apiKey = $config.apiKey

    if ([string]::IsNullOrWhiteSpace($projectId) -or [string]::IsNullOrWhiteSpace($apiKey) -or $projectId -like "*your-firebase*") {
        Add-Content -Path $logFile -Value "[$timestamp] Skip: Unconfigured credentials."
        exit 0
    }

    $rawJson = Get-Content -Path $FilePath -Raw -Encoding UTF8
    $items = $rawJson | ConvertFrom-Json

    $uploadCount = 0
    foreach ($item in $items) {
        $docId = $item.id
        if ([string]::IsNullOrWhiteSpace($docId)) { continue }

        $url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$Collection/$docId`?key=$apiKey"
        
        $itemJsonString = $item | ConvertTo-Json -Compress -Depth 10
        $firestorePayload = @{
            fields = @{
                documentId = @{ stringValue = [string]$docId }
                collection = @{ stringValue = [string]$Collection }
                pillar = @{ stringValue = $(if ($item.pillar) { [string]$item.pillar } else { "TOP_200_ADMISSION" }) }
                title = @{ stringValue = $(if ($item.title) { [string]$item.title } else { "" }) }
                university = @{ stringValue = $(if ($item.university) { [string]$item.university } else { "" }) }
                country = @{ stringValue = $(if ($item.country) { [string]$item.country } else { "" }) }
                imageUrl = @{ stringValue = $(if ($item.imageUrl) { [string]$item.imageUrl } else { "" }) }
                officialUrl = @{ stringValue = $(if ($item.officialUrl) { [string]$item.officialUrl } else { "" }) }
                payloadJson = @{ stringValue = [string]$itemJsonString }
                syncedAt = @{ stringValue = (Get-Date).ToString("o") }
            }
        } | ConvertTo-Json -Depth 5

        $utf8Bytes = [System.Text.Encoding]::UTF8.GetBytes($firestorePayload)

        $response = Invoke-RestMethod -Uri $url -Method Patch -Body $utf8Bytes -ContentType "application/json; charset=utf-8" -TimeoutSec 15
        $uploadCount++
    }

    Add-Content -Path $logFile -Value "[$timestamp] Success: Synchronized $uploadCount 4-pillar items to Firestore collection '$Collection'."
} catch {
    Add-Content -Path $logFile -Value "[$timestamp] Sync Error: $($_.Exception.Message)"
}
