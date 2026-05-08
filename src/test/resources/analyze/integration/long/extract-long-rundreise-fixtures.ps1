$ErrorActionPreference = 'Stop'

$base = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspace = Resolve-Path (Join-Path $base '..\..\..\..\..')

$cases = @(
    @{
        Source = Join-Path $workspace 'main\results\rundreise_qwen3_8b.txt'
        Target = Join-Path $base 'qwen3-8b-answers.txt'
        StartMarker = 'Hier ist eine **Rundreise'
        NextPattern = 'Hier ist eine \*\*Rundreise'
    },
    @{
        Source = Join-Path $workspace 'main\results\rundreise_sonnet45.txt'
        Target = Join-Path $base 'sonnet45-answers.txt'
        StartMarker = '# Rundreise'
        NextPattern = '# Rundreise'
    }
)

foreach ($case in $cases) {
    $raw = Get-Content -Raw -Encoding UTF8 $case.Source
    $start = $raw.IndexOf($case.StartMarker)
    if ($start -lt 0) {
        throw "Start marker not found in $($case.Source): $($case.StartMarker)"
    }

    $body = $raw.Substring($start)
    $body = [regex]::Replace($body, '\r?\nProcess finished with exit code 0\s*$', '')
    $parts = [regex]::Split($body, ",\r?\n(?=$($case.NextPattern))")

    $answers = foreach ($part in $parts) {
        $value = $part.Trim()
        if ($value.EndsWith(',')) {
            $value = $value.Substring(0, $value.Length - 1).Trim()
        }
        if ($value) {
            $value
        }
    }

    $separator = "`n`n===== ANSWER =====`n`n"
    [System.IO.File]::WriteAllText($case.Target, ($answers -join $separator), [System.Text.UTF8Encoding]::new($false))
    Write-Host "written $($case.Target) with $($answers.Count) answers"
}
