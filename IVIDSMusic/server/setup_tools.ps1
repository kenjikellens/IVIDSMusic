$ServerDir = Get-Location
$BinDir = Join-Path $ServerDir "bin"

# Ensure bin directory exists
if (-not (Test-Path $BinDir)) {
    New-Item -ItemType Directory -Path $BinDir
}

# Download yt-dlp
Write-Host "Downloading yt-dlp.exe..."
$YtdlpUrl = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe"
$YtdlpPath = Join-Path $BinDir "yt-dlp.exe"
Invoke-WebRequest -Uri $YtdlpUrl -OutFile $YtdlpPath

# Download ffmpeg (portable)
# Note: ffmpeg is harder to download directly as a simple .exe. 
# We'll use a known zip archive and extract it.
Write-Host "Downloading ffmpeg..."
$FfmpegUrl = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip"
$ZipPath = Join-Path $ServerDir "ffmpeg.zip"
Invoke-WebRequest -Uri $FfmpegUrl -OutFile $ZipPath

Write-Host "Extracting ffmpeg..."
Expand-Archive -Path $ZipPath -DestinationPath $ServerDir -Force

# Locate the ffmpeg.exe and move it to bin
$ExtractedDir = Get-ChildItem -Path $ServerDir -Filter "ffmpeg-master*" -Directory | Select-Object -First 1
if ($ExtractedDir) {
    Copy-Item (Join-Path $ExtractedDir.FullName "bin\ffmpeg.exe") $BinDir
    Copy-Item (Join-Path $ExtractedDir.FullName "bin\ffprobe.exe") $BinDir
}

# Cleanup
Remove-Item $ZipPath
Remove-Item -Recurse $ExtractedDir.FullName

Write-Host "Setup Complete. yt-dlp and ffmpeg are in $BinDir"
