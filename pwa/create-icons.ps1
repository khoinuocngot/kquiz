Add-Type -AssemblyName System.Drawing

function New-KquizIcon {
    param([int]$Size, [string]$Path)

    $bitmap = New-Object System.Drawing.Bitmap($Size, $Size)
    $g = [System.Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAlias

    # Background rounded rect
    $bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(13, 115, 119))
    $gpath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $r = [int]($Size * 0.18)
    $gpath.AddArc(0, 0, $r, $r, 180, 90)
    $gpath.AddArc($Size - $r, 0, $r, $r, 270, 90)
    $gpath.AddArc($Size - $r, $Size - $r, $r, $r, 0, 90)
    $gpath.AddArc(0, $Size - $r, $r, $r, 90, 90)
    $gpath.CloseFigure()
    $g.FillPath($bgBrush, $gpath)

    # Letter K
    $fontSize = [int]($Size * 0.55)
    $font = New-Object System.Drawing.Font("Arial", $fontSize, [System.Drawing.FontStyle]::Bold)
    $textBrush = [System.Drawing.Brushes]::White
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $rect = New-Object System.Drawing.RectangleF(0, 0, $Size, $Size)
    $g.DrawString("K", $font, $textBrush, $rect, $sf)

    # Accent dot
    $accentBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 107, 107))
    $dotSize = [int]($Size * 0.2)
    $dotX = [int]($Size * 0.68)
    $dotY = [int]($Size * 0.12)
    $g.FillEllipse($accentBrush, $dotX, $dotY, $dotSize, $dotSize)

    $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)

    $g.Dispose()
    $bitmap.Dispose()
    $bgBrush.Dispose()
    $accentBrush.Dispose()
    $font.Dispose()
    $sf.Dispose()
    $gpath.Dispose()
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
New-KquizIcon -Size 192 -Path "$scriptDir\icon-192.png"
New-KquizIcon -Size 512 -Path "$scriptDir\icon-512.png"
Write-Host "Icons created!"
