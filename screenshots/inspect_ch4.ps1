$docxPath = (Resolve-Path "OOM_Evaluation_Submission/01_Project_Report/Electricity_Billing_Management_System_OOM_Report.docx").Path
$word = New-Object -ComObject Word.Application
$word.Visible = $false
try {
    $doc = $word.Documents.Open($docxPath)
    for ($i = 11; $i -le 17; $i++) {
        $rng = $doc.GoTo(1, 1, $i)
        $rng.Select()
        $text = $word.Selection.Bookmarks("\Page").Range.Text.Trim()
        Write-Host ("=" * 60)
        Write-Host "PAGE $i (Length: $($text.Length))"
        Write-Host ("=" * 60)
        Write-Host $text
    }
    $doc.Close($false)
} finally {
    $word.Quit()
}
