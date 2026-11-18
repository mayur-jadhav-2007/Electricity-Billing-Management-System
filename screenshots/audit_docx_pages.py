import os
import win32com.client

def audit_document(docx_path):
    abs_path = os.path.abspath(docx_path)
    if not os.path.exists(abs_path):
        print(f"Error: {abs_path} does not exist.")
        return

    word = win32com.client.Dispatch("Word.Application")
    word.Visible = False
    try:
        doc = word.Documents.Open(abs_path)
        doc.Fields.Update()
        
        # wdStatisticPages = 2
        total_pages = doc.ComputeStatistics(2)
        print(f"Total Pages: {total_pages}\n")
        print(f"{'Page':<6} | {'Chars':<6} | {'Words':<6} | {'First 40 chars':<42} | {'Last 40 chars':<42}")
        print("-" * 110)
        
        flagged_pages = []
        for i in range(1, total_pages + 1):
            # wdGoToPage = 1, wdGoToAbsolute = 1
            rng = doc.GoTo(1, 1, i)
            rng.Select()
            page_text = word.Selection.Bookmarks(r"\Page").Range.Text.strip()
            char_count = len(page_text)
            word_count = len(page_text.split())
            first_40 = repr(page_text[:40].replace('\r', ' ').replace('\n', ' '))
            last_40 = repr(page_text[-40:].replace('\r', ' ').replace('\n', ' '))
            
            flag = ""
            if char_count < 250:
                flag = " *** POTENTIAL STRANDED / SHORT PAGE ***"
                flagged_pages.append((i, char_count, first_40, last_40))
                
            print(f"{i:<6} | {char_count:<6} | {word_count:<6} | {first_40:<42} | {last_40:<42}{flag}")
            
        doc.Close(False)
        print("\n" + "=" * 80)
        if flagged_pages:
            print(f"WARNING: Found {len(flagged_pages)} flagged short pages:")
            for p, c, f, l in flagged_pages:
                print(f"  Page {p}: {c} chars -> First: {f} | Last: {l}")
        else:
            print("SUCCESS: Zero stranded / short pages found! All pages are well-filled.")
        print("=" * 80)
        
    finally:
        word.Quit()

if __name__ == "__main__":
    audit_document("OOM_Evaluation_Submission/01_Project_Report/Electricity_Billing_Management_System_OOM_Report.docx")
