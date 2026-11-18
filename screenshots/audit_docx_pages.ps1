$docxPath = (Resolve-Path "OOM_Evaluation_Submission/01_Project_Report/Electricity_Billing_Management_System_OOM_Report.docx").Path

$word = New-Object -ComObject Word.Application
$word.Visible = $false
try {
    $doc = $word.Documents.Open($docxPath)
    $doc.Fields.Update()
    
    $totalPages = $doc.ComputeStatistics(2) # wdStatisticPages = 2
    Write-Host "Total Pages: $totalPages"
    Write-Host ("{0,-6} | {1,-6} | {2,-6} | {3,-40} | {4,-40}" -f "Page", "Chars", "Words", "First 40 chars", "Last 40 chars")
    Write-Host ("-" * 115)
    
    $flagged = @()
    for ($i = 1; $i -le $totalPages; $i++) {
        $rng = $doc.GoTo(1, 1, $i)
        $rng.Select()
        $text = $word.Selection.Bookmarks("\Page").Range.Text.Trim()
        $charCount = $text.Length
        $wordCount = ($text -split '\s+').Count
        
        $cleanText = $text -replace "`r", " " -replace "`n", " "
        $first40 = if ($cleanText.Length -gt 40) { $cleanText.Substring(0, 40) } else { $cleanText }
        $last40 = if ($cleanText.Length -gt 40) { $cleanText.Substring($cleanText.Length - 40) } else { $cleanText }
        
        $flag = ""
        if ($charCount -lt 250) {
            $flag = " *** POTENTIAL STRANDED / SHORT PAGE ***"
            $flagged += [PSCustomObject]@{ Page = $i; Chars = $charCount; First = $first40; Last = $last40 }
        }
        
        Write-Host ("{0,-6} | {1,-6} | {2,-6} | {3,-40} | {4,-40}{5}" -f $i, $charCount, $wordCount, $first40, $last40, $flag)
    }
    
    $doc.Close($false)
    Write-Host ("`n" + ("=" * 80))
    if ($flagged.Count -gt 0) {
        Write-Host "WARNING: Found $($flagged.Count) flagged short pages:"
        foreach ($f in $flagged) {
            Write-Host "  Page $($f.Page): $($f.Chars) chars -> First: '$($f.First)' | Last: '$($f.Last)'"
        }
    } else {
        Write-Host "SUCCESS: Zero stranded / short pages found! All pages are well-filled."
    }
    Write-Host ("=" * 80)
} finally {
    $word.Quit()
}
