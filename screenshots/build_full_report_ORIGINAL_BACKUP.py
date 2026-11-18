import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

from report_helpers import (
    set_cell_shading, set_cell_margins, set_table_borders,
    add_captioned_image, add_code_snippet
)

def create_full_report():
    doc = docx.Document()
    
    # Page setup: A4, 1 inch margins
    for section in doc.sections:
        section.page_width = Inches(8.27)
        section.page_height = Inches(11.69)
        section.top_margin = Inches(0.9)
        section.bottom_margin = Inches(0.9)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)
        
        # Header and Footer
        footer = section.footer
        p_ft = footer.paragraphs[0]
        p_ft.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        p_ft.paragraph_format.space_before = Pt(0)
        p_ft.paragraph_format.space_after = Pt(0)
        r_ft = p_ft.add_run("Electricity Billing Management System  |  IIIT Allahabad")
        r_ft.font.name = "Times New Roman"
        r_ft.font.size = Pt(8.5)
        r_ft.font.color.rgb = RGBColor(100, 116, 139)

    # Base Styles Setup
    style_normal = doc.styles['Normal']
    style_normal.font.name = 'Times New Roman'
    style_normal.font.size = Pt(11)
    style_normal.font.color.rgb = RGBColor(15, 23, 42)
    style_normal.paragraph_format.line_spacing = 1.22
    style_normal.paragraph_format.space_after = Pt(3)
    style_normal.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY

    def add_title(text):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_before = Pt(6)
        p.paragraph_format.space_after = Pt(6)
        run = p.add_run(text)
        run.font.name = 'Times New Roman'
        run.font.size = Pt(15)
        run.font.bold = True
        run.font.color.rgb = RGBColor(30, 58, 138)
        return p

    def add_ch_heading(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(10)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = 'Times New Roman'
        run.font.size = Pt(14)
        run.font.bold = True
        run.font.color.rgb = RGBColor(30, 58, 138)
        return p

    def add_sec_heading(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(8)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = 'Times New Roman'
        run.font.size = Pt(12.5)
        run.font.bold = True
        run.font.color.rgb = RGBColor(15, 23, 42)
        return p

    def add_subsec_heading(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(6)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = 'Times New Roman'
        run.font.size = Pt(11.5)
        run.font.bold = True
        run.font.color.rgb = RGBColor(30, 41, 59)
        return p

    def add_body(text):
        p = doc.add_paragraph(text)
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        p.paragraph_format.line_spacing = 1.18
        p.paragraph_format.space_after = Pt(2)
        return p

    def add_bullet(text):
        p = doc.add_paragraph(style='List Bullet')
        p.paragraph_format.line_spacing = 1.18
        p.paragraph_format.space_after = Pt(1.5)
        run = p.add_run(text)
        run.font.name = 'Times New Roman'
        run.font.size = Pt(11)
        return p

    # =========================================================================
    # PRELIMINARY PAGES (Pages 1 to 5)
    # =========================================================================

    # 1. COVER PAGE
    p_cov_top = doc.add_paragraph()
    p_cov_top.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cov_top.paragraph_format.space_before = Pt(24)
    p_cov_top.paragraph_format.space_after = Pt(4)
    r = p_cov_top.add_run("INDIAN INSTITUTE OF INFORMATION TECHNOLOGY, ALLAHABAD")
    r.font.name = "Times New Roman"
    r.font.size = Pt(13.5)
    r.font.bold = True
    r.font.color.rgb = RGBColor(30, 58, 138)

    p_cov_dept = doc.add_paragraph()
    p_cov_dept.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cov_dept.paragraph_format.space_after = Pt(24)
    r = p_cov_dept.add_run("DEPARTMENT OF INFORMATION TECHNOLOGY\nDEVGHAT, JHALWA, PRAYAGRAJ – 211015, UTTAR PRADESH, INDIA")
    r.font.name = "Times New Roman"
    r.font.size = Pt(10.5)
    r.font.bold = True
    r.font.color.rgb = RGBColor(71, 85, 105)

    p_cov_title = doc.add_paragraph()
    p_cov_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cov_title.paragraph_format.space_before = Pt(20)
    p_cov_title.paragraph_format.space_after = Pt(10)
    r = p_cov_title.add_run("ELECTRICITY BILLING MANAGEMENT SYSTEM")
    r.font.name = "Times New Roman"
    r.font.size = Pt(20)
    r.font.bold = True
    r.font.color.rgb = RGBColor(15, 23, 42)

    p_cov_sub = doc.add_paragraph()
    p_cov_sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cov_sub.paragraph_format.space_after = Pt(28)
    r = p_cov_sub.add_run("A Project Report Submitted in Partial Fulfillment of the Requirements for the Course\nOBJECT-ORIENTED METHODOLOGY (OOM)")
    r.font.name = "Times New Roman"
    r.font.size = Pt(12)
    r.font.bold = True
    r.font.color.rgb = RGBColor(2, 132, 199)

    p_cov_degree = doc.add_paragraph()
    p_cov_degree.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cov_degree.paragraph_format.space_after = Pt(36)
    r = p_cov_degree.add_run("Bachelor of Technology (B.Tech) in Information Technology\nSemester IV  |  Section B  |  Academic Year: 2026–27")
    r.font.name = "Times New Roman"
    r.font.size = Pt(11.5)
    r.font.bold = True
    r.font.color.rgb = RGBColor(15, 23, 42)

    cov_tbl = doc.add_table(rows=1, cols=2)
    cov_tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    cov_tbl.autofit = False
    cov_tbl.columns[0].width = Inches(3.1)
    cov_tbl.columns[1].width = Inches(3.1)

    c_left = cov_tbl.cell(0, 0)
    p_sub_by = c_left.paragraphs[0]
    p_sub_by.paragraph_format.line_spacing = 1.25
    r = p_sub_by.add_run("SUBMITTED BY:\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(11)
    r.font.bold = True
    r.font.color.rgb = RGBColor(30, 58, 138)
    r2 = p_sub_by.add_run(
        "1. Mayur Jadhav (IIT2025202)\n"
        "2. Venkatesh Sarkate (IIT2025176)\n"
        "3. Pawan Yadav (IIT2025174)\n"
        "B.Tech (IT), Section B"
    )
    r2.font.name = "Times New Roman"
    r2.font.size = Pt(10.5)

    c_right = cov_tbl.cell(0, 1)
    p_guide = c_right.paragraphs[0]
    p_guide.paragraph_format.line_spacing = 1.25
    p_guide.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    r = p_guide.add_run("UNDER THE GUIDANCE OF:\n")
    r.font.name = "Times New Roman"
    r.font.size = Pt(11)
    r.font.bold = True
    r.font.color.rgb = RGBColor(30, 58, 138)
    r2 = p_guide.add_run(
        "Dr. KM Pooja\n"
        "Assistant Professor\n"
        "Department of Information Technology\n"
        "IIIT Allahabad"
    )
    r2.font.name = "Times New Roman"
    r2.font.size = Pt(10.5)

    doc.add_page_break()

    # 2. CERTIFICATE PAGE
    add_title("CERTIFICATE")
    add_body(
        "This is to certify that the project report entitled \"ELECTRICITY BILLING MANAGEMENT SYSTEM\" "
        "is a bona fide record of the work carried out by Mayur Jadhav (Enrollment No. IIT2025202), "
        "Venkatesh Sarkate (Enrollment No. IIT2025176), and Pawan Yadav (Enrollment No. IIT2025174), "
        "students of Bachelor of Technology in Information Technology, Section B, at the Indian Institute "
        "of Information Technology, Allahabad (IIITA), in partial fulfillment of the requirements for the "
        "evaluation of the course Object-Oriented Methodology (OOM) during the academic year 2026–27."
    )
    add_body(
        "The project work has been conducted under my direct supervision and guidance. To the best of my "
        "knowledge, the matter embodied in this report has not been submitted to any other university or "
        "institute for the award of any degree or diploma."
    )
    
    doc.add_paragraph().paragraph_format.space_before = Pt(28)
    sig_tbl = doc.add_table(rows=2, cols=2)
    sig_tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    sig_tbl.columns[0].width = Inches(3.1)
    sig_tbl.columns[1].width = Inches(3.1)
    
    c1 = sig_tbl.cell(0, 0)
    p = c1.paragraphs[0]
    p.add_run("______________________________\nDr. KM Pooja\nFaculty Guide & Supervisor\nDepartment of IT, IIIT Allahabad").font.size = Pt(10.5)
    
    c2 = sig_tbl.cell(0, 1)
    p = c2.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    p.add_run("______________________________\nHead of Department\nDepartment of Information Technology\nIIIT Allahabad").font.size = Pt(10.5)
    
    c3 = sig_tbl.cell(1, 0)
    p = c3.paragraphs[0]
    p.paragraph_format.space_before = Pt(28)
    p.add_run("Date: ________________________\nPlace: Prayagraj, India").font.size = Pt(10.5)
    
    c4 = sig_tbl.cell(1, 1)
    p = c4.paragraphs[0]
    p.paragraph_format.space_before = Pt(28)
    p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    p.add_run("______________________________\nExternal Examiner\n(Seal & Signature)").font.size = Pt(10.5)

    doc.add_page_break()

    # 3. DECLARATION PAGE
    add_title("DECLARATION")
    add_body(
        "We hereby declare that this project report entitled \"ELECTRICITY BILLING MANAGEMENT SYSTEM\" "
        "submitted by us to the Department of Information Technology, Indian Institute of Information Technology, "
        "Allahabad, is an authentic record of our own original work completed under the guidance of Dr. KM Pooja, "
        "Faculty Guide, Department of IT, IIIT Allahabad."
    )
    add_body(
        "We confirm that the application code, object-oriented design models, database relational schemas, "
        "UML diagrams, and testing results described in this document accurately represent the actual desktop "
        "software implemented by us in Core Java using Swing/AWT and MySQL database connectivity. We have "
        "not plagiarized or incorporated uncredited material from any prior work."
    )

    doc.add_paragraph().paragraph_format.space_before = Pt(30)
    dec_tbl = doc.add_table(rows=1, cols=3)
    dec_tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    dec_tbl.columns[0].width = Inches(2.1)
    dec_tbl.columns[1].width = Inches(2.1)
    dec_tbl.columns[2].width = Inches(2.1)

    members_dec = [
        ("Mayur Jadhav", "IIT2025202"),
        ("Venkatesh Sarkate", "IIT2025176"),
        ("Pawan Yadav", "IIT2025174")
    ]
    for i, (mname, menr) in enumerate(members_dec):
        cell = dec_tbl.cell(0, i)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.add_run(f"___________________\n{mname}\nEnrollment: {menr}\nB.Tech IT, Section B").font.size = Pt(10)

    p_date = doc.add_paragraph()
    p_date.paragraph_format.space_before = Pt(24)
    p_date.add_run("Date: ________________________\nPlace: Prayagraj, India").font.size = Pt(10.5)

    doc.add_page_break()

    # 4. ACKNOWLEDGEMENT & ABSTRACT (Combined on Page 4 & 5)
    add_title("ACKNOWLEDGEMENT")
    add_body(
        "We express our deep sense of gratitude and sincere thanks to our esteemed project guide, "
        "Dr. KM Pooja, Assistant Professor, Department of Information Technology, Indian Institute "
        "of Information Technology, Allahabad, for her invaluable guidance, constructive criticism, "
        "and continuous encouragement throughout the course of this Object-Oriented Methodology project."
    )
    add_body(
        "We are also thankful to the Head of Department, Department of Information Technology, IIIT Allahabad, "
        "and the faculty members of the institute for providing the technical infrastructure, laboratory "
        "resources, and academic atmosphere necessary for the successful design, implementation, and evaluation "
        "of this desktop utility management application."
    )
    p_ack_sign = doc.add_paragraph()
    p_ack_sign.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    p_ack_sign.paragraph_format.space_before = Pt(12)
    p_ack_sign.paragraph_format.line_spacing = 1.2
    r = p_ack_sign.add_run(
        "Mayur Jadhav (IIT2025202)  •  Venkatesh Sarkate (IIT2025176)  •  Pawan Yadav (IIT2025174)\n"
        "B.Tech (IT), 2nd Year, Section B  |  IIIT Allahabad"
    )
    r.font.size = Pt(10)
    r.font.bold = True

    add_title("ABSTRACT")
    add_body(
        "Electricity billing is a critical operational workflow for utility distribution companies, requiring "
        "accurate mathematical computations, reliable consumer record keeping, role-based authorization, and "
        "timely transaction reconciliation. Traditional manual billing systems relying on physical paper registers "
        "suffer from arithmetic miscalculations in tiered tariffs, delayed invoice distribution, data loss vulnerabilities, "
        "and lack of consumer self-service transparency."
    )
    add_body(
        "This project presents the Electricity Billing Management System, a robust desktop application engineered "
        "in Core Java (JDK 17) using Java Swing/AWT and a normalized MySQL relational database connected via JDBC. "
        "The primary objective is to embody foundational Object-Oriented Methodology (OOM) principles—Encapsulation, "
        "Hierarchical Inheritance, Polymorphism, Abstraction, and Discrete Entity Modeling—within an authentic software engineering solution."
    )
    add_body(
        "The application provides dual role-based access control, segregating an Administrator Operational Hub from a "
        "Customer Self-Service Portal. Implemented modules include PBKDF2WithHmacSHA256 password security, customer onboarding, "
        "tariff calculation incorporating tiered slabs and fixed taxes, formal printable invoices, simulated multi-channel "
        "checkout with atomic JDBC transaction commits and rollbacks, complaint lifecycle tracking, and audit logging."
    )
    add_body("Keywords: Object-Oriented Methodology, Core Java, Java Swing, JDBC, MySQL, Electricity Billing, Software Engineering.")

    doc.add_page_break()

    # 5. TABLE OF CONTENTS & LISTS (Page 5)
    add_title("TABLE OF CONTENTS")
    toc_items = [
        ("Certificate", "ii"),
        ("Declaration", "iii"),
        ("Acknowledgement & Abstract", "iv"),
        ("Table of Contents & Lists", "v"),
        ("Chapter 1: Introduction", "6"),
        ("Chapter 2: System Analysis", "9"),
        ("Chapter 3: Technology and Tools Used", "12"),
        ("Chapter 4: Object-Oriented Methodology and Design", "13"),
        ("Chapter 5: System Architecture and Module Description", "17"),
        ("Chapter 6: UML Diagrams and System Modelling", "19"),
        ("Chapter 7: Database Design", "22"),
        ("Chapter 8: Implementation and User Interface", "26"),
        ("Chapter 9: Testing and Results", "32"),
        ("Chapter 10: Conclusion and Future Scope", "34"),
        ("References", "35")
    ]
    
    tbl_toc = doc.add_table(rows=len(toc_items), cols=2)
    tbl_toc.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl_toc.columns[0].width = Inches(5.3)
    tbl_toc.columns[1].width = Inches(0.9)
    for i, (title, page) in enumerate(toc_items):
        c0 = tbl_toc.cell(i, 0)
        c1 = tbl_toc.cell(i, 1)
        p0 = c0.paragraphs[0]
        p0.paragraph_format.line_spacing = 1.15
        p0.paragraph_format.space_after = Pt(1)
        r0 = p0.add_run(title)
        r0.font.size = Pt(10)
        if title.startswith("Chapter") or title in ["Certificate", "Declaration", "Acknowledgement & Abstract", "Table of Contents & Lists", "References"]:
            r0.font.bold = True
            
        p1 = c1.paragraphs[0]
        p1.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        p1.paragraph_format.line_spacing = 1.15
        p1.paragraph_format.space_after = Pt(1)
        r1 = p1.add_run(page)
        r1.font.size = Pt(10)

    p_gap = doc.add_paragraph()
    p_gap.paragraph_format.space_before = Pt(8)
    p_gap.paragraph_format.space_after = Pt(4)
    r = p_gap.add_run("LIST OF FIGURES & TABLES SUMMARY:")
    r.font.bold = True
    r.font.size = Pt(10.5)
    r.font.color.rgb = RGBColor(30, 58, 138)
    
    add_body("Figures 6.1–6.6: UML Use Case, Class, Sequence (Billing & Payment), Activity, and Component Diagrams (pp. 19–21).")
    add_body("Figure 7.1 & Tables 7.1–7.8: Relational ER Diagram and Database Table Specifications (pp. 22–25).")
    add_body("Figures 8.1–8.12: Verified Application UI Screenshots (Login, Dashboards, Invoices, Gateway, Receipts, Grievances, Audits) (pp. 26–31).")
    add_body("Tables 2.1, 2.2 & 9.1: Hardware, Software Specifications and Comprehensive System Verification Test Cases (pp. 11, 32–33).")

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 1: INTRODUCTION (Starts Page 1)
    # =========================================================================
    add_ch_heading("CHAPTER 1: INTRODUCTION")
    
    add_sec_heading("1.1 Introduction to Electricity Billing Management System")
    add_body(
        "Electricity distribution is a vital public utility infrastructure that sustains urban, commercial, "
        "and industrial civil life. In any modern power distribution jurisdiction, utility boards manage tens of "
        "thousands of consumer connections, ranging from residential households to agricultural and industrial facilities. "
        "Accurately calculating electricity consumption, distributing recurring monthly bills, recording revenue collections, "
        "and resolving consumer service grievances are central to the fiscal sustainability and operational efficiency of "
        "the power distribution agency."
    )
    add_body(
        "The Electricity Billing Management System developed in this project is an interactive, standalone desktop "
        "enterprise application architected in Core Java (JDK 17) using the Java Swing/AWT graphical user interface framework "
        "and backed by a relational MySQL database server connected via Java Database Connectivity (JDBC). The application "
        "automates the entire billing lifecycle, providing centralized administrative management for utility personnel alongside "
        "a transparent self-service portal for electricity consumers."
    )

    add_sec_heading("1.2 Background of Electricity Billing")
    add_body(
        "Historically, electricity utility providers managed billing operations through physical paper ledgers, manual "
        "meter-reading notebooks, and hand-computed billing sheets. Field operators conducted monthly physical visits to each "
        "consumer premises to inspect electromechanical meters, note down cumulative units on index cards, and transport "
        "these physical notes to regional billing offices. Clerks applied stepped tariff slabs, calculated government electricity "
        "duty and municipal taxes by hand, produced paper bill slips, and mailed or hand-delivered them to consumer doorsteps."
    )
    add_body(
        "While this conventional methodology functioned adequately during earlier decades when consumer numbers were small "
        "and tariff structures simple, modern multi-tier tariff regulations have rendered manual billing obsolete, error-prone, "
        "and economically unsustainable."
    )

    add_sec_heading("1.3 Need for an Automated Billing System")
    add_body(
        "The transition from manual paper record keeping to computerized billing is driven by several urgent operational imperatives:"
    )
    add_bullet("Elimination of Arithmetic Errors: Tiered electricity pricing (where units consumed beyond thresholds are taxed at progressive rates, combined with meter rent, fixed capacity charges, and swacch bharat taxes) involves arithmetic that frequently results in manual miscalculations.")
    add_bullet("Rapid Bill Distribution: Manual preparation of thousands of paper invoices creates multi-week lags between power consumption and revenue realization, constraining cash flow for utility distributors.")
    add_bullet("Data Centralization: Centralized digital databases enable utility operators to look up customer records, past billing cycles, and uncollected dues within seconds, replacing disorganized filing cabinets.")
    add_bullet("Customer Convenience: Consumers in modern utility environments expect itemized breakdowns of energy usage, historical comparison tables, and simulated checkout options without standing in physical queues.")
    add_bullet("Auditability: Automated software records immutable audit trails of customer transactions, payment settlements, and administrative status modifications, deterring fraudulent manipulation.")

    add_sec_heading("1.4 Problem Statement")
    add_body(
        "Manual electricity billing in regional utility offices suffers from frequent arithmetic calculation errors in "
        "multi-tier tariff computations, severe time lags in billing record generation, high vulnerability to ledger misplacement "
        "or physical deterioration, lack of consumer self-service access, and absence of verifiable audit trails for financial "
        "settlements and consumer grievances. There is a critical need for an automated desktop application that enforces "
        "mathematical accuracy in bill generation, centralizes customer and meter data in a relational database, separates "
        "administrative controls from customer access, and maintains strict transactional integrity."
    )

    add_sec_heading("1.5 Motivation")
    add_body(
        "The motivation behind this project is twofold. From an applied domain perspective, power distribution is one of "
        "the most foundational societal sectors; engineering an automated solution addresses real-world challenges of revenue "
        "leakage, administrative inefficiency, and consumer dissatisfaction. From an academic perspective, an electricity billing "
        "system provides an exemplary domain to study, design, and demonstrate Object-Oriented Methodology (OOM). The domain "
        "naturally maps real-world entities (Consumers, Meters, Tariffs, Invoices, Payments, Complaints) into discrete software "
        "classes, requiring encapsulation of state, hierarchical inheritance across user interfaces, polymorphic event handling, "
        "and clean architectural abstraction between user interfaces, service logic, and database persistence layers."
    )

    add_sec_heading("1.6 Project Objectives")
    add_body("The primary objectives achieved in this project include:")
    add_bullet("Automating Electricity Bill Computation: Implement an automated mathematical engine that accepts consumed units, looks up tariff constants (cost per unit, meter rent, service charges, fixed tax) from MySQL, and computes the exact payable total without arithmetic mistakes (CalculateBill.java).")
    add_bullet("Centralizing Consumer and Meter Hardware Records: Establish a relational schema storing customer personal demographics, hardware meter numbers, phase configurations, installation locations, and operational statuses (NewCustomer.java, MeterInfo.java, CustomerDetails.java).")
    add_bullet("Streamlining Payment Processing & Receipts: Provide instant pending bill lookups, multi-channel simulated payment processing (Card, UPI, NetBanking), atomic ledger status updates to 'Paid', and printable official payment receipts (PayBill.java, PaymentGateway.java, PaymentReceiptDialog.java).")
    add_bullet("Implementing Role-Based Access Control: Design responsive Swing desktop interfaces clearly delineating Administrator controls from Customer self-service portals, backed by secure PBKDF2WithHmacSHA256 password hashing (Login.java, Project.java, PasswordUtils.java).")
    add_bullet("Integrating Consumer Grievances & Audit Logging: Provide a formal complaint lifecycle tracker enabling customers to submit tickets and staff to update resolution remarks, accompanied by system activity audit logging (SubmitComplaint.java, ComplaintManagement.java, AuditLogViewer.java).")

    add_sec_heading("1.7 Scope of the Project")
    add_body(
        "The project encompasses a full-featured desktop management system suitable for local regional electricity utility "
        "branch offices. The scope includes customer registration, hardware meter specification assignment, monthly unit "
        "logging, automated invoice generation, billing ledger inspection, multi-channel payment checkout simulation, official "
        "tax receipt printing, consumer complaint filing, ticket resolution workflows, in-app administrative broadcast notices, "
        "and executive summary analytics. The system operates as a native Java desktop application executing on standard PC "
        "hardware connected over localhost or a local area network (LAN) to a MySQL database server."
    )

    add_sec_heading("1.8 Limitations of the Existing Manual System")
    add_body(
        "The conventional manual approach exhibits fundamental structural flaws: arithmetic errors in stepped calculations, "
        "vulnerability of physical paper ledgers to fire, water, and misplacement, duplicate meter registrations due to lack "
        "of unique database constraints, long queues at cash collection counters, inability to generate duplicate tax receipts "
        "upon loss, and the complete absence of automated audit logs to trace unauthorized record modifications."
    )

    add_sec_heading("1.9 Proposed Solution")
    add_body(
        "The proposed solution is the Electricity Billing Management System. By developing a desktop application in Core Java "
        "with Swing GUI components and an underlying MySQL relational database, the system ensures that all billing computations "
        "are executed deterministically by code. Primary key and foreign key constraints in the database guarantee that every bill "
        "is bound to an active customer and meter. Password hashing secures credentials, and explicit JDBC transaction management "
        "(setAutoCommit, commit, rollback) guarantees atomic ledger settlement during payments."
    )

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 2: SYSTEM ANALYSIS
    # =========================================================================
    add_ch_heading("CHAPTER 2: SYSTEM ANALYSIS")

    add_sec_heading("2.1 Existing Manual Billing System Overview")
    add_body(
        "In a conventional manual billing operational model, every administrative action is tied to physical paper artifacts. "
        "New consumer applications are submitted via printed paper forms, manual account ledgers record hardware meter assignments, "
        "field inspection books log monthly cumulative meter readings, and clerks calculate monthly payable amounts on paper "
        "slips using handheld calculators. Customers receive carbon-copy paper invoices and submit physical cash or paper "
        "cheques at branch cash counters, receiving stamped receipts in return."
    )

    add_sec_heading("2.2 Problems and Vulnerabilities of the Existing Approach")
    add_body("Comprehensive analysis reveals five major operational deficiencies in manual setups:")
    add_bullet("Human Calculation Errors: Tiered calculations involving decimal tax rates (e.g., Swacch Bharat Tax, service charges, variable unit slabs) are prone to human arithmetic errors when computed manually for thousands of consumers.")
    add_bullet("High Operational Latency: Generating, writing, verifying, and distributing thousands of manual paper bills requires weeks of manual effort, delaying utility revenue collection.")
    add_bullet("Physical Misplacement and Data Loss: Paper registers suffer from physical wear and tear, ink fading, water damage, or accidental loss. Searching historical records requires searching through stacks of physical volumes.")
    add_bullet("Lack of Consumer Transparency: Customers cannot independently verify past consumption trends, compare seasonal electricity usage, or check pending balances without physically visiting utility offices.")
    add_bullet("Security and Audit Deficits: Physical paper ledgers lack access control mechanisms; any individual with physical access to the ledger can modify numbers or alter statuses without leaving an audit trail.")

    add_sec_heading("2.3 Proposed Automated System")
    add_body(
        "The proposed automated system replaces paper ledgers with a normalized relational database (`ebs`) and provides "
        "interactive Java Swing graphical interfaces. The system guarantees mathematical accuracy by computing bills via code, "
        "enforces data integrity through primary and foreign key constraints on `meter_no`, secures user authentication through "
        "cryptographic password hashing, enables immediate billing lookups and printable invoice generation, supports simulated "
        "multi-channel checkout, and logs system activities in an immutable audit table."
    )

    add_sec_heading("2.4 Functional Requirements")
    add_body("The software implements the following ten functional requirements (FR):")
    add_bullet("FR-01 (Role-Based Authentication): Authenticates Administrators and Customers using unique usernames, passwords, and assigned roles, redirecting users to their corresponding dashboard.")
    add_bullet("FR-02 (Customer Onboarding): Enables Administrators to register new consumers, capture personal demographics, and automatically generate unique hardware meter numbers.")
    add_bullet("FR-03 (Meter Hardware Configuration): Configures meter hardware attributes, including meter location (Indoor/Outdoor), meter type (Electronic/Solar/Smart), phase codes, and billing types.")
    add_bullet("FR-04 (Tariff Rate Management): Retrieves and applies statutory tariff parameters (cost per unit, meter rent, service charges, fixed taxes) stored in the database.")
    add_bullet("FR-05 (Bill Calculation Engine): Accepts monthly units consumed, calculates total payable amounts, and persists new bill records with status 'Not Paid'.")
    add_bullet("FR-06 (Invoicing & Formal Printing): Generates structured, printable bill invoices displaying itemized breakdowns of units, base energy costs, taxes, and total payable dues.")
    add_bullet("FR-07 (Payment Gateway Simulation): Allows consumers to view outstanding dues, select payment channels (Card, UPI, NetBanking), and settle bills atomically.")
    add_bullet("FR-08 (Official Payment Receipts): Upon successful payment, generates an official printable tax receipt and updates the bill ledger status to 'Paid'.")
    add_bullet("FR-09 (Consumer Grievance Management): Enables consumers to file service complaints and allows utility administrators to review tickets, assign staff, and update resolution statuses.")
    add_bullet("FR-10 (System Activity & Audit Logging): Records significant operational actions (logins, bill calculations, payments, complaint updates) in a dedicated audit log.")

    add_sec_heading("2.5 Non-Functional Requirements")
    add_bullet("Accuracy & Determinism: All tariff mathematical calculations must be strictly deterministic, using standard decimal precision to avoid rounding errors.")
    add_bullet("Data Integrity & Consistency: Referential integrity across customer, meter_info, login, and bill must be strictly maintained via foreign key constraints linked to meter_no.")
    add_bullet("Usability & Responsiveness: Graphical screens must load promptly, present clear visual contrast, provide intuitive button navigation, and display clear dialog notifications.")
    add_bullet("Security & Access Control: User passwords must be protected using PBKDF2WithHmacSHA256 hashing. Customer users must be prevented from accessing administrative control windows.")
    add_bullet("Maintainability: Source code must adhere to clean Object-Oriented principles, modularizing user interfaces, service abstractions, and database utilities into discrete classes.")

    add_sec_heading("2.6 Hardware & Software Requirements")
    add_body("Table 2.1 and Table 2.2 specify the verified execution environment:")
    
    # Table 2.1
    p = doc.add_paragraph()
    r = p.add_run("Table 2.1: Minimum Hardware Requirements for System Deployment")
    r.font.bold = True
    r.font.size = Pt(10)
    tbl_hw = doc.add_table(rows=5, cols=2)
    tbl_hw.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(tbl_hw)
    hw_rows = [
        ("Component", "Minimum Requirement"),
        ("Processor", "Intel Core i3 / AMD Ryzen 3 or equivalent (x86_64 architecture)"),
        ("System Memory (RAM)", "4 GB RAM (8 GB recommended for concurrent IDE development)"),
        ("Hard Disk Storage", "500 MB free space for JDK runtime, MySQL server, and application jar"),
        ("Display Resolution", "1280 x 720 minimum (1920 x 1080 recommended for optimal UI rendering)")
    ]
    for r_idx, (c1, c2) in enumerate(hw_rows):
        row = tbl_hw.rows[r_idx]
        row.cells[0].paragraphs[0].text = c1
        row.cells[1].paragraphs[0].text = c2
        if r_idx == 0:
            set_cell_shading(row.cells[0], "F1F5F9")
            set_cell_shading(row.cells[1], "F1F5F9")
            row.cells[0].paragraphs[0].runs[0].font.bold = True
            row.cells[1].paragraphs[0].runs[0].font.bold = True

    # Table 2.2
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(6)
    r = p.add_run("Table 2.2: Software Environment and Platform Specifications")
    r.font.bold = True
    r.font.size = Pt(10)
    tbl_sw = doc.add_table(rows=6, cols=2)
    tbl_sw.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(tbl_sw)
    sw_rows = [
        ("Software Component", "Specification / Version"),
        ("Operating System", "Microsoft Windows 10 / Windows 11 (64-bit) or Linux"),
        ("Java Development Kit", "Oracle JDK 17 (Java SE 17.0.x)"),
        ("Database Server", "MySQL Server 8.0.x / 8.4.x Community Edition"),
        ("Database Connector", "MySQL Connector/J 8.0.28 (mysql-connector-java-8.0.28.jar)"),
        ("Build System & IDE", "Apache Ant 1.10.x, NetBeans IDE 17 / 20 / VS Code")
    ]
    for r_idx, (c1, c2) in enumerate(sw_rows):
        row = tbl_sw.rows[r_idx]
        row.cells[0].paragraphs[0].text = c1
        row.cells[1].paragraphs[0].text = c2
        if r_idx == 0:
            set_cell_shading(row.cells[0], "F1F5F9")
            set_cell_shading(row.cells[1], "F1F5F9")
            row.cells[0].paragraphs[0].runs[0].font.bold = True
            row.cells[1].paragraphs[0].runs[0].font.bold = True

    add_sec_heading("2.7 User Roles & Feasibility Analysis")
    add_body(
        "Administrator Role: Customer onboarding, meter hardware assignment, monthly consumption logging, bill calculation, "
        "grievance resolution, broadcast notices, and audit log inspection.\n"
        "Customer Role: View profile, inspect billing ledgers, initiate simulated payments, print receipts, and lodge complaints."
    )
    add_body(
        "Feasibility Analysis: Technical feasibility is proven through mature Java/Swing/JDBC/MySQL components. Operational "
        "feasibility is achieved via intuitive, self-explanatory UI forms. Economic feasibility is demonstrated with 100% open-source "
        "technologies running on commodity hardware."
    )

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 3: TECHNOLOGY AND TOOLS USED
    # =========================================================================
    add_ch_heading("CHAPTER 3: TECHNOLOGY AND TOOLS USED")

    add_sec_heading("3.1 Core Java (JDK 17) & Object-Oriented Methodology")
    add_body(
        "Java is a class-based, object-oriented, concurrent, and platform-independent programming language. "
        "The project is compiled against Java 17 (LTS), leveraging strong type safety, automatic garbage collection, "
        "and robust exception handling. Core Java provides the structural backbone of the application: classes "
        "model billing entities, collections manage datasets, and try-with-resources constructs guarantee safe "
        "closure of database socket connections."
    )

    add_sec_heading("3.2 Java Swing and AWT GUI Toolkit")
    add_body(
        "Java Swing is part of the Java Foundation Classes (JFC), providing lightweight, native-independent GUI components. "
        "The application utilizes `JFrame` as the primary top-level window container, `JPanel` for hierarchical layout grouping, "
        "`JTable` and `JScrollPane` for displaying tabular customer and billing records, `JTextField` and `Choice` for input "
        "controls, and `JOptionPane` for modal feedback. Swing's event-driven architecture employs listeners (`ActionListener`, "
        "`WindowListener`) to decouple user interactions from background business execution."
    )

    add_sec_heading("3.3 Java Database Connectivity (JDBC)")
    add_body(
        "JDBC is an industry-standard Java API that enables uniform connectivity between Java applications and relational "
        "databases. The application uses the MySQL Connector/J driver (`com.mysql.cj.jdbc.Driver`). Centralized connectivity "
        "is encapsulated inside `Conn.java`, which provides `Connection` and `Statement` objects. The application utilizes "
        "`PreparedStatement` to parameterize dynamic SQL queries, preventing SQL injection vulnerabilities."
    )

    add_sec_heading("3.4 MySQL Relational Database Management System")
    add_body(
        "MySQL 8.0/8.4 is an enterprise-grade open-source Relational Database Management System (RDBMS). It provides structured "
        "data persistence using the InnoDB storage engine, enforcing ACID (Atomicity, Consistency, Isolation, Durability) "
        "guarantees. Foreign key constraints ensure strict referential integrity across customer records, meter hardware, and "
        "monthly invoices."
    )

    add_sec_heading("3.5 Apache Ant Build System (build.xml)")
    add_body(
        "Apache Ant is an XML-driven build tool that automates compiling, packaging, and executing Java software. The project "
        "utilizes standard Ant targets in `build.xml` (`init`, `compile`, `jar`, `run`, `clean`) configured via "
        "`nbproject/project.properties` with `javac.source=17` and `javac.target=17`, ensuring deterministic builds across environments."
    )

    add_sec_heading("3.6 NetBeans IDE & Development Environment")
    add_body(
        "The application project was configured and verified within the NetBeans IDE environment. NetBeans provides integrated "
        "Ant project synchronization, syntax highlighting, visual form validation, and automated compile-on-save capabilities "
        "compatible with standard Java desktop project conventions."
    )

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 4: OBJECT-ORIENTED METHODOLOGY AND DESIGN
    # =========================================================================
    add_ch_heading("CHAPTER 4: OBJECT-ORIENTED METHODOLOGY AND DESIGN")
    add_body(
        "Object-Oriented Methodology (OOM) models complex software systems as cooperating, discrete objects that encapsulate "
        "both state (data) and behavior (methods). This chapter demonstrates how every major object-oriented principle is "
        "practically implemented in the actual source code of the Electricity Billing Management System."
    )

    # 4.1 Classes and Objects
    add_sec_heading("4.1 Classes and Objects (Modeling Real-World Entities)")
    add_body(
        "Meaning and Purpose: A class is a programmer-defined blueprint from which individual objects are created. "
        "An object is a concrete, runtime instance holding discrete attribute values while sharing common behavior. "
        "Real-world utility entities—such as Customers, Meters, Calculations, and Payments—are modeled as dedicated Java classes."
    )
    add_code_snippet(doc, 
"""// Class Definition modeling the New Customer Onboarding Entity
public class NewCustomer extends JFrame implements ActionListener {
    JTextField tfname, tfaddress, tfstate, tfcity, tfemail, tfphone;
    JButton next, cancel;
    JLabel lblmeter;

    NewCustomer() {
        setTitle("Electricity Billing System - New Customer Registration");
        setSize(700, 500);
        // ... Layout initialization ...
    }
}

// Runtime Object Instantiation inside Project.java
NewCustomer customerWizard = new NewCustomer();
customerWizard.setVisible(true);"""
    )
    add_body(
        "Explanation & Application Contribution: `public class NewCustomer extends JFrame` declares the class blueprint. "
        "Instance variables (`tfname`, `lblmeter`) represent the specific state of a customer onboarding session. "
        "In `Project.java`, `new NewCustomer()` dynamically instantiates an independent object in heap memory. "
        "Classes provide modular separation: new customer registration is isolated inside `NewCustomer.java` without "
        "interfering with bill calculation (`CalculateBill.java`) or payment processing (`PayBill.java`)."
    )

    # 4.2 Encapsulation
    add_sec_heading("4.2 Encapsulation (Data Hiding & State Protection)")
    add_body(
        "Meaning and Purpose: Encapsulation bundles data fields and methods into a single unit while restricting direct "
        "outside access using `private`. Access is provided solely through controlled public getters, preventing accidental or unauthorized mutation."
    )
    add_code_snippet(doc,
"""public final class SessionContext {
    private final String username;
    private final String role;
    private final String meterNo;

    public SessionContext(String username, String role, String meterNo) {
        this.username = username; this.role = role; this.meterNo = meterNo;
    }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getMeterNo() { return meterNo; }
}"""
    )
    add_body(
        "Explanation & Application Contribution: Instance variables `username`, `role`, and `meterNo` are declared `private final`. "
        "The `private` modifier prevents external classes from altering credentials directly. The `final` modifier ensures immutability. "
        "Public getters provide read-only access. Encapsulation prevents unauthorized role privilege escalation; a customer cannot "
        "modify their assigned role to 'Admin' at runtime because the session state is protected."
    )

    # 4.3 Inheritance
    add_sec_heading("4.3 Inheritance (Code Reuse & Hierarchical Specialization)")
    add_body(
        "Meaning and Purpose: Inheritance allows a child class to inherit attributes and methods from an existing parent superclass "
        "using `extends`. This promotes code reuse and establishes an 'IS-A' relationship hierarchy across related components."
    )
    add_code_snippet(doc,
"""// Project dashboard inherits top-level window behavior from JFrame
public class Project extends JFrame implements ActionListener {
    public Project(String atype, String meter) {
        super("Electricity Billing System"); // Invokes JFrame superclass constructor
        setSize(1366, 768); setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
// Payment receipt dialog specializes JDialog modal window
public class PaymentReceiptDialog extends JDialog implements ActionListener {
    public PaymentReceiptDialog(JFrame parent, String title, boolean modal) {
        super(parent, title, modal); setSize(600, 700);
    }
}"""
    )
    add_body(
        "Explanation & Application Contribution: `Project` inherits window management capabilities—such as `setSize()`, `setVisible()`, "
        "and layout management—without re-implementing native window bindings. Similarly, `PaymentReceiptDialog` specializes `JDialog`, "
        "acquiring focus-trapping modal rendering directly. Inheritance eliminates hundreds of lines of boilerplate window code across screens."
    )

    # 4.4 Polymorphism
    add_sec_heading("4.4 Polymorphism (Interface Implementation & Method Overriding)")
    add_body(
        "Meaning and Purpose: Polymorphism enables uniform invocation of operations across varying object types. In Java, "
        "polymorphism is achieved through Interface Implementation and Method Overriding."
    )
    add_code_snippet(doc,
"""public class Project extends JFrame implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent ae) {
        String msg = ae.getActionCommand();
        if (msg.equals("New Customer")) trackSubWindow(new NewCustomer(), "New Customer");
        else if (msg.equals("Calculate Bill")) trackSubWindow(new CalculateBill(), "Calculate Bill");
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(new Color(30, 58, 138)); // Active sidebar indicator highlight
        g2d.fillRect(0, 0, 4, getHeight()); g2d.dispose();
    }
}"""
    )
    add_body(
        "Explanation & Application Contribution: Swing dispatches `actionPerformed(ActionEvent ae)` polymorphically whenever a user clicks "
        "a button or menu item. Each screen implements `actionPerformed` to execute its specific response. In addition, `paintComponent(Graphics g)` "
        "overrides Swing's default rendering method to custom-draw active royal-blue navigation indicator bars on the sidebar."
    )

    # 4.5 Abstraction
    add_sec_heading("4.5 Abstraction (Hiding Complex Implementation Logistics)")
    add_body(
        "Meaning and Purpose: Abstraction separates external operational interfaces from internal execution complexities. The caller interacts "
        "with simple, high-level methods while low-level socket connections, SQL parsing, and driver handshakes remain hidden."
    )
    add_code_snippet(doc,
"""public class Conn implements AutoCloseable {
    public Connection c; public Statement s;
    public Conn() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            c = DriverManager.getConnection("jdbc:mysql://localhost:3306/ebs", "root", "");
            s = c.createStatement();
        } catch (Exception e) { e.printStackTrace(); }
    }
    @Override
    public void close() {
        if (s != null) try { s.close(); } catch (Exception ignored) {}
        if (c != null) try { c.close(); } catch (Exception ignored) {}
    }
}"""
    )
    add_body(
        "Explanation & Application Contribution: `Conn.java` encapsulates all JDBC logistics—driver loading, connection string parsing, "
        "credential passing, statement creation, and socket cleanup. GUI screens simply invoke `new Conn()`, oblivious to database "
        "connection protocols. If the database host or driver changes, only `Conn.java` requires modification."
    )

    # 4.6 Constructors
    add_sec_heading("4.6 Constructors (Object Lifecycle & State Initialization)")
    add_body(
        "Meaning and Purpose: A constructor is invoked automatically when an object is instantiated to initialize newly created "
        "objects with valid starting states."
    )
    add_code_snippet(doc,
"""public class MeterInfo extends JFrame implements ActionListener {
    String meternumber;
    Choice meterlocation, metertype, phasecode, billtype;
    // Parameterized Constructor initializing specific meter context
    MeterInfo(String meternumber) {
        this.meternumber = meternumber; // Binds meter number to current instance
        setTitle("Electricity Billing System - Meter Information");
        setSize(700, 500); setLocation(400, 200);
    }
}"""
    )
    add_body(
        "Explanation & Application Contribution: `MeterInfo(String meternumber)` is a parameterized constructor. When `NewCustomer` "
        "completes customer onboarding, it passes the generated meter number string to `new MeterInfo(meternumber)`. The constructor "
        "binds this parameter to `this.meternumber`, guaranteeing that a `MeterInfo` window cannot exist without an associated customer record."
    )

    # 4.7 Interfaces
    add_sec_heading("4.7 Interfaces (Architectural Contracts & Decoupling)")
    add_body(
        "Meaning and Purpose: An interface defines a formal contract of method signatures without implementation bodies. "
        "Classes implement interfaces using `implements`, guaranteeing that required behaviors are provided."
    )
    add_code_snippet(doc,
"""// Interface Contract from standard Java API
public interface ActionListener extends EventListener {
    void actionPerformed(ActionEvent e);
}
// Concrete GUI Form implementing the ActionListener contract
public class CalculateBill extends JFrame implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent ae) {
        // Enforces concrete implementation of button click responses
    }
}"""
    )
    add_body(
        "Explanation & Application Contribution: `CalculateBill implements ActionListener` guarantees a concrete definition for "
        "`actionPerformed(ActionEvent ae)`. Similarly, `Conn implements AutoCloseable` promises a definition for `close()`, enabling "
        "safe usage inside Java try-with-resources blocks."
    )

    # 4.8 Method Overriding
    add_sec_heading("4.8 Method Overriding (Specialized Behavior Customization)")
    add_body(
        "Meaning and Purpose: Method overriding occurs when a subclass provides a specific implementation of a method already declared "
        "in its superclass, maintaining identical signature and return type. It enables runtime polymorphism."
    )
    add_code_snippet(doc,
"""public class Conn implements AutoCloseable {
    @Override
    public void close() {
        if (s != null) try { s.close(); } catch (Exception ignored) {}
        if (c != null) try { c.close(); } catch (Exception ignored) {}
    }
}"""
    )
    add_body(
        "Explanation & Application Contribution: `Conn.java` overrides `AutoCloseable.close()`. The `@Override` annotation instructs the compiler "
        "to verify that the signature matches `AutoCloseable`. The custom implementation closes both the Statement and Connection, preventing "
        "database connection leaks by releasing server socket resources automatically."
    )

    # 4.9 Access Modifiers
    add_sec_heading("4.9 Access Modifiers (Access Control Boundary Enforcement)")
    add_body(
        "Meaning and Purpose: Access modifiers (`private`, `public`, `protected`, package-private) set visibility boundaries, "
        "preventing unauthorized external coupling."
    )
    add_code_snippet(doc,
"""public class Conn implements AutoCloseable {
    public Connection c; public Statement s; // Public access for query execution
    private static String dbUrl, dbUser, dbPass; // Encapsulated database configuration
}
public class Project extends JFrame implements ActionListener {
    private JFrame activeSubWindow = null; // Private: tracks current open subwindow
    public void trackSubWindow(JFrame win, String nav) { ... } // Controlled Public API
}"""
    )
    add_body(
        "Explanation & Application Contribution: In `Conn.java`, database credentials (`dbUrl`, `dbUser`, `dbPass`) are declared `private static`, "
        "preventing external classes from tampering with database connection configurations. In `Project.java`, `activeSubWindow` is `private`, "
        "while `trackSubWindow()` is `public` to allow controlled registration."
    )

    # 4.10 Relationships Between Classes
    add_sec_heading("4.10 Relationships Between Classes (Association, Dependency, Aggregation)")
    add_body(
        "Association: A structural relationship where one class knows about another (`Project` dashboard associates with `NewCustomer` and `CustomerDetails`).\n"
        "Dependency: A temporary 'uses-a' relationship during method execution (`CalculateBill` depends on `Conn` to execute database queries).\n"
        "Aggregation / Composition: A 'has-a' whole-part relationship (`UserSession` maintains a reference to an encapsulated `SessionContext` object)."
    )

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 5: SYSTEM ARCHITECTURE AND MODULE DESCRIPTION
    # =========================================================================
    add_ch_heading("CHAPTER 5: SYSTEM ARCHITECTURE AND MODULE DESCRIPTION")

    add_sec_heading("5.1 Overall System Architecture & Workflow")
    add_body(
        "The Electricity Billing Management System is architected as a modular, three-tier desktop utility solution:\n"
        "1. Presentation Layer (Swing GUI): Contains interactive windows, forms, tables, and dialogs. Captures user input and presents visual feedback.\n"
        "2. Business Logic & Security Tier: Enforces tariff rules, stepped energy rates, session management (UserSession, SessionContext), password hashing (PasswordUtils), and service abstractions.\n"
        "3. Data Persistence Tier: Consists of the database wrapper (Conn.java) connecting via JDBC to MySQL (`ebs`), executing parameterized SQL queries."
    )

    modules_desc = [
        ("5.2 Authentication and Login Module",
         "Authenticates users and enforces role-based authorization. Users enter username, password, and select role ('Admin' or 'Customer'). "
         "Passwords are verified using PBKDF2WithHmacSHA256 password hashing. Session state is initialized and the user is redirected to the corresponding dashboard. "
         "Classes: Login.java, Signup.java, PasswordUtils.java, UserSession.java, SessionContext.java. Table: `login`."),

        ("5.3 Admin Dashboard Module",
         "Central operational hub for utility staff. Features an active sidebar navigation menu with dynamic state tracking, live metric overview cards "
         "(Total Customers, Active Accounts, Total Invoices, Paid/Unpaid bills), and quick-action shortcuts for administrative workflows. Class: Project.java (atype='Admin')."),

        ("5.4 Customer Dashboard Module",
         "Self-service portal for electricity consumers. Displays account summary details, assigned meter number, connection status, latest bill amount, "
         "units consumed, pending dues, and quick-action buttons for Pay Bill, View Bill, and Complaints. Class: Project.java (atype='Customer')."),

        ("5.5 Customer Registration and Management Module",
         "Guided two-step wizard for onboarding new electricity consumers. Captures customer name, street address, city, state, email, and phone number, "
         "and automatically generates a unique 6-digit hardware meter number. Classes: NewCustomer.java, CustomerDetails.java. Table: `customer`."),

        ("5.6 Meter Information Management Module",
         "Second stage of the onboarding wizard. Configures physical meter hardware specifications: meter location (Indoor/Outdoor), meter type "
         "(Electronic, Solar, Smart Meter), phase code (Single/Three Phase), and bill type. Class: MeterInfo.java. Table: `meter_info`."),

        ("5.7 Electricity Bill Calculation Module",
         "Accepts an active customer meter number, billing month, and units consumed. Automatically queries statutory tax rates from MySQL, calculates "
         "total payable dues using stepped tariff logic, and creates a new pending bill record. Class: CalculateBill.java. Tables: `tax`, `bill`."),

        ("5.8 Bill Generation and Bill Details Module",
         "Generates formal, itemized electricity invoices suitable for official printing. Displays consumer details, meter specifications, units consumed, "
         "base energy charge, meter rent, service taxes, and total payable amount. Classes: GenerateBill.java, BillDetails.java. Tables: `customer`, `meter_info`, `tax`, `bill`."),

        ("5.9 Payment Processing Module",
         "Allows consumers to inspect outstanding billing dues, view itemized charges, and initiate simulated online payment checkout across multiple channels "
         "(Card, UPI, NetBanking). Enforces atomic JDBC transaction commit/rollback. Classes: PayBill.java, PaymentGateway.java. Table: `bill`."),

        ("5.10 Payment Receipt Module",
         "Modal dialog displayed immediately upon successful payment settlement. Formats an official printable tax receipt showing transaction reference, "
         "timestamp, payment mode, cleared amount, and zero outstanding balance. Class: PaymentReceiptDialog.java."),

        ("5.11 Complaint Submission and Management Module",
         "Consumer grievance redressal system. Customers lodge complaints by selecting a category (Billing, Meter, Outage), entering a subject, and providing details. "
         "Administrators review tickets, assign priority, and update resolution remarks. Classes: SubmitComplaint.java, ComplaintManagement.java, MyComplaints.java. Tables: `complaints`, `complaint_history`."),

        ("5.12 Notification System Module",
         "In-app communication system. Utility administrators broadcast general notices to all consumers or targeted alerts to specific accounts. "
         "Consumers receive notifications in a dedicated in-app notification hub. Classes: CustomerNotificationHub.java, AdminMessageHub.java."),

        ("5.13 Analytics and Audit Logging Module",
         "Provides administrative oversight. Displays live summary metric cards (revenue collected, outstanding dues, customer counts) and maintains "
         "an audit log table recording system actions (logins, bill calculations, payments) with CSV export. Classes: AnalyticsDashboard.java, AuditLogViewer.java, AuditService.java. Table: `audit_logs`.")
    ]

    for m_title, m_body in modules_desc:
        add_sec_heading(m_title)
        add_body(m_body)

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 6: UML DIAGRAMS AND SYSTEM MODELLING
    # =========================================================================
    add_ch_heading("CHAPTER 6: UML DIAGRAMS AND SYSTEM MODELLING")
    add_body(
        "Unified Modeling Language (UML) provides standard visual notations for specifying, constructing, and documenting "
        "software artifacts. This chapter presents the structural, behavioral, and architectural UML models representing "
        "the Electricity Billing Management System, matched directly to the underlying Java source code."
    )

    # 6.1 Use Case Diagram
    add_sec_heading("6.1 UML Use Case Diagram")
    add_body(
        "The Use Case Diagram specifies the functional boundary of the system and interactions between external actors (Administrator and Customer) "
        "and supported use cases. The Administrator participates in administrative use cases (Customer Registration, Tariff Configuration, Bill Calculation, "
        "Analytics, Complaints, and Audit Logs), while the Customer participates in self-service use cases (Account Inspection, Bill Payment, Receipt Printing, and Grievances)."
    )
    add_captioned_image(doc, "generated_diagrams/use_case_diagram.png", "Figure 6.1: UML Use Case Diagram for Electricity Billing Management System", width=Inches(4.1))

    # 6.2 Class Diagram
    add_sec_heading("6.2 UML Class Diagram")
    add_body(
        "The Class Diagram illustrates the structural hierarchy, class attributes, methods, inheritance relationships, interface implementations, "
        "and associations. Key structural relationships include GUI windows extending `javax.swing.JFrame`, dialogs extending `javax.swing.JDialog`, "
        "event handlers implementing `java.awt.event.ActionListener`, and data access abstracted behind `Conn.java` (implementing `AutoCloseable`)."
    )
    add_captioned_image(doc, "generated_diagrams/class_diagram.png", "Figure 6.2: UML Class Diagram Demonstrating OOP Structural Hierarchy", width=Inches(4.1))

    # 6.3 Sequence Diagram: Bill Generation
    add_sec_heading("6.3 UML Sequence Diagram – Electricity Bill Generation")
    add_body(
        "This Sequence Diagram traces the chronological message exchange between the Administrator actor, Project dashboard, CalculateBill form, "
        "Conn data layer, and MySQL database during monthly bill calculation: active meter numbers are retrieved, tariff constants queried from `tax`, "
        "total amounts calculated, and new records inserted into `bill`."
    )
    add_captioned_image(doc, "generated_diagrams/sequence_bill_generation.png", "Figure 6.3: UML Sequence Diagram – Electricity Bill Calculation and Generation", width=Inches(4.1))

    # 6.4 Sequence Diagram: Bill Payment
    add_sec_heading("6.4 UML Sequence Diagram – Bill Payment & Settlement")
    add_body(
        "This Sequence Diagram illustrates the end-to-end checkout and ledger settlement workflow. The Customer inspects pending bills in `PayBill`, "
        "initiates checkout in `PaymentGateway`, selects a simulated payment channel, and executes an atomic JDBC update (`conn.setAutoCommit(false); UPDATE bill SET status='Paid'; conn.commit()`)."
    )
    add_captioned_image(doc, "generated_diagrams/sequence_diagram.png", "Figure 6.4: UML Sequence Diagram – Customer Bill Payment and Ledger Settlement", width=Inches(4.1))

    # 6.5 Activity Diagram
    add_sec_heading("6.5 UML Activity Diagram – Billing & Payment Workflow")
    add_body(
        "The Activity Diagram models the dynamic operational control flow across three concurrent swimlanes: Administrator, System/Billing Engine, "
        "and Consumer. It captures the workflow from initial customer registration and monthly unit consumption entry through automated bill calculation, "
        "customer login, simulated payment checkout, conditional transaction rollback on failure, and receipt dispatch."
    )
    add_captioned_image(doc, "generated_diagrams/activity_diagram.png", "Figure 6.5: UML Activity Diagram – End-to-End Billing and Payment Workflow", width=Inches(4.1))

    # 6.6 Component Diagram
    add_sec_heading("6.6 UML Component Diagram")
    add_body(
        "The Component Diagram depicts the high-level software subsystems and interconnecting interfaces: Presentation Layer (Swing GUI frames), "
        "Session & Security Subsystem (UserSession, PasswordUtils), Business Logic Tier, Data Access Layer (`Conn.java` JDBC wrapper), and the MySQL Database (`ebs`)."
    )
    add_captioned_image(doc, "generated_diagrams/component_diagram.png", "Figure 6.6: UML Component Diagram – Subsystems, Layers and Connectors", width=Inches(4.1))

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 7: DATABASE DESIGN
    # =========================================================================
    add_ch_heading("CHAPTER 7: DATABASE DESIGN")

    add_sec_heading("7.1 Database Overview & Architecture (`ebs`)")
    add_body(
        "The application utilizes a normalized MySQL relational database named `ebs` executing on the InnoDB storage engine. "
        "The schema enforces referential integrity through foreign key constraints anchored to `customer.meter_no`. Primary keys "
        "prevent duplicate registrations, while normalized tables eliminate data redundancy."
    )

    add_sec_heading("7.2 Detailed Database Table Specifications")

    def add_schema_table_compact(caption, col_specs):
        p = doc.add_paragraph()
        r = p.add_run(caption)
        r.font.bold = True
        r.font.size = Pt(10)
        
        tbl = doc.add_table(rows=len(col_specs) + 1, cols=4)
        tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        set_table_borders(tbl)
        tbl.columns[0].width = Inches(1.5)
        tbl.columns[1].width = Inches(1.4)
        tbl.columns[2].width = Inches(1.3)
        tbl.columns[3].width = Inches(2.1)
        
        headers = ["Column Name", "Data Type", "Constraint", "Description"]
        for c_idx, h in enumerate(headers):
            cell = tbl.cell(0, c_idx)
            cell.paragraphs[0].text = h
            set_cell_shading(cell, "F1F5F9")
            cell.paragraphs[0].runs[0].font.bold = True
            cell.paragraphs[0].runs[0].font.size = Pt(9.5)
            
        for r_idx, row_data in enumerate(col_specs):
            for c_idx, val in enumerate(row_data):
                cell = tbl.cell(r_idx + 1, c_idx)
                p = cell.paragraphs[0]
                p.paragraph_format.line_spacing = 1.1
                p.paragraph_format.space_after = Pt(1)
                r = p.add_run(val)
                r.font.size = Pt(9)

    add_schema_table_compact("Table 7.1: `customer` Master Table Specification", [
        ("meter_no", "VARCHAR(20)", "PRIMARY KEY", "Unique 6-digit hardware meter identifier"),
        ("name", "VARCHAR(100)", "NOT NULL", "Full legal name of the registered consumer"),
        ("address", "VARCHAR(200)", "DEFAULT NULL", "Physical street address of premises"),
        ("city", "VARCHAR(50)", "DEFAULT NULL", "City or municipality jurisdiction"),
        ("state", "VARCHAR(50)", "DEFAULT NULL", "State jurisdiction"),
        ("email", "VARCHAR(100)", "DEFAULT NULL", "Contact email address of consumer"),
        ("phone", "VARCHAR(20)", "DEFAULT NULL", "Contact telephone / mobile number"),
        ("status", "VARCHAR(20)", "DEFAULT 'Active'", "Operational connection status ('Active'/'Inactive')")
    ])

    add_schema_table_compact("Table 7.2: `meter_info` Table Specification", [
        ("meter_no", "VARCHAR(20)", "PRIMARY KEY, FK", "Foreign key referencing customer(meter_no)"),
        ("meter_location", "VARCHAR(100)", "DEFAULT NULL", "Physical mounting location ('Indoor'/'Outdoor')"),
        ("meter_type", "VARCHAR(50)", "DEFAULT NULL", "Hardware technology ('Electronic'/'Solar'/'Smart')"),
        ("phase_code", "VARCHAR(20)", "DEFAULT NULL", "Electrical phase wiring ('011' Single, '022' Three)"),
        ("bill_type", "VARCHAR(50)", "DEFAULT NULL", "Billing category ('Normal'/'Commercial'/'Industrial')"),
        ("days", "VARCHAR(10)", "DEFAULT NULL", "Billing cycle duration (default 30 days)")
    ])

    add_schema_table_compact("Table 7.3: `login` Table Specification", [
        ("meter_no", "VARCHAR(20)", "PRIMARY KEY, FK", "Foreign key referencing customer(meter_no)"),
        ("username", "VARCHAR(50)", "NOT NULL", "Unique user authentication handle"),
        ("name", "VARCHAR(100)", "DEFAULT NULL", "Associated user display name"),
        ("password", "VARCHAR(255)", "NOT NULL", "PBKDF2WithHmacSHA256 salted password hash"),
        ("user", "VARCHAR(20)", "NOT NULL", "Authorization role ('Admin' or 'Customer')")
    ])

    add_schema_table_compact("Table 7.4: `tax` Parameter Table Specification", [
        ("cost_per_unit", "DECIMAL(10,2)", "DEFAULT NULL", "Base energy consumption rate per unit (kWh)"),
        ("meter_rent", "DECIMAL(10,2)", "DEFAULT NULL", "Monthly physical meter maintenance rent"),
        ("service_charge", "DECIMAL(10,2)", "DEFAULT NULL", "Distribution utility service surcharge"),
        ("swacch_bharat_tax", "DECIMAL(10,2)", "DEFAULT NULL", "Statutory Swacch Bharat environmental cess"),
        ("fixed_tax", "DECIMAL(10,2)", "DEFAULT NULL", "Base capacity connection tax")
    ])

    add_schema_table_compact("Table 7.5: `bill` Ledger Table Specification", [
        ("meter_no", "VARCHAR(20)", "FOREIGN KEY", "Foreign key referencing customer(meter_no)"),
        ("month", "VARCHAR(20)", "DEFAULT NULL", "Billing cycle month (e.g., 'January')"),
        ("units", "VARCHAR(20)", "DEFAULT NULL", "Cumulative electrical units consumed"),
        ("total_bill", "VARCHAR(20)", "DEFAULT NULL", "Total calculated payable amount"),
        ("status", "VARCHAR(20)", "DEFAULT 'Not Paid'", "Payment settlement status ('Paid'/'Not Paid')")
    ])

    add_schema_table_compact("Table 7.6: `complaints` Grievance Table Specification", [
        ("complaint_id", "INT AUTO_INC", "PRIMARY KEY", "Unique auto-incrementing grievance tracking ID"),
        ("meter_no", "VARCHAR(20)", "FOREIGN KEY", "Foreign key referencing customer(meter_no)"),
        ("reference_number", "VARCHAR(30)", "UNIQUE", "Human-readable complaint tracking reference"),
        ("category", "VARCHAR(50)", "DEFAULT NULL", "Grievance category (Billing, Meter, Outage)"),
        ("subject", "VARCHAR(150)", "DEFAULT NULL", "Brief summary description of the issue"),
        ("description", "TEXT", "DEFAULT NULL", "Full explanatory details of the grievance"),
        ("priority", "VARCHAR(20)", "DEFAULT 'Medium'", "Urgency classification ('High'/'Medium'/'Low')"),
        ("status", "VARCHAR(20)", "DEFAULT 'Open'", "Ticket status ('Open'/'In Progress'/'Resolved')"),
        ("created_at", "TIMESTAMP", "DEFAULT CURRENT_TS", "Audit timestamp of ticket creation")
    ])

    add_schema_table_compact("Table 7.7: `complaint_history` Audit Table Specification", [
        ("history_id", "INT AUTO_INC", "PRIMARY KEY", "Unique history entry identifier"),
        ("complaint_id", "INT", "FOREIGN KEY", "Foreign key referencing complaints(complaint_id)"),
        ("action", "VARCHAR(50)", "DEFAULT NULL", "Action performed (Status Update, Staff Assignment)"),
        ("previous_value", "VARCHAR(100)", "DEFAULT NULL", "State prior to administrative action"),
        ("new_value", "VARCHAR(100)", "DEFAULT NULL", "State following administrative action"),
        ("performed_by", "VARCHAR(50)", "DEFAULT NULL", "User ID of administrator executing action"),
        ("remarks", "TEXT", "DEFAULT NULL", "Administrative notes and resolution explanation"),
        ("created_at", "TIMESTAMP", "DEFAULT CURRENT_TS", "Audit timestamp of state transition")
    ])

    add_schema_table_compact("Table 7.8: `audit_logs` System Activity Table Specification", [
        ("log_id", "INT AUTO_INC", "PRIMARY KEY", "Unique auto-incrementing audit log entry ID"),
        ("actor_id", "VARCHAR(50)", "DEFAULT NULL", "User ID of actor initiating the operational event"),
        ("actor_role", "VARCHAR(20)", "DEFAULT NULL", "Role classification ('Admin' / 'Customer')"),
        ("action_type", "VARCHAR(50)", "DEFAULT NULL", "Action classification ('LOGIN', 'BILL_CALC', 'PAYMENT')"),
        ("entity_type", "VARCHAR(50)", "DEFAULT NULL", "Domain entity affected ('METER', 'BILL', 'TICKET')"),
        ("details", "TEXT", "DEFAULT NULL", "Structured JSON or text log payload"),
        ("created_at", "TIMESTAMP", "DEFAULT CURRENT_TS", "Tamper-evident timestamp of event recording")
    ])

    # 7.3 ER Diagram
    add_sec_heading("7.3 Entity-Relationship (ER) Diagram")
    add_body(
        "The Relational Entity-Relationship Diagram visualizes tables, primary keys, foreign keys, and cardinalities within `ebs`. "
        "The master entity `CUSTOMER` maintains a 1-to-1 relationship with `METER_INFO` and `LOGIN`, and a 1-to-Many relationship with `BILL` "
        "and `COMPLAINTS`. Furthermore, `COMPLAINTS` maintains a 1-to-Many relationship with `COMPLAINT_HISTORY` for complete grievance auditing."
    )
    add_captioned_image(doc, "generated_diagrams/er_diagram.png", "Figure 7.1: Relational Entity-Relationship (ER) Diagram for `ebs` Database Schema", width=Inches(4.1))

    # 7.4 JDBC & Transactions
    add_sec_heading("7.4 JDBC Connectivity, PreparedStatements & Transactions")
    add_body(
        "Database connectivity is centralized within `Conn.java`, which loads `com.mysql.cj.jdbc.Driver` and connects via "
        "`jdbc:mysql://localhost:3306/ebs`. To prevent SQL injection, dynamic queries use parameterized `PreparedStatement` with typed "
        "setters (`setString()`, `setInt()`). Multi-step database updates employ explicit JDBC transaction management: "
        "`conn.setAutoCommit(false);` executes statements in isolation. `conn.commit();` commits changes permanently, while "
        "`conn.rollback();` restores records upon failure, preventing partial ledger updates."
    )

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 8: IMPLEMENTATION AND USER INTERFACE
    # =========================================================================
    add_ch_heading("CHAPTER 8: IMPLEMENTATION AND USER INTERFACE")
    add_body(
        "This chapter showcases the user interface implementation of the Electricity Billing Management System. "
        "All visual figures presented below are authentic screenshots captured directly from the running Java Swing application."
    )

    screens_info = [
        ("login_screen.png", "Figure 8.1: Role-Based User Login and Authentication Screen",
         "The Login Screen provides a secure authentication entry point. Users select their role ('Admin' or 'Customer'), enter username and password. "
         "Passwords are verified using PBKDF2WithHmacSHA256 password hashing. User Role: All Users."),

        ("admin_nav_dashboard_active.png", "Figure 8.2: Administrator Control Hub and Navigation Dashboard",
         "The Administrator Dashboard serves as the central control hub, featuring live system metric cards (Total Customers, Active Accounts, "
         "Total Invoices, Paid/Unpaid bills), quick shortcuts, and persistent royal-blue sidebar navigation. User Role: Administrator."),

        ("customer_nav_dashboard_active.png", "Figure 8.3: Customer Self-Service Account Overview Portal",
         "The Customer Portal provides a transparent self-service interface displaying account details, assigned meter number, current consumption, "
         "latest bill amount, and quick-action shortcuts for Pay Bill, View Bill, and Complaints. User Role: Customer."),

        ("new_customer_screen.png", "Figure 8.4: Customer Onboarding and Registration Wizard Form",
         "The New Customer interface enables administrators to register new consumers. Captures customer name, address, city, state, email, "
         "phone number, and automatically generates a unique 6-digit hardware meter number. User Role: Administrator."),

        ("meter_info_screen.png", "Figure 8.5: Meter Hardware Specifications and Phase Configuration Screen",
         "The Meter Information form configures physical meter attributes: mounting location (Indoor/Outdoor), meter type (Electronic/Solar/Smart), "
         "phase code (Single/Three Phase), and billing cycle duration. User Role: Administrator."),

        ("calculate_bill_screen.png", "Figure 8.6: Monthly Electricity Consumption and Bill Calculation Interface",
         "The Calculate Bill screen accepts an active meter number, billing month, and cumulative units consumed. Automatically queries statutory "
         "tariff rates from MySQL and creates a new pending bill record. User Role: Administrator."),

        ("bill_details_screen.png", "Figure 8.7: Comprehensive Billing Ledger and Account History View",
         "The Bill Details screen displays a scrollable, tabular ledger of all generated bills across consumers. Utility staff can search, "
         "filter by meter number, inspect unit charges, check payment statuses, and trigger printing. User Role: Administrator."),

        ("payment_gateway_screen.png", "Figure 8.8: Simulated Multi-Channel Checkout Payment Gateway",
         "The Payment Gateway simulates a secure online checkout environment. Consumers select their preferred payment channel (Card, "
         "UPI, NetBanking) and submit payment. Executes atomic JDBC transaction commit and rollback. User Role: Customer."),

        ("payment_receipt_screen.png", "Figure 8.9: Official Printable Payment Tax Receipt Dialog",
         "Modal receipt dialog rendered immediately upon successful payment. Displays formal payment tax receipt with transaction reference, "
         "cleared amount, date, and zero outstanding balance, with direct print capability. User Role: Customer."),

        ("complaint_management_screen.png", "Figure 8.10: Customer Grievance Resolution and Complaint Management",
         "Administrative grievance interface. Utility staff can review filed complaints, filter by priority, assign investigation staff, "
         "and record resolution remarks, maintaining complete grievance history. User Role: Administrator."),

        ("customer_notification_hub.png", "Figure 8.11: Customer In-App Notification Hub and Broadcast Alerts",
         "In-app notification center for consumers. Displays broadcast administrative announcements, scheduled maintenance notices, and "
         "billing reminders dispatched by the utility management team. User Role: Customer."),

        ("audit_log_viewer_screen.png", "Figure 8.12: System Activity Log Viewer and Security Audit Trail",
         "The Audit Log Viewer provides system activity tracking. Records operational events (user logins, bill calculations, payments, "
         "complaint transitions) with timestamp, actor role, and JSON event payload, with CSV export. User Role: Administrator.")
    ]

    for spath, scaption, sdesc in screens_info:
        add_captioned_image(doc, spath, scaption, width=Inches(3.9))
        add_body(sdesc)

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 9: TESTING AND RESULTS
    # =========================================================================
    add_ch_heading("CHAPTER 9: TESTING AND RESULTS")

    add_sec_heading("9.1 Testing Strategy and Approach")
    add_body(
        "A multi-tiered testing strategy was conducted to verify functional correctness, mathematical accuracy, and data consistency:\n"
        "• Unit Testing: Validating mathematical formulas, tariff stepped logic, and string utilities.\n"
        "• Integration Testing: Testing JDBC queries across Conn.java, validating transaction commit/rollback.\n"
        "• System Testing: Verifying complete end-to-end user workflows (registration, billing, payment, complaints).\n"
        "• Black-Box GUI Testing: Validating button clicks, form validations, active navigation highlights, and modal dialogs."
    )

    add_sec_heading("9.2 Automated Verification Test Suites")
    add_body(
        "The repository includes dedicated automated verification test suites (`Phase1VerificationTest.java` through `Phase9VerificationTest.java` "
        "and `ApplicationTest.java`). These test suites execute automated assertions validating database connectivity, PBKDF2 password "
        "verification, customer records availability, tariff calculation precision, payment ledger atomicity, and sidebar navigation state integrity."
    )

    add_sec_heading("9.3 Detailed Test Cases & Execution Results")
    add_body("Table 9.1 details key functional and system test cases executed against the working application:")

    # Table 9.1
    p = doc.add_paragraph()
    r = p.add_run("Table 9.1: Comprehensive Functional and System Verification Test Results")
    r.font.bold = True
    r.font.size = Pt(10)

    test_cases = [
        ("TC-01", "Admin Login with valid credentials", "Authenticate admin; redirect to Admin Dashboard", "Admin authenticated successfully; Admin Dashboard launched", "PASSED"),
        ("TC-02", "Customer Login with valid credentials", "Authenticate customer; redirect to Customer Portal", "Customer authenticated successfully; Customer Portal launched", "PASSED"),
        ("TC-03", "Login with incorrect password", "Deny access; display 'Invalid Login' dialog", "Access denied; 'Invalid Login' error dialog displayed", "PASSED"),
        ("TC-04", "New Customer Registration", "Insert record into customer; auto-generate 6-digit meter", "Record inserted into customer table; meter generated", "PASSED"),
        ("TC-05", "Meter Hardware Configuration", "Insert hardware specs into meter_info linked by meter_no", "Meter specs inserted; foreign key constraint verified", "PASSED"),
        ("TC-06", "Electricity Bill Calculation", "Compute Units*Rate + Rent + Taxes; insert into bill", "Total computed deterministically; bill inserted as 'Not Paid'", "PASSED"),
        ("TC-07", "Pending Bill Lookup in PayBill", "Query bill record for selected meter and month", "Units consumed, bill amount, and status displayed accurately", "PASSED"),
        ("TC-08", "Payment Checkout Settlement", "Process payment; setAutoCommit(false); commit transaction", "Status updated to 'Paid'; transaction committed to MySQL", "PASSED"),
        ("TC-09", "Simulated Payment Cancellation", "Trigger rollback; leave bill status unchanged", "Rollback executed; bill status remains 'Not Paid'", "PASSED"),
        ("TC-10", "Payment Receipt Generation", "Display modal receipt with itemized breakdown and print", "Modal receipt dialog rendered with zero balance and print support", "PASSED"),
        ("TC-11", "Customer Complaint Submission", "Insert grievance ticket into complaints table", "Ticket inserted with unique tracking ID; status='Open'", "PASSED"),
        ("TC-12", "Admin Complaint Resolution", "Update ticket status and insert into complaint_history", "Status updated to 'Resolved'; audit history entry created", "PASSED"),
        ("TC-13", "Audit Trail Logging", "Record event in audit_logs with timestamp and payload", "Log entry persisted; searchable via AuditLogViewer", "PASSED"),
        ("TC-14", "Sidebar Active Navigation", "Highlight active item; reset to Dashboard on window close", "Navigation highlight tracks active window; resets cleanly on close", "PASSED")
    ]

    tbl_tc = doc.add_table(rows=len(test_cases) + 1, cols=5)
    tbl_tc.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(tbl_tc)
    tbl_tc.columns[0].width = Inches(0.8)
    tbl_tc.columns[1].width = Inches(1.8)
    tbl_tc.columns[2].width = Inches(1.8)
    tbl_tc.columns[3].width = Inches(1.8)
    tbl_tc.columns[4].width = Inches(0.8)

    tc_headers = ["Test ID", "Test Scenario", "Expected Result", "Actual Result", "Status"]
    for c_idx, h in enumerate(tc_headers):
        cell = tbl_tc.cell(0, c_idx)
        cell.paragraphs[0].text = h
        set_cell_shading(cell, "F1F5F9")
        cell.paragraphs[0].runs[0].font.bold = True
        cell.paragraphs[0].runs[0].font.size = Pt(9.5)

    for r_idx, tc_row in enumerate(test_cases):
        for c_idx, val in enumerate(tc_row):
            cell = tbl_tc.cell(r_idx + 1, c_idx)
            p = cell.paragraphs[0]
            p.paragraph_format.line_spacing = 1.1
            p.paragraph_format.space_after = Pt(1)
            r = p.add_run(val)
            r.font.size = Pt(8.5)
            if c_idx == 4:
                r.font.bold = True
                r.font.color.rgb = RGBColor(5, 150, 105)

    add_sec_heading("9.4 Build Verification & Observed Limitations")
    add_body(
        "Build Verification: Verified using Apache Ant (`build.xml`) and JDK 17 compiler (`javac`). The complete source code compiles with "
        "zero compilation errors against JDK 17 compatibility specifications (`javac.source=17`, `javac.target=17`).\n"
        "Observed Limitations: Monthly consumption currently requires administrative entry into `CalculateBill.java`; payment gateway operates "
        "in demonstration simulation mode; and the system operates within a desktop environment requiring direct TCP connectivity to MySQL."
    )

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 10: CONCLUSION AND FUTURE SCOPE
    # =========================================================================
    add_ch_heading("CHAPTER 10: CONCLUSION AND FUTURE SCOPE")

    add_sec_heading("10.1 Project Summary & Objectives Achieved")
    add_body(
        "The Electricity Billing Management System project successfully designed, implemented, and verified an end-to-end "
        "desktop utility management software application. By replacing manual paper billing with an automated Core Java "
        "solution backed by a normalized MySQL relational database, the system streamlines customer registration, meter hardware "
        "configuration, stepped tariff calculation, invoice generation, payment clearance, and grievance tracking."
    )
    add_bullet("Full Billing Lifecycle Automation: Replaced manual arithmetic computations with an automated calculation engine.")
    add_bullet("Centralized Data Persistence: Organized customer, meter, and billing records into normalized relational tables enforcing referential integrity.")
    add_bullet("Dual-Portal Security: Implemented role-based authorization separating Administrator controls from Customer portals with PBKDF2 password hashing.")
    add_bullet("Transactional Settlement: Guaranteed ledger consistency through explicit JDBC transaction commit and rollback management.")
    add_bullet("Formal Documentation & Verification: Modeled system architecture via UML diagrams and validated functionality with automated test suites.")

    add_sec_heading("10.2 OOM Concepts Demonstrated & Benefits")
    add_body(
        "The project serves as an authentic practical demonstration of Object-Oriented Methodology. Encapsulation protects user session "
        "credentials inside `SessionContext.java`; hierarchical inheritance leverages the Swing framework across `JFrame` and `JDialog` subclasses; "
        "polymorphism powers event-driven interaction via `ActionListener` and custom component graphics rendering; and abstraction isolates "
        "low-level JDBC database logistics within `Conn.java`.\n"
        "Practical benefits include elimination of manual math errors, rapid record lookup in seconds, consumer visibility into consumption, and financial ledger consistency."
    )

    add_sec_heading("10.3 Current Limitations & Prospective Future Scope")
    add_body(
        "Current limitations include desktop-only deployment, manual entry of consumption readings, and simulated payment gateways.\n"
        "Prospective future enhancements beyond the current scope include:"
    )
    add_bullet("IoT Smart Meter Integration: Connecting IoT smart meters using cellular or Wi-Fi telemetry (MQTT) to transmit consumption automatically, eliminating manual meter readings.")
    add_bullet("Cloud Database Deployment: Migrating standalone MySQL to cloud-managed clusters (AWS RDS / Azure) with HikariCP connection pooling for multi-branch scalability.")
    add_bullet("Automated SMS & Email Gateways: Incorporating communication APIs (Twilio, SendGrid) to send instant SMS alerts and PDF email invoices upon billing.")
    add_bullet("Cross-Platform Mobile Apps: Developing RESTful Spring Boot backend APIs to support companion mobile applications (Android/Flutter) for consumer bill payment.")
    add_bullet("Consumption Trend Analysis: Deploying machine learning predictive models to analyze seasonal consumption trends and forecast peak-demand hours.")

    doc.add_page_break()
    add_ch_heading("REFERENCES")
    refs = [
        "Oracle Corporation, \"Java Platform, Standard Edition (Java SE) 17 Documentation,\" Oracle Technology Network, 2021. [Online]. Available: https://docs.oracle.com/en/java/javase/17/",
        "Oracle Corporation, \"Creating a GUI With Swing: The Java Tutorials,\" Oracle Documentation, 2021. [Online]. Available: https://docs.oracle.com/javase/tutorial/uiswing/",
        "Oracle Corporation, \"JDBC (Java Database Connectivity) Overview and Specifications,\" Oracle Technology Network, 2021. [Online]. Available: https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/",
        "MySQL AB / Oracle Corporation, \"MySQL 8.0 Reference Manual,\" MySQL Documentation Library, 2024. [Online]. Available: https://dev.mysql.com/doc/refman/8.0/en/",
        "MySQL AB / Oracle Corporation, \"MySQL Connector/J 8.0 Developer Guide,\" MySQL Documentation Library, 2024. [Online]. Available: https://dev.mysql.com/doc/connector-j/8.0/en/",
        "Apache Software Foundation, \"Apache Ant Manual and User Guide (Version 1.10.x),\" Apache Software Foundation, 2023. [Online]. Available: https://ant.apache.org/manual/",
        "E. Gamma, R. Helm, R. Johnson, and J. Vlissides, \"Design Patterns: Elements of Reusable Object-Oriented Software,\" Addison-Wesley Professional, 1994.",
        "G. Booch, J. Rumbaugh, and I. Jacobson, \"The Unified Modeling Language User Guide,\" 2nd ed., Addison-Wesley Professional, 2005.",
        "Uttar Pradesh Electricity Regulatory Commission (UPERC), \"Tariff Order for Multi-Year Distribution Tariff and True-Up,\" Regulatory Electricity Tariff Document, Lucknow, India, 2023.",
        "National Institute of Standards and Technology (NIST), \"Digital Identity Guidelines: Authentication and Lifecycle Management,\" NIST Special Publication 800-63B, U.S. Department of Commerce, 2020."
    ]
    for idx, r_text in enumerate(refs):
        p = doc.add_paragraph()
        p.paragraph_format.line_spacing = 1.15
        p.paragraph_format.space_after = Pt(3)
        r = p.add_run(f"[{idx+1}]  {r_text}")
        r.font.name = "Times New Roman"
        r.font.size = Pt(9.5)

    # Save DOCX
    output_dir = "OOM_Evaluation_Submission/01_Project_Report"
    os.makedirs(output_dir, exist_ok=True)
    docx_path = os.path.join(output_dir, "Electricity_Billing_Management_System_OOM_Report.docx")
    doc.save(docx_path)
    print(f"Report saved successfully to DOCX: {os.path.abspath(docx_path)}")
    return docx_path

if __name__ == "__main__":
    create_full_report()
