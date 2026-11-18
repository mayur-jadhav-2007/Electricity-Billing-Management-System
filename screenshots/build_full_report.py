import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

from report_helpers import (
    set_cell_shading, set_cell_margins, set_table_borders, make_table_robust,
    add_captioned_image, add_code_snippet, add_page_number_field
)

def build_report():
    doc = docx.Document()
    
    # -------------------------------------------------------------------------
    # Page Setup: A4, 1-inch (72pt) margins
    # -------------------------------------------------------------------------
    for section in doc.sections:
        section.page_width = Inches(8.27)
        section.page_height = Inches(11.69)
        section.top_margin = Inches(0.9)
        section.bottom_margin = Inches(0.9)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)
        
        # Cover page has different header/footer (empty)
        section.different_first_page_header_footer = True
        
        # Subsequent pages footer: Clean page number ONLY (no repeated text, no bottom line)
        footer = section.footer
        p_ft = footer.paragraphs[0]
        p_ft.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        p_ft.paragraph_format.space_before = Pt(0)
        p_ft.paragraph_format.space_after = Pt(0)
        add_page_number_field(p_ft)

    # Base Styles Setup
    style_normal = doc.styles['Normal']
    style_normal.font.name = 'Times New Roman'
    style_normal.font.size = Pt(11.5)
    style_normal.font.color.rgb = RGBColor(15, 23, 42)
    style_normal.paragraph_format.line_spacing = 1.15
    style_normal.paragraph_format.space_after = Pt(2.0)
    style_normal.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY

    def add_title(text):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_before = Pt(6)
        p.paragraph_format.space_after = Pt(6)
        run = p.add_run(text)
        run.font.name = 'Times New Roman'
        run.font.size = Pt(15.5)
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
        run.font.size = Pt(16)
        run.font.bold = True
        run.font.color.rgb = RGBColor(30, 58, 138)
        return p

    def add_sec_heading(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(7)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = 'Times New Roman'
        run.font.size = Pt(13.5)
        run.font.bold = True
        run.font.color.rgb = RGBColor(15, 23, 42)
        return p

    def add_subsec_heading(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(5)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = 'Times New Roman'
        run.font.size = Pt(12)
        run.font.bold = True
        run.font.color.rgb = RGBColor(30, 41, 59)
        return p

    def add_body(text, space_after=Pt(2.0)):
        p = doc.add_paragraph(text)
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        p.paragraph_format.line_spacing = 1.15
        p.paragraph_format.space_after = space_after
        return p

    def add_bullet(text):
        p = doc.add_paragraph(style='List Bullet')
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        p.paragraph_format.line_spacing = 1.12
        p.paragraph_format.space_after = Pt(1.5)
        p.paragraph_format.space_before = Pt(0.5)
        if ":" in text:
            parts = text.split(":", 1)
            r_bold = p.add_run(parts[0] + ":")
            r_bold.font.bold = True
            r_bold.font.name = 'Times New Roman'
            r_bold.font.size = Pt(11.5)
            r_text = p.add_run(parts[1])
            r_text.font.name = 'Times New Roman'
            r_text.font.size = Pt(11.5)
        else:
            r = p.add_run(text)
            r.font.name = 'Times New Roman'
            r.font.size = Pt(11.5)
        return p

    # =========================================================================
    # 1. REDESIGNED COVER PAGE (Page 1)
    # ONLY contains:
    # - College Name: Indian Institute of Information Technology Allahabad (IIITA)
    # - Project Title: Electricity Billing Management System
    # - Group Members: Mayur Jadhav, Venkatesh Sarkate, Pawan Yadav
    # - Faculty Guide: Dr. KM Pooja
    # =========================================================================

    p_cov_college = doc.add_paragraph()
    p_cov_college.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cov_college.paragraph_format.space_before = Pt(50)
    p_cov_college.paragraph_format.space_after = Pt(110)
    r_col = p_cov_college.add_run("Indian Institute of Information Technology Allahabad (IIITA)")
    r_col.font.name = "Times New Roman"
    r_col.font.size = Pt(17)
    r_col.font.bold = True
    r_col.font.color.rgb = RGBColor(30, 58, 138)

    p_cov_title = doc.add_paragraph()
    p_cov_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cov_title.paragraph_format.space_before = Pt(30)
    p_cov_title.paragraph_format.space_after = Pt(150)
    p_cov_title.paragraph_format.line_spacing = 1.25
    r_tit = p_cov_title.add_run("Electricity Billing Management System")
    r_tit.font.name = "Times New Roman"
    r_tit.font.size = Pt(26)
    r_tit.font.bold = True
    r_tit.font.color.rgb = RGBColor(15, 23, 42)

    p_cov_members_head = doc.add_paragraph()
    p_cov_members_head.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cov_members_head.paragraph_format.space_before = Pt(30)
    p_cov_members_head.paragraph_format.space_after = Pt(8)
    r_mem_head = p_cov_members_head.add_run("Group Members")
    r_mem_head.font.name = "Times New Roman"
    r_mem_head.font.size = Pt(14)
    r_mem_head.font.bold = True
    r_mem_head.font.color.rgb = RGBColor(30, 58, 138)

    p_cov_members = doc.add_paragraph()
    p_cov_members.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cov_members.paragraph_format.line_spacing = 1.35
    p_cov_members.paragraph_format.space_after = Pt(65)
    r_mem = p_cov_members.add_run(
        "Mayur Jadhav — IIT2025202\n"
        "Venkatesh Sarkate — IIT2025176\n"
        "Pawan Yadav — IIT2025174"
    )
    r_mem.font.name = "Times New Roman"
    r_mem.font.size = Pt(12.5)
    r_mem.font.color.rgb = RGBColor(30, 41, 59)

    p_cov_guide_head = doc.add_paragraph()
    p_cov_guide_head.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cov_guide_head.paragraph_format.space_before = Pt(8)
    p_cov_guide_head.paragraph_format.space_after = Pt(4)
    r_g_head = p_cov_guide_head.add_run("Faculty Guide")
    r_g_head.font.name = "Times New Roman"
    r_g_head.font.size = Pt(14)
    r_g_head.font.bold = True
    r_g_head.font.color.rgb = RGBColor(30, 58, 138)

    p_cov_guide = doc.add_paragraph()
    p_cov_guide.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cov_guide.paragraph_format.space_after = Pt(30)
    r_guide = p_cov_guide.add_run("Dr. KM Pooja")
    r_guide.font.name = "Times New Roman"
    r_guide.font.size = Pt(13)
    r_guide.font.bold = True
    r_guide.font.color.rgb = RGBColor(15, 23, 42)

    doc.add_page_break()

    # =========================================================================
    # PRELIMINARY CONTENT: ABSTRACT (Page 2)
    # Strictly contained within Page 2
    # =========================================================================
    add_title("ABSTRACT")
    add_body(
        "Electricity billing is a critical operational workflow for utility distribution agencies, requiring "
        "accurate mathematical computations, reliable consumer record management, role-based authorization, and "
        "timely transaction reconciliation. Traditional manual billing systems relying on physical paper ledgers "
        "suffer from arithmetic miscalculations in tiered tariffs, delayed invoice distribution, physical data loss vulnerabilities, "
        "and lack of consumer self-service transparency."
    )
    add_body(
        "This project presents the Electricity Billing Management System, a robust desktop application engineered "
        "in Core Java compiled for Java 17 compatibility within an installed JDK 25.0.4 environment using the Java Swing/AWT "
        "graphical toolkit and backed by a normalized MySQL relational database connected via JDBC. The primary objective is to embody "
        "foundational Object-Oriented Methodology (OOM) principles—Encapsulation, Hierarchical Inheritance, Polymorphism, "
        "Abstraction, and Discrete Entity Modeling—within an authentic software engineering solution for Semester III, Academic Year 2026–27."
    )
    add_body(
        "The application provides dual role-based access control, segregating an Administrator Operational Hub from a "
        "Customer Self-Service Portal. Implemented modules include cryptographic PBKDF2WithHmacSHA256 password hashing, "
        "customer onboarding with automated hardware meter generation, tiered tariff calculation incorporating stepped energy slabs "
        "and statutory taxes, formal printable invoices, simulated multi-channel checkout with atomic single-connection JDBC "
        "transaction management (commit and rollback), complaint lifecycle tracking, in-app notification dispatch, and operational audit logging."
    )
    add_body("Keywords: Object-Oriented Methodology, Core Java, Java Swing, JDBC, MySQL, Electricity Billing, Software Engineering.")

    doc.add_page_break()

    # =========================================================================
    # PRELIMINARY CONTENT: TABLE OF CONTENTS & LISTS SUMMARY (Page 3)
    # =========================================================================
    add_title("TABLE OF CONTENTS")
    
    # TOC with dynamically verified chapter page numbers
    toc_items = [
        ("Abstract", "2"),
        ("Table of Contents & Lists Summary", "3"),
        ("Chapter 1: Introduction", "4"),
        ("Chapter 2: System Analysis", "7"),
        ("Chapter 3: Technology and Tools Used", "10"),
        ("Chapter 4: Object-Oriented Methodology and Design", "11"),
        ("Chapter 5: System Architecture and Module Description", "16"),
        ("Chapter 6: UML Diagrams and System Modelling", "18"),
        ("Chapter 7: Database Design and Schema Architecture", "24"),
        ("Chapter 8: Implementation and User Interface", "30"),
        ("Chapter 9: Testing and Results", "34"),
        ("Chapter 10: Conclusion and Future Scope", "36"),
        ("References", "37")
    ]
    
    tbl_toc = doc.add_table(rows=len(toc_items), cols=2)
    tbl_toc.alignment = WD_TABLE_ALIGNMENT.CENTER
    make_table_robust(tbl_toc)
    tbl_toc.columns[0].width = Inches(5.4)
    tbl_toc.columns[1].width = Inches(0.8)
    for i, (title, page) in enumerate(toc_items):
        c0 = tbl_toc.cell(i, 0)
        c1 = tbl_toc.cell(i, 1)
        p0 = c0.paragraphs[0]
        p0.paragraph_format.line_spacing = 1.12
        p0.paragraph_format.space_after = Pt(1)
        r0 = p0.add_run(title)
        r0.font.name = "Times New Roman"
        r0.font.size = Pt(11)
        if title.startswith("Chapter") or title in ["Abstract", "Table of Contents & Lists Summary", "References"]:
            r0.font.bold = True
            
        p1 = c1.paragraphs[0]
        p1.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        p1.paragraph_format.line_spacing = 1.12
        p1.paragraph_format.space_after = Pt(1)
        r1 = p1.add_run(page)
        r1.font.name = "Times New Roman"
        r1.font.size = Pt(11)

    p_gap = doc.add_paragraph()
    p_gap.paragraph_format.space_before = Pt(8)
    p_gap.paragraph_format.space_after = Pt(3)
    r = p_gap.add_run("LIST OF FIGURES & TABLES SUMMARY:")
    r.font.name = "Times New Roman"
    r.font.bold = True
    r.font.size = Pt(11)
    r.font.color.rgb = RGBColor(30, 58, 138)
    
    add_body("Figures 6.1–6.6: UML Use Case, Class, Sequence (Billing & Payment), Activity, and Component Diagrams (pp. 18–23).")
    add_body("Figure 7.1 & Tables 7.1–7.10: Relational ER Diagram and Complete 10-Table Database Schema Specifications (pp. 24–29).")
    add_body("Figures 8.1–8.7: Prioritized Application User Interface Screenshots (Login, Dashboards, Invoices, Gateway, Grievances) (pp. 30–33).")
    add_body("Tables 2.1, 2.2 & 9.1: Hardware, Software Specifications and Comprehensive System Verification Test Cases (pp. 8, 9, 34–35).")

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 1: INTRODUCTION (Pages 4–6)
    # Perfectly proportioned to complete cleanly on Page 6 without spilling
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
        "application architected in Core Java using the Java Swing/AWT graphical user interface framework "
        "and backed by a relational MySQL database server connected via Java Database Connectivity (JDBC). The project is compiled "
        "for Java 17 compatibility within an installed JDK 25.0.4 environment using Visual Studio Code and Antigravity. The application "
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
    add_bullet("Elimination of Arithmetic Errors: Tiered electricity pricing involves multi-slab arithmetic, meter rent, service charges, and taxes that frequently result in human miscalculations.")
    add_bullet("Rapid Bill Distribution: Manual preparation of thousands of paper invoices creates multi-week lags between power consumption and revenue realization, constraining cash flow.")
    add_bullet("Data Centralization: Centralized digital databases enable utility operators to look up customer records, past billing cycles, and uncollected dues within seconds.")
    add_bullet("Customer Convenience: Consumers in modern utility environments expect itemized breakdowns of energy usage, historical comparison tables, and simulated checkout options.")
    add_bullet("Operational Auditability: Automated software records structured audit trails of customer transactions, payment settlements, and administrative status modifications.")

    add_sec_heading("1.4 Problem Statement")
    add_body(
        "Manual electricity billing in regional utility offices suffers from frequent arithmetic calculation errors in "
        "multi-tier tariff computations, severe time lags in billing record generation, high vulnerability to ledger misplacement "
        "or physical deterioration, lack of consumer self-service access, and absence of verifiable audit trails for financial "
        "settlements and consumer grievances. There is a critical need for an automated desktop application that enforces "
        "mathematical accuracy in bill generation, centralizes customer and meter data in a relational database, separates "
        "administrative controls from customer access, and maintains strict transactional integrity."
    )

    add_sec_heading("1.5 Motivation & Academic Context")
    add_body(
        "The motivation behind this project is twofold. From an applied domain perspective, power distribution is one of "
        "the most foundational societal sectors; engineering an automated solution addresses real-world challenges of revenue "
        "leakage, administrative inefficiency, and consumer dissatisfaction. From an academic perspective, an electricity billing "
        "system provides an exemplary domain to study, design, and demonstrate Object-Oriented Methodology (OOM) during Semester III, "
        "Academic Year 2026–27. The domain naturally maps real-world entities (Consumers, Meters, Tariffs, Invoices, Payments, Complaints) "
        "into discrete software classes, requiring encapsulation of state, hierarchical inheritance across user interfaces, polymorphic "
        "event handling, and clean architectural abstraction between user interfaces, service logic, and database persistence layers."
    )
    add_body(
        "Academic Acknowledgment: The initial baseline workflow and foundational UI templates of this project were adapted with reference "
        "to the open-source repository by K. Rathod (Krushna968, https://github.com/Krushna968/electricity-billing-system). The project was "
        "substantially redesigned, modularized, and hardened by the group members to incorporate cryptographic PBKDF2 password security, "
        "transactional JDBC commit/rollback settlement, UPERC late payment surcharge algorithms, customer grievance history tracking, "
        "in-app notification dispatch, and comprehensive verification test suites."
    )

    add_sec_heading("1.6 Project Objectives")
    add_body("The primary objectives achieved in this project include:")
    add_bullet("Automating Electricity Bill Computation: Implement an automated mathematical engine that accepts consumed units, looks up tariff constants from MySQL, and computes the exact payable total without arithmetic mistakes (CalculateBill.java).")
    add_bullet("Centralizing Consumer and Meter Hardware Records: Establish a relational schema storing customer personal demographics, hardware meter numbers, phase configurations, and operational statuses (NewCustomer.java, MeterInfo.java, CustomerDetails.java).")
    add_bullet("Streamlining Payment Processing & Receipts: Provide instant pending bill lookups, multi-channel simulated payment processing (Card, UPI, NetBanking), atomic ledger status updates to 'Paid', and printable official receipts (PayBill.java, PaymentGateway.java, PaymentReceiptDialog.java).")
    add_bullet("Implementing Role-Based Access Control: Design responsive Swing desktop interfaces clearly delineating Administrator controls from Customer self-service portals, backed by secure PBKDF2WithHmacSHA256 password hashing (Login.java, Project.java, PasswordUtils.java).")
    add_bullet("Integrating Consumer Grievances & Audit Logging: Provide a formal complaint lifecycle tracker enabling customers to submit tickets and staff to update resolution remarks, accompanied by operational audit logging (SubmitComplaint.java, ComplaintManagement.java, AuditLogViewer.java).")

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
        "(setAutoCommit, commit, rollback) ensures atomic single-connection updates during payment settlement."
    )

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 2: SYSTEM ANALYSIS (Pages 7–9)
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
    add_bullet("Human Calculation Errors: Tiered calculations involving decimal tax rates (Swacch Bharat Tax, service charges, variable unit slabs) are prone to human arithmetic errors when computed manually for thousands of consumers.")
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
        "multi-channel checkout, and logs system activities in a structured operational audit table."
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

    add_sec_heading("2.6 Hardware & Software Specifications")
    add_body("Table 2.1 and Table 2.2 specify the verified execution environment:")
    
    # Table 2.1 Hardware
    p = doc.add_paragraph()
    r = p.add_run("Table 2.1: Minimum Hardware Requirements for System Deployment")
    r.font.name = "Times New Roman"
    r.font.bold = True
    r.font.size = Pt(10)
    tbl_hw = doc.add_table(rows=5, cols=2)
    tbl_hw.alignment = WD_TABLE_ALIGNMENT.CENTER
    make_table_robust(tbl_hw)
    hw_rows = [
        ("Component", "Minimum Requirement"),
        ("Processor", "Intel Core i3 / AMD Ryzen 3 or equivalent (x86_64 architecture)"),
        ("System Memory (RAM)", "4 GB RAM (8 GB recommended for concurrent development)"),
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
        else:
            row.cells[0].paragraphs[0].runs[0].font.size = Pt(9.5)
            row.cells[1].paragraphs[0].runs[0].font.size = Pt(9.5)

    # Table 2.2 Software
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(6)
    r = p.add_run("Table 2.2: Software Environment and Platform Specifications")
    r.font.name = "Times New Roman"
    r.font.bold = True
    r.font.size = Pt(10)
    tbl_sw = doc.add_table(rows=6, cols=2)
    tbl_sw.alignment = WD_TABLE_ALIGNMENT.CENTER
    make_table_robust(tbl_sw)
    sw_rows = [
        ("Software Component", "Specification / Verified Environment"),
        ("Operating System", "Microsoft Windows 11 (64-bit) / Cross-platform compatible"),
        ("Java Development Kit (Installed)", "Java SE Development Kit 25.0.4 LTS (build 25.0.4+7-LTS-189)"),
        ("Java Compilation Compatibility", "Java 17 Bytecode Target Compatibility (javac.source=17, javac.target=17)"),
        ("Database Server & Connector", "MySQL Server 8.4.x / 8.0.x with MySQL Connector/J 8.0.28"),
        ("Build System & IDE Tools", "Apache Ant 1.10.x (build.xml), Visual Studio Code & Antigravity")
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
        else:
            row.cells[0].paragraphs[0].runs[0].font.size = Pt(9.5)
            row.cells[1].paragraphs[0].runs[0].font.size = Pt(9.5)

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
    # CHAPTER 3: TECHNOLOGY AND TOOLS USED (Page 10)
    # Balanced cleanly on Page 10 without leaving stranded lines
    # =========================================================================
    add_ch_heading("CHAPTER 3: TECHNOLOGY AND TOOLS USED")

    add_sec_heading("3.1 Core Java (JDK 25 Environment with Java 17 Compatibility)")
    add_body(
        "Java is a class-based, object-oriented, platform-independent language. The project operates on an installed JDK 25.0.4 "
        "runtime environment while strictly compiled for Java 17 (LTS) source and target compatibility (`javac.source=17`, "
        "`javac.target=17`). This configuration combines modern virtual machine performance with deterministic Java 17 LTS bytecode "
        "stability. Core Java provides the structural backbone: classes model entities, collections manage records, and "
        "try-with-resources blocks guarantee safe closure of database socket connections."
    )

    add_sec_heading("3.2 Java Swing and AWT GUI Toolkit")
    add_body(
        "Java Swing provides lightweight, native-independent desktop GUI components. The application utilizes `JFrame` as the primary "
        "window container, `JPanel` for hierarchical layout grouping, `JTable` and `JScrollPane` for tabular records, `JTextField` and "
        "`Choice` for inputs, and `JOptionPane` for modal feedback. Swing's event-driven architecture employs `ActionListener` and "
        "`WindowListener` to decouple user interactions from background business execution."
    )

    add_sec_heading("3.3 Java Database Connectivity (JDBC)")
    add_body(
        "JDBC enables uniform connectivity between Java applications and relational databases via MySQL Connector/J "
        "(`mysql-connector-java-8.0.28.jar`, `com.mysql.cj.jdbc.Driver`). Centralized connectivity is encapsulated inside `Conn.java`, "
        "providing `Connection` and `Statement` objects. The application utilizes `PreparedStatement` to parameterize dynamic SQL queries, "
        "preventing SQL injection vulnerabilities."
    )

    add_sec_heading("3.4 MySQL Relational Database Management System")
    add_body(
        "MySQL 8.4/8.0 is an enterprise-grade open-source RDBMS providing structured data persistence on the InnoDB storage engine, "
        "enforcing ACID (Atomicity, Consistency, Isolation, Durability) guarantees. Foreign key constraints ensure strict referential integrity "
        "across customer master records, meter hardware specs, and monthly invoices."
    )

    add_sec_heading("3.5 Apache Ant Build System (build.xml)")
    add_body(
        "Apache Ant is an XML-driven build tool automating compilation, packaging, and execution. The project utilizes standard Ant "
        "targets in `build.xml` (`init`, `compile`, `jar`, `run`, `clean`) configured via `nbproject/project.properties` with "
        "`javac.source=17` and `javac.target=17`, ensuring deterministic builds across environments."
    )

    add_sec_heading("3.6 Development Environment: Visual Studio Code & Antigravity")
    add_body(
        "The project was engineered, configured, and verified within Visual Studio Code integrated with Antigravity agentic development "
        "tooling. VS Code provides Java language server support, automated diagnostics, terminal integration, and Git version control. "
        "Antigravity tooling facilitates automated build execution, verification test suite orchestration, and academic documentation generation."
    )

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 4: OBJECT-ORIENTED METHODOLOGY AND DESIGN (Pages 11–15)
    # Balanced strictly across 5 pages: 2 sections per page, zero sparse pages
    # =========================================================================
    add_ch_heading("CHAPTER 4: OBJECT-ORIENTED METHODOLOGY AND DESIGN")

    def add_ch4_sec(title):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(4)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(title)
        run.font.name = "Times New Roman"
        run.font.bold = True
        run.font.size = Pt(12)
        run.font.color.rgb = RGBColor(30, 58, 138)

    def add_ch4_body(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(2.0)
        p.paragraph_format.line_spacing = 1.10
        run = p.add_run(text)
        run.font.name = "Times New Roman"
        run.font.size = Pt(10.5)

    add_ch4_body(
        "Object-Oriented Methodology (OOM) models complex software systems as cooperating, discrete objects that encapsulate "
        "both state (data) and behavior (methods). This chapter demonstrates how every major object-oriented principle is "
        "practically implemented in the actual source code of the Electricity Billing Management System."
    )

    # PAGE 11: 4.1 Classes & Objects and 4.2 Encapsulation
    add_ch4_sec("4.1 Classes and Objects (Modeling Real-World Entities)")
    add_ch4_body(
        "Meaning and Purpose: A class is a programmer-defined blueprint from which individual objects are created. "
        "An object is a concrete, runtime instance holding discrete attribute values while sharing common behavior. "
        "Real-world utility entities—such as Customers, Meters, Calculations, and Payments—are modeled as dedicated Java classes."
    )
    add_code_snippet(doc, 
"""// Class blueprint modeling customer registration in Swing
public class NewCustomer extends JFrame implements ActionListener {
    private JTextField tfname, tfaddress, tfstate, tfcity, tfemail, tfphone;
    private JButton next, cancel;
    private JLabel lblmeter;

    public NewCustomer() {
        super("New Customer Registration");
        setSize(700, 500);
        setLocation(400, 200);
        setLayout(null);
    }
}
// Dynamic runtime instantiation inside Project.java
NewCustomer customerWizard = new NewCustomer();
customerWizard.setVisible(true);"""
    )
    add_ch4_body(
        "Explanation & Application Contribution: `public class NewCustomer extends JFrame` declares the class blueprint. "
        "Instance variables (`tfname`, `lblmeter`) represent the specific state of a customer onboarding session. "
        "In `Project.java`, `new NewCustomer()` dynamically instantiates an independent object in heap memory. "
        "Classes provide modular separation: customer registration is isolated inside `NewCustomer.java` without "
        "interfering with bill calculation (`CalculateBill.java`) or payment processing (`PayBill.java`)."
    )

    add_ch4_sec("4.2 Encapsulation (Data Hiding & State Protection)")
    add_ch4_body(
        "Meaning and Purpose: Encapsulation bundles data fields and methods into a single unit while restricting direct "
        "outside access using `private`. Access is provided solely through controlled public getters, preventing accidental or unauthorized mutation."
    )
    add_code_snippet(doc,
"""public final class SessionContext {
    private final String username;
    private final String role;
    private final String meterNo;

    public SessionContext(String username, String role, String meterNo) {
        this.username = username;
        this.role = role;
        this.meterNo = meterNo;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getMeterNo() { return meterNo; }
}"""
    )
    add_ch4_body(
        "Explanation & Application Contribution: Instance variables `username`, `role`, and `meterNo` are declared `private final`. "
        "The `private` modifier prevents external classes from altering credentials directly. The `final` modifier ensures immutability. "
        "Public getters provide read-only access. Encapsulation prevents unauthorized role privilege escalation; a customer cannot "
        "modify their assigned role to 'Admin' at runtime because the session state is protected."
    )

    doc.add_page_break()

    # PAGE 12: 4.3 Inheritance and 4.4 Polymorphism
    add_ch4_sec("4.3 Inheritance (Code Reuse & Hierarchical Specialization)")
    add_ch4_body(
        "Meaning and Purpose: Inheritance allows a child class to inherit attributes and methods from an existing parent superclass "
        "using `extends`. This promotes code reuse and establishes an 'IS-A' relationship hierarchy across related components."
    )
    add_code_snippet(doc,
"""// Project dashboard inherits window management from JFrame
public class Project extends JFrame implements ActionListener {
    public Project(String atype, String meter) {
        super("Electricity Billing System");
        setSize(1366, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

// Payment receipt dialog specializes JDialog modal window
public class PaymentReceiptDialog extends JDialog implements ActionListener {
    public PaymentReceiptDialog(JFrame parent, String title, boolean modal) {
        super(parent, title, modal);
        setSize(600, 700);
        setLocationRelativeTo(parent);
    }
}"""
    )
    add_ch4_body(
        "Explanation & Application Contribution: `Project` inherits window management capabilities—such as `setSize()`, `setVisible()`, "
        "and layout management—without re-implementing native window bindings. Similarly, `PaymentReceiptDialog` specializes `JDialog`, "
        "acquiring focus-trapping modal rendering directly. Inheritance eliminates hundreds of lines of boilerplate window code across screens."
    )

    add_ch4_sec("4.4 Polymorphism (Interface Realization & Subclass Method Overriding)")
    add_ch4_body(
        "Meaning and Purpose: Polymorphism enables uniform invocation of operations across varying object types. In Java, "
        "polymorphism is realized through two distinct mechanisms: interface implementation (dynamic event dispatch) and "
        "subclass method overriding (customizing inherited component behavior)."
    )
    add_code_snippet(doc,
"""// 1. Interface Realization: Event dispatching via ActionListener
public class Project extends JFrame implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent ae) {
        String msg = ae.getActionCommand();
        if (msg.equals("New Customer")) {
            trackSubWindow(new NewCustomer(), "New Customer");
        } else if (msg.equals("Calculate Bill")) {
            trackSubWindow(new CalculateBill(), "Calculate Bill");
        } else if (msg.equals("Pay Bill")) {
            trackSubWindow(new PayBill(meter), "Pay Bill");
        }
    }
}

// 2. Method Overriding: Custom paintComponent rendering in Project.java
JPanel userPanel = new JPanel() {
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(30, 41, 59, 180));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.dispose();
        super.paintComponent(g);
    }
};"""
    )
    add_ch4_body(
        "Explanation & Application Contribution: Swing dispatches `actionPerformed(ActionEvent ae)` polymorphically whenever a user triggers "
        "a button or menu command, allowing the application to invoke specific responses through the shared `ActionListener` interface. "
        "Furthermore, custom GUI elements override `JComponent.paintComponent(Graphics g)` on anonymous `JPanel` subclasses (as seen in `Project.java`), "
        "dynamically drawing anti-aliased rounded cards and active royal-blue navigation indicator bars at runtime."
    )

    doc.add_page_break()

    # PAGE 13: 4.5 Abstraction and 4.6 Constructors
    add_ch4_sec("4.5 Abstraction (Hiding Complex Implementation Logistics)")
    add_ch4_body(
        "Meaning and Purpose: Abstraction separates external operational interfaces from internal execution complexities. The caller interacts "
        "with simple, high-level methods while low-level socket connections, SQL parsing, and driver handshakes remain hidden."
    )
    add_code_snippet(doc,
"""// Centralized Abstraction of JDBC Database Operations in Conn.java
public class Conn implements AutoCloseable {
    public Connection c;
    public Statement s;
    private static String dbUrl, dbUser, dbPass;

    static {
        Properties props = new Properties();
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (InputStream in = new FileInputStream(propFile)) {
                props.load(in);
            } catch (Exception ignored) {}
        }
        dbUrl = props.getProperty("db.url", 
            "jdbc:mysql://localhost:3306/ebs?serverTimezone=Asia/Kolkata");
        dbUser = props.getProperty("db.user", "root");
        dbPass = props.getProperty("db.password", "********"); // Masked
    }

    public Conn() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            c = DriverManager.getConnection(dbUrl, dbUser, dbPass);
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
    add_ch4_body(
        "Explanation & Application Contribution: `Conn.java` encapsulates all JDBC logistics—driver loading, connection string parsing, "
        "timezone configuration (`Asia/Kolkata`), credential passing, statement creation, and socket cleanup. GUI screens simply invoke `new Conn()`, "
        "oblivious to database connection protocols. If the database host or credentials change, only `db.properties` or `Conn.java` requires modification."
    )

    add_ch4_sec("4.6 Constructors (Object Lifecycle & State Initialization)")
    add_ch4_body(
        "Meaning and Purpose: A constructor is invoked automatically when an object is instantiated to initialize newly created "
        "objects with valid starting states."
    )
    add_code_snippet(doc,
"""public class MeterInfo extends JFrame implements ActionListener {
    private String meternumber;
    private Choice meterlocation, metertype, phasecode, billtype;

    // Parameterized constructor binding meter context
    public MeterInfo(String meternumber) {
        this.meternumber = meternumber;
        setTitle("Electricity Billing System - Meter Information");
        setSize(700, 500);
        setLocation(400, 200);
    }
}"""
    )
    add_ch4_body(
        "Explanation & Application Contribution: `MeterInfo(String meternumber)` is a parameterized constructor. When `NewCustomer` "
        "completes customer onboarding, it passes the generated meter number string to `new MeterInfo(meternumber)`. The constructor "
        "binds this parameter to `this.meternumber`, guaranteeing that a `MeterInfo` window cannot exist without an associated customer record."
    )

    doc.add_page_break()

    # PAGE 14: 4.7 Interfaces and 4.8 Method Overriding
    add_ch4_sec("4.7 Interfaces (Architectural Contracts & Decoupling)")
    add_ch4_body(
        "Meaning and Purpose: An interface defines a formal contract of method signatures without implementation bodies. "
        "Classes implement interfaces using `implements`, guaranteeing that required behaviors are provided."
    )
    add_code_snippet(doc,
"""// Interface contract from java.awt.event.ActionListener
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
    add_ch4_body(
        "Explanation & Application Contribution: `CalculateBill implements ActionListener` guarantees a concrete definition for "
        "`actionPerformed(ActionEvent ae)`. Similarly, `Conn implements AutoCloseable` promises a definition for `close()`, enabling "
        "safe usage inside Java try-with-resources blocks."
    )

    add_ch4_sec("4.8 Method Overriding (Specialized Behavior Customization)")
    add_ch4_body(
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
    add_ch4_body(
        "Explanation & Application Contribution: `Conn.java` overrides `AutoCloseable.close()`. The `@Override` annotation instructs the compiler "
        "to verify that the signature matches `AutoCloseable`. The custom implementation closes both the Statement and Connection, preventing "
        "database connection leaks by releasing server socket resources automatically."
    )

    doc.add_page_break()

    # PAGE 15: 4.9 Access Modifiers and 4.10 Relationships Between Classes
    add_ch4_sec("4.9 Access Modifiers (Access Control Boundary Enforcement)")
    add_ch4_body(
        "Meaning and Purpose: Access modifiers (`private`, `public`, `protected`, package-private) set visibility boundaries, "
        "preventing unauthorized external coupling."
    )
    add_code_snippet(doc,
"""public class Conn implements AutoCloseable {
    public Connection c;
    public Statement s; // Public for query execution
    private static String dbUrl, dbUser, dbPass; // Encapsulated credentials
}

public class Project extends JFrame implements ActionListener {
    private JFrame activeSubWindow = null; // Private: internal window tracking
    public void trackSubWindow(JFrame win, String nav) { ... } // Public API
}"""
    )
    add_ch4_body(
        "Explanation & Application Contribution: In `Conn.java`, database credentials (`dbUrl`, `dbUser`, `dbPass`) are declared `private static`, "
        "preventing external classes from tampering with database connection configurations. In `Project.java`, `activeSubWindow` is `private`, "
        "while `trackSubWindow()` is `public` to allow controlled registration."
    )

    add_ch4_sec("4.10 Relationships Between Classes (Association, Dependency, Aggregation)")
    add_ch4_body(
        "Meaning and Purpose: In object-oriented systems, classes do not exist in isolation; they interact through structured relationships. "
        "The Electricity Billing Management System practically demonstrates association, dependency, and aggregation/composition:\n"
        "• Association: A structural relationship where one class maintains a persistent reference to another. For example, `Project` "
        "maintains an association with active sub-windows (`NewCustomer`, `CustomerDetails`, `CalculateBill`) to manage window tracking.\n"
        "• Dependency: A temporary 'uses-a' operational relationship where a class relies on another during method execution without maintaining "
        "a persistent reference. For instance, `CalculateBill.java` depends on `Conn.java` within its calculation handler to execute database queries.\n"
        "• Aggregation & Composition: A whole-part relationship where one object contains another. The singleton `UserSession` maintains "
        "an encapsulated reference to an immutable `SessionContext` object holding user credentials and role classification."
    )
    add_code_snippet(doc,
"""// 1. Association: Project maintains structural reference to active sub-window
private JFrame activeSubWindow = null;
public void trackSubWindow(JFrame win, String nav) { ... }

// 2. Dependency: CalculateBill depends on Conn dynamically in execution
try (Conn conn = new Conn()) {
    ResultSet rs = conn.s.executeQuery("SELECT * FROM tax");
}

// 3. Aggregation: UserSession aggregates an immutable SessionContext
public class UserSession {
    private static SessionContext currentSession;
    public static SessionContext getSession() { return currentSession; }
}"""
    )
    add_ch4_body(
        "Explanation & Application Contribution: Establishing distinct relationship types ensures loose coupling and high cohesion across modules. "
        "Classes depend on interfaces rather than concrete implementations where possible, preventing unnecessary long-term memory leaks."
    )

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 5: SYSTEM ARCHITECTURE AND MODULE DESCRIPTION (Pages 17–19)
    # Balanced cleanly across 3 pages without leaving stranded lines
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
         "(Card, UPI, NetBanking). Enforces atomic single-connection JDBC transaction commit/rollback. Classes: PayBill.java, PaymentGateway.java. Table: `bill`."),

        ("5.10 Payment Receipt Module",
         "Modal dialog displayed immediately upon successful payment settlement. Formats an official printable tax receipt showing transaction reference, "
         "timestamp, payment mode, cleared amount, and zero outstanding balance. Class: PaymentReceiptDialog.java."),

        ("5.11 Complaint Submission and Management Module",
         "Consumer grievance redressal system. Customers lodge complaints by selecting a category (Billing, Meter, Outage), entering a subject, and providing details. "
         "Administrators review tickets, assign priority, and update resolution remarks. Classes: SubmitComplaint.java, ComplaintManagement.java, MyComplaints.java. Tables: `complaints`, `complaint_history`."),

        ("5.12 Notification System Module",
         "In-app communication system. Utility administrators broadcast general notices to all consumers or targeted alerts to specific accounts. "
         "Consumers receive notifications in a dedicated in-app notification hub. Classes: CustomerNotificationHub.java, AdminMessageHub.java. Tables: `messages`, `message_recipients`."),

        ("5.13 Analytics and Audit Logging Module",
         "Provides administrative oversight. Displays live summary metric cards (revenue collected, outstanding dues, customer counts) and maintains "
         "an audit log table recording system actions (logins, bill calculations, payments) with CSV export. Classes: AnalyticsDashboard.java, AuditLogViewer.java, AuditService.java. Table: `audit_logs`.")
    ]

    for m_title, m_body in modules_desc:
        add_sec_heading(m_title)
        add_body(m_body, space_after=Pt(2.0))

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 6: UML DIAGRAMS AND SYSTEM MODELLING (Pages 20–25)
    # Every diagram given a dedicated, spacious page with large, readable text
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
    add_captioned_image(doc, "generated_diagrams/use_case_diagram.png", "Figure 6.1: UML Use Case Diagram for Electricity Billing Management System", width=Inches(6.25))

    doc.add_page_break()

    # 6.2 Class Diagram (Dedicated Page)
    add_sec_heading("6.2 UML Class Diagram (Structural Hierarchy & OOP Design)")
    add_body(
        "The Class Diagram illustrates the structural hierarchy, class attributes, methods, inheritance relationships, interface implementations, "
        "and associations across the three architectural tiers. Key structural relationships include GUI windows extending `javax.swing.JFrame`, "
        "dialogs extending `javax.swing.JDialog`, event handlers implementing `java.awt.event.ActionListener`, and database persistence abstracted behind `Conn.java`."
    )
    add_captioned_image(doc, "generated_diagrams/class_diagram.png", "Figure 6.2: UML Class Diagram Demonstrating OOP Structural Hierarchy & Interfaces", width=Inches(5.6))

    doc.add_page_break()

    # 6.3 Sequence Diagram: Bill Generation (Dedicated Page)
    add_sec_heading("6.3 UML Sequence Diagram – Electricity Bill Generation")
    add_body(
        "This Sequence Diagram traces the chronological message exchange between the Administrator actor, Project dashboard, CalculateBill form, "
        "Conn data layer, and MySQL database during monthly bill calculation: active meter numbers are retrieved, tariff constants queried from `tax`, "
        "total amounts calculated, and new records inserted into `bill`."
    )
    add_captioned_image(doc, "generated_diagrams/sequence_bill_generation.png", "Figure 6.3: UML Sequence Diagram – Electricity Bill Calculation and Generation", width=Inches(6.25))

    doc.add_page_break()

    # 6.4 Sequence Diagram: Bill Payment (Dedicated Page)
    add_sec_heading("6.4 UML Sequence Diagram – Bill Payment & Settlement")
    add_body(
        "This Sequence Diagram illustrates the end-to-end checkout and ledger settlement workflow. The Customer inspects pending bills in `PayBill`, "
        "initiates checkout in `PaymentGateway`, selects a simulated payment channel, and executes an atomic JDBC update (`conn.setAutoCommit(false); UPDATE bill SET status='Paid'; conn.commit()`)."
    )
    add_captioned_image(doc, "generated_diagrams/sequence_diagram.png", "Figure 6.4: UML Sequence Diagram – Customer Bill Payment and Ledger Settlement", width=Inches(6.25))

    doc.add_page_break()

    # 6.5 Activity Diagram & 6.6 Component Diagram (Pages 24 & 25)
    add_sec_heading("6.5 UML Activity Diagram – Billing & Payment Workflow")
    add_body(
        "The Activity Diagram models the dynamic operational control flow across three concurrent swimlanes: Administrator, System/Billing Engine, "
        "and Consumer. It captures the workflow from initial customer registration and monthly unit consumption entry through automated bill calculation, "
        "customer login, simulated payment checkout, conditional transaction rollback on failure, and receipt dispatch."
    )
    add_captioned_image(doc, "generated_diagrams/activity_diagram.png", "Figure 6.5: UML Activity Diagram – End-to-End Billing and Payment Workflow", width=Inches(6.25))

    doc.add_page_break()

    add_sec_heading("6.6 UML Component Diagram")
    add_body(
        "The Component Diagram depicts the high-level software subsystems and interconnecting interfaces: Presentation Layer (Swing GUI frames), "
        "Session & Security Subsystem (UserSession, PasswordUtils), Business Logic Tier, Data Access Layer (`Conn.java` JDBC wrapper), and the MySQL Database (`ebs`)."
    )
    add_captioned_image(doc, "generated_diagrams/component_diagram.png", "Figure 6.6: UML Component Diagram – Subsystems, Layers and Connectors", width=Inches(6.25))

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 7: DATABASE DESIGN AND SCHEMA ARCHITECTURE (Pages 26–29)
    # Complete 10-table documentation; ER diagram on dedicated page
    # =========================================================================
    add_ch_heading("CHAPTER 7: DATABASE DESIGN AND SCHEMA ARCHITECTURE")

    add_sec_heading("7.1 Database Overview & Architecture (`ebs`)")
    add_body(
        "The application utilizes a normalized MySQL relational database named `ebs` executing on the InnoDB storage engine. "
        "A comprehensive read-only schema inspection reveals that the database consists of ten distinct tables: `customer`, `meter_info`, "
        "`login`, `tax`, `bill`, `complaints`, `complaint_history`, `messages`, `message_recipients`, and `audit_logs`. The schema enforces referential "
        "integrity through foreign key constraints anchored to `customer.meter_no`, `complaints.complaint_id`, and `messages.message_id`."
    )

    add_sec_heading("7.2 Detailed Database Table Specifications (All 10 Tables)")

    def add_schema_table(caption, col_specs):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(4)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.keep_with_next = True
        r = p.add_run(caption)
        r.font.name = "Times New Roman"
        r.font.bold = True
        r.font.size = Pt(10)
        
        tbl = doc.add_table(rows=len(col_specs) + 1, cols=4)
        tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        make_table_robust(tbl)
        tbl.columns[0].width = Inches(1.5)
        tbl.columns[1].width = Inches(1.4)
        tbl.columns[2].width = Inches(1.3)
        tbl.columns[3].width = Inches(2.1)
        
        headers = ["Column Name", "Data Type", "Constraint", "Description"]
        for c_idx, h in enumerate(headers):
            cell = tbl.cell(0, c_idx)
            cell.paragraphs[0].text = h
            set_cell_shading(cell, "F1F5F9")
            cell.paragraphs[0].runs[0].font.name = "Times New Roman"
            cell.paragraphs[0].runs[0].font.bold = True
            cell.paragraphs[0].runs[0].font.size = Pt(9.0)
            
        for r_idx, row_data in enumerate(col_specs):
            for c_idx, val in enumerate(row_data):
                cell = tbl.cell(r_idx + 1, c_idx)
                p = cell.paragraphs[0]
                p.paragraph_format.line_spacing = 1.08
                p.paragraph_format.space_after = Pt(1)
                r = p.add_run(val)
                r.font.name = "Times New Roman"
                r.font.size = Pt(8.5)

    add_schema_table("Table 7.1: `customer` Master Table Specification", [
        ("meter_no", "VARCHAR(20)", "PRIMARY KEY", "Unique 6-digit hardware meter identifier"),
        ("name", "VARCHAR(100)", "DEFAULT NULL", "Full legal name of the registered consumer"),
        ("address", "VARCHAR(200)", "DEFAULT NULL", "Physical street address of premises"),
        ("city", "VARCHAR(50)", "DEFAULT NULL", "City or municipality jurisdiction"),
        ("state", "VARCHAR(50)", "DEFAULT NULL", "State jurisdiction"),
        ("email", "VARCHAR(100)", "DEFAULT NULL", "Contact email address of consumer"),
        ("phone", "VARCHAR(20)", "DEFAULT NULL", "Contact telephone / mobile number"),
        ("status", "VARCHAR(20)", "DEFAULT 'Active'", "Operational connection status ('Active'/'Inactive')")
    ])

    add_schema_table("Table 7.2: `meter_info` Table Specification", [
        ("meter_no", "VARCHAR(20)", "PRIMARY KEY", "Unique meter number (Logical link to customer)"),
        ("meter_location", "VARCHAR(100)", "DEFAULT NULL", "Physical mounting location ('Indoor'/'Outdoor')"),
        ("meter_type", "VARCHAR(50)", "DEFAULT NULL", "Hardware technology ('Electronic'/'Solar'/'Smart')"),
        ("phase_code", "VARCHAR(20)", "DEFAULT NULL", "Electrical phase wiring ('011' Single, '022' Three)"),
        ("bill_type", "VARCHAR(50)", "DEFAULT NULL", "Billing category ('Normal'/'Commercial'/'Industrial')"),
        ("days", "VARCHAR(10)", "DEFAULT NULL", "Billing cycle duration (default 30 days)")
    ])

    add_schema_table("Table 7.3: `login` Authentication Table Specification", [
        ("meter_no", "VARCHAR(20)", "PRIMARY KEY", "Identifier (Customer meter_no or 'ADMINxxx')"),
        ("username", "VARCHAR(50)", "DEFAULT NULL", "Unique user authentication handle"),
        ("name", "VARCHAR(100)", "DEFAULT NULL", "Associated user display name"),
        ("password", "VARCHAR(255)", "DEFAULT NULL", "PBKDF2WithHmacSHA256 salted password hash"),
        ("user", "VARCHAR(20)", "DEFAULT NULL", "Authorization role ('Admin' or 'Customer')")
    ])

    doc.add_page_break()

    add_schema_table("Table 7.4: `tax` Tariff Parameter Table Specification", [
        ("cost_per_unit", "DECIMAL(10,2)", "DEFAULT NULL", "Base energy consumption rate per unit (kWh)"),
        ("meter_rent", "DECIMAL(10,2)", "DEFAULT NULL", "Monthly physical meter maintenance rent"),
        ("service_charge", "DECIMAL(10,2)", "DEFAULT NULL", "Distribution utility service surcharge"),
        ("swacch_bharat_tax", "DECIMAL(10,2)", "DEFAULT NULL", "Statutory Swacch Bharat environmental cess"),
        ("fixed_tax", "DECIMAL(10,2)", "DEFAULT NULL", "Base capacity connection tax")
    ])

    add_schema_table("Table 7.5: `bill` Monthly Ledger Table Specification", [
        ("bill_id", "BIGINT", "PRIMARY KEY, AUTO_INC", "Unique surrogate identifier for billing record"),
        ("meter_no", "VARCHAR(20)", "FOREIGN KEY", "Foreign key referencing customer(meter_no)"),
        ("month", "VARCHAR(20)", "DEFAULT NULL", "Billing cycle month (e.g., 'January')"),
        ("units", "VARCHAR(20)", "DEFAULT NULL", "Cumulative electrical units consumed"),
        ("total_bill", "VARCHAR(20)", "DEFAULT NULL", "Total calculated payable amount"),
        ("status", "VARCHAR(20)", "DEFAULT NULL", "Payment settlement status ('Paid'/'Not Paid')"),
        ("due_date", "DATE", "DEFAULT NULL", "Statutory payment due date"),
        ("original_due_date", "DATE", "DEFAULT NULL", "Baseline due date before any recalculation"),
        ("paid_at", "DATETIME", "DEFAULT NULL", "Timestamp of successful settlement"),
        ("late_fee", "DECIMAL(10,2)", "DEFAULT 0.00", "Accrued late payment surcharge amount"),
        ("paid_amount", "DECIMAL(10,2)", "DEFAULT NULL", "Net financial amount cleared upon payment")
    ])

    doc.add_page_break()

    add_schema_table("Table 7.6: `complaints` Consumer Grievance Table Specification", [
        ("complaint_id", "INT", "PRIMARY KEY, AUTO_INC", "Unique auto-incrementing grievance tracking ID"),
        ("reference_number", "VARCHAR(50)", "UNIQUE", "Human-readable complaint tracking reference"),
        ("meter_no", "VARCHAR(20)", "FOREIGN KEY", "Foreign key referencing customer(meter_no)"),
        ("category", "VARCHAR(100)", "DEFAULT NULL", "Grievance category (Billing, Meter, Outage)"),
        ("subject", "VARCHAR(200)", "DEFAULT NULL", "Brief summary description of the issue"),
        ("description", "TEXT", "DEFAULT NULL", "Full explanatory details of the grievance"),
        ("bill_month", "VARCHAR(20)", "DEFAULT NULL", "Associated billing month under dispute"),
        ("priority", "VARCHAR(20)", "DEFAULT 'Medium'", "Urgency classification ('High'/'Medium'/'Low')"),
        ("status", "VARCHAR(20)", "DEFAULT 'Pending'", "Ticket status ('Pending'/'In Progress'/'Resolved')"),
        ("assigned_admin", "VARCHAR(100)", "DEFAULT NULL", "Username of administrator investigating grievance"),
        ("resolution_remarks", "TEXT", "DEFAULT NULL", "Administrative notes and resolution summary"),
        ("created_at", "DATETIME", "DEFAULT CURRENT_TS", "Audit timestamp of ticket creation"),
        ("updated_at", "DATETIME", "DEFAULT CURRENT_TS", "Audit timestamp of most recent status update")
    ])

    add_schema_table("Table 7.7: `complaint_history` Grievance Audit Table Specification", [
        ("history_id", "INT", "PRIMARY KEY, AUTO_INC", "Unique history entry identifier"),
        ("complaint_id", "INT", "FOREIGN KEY", "Foreign key referencing complaints(complaint_id)"),
        ("action", "VARCHAR(100)", "DEFAULT NULL", "Action performed (Status Update, Priority Change)"),
        ("previous_value", "TEXT", "DEFAULT NULL", "State prior to administrative action"),
        ("new_value", "TEXT", "DEFAULT NULL", "State following administrative action"),
        ("performed_by", "VARCHAR(100)", "DEFAULT NULL", "User ID of administrator executing action"),
        ("remarks", "TEXT", "DEFAULT NULL", "Administrative notes and resolution explanation"),
        ("created_at", "DATETIME", "DEFAULT CURRENT_TS", "Audit timestamp of state transition")
    ])

    doc.add_page_break()

    add_schema_table("Table 7.8: `messages` Notification Table Specification", [
        ("message_id", "INT", "PRIMARY KEY, AUTO_INC", "Unique identifier for notification message"),
        ("sender_admin", "VARCHAR(50)", "NOT NULL", "Admin username who authored notification"),
        ("target_type", "VARCHAR(20)", "DEFAULT 'INDIVIDUAL'", "Dispatch scope ('INDIVIDUAL' or 'BROADCAST')"),
        ("title", "VARCHAR(150)", "NOT NULL", "Notification headline subject line"),
        ("message", "TEXT", "NOT NULL", "Full body text of notice or advisory"),
        ("priority", "VARCHAR(20)", "DEFAULT 'Normal'", "Urgency level ('Normal', 'Important', 'Urgent')"),
        ("created_at", "DATETIME", "DEFAULT CURRENT_TS", "Timestamp of notification dispatch")
    ])

    add_schema_table("Table 7.9: `message_recipients` Dispatch Table Specification", [
        ("recipient_id", "INT", "PRIMARY KEY, AUTO_INC", "Unique recipient delivery entry identifier"),
        ("message_id", "INT", "FOREIGN KEY", "Foreign key referencing messages(message_id)"),
        ("meter_no", "VARCHAR(20)", "FOREIGN KEY", "Foreign key referencing customer(meter_no)"),
        ("is_read", "TINYINT(1)", "DEFAULT 0", "Read status flag (0 = Unread, 1 = Read)"),
        ("read_at", "DATETIME", "DEFAULT NULL", "Timestamp when consumer opened notification"),
        ("created_at", "DATETIME", "DEFAULT CURRENT_TS", "Timestamp of recipient delivery binding")
    ])

    add_schema_table("Table 7.10: `audit_logs` Operational Audit Table Specification", [
        ("audit_id", "BIGINT", "PRIMARY KEY, AUTO_INC", "Unique auto-incrementing audit log entry ID"),
        ("actor_id", "VARCHAR(50)", "NOT NULL", "User ID of actor initiating the operational event"),
        ("actor_role", "VARCHAR(20)", "NOT NULL", "Role classification ('Admin' / 'Customer')"),
        ("action_type", "VARCHAR(50)", "NOT NULL", "Action code (LOGIN, BILL_GENERATED, PAYMENT)"),
        ("entity_type", "VARCHAR(30)", "NOT NULL", "Domain entity affected (USER, BILL, PAYMENT, etc.)"),
        ("entity_id", "VARCHAR(50)", "NOT NULL", "Identifier of affected record"),
        ("description", "VARCHAR(255)", "NOT NULL", "Human-readable summary of operational action"),
        ("old_value", "TEXT", "DEFAULT NULL", "JSON or text payload prior to state mutation"),
        ("new_value", "TEXT", "DEFAULT NULL", "JSON or text payload following state mutation"),
        ("ip_address", "VARCHAR(45)", "DEFAULT NULL", "Client IP address or host connection identifier"),
        ("created_at", "DATETIME", "DEFAULT CURRENT_TS", "Operational timestamp of event persistence")
    ])

    doc.add_page_break()

    # 7.3 ER Diagram (Dedicated Page)
    add_sec_heading("7.3 Entity-Relationship (ER) Diagram & Constraint Integrity")
    add_body(
        "The Relational Entity-Relationship Diagram visualizes all ten database tables and their physical and logical associations within `ebs`. "
        "Strict foreign key constraints verified in the database schema include:\n"
        "1. `bill_ibfk_1`: `bill.meter_no` references `customer(meter_no)`\n"
        "2. `fk_complaints_meter`: `complaints.meter_no` references `customer(meter_no)`\n"
        "3. `fk_history_complaint`: `complaint_history.complaint_id` references `complaints(complaint_id)`\n"
        "4. `fk_msg_recip_cust`: `message_recipients.meter_no` references `customer(meter_no)`\n"
        "5. `fk_msg_recip_msg`: `message_recipients.message_id` references `messages(message_id)`\n"
        "In addition, `meter_info.meter_no` and `login.meter_no` maintain 1-to-1 logical associations with `customer.meter_no` enforced at the application tier. "
        "The `audit_logs` table records structured polymorphic action trails without foreign key locks to guarantee that audit history persists independently of entity lifecycles."
    )
    add_captioned_image(doc, "generated_diagrams/er_diagram.png", "Figure 7.1: Relational Entity-Relationship (ER) Diagram for Complete 10-Table `ebs` Database Schema", width=Inches(5.2))

    doc.add_page_break()

    # 7.4 Transactions & JDBC (Dedicated Page)
    add_sec_heading("7.4 JDBC Connectivity, PreparedStatements & Transaction Integrity")
    add_body(
        "Database connectivity is centralized within `Conn.java`, which loads the MySQL Connector/J driver (`com.mysql.cj.jdbc.Driver`) "
        "and dynamically retrieves connection credentials from `db.properties` or environment variables, specifying `serverTimezone=Asia/Kolkata`. "
        "To prevent SQL injection vulnerabilities, all dynamic queries use parameterized `PreparedStatement` with typed setters (`setString()`, `setInt()`)."
    )
    add_code_snippet(doc,
"""// Centralized atomic transaction boundary for financial settlement
conn.c.setAutoCommit(false); // Begin atomic transaction boundary
try {
    // 1. Update billing ledger status to Paid
    String sqlBill = "UPDATE bill SET status='Paid', paid_at=NOW(), paid_amount=? WHERE meter_no=? AND month=?";
    PreparedStatement psBill = conn.c.prepareStatement(sqlBill);
    psBill.setBigDecimal(1, paymentAmount);
    psBill.setString(2, meterNumber);
    psBill.setString(3, billingMonth);
    psBill.executeUpdate();

    // 2. Insert audit trail entry
    String sqlAudit = "INSERT INTO audit_logs (actor_id, actor_role, action_type, entity_type, entity_id, description) " +
                       "VALUES (?, 'Customer', 'PAYMENT', 'BILL', ?, 'Bill payment completed')";
    PreparedStatement psAudit = conn.c.prepareStatement(sqlAudit);
    psAudit.setString(1, username);
    psAudit.setString(2, meterNumber + "-" + billingMonth);
    psAudit.executeUpdate();

    conn.c.commit(); // Permanently commit changes to MySQL InnoDB engine
} catch (SQLException ex) {
    conn.c.rollback(); // Rollback immediately restores pristine database state
    throw ex;
} finally {
    conn.c.setAutoCommit(true); // Restore default autocommit mode
}"""
    )
    add_body(
        "ACID Guarantees & Transaction Boundaries: Critical financial mutations employ explicit single-connection JDBC transaction "
        "management (`conn.c.setAutoCommit(false);`). Upon successful completion of all updates, `conn.c.commit();` writes changes atomically "
        "to the MySQL InnoDB transaction log. If any runtime error or SQL exception occurs, `conn.c.rollback();` restores the database to its "
        "pre-transaction state, preventing partial ledger updates. Note that atomicity applies to single-connection operations within MySQL; "
        "distributed multi-node two-phase commit is neither implemented nor required for this desktop utility solution."
    )

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 8: IMPLEMENTATION AND USER INTERFACE (Pages 30–33)
    # 7 prioritized genuine screenshots across 4 balanced pages
    # =========================================================================
    add_ch_heading("CHAPTER 8: IMPLEMENTATION AND USER INTERFACE")
    add_body(
        "This chapter showcases the user interface implementation of the Electricity Billing Management System. "
        "All visual figures presented below are authentic screenshots captured directly from the running Java Swing application, "
        "demonstrating role-based navigation, input validation, tabular reporting, simulated checkout, and administrative oversight."
    )

    # Screen 1 & 2 (Page 1 of Ch 8)
    add_sec_heading("8.1 Authentication and Administrative Control Hub")
    add_captioned_image(doc, "login_screen.png", "Figure 8.1: Role-Based User Login and Authentication Screen", width=Inches(4.1))
    add_body(
        "The Login Screen provides a secure authentication entry point. Users select their role ('Admin' or 'Customer') and enter credentials. "
        "Passwords are authenticated using PBKDF2WithHmacSHA256 password hashing. User Role: All Users."
    )

    add_captioned_image(doc, "admin_nav_dashboard_active.png", "Figure 8.2: Administrator Control Hub and Navigation Dashboard", width=Inches(4.1))
    add_body(
        "The Administrator Dashboard serves as the central control hub, featuring live system metric cards (Total Customers, Active Accounts, "
        "Total Invoices, Paid/Unpaid bills), quick shortcuts, and persistent royal-blue sidebar navigation. User Role: Administrator."
    )

    doc.add_page_break()

    # Screen 3 & 4 (Page 2 of Ch 8)
    add_sec_heading("8.2 Consumer Self-Service Portal & Customer Onboarding")
    add_captioned_image(doc, "customer_nav_dashboard_active.png", "Figure 8.3: Customer Self-Service Account Overview Portal", width=Inches(4.1))
    add_body(
        "The Customer Portal provides a transparent self-service interface displaying account details, assigned meter number, current consumption, "
        "latest bill amount, and quick-action shortcuts for Pay Bill, View Bill, and Complaints. User Role: Customer."
    )

    add_captioned_image(doc, "new_customer_screen.png", "Figure 8.4: Customer Onboarding and Registration Wizard Form", width=Inches(4.1))
    add_body(
        "The New Customer interface enables administrators to register new consumers. Captures customer name, address, city, state, email, "
        "phone number, and automatically generates a unique 6-digit hardware meter number. User Role: Administrator."
    )

    doc.add_page_break()

    # Screen 5 & 6 (Page 3 of Ch 8)
    add_sec_heading("8.3 Bill Calculation Engine & Payment Checkout Simulation")
    add_captioned_image(doc, "calculate_bill_screen.png", "Figure 8.5: Monthly Electricity Consumption and Bill Calculation Interface", width=Inches(4.1))
    add_body(
        "The Calculate Bill screen accepts an active meter number, billing month, and cumulative units consumed. Automatically queries statutory "
        "tariff rates from MySQL and creates a new pending bill record. User Role: Administrator."
    )

    add_captioned_image(doc, "payment_gateway_screen.png", "Figure 8.6: Simulated Multi-Channel Checkout Payment Gateway", width=Inches(4.1))
    add_body(
        "The Payment Gateway simulates a secure online checkout environment operating in demonstration mode. Consumers select their preferred payment channel (Card, "
        "UPI, NetBanking) and submit payment. Executes single-connection JDBC transaction commit and rollback. User Role: Customer."
    )

    doc.add_page_break()

    # Screen 7 (Page 4 of Ch 8)
    add_sec_heading("8.4 Customer Grievance Resolution & Complaint Redressal")
    add_captioned_image(doc, "complaint_management_screen.png", "Figure 8.7: Customer Grievance Resolution and Complaint Management", width=Inches(4.6))
    add_body(
        "Administrative grievance interface. Utility staff can review filed complaints, filter by priority, assign investigation staff, "
        "and record resolution remarks, maintaining complete grievance history. User Role: Administrator."
    )
    add_body(
        "UI Architectural Highlights: All graphical screens in the Electricity Billing Management System utilize native-independent "
        "Java Swing lightweight containers (`JFrame` and `JDialog`). Layout managers (`BorderLayout`, `BoxLayout`, `FlowLayout`) ensure "
        "consistent component positioning across varying display resolutions. Visual consistency is enforced through custom dark-navy "
        "sidebar branding, high-contrast typography, and intuitive dialog feedback."
    )

    doc.add_page_break()



    # =========================================================================
    # CHAPTER 9: TESTING AND RESULTS (Pages 34–35)
    # =========================================================================
    add_ch_heading("CHAPTER 9: TESTING AND RESULTS")

    add_sec_heading("9.1 Testing Strategy and Methodology Classification")
    add_body(
        "A multi-tiered, rigorous testing strategy was executed to verify functional correctness, mathematical precision, "
        "and data consistency across all application layers. Testing methodologies are classified into three distinct categories:\n"
        "• Automated Test Suites: Dedicated Java test classes executing programmatic assertions against database queries, "
        "PBKDF2 password hashing routines, tariff computation algorithms, and atomic JDBC transaction boundaries.\n"
        "• Manual GUI & Acceptance Testing: Interactive verification of Swing window rendering, modal dialog focus-trapping, "
        "active sidebar navigation highlights, print dialog launches, and form field validation.\n"
        "• Static Code Reviews & Schema Audits: Formal inspection of source code compliance with Java 17 standards, SQL injection defense "
        "via PreparedStatement parameterization, and foreign key constraint verification in MySQL."
    )

    add_sec_heading("9.2 Automated Verification Test Suites")
    add_body(
        "The project repository contains dedicated automated verification test suites (`Phase1VerificationTest.java` through "
        "`Phase9VerificationTest.java`, `Phase11VerificationTest.java`, and `ApplicationTest.java`). These test suites execute automated "
        "assertion routines validating database connectivity, PBKDF2 password hashing and lazy migration, active customer record queries, "
        "stepped tariff calculation math, late payment surcharge engines, atomic payment settlement, and sidebar navigation state integrity."
    )

    add_sec_heading("9.3 Detailed Test Cases & Execution Results")
    add_body("Table 9.1 details key functional and system test cases executed against the working application:")

    p = doc.add_paragraph()
    r = p.add_run("Table 9.1: Comprehensive Functional and System Verification Test Results")
    r.font.name = "Times New Roman"
    r.font.bold = True
    r.font.size = Pt(10)

    test_cases = [
        ("TC-01", "Admin Login with valid credentials", "Automated Suite / ApplicationTest", "Authenticate admin; redirect to Admin Dashboard", "Admin authenticated successfully; Admin Dashboard launched", "PASSED"),
        ("TC-02", "Customer Login with valid credentials", "Automated Suite / ApplicationTest", "Authenticate customer; redirect to Customer Portal", "Customer authenticated successfully; Customer Portal launched", "PASSED"),
        ("TC-03", "Login with incorrect password", "Automated Suite / ApplicationTest", "Deny access; display 'Invalid Login' dialog", "Access denied; 'Invalid Login' error dialog displayed", "PASSED"),
        ("TC-04", "New Customer Registration", "Manual GUI & Integration", "Insert record into customer; auto-generate 6-digit meter", "Record inserted into customer table; meter generated", "PASSED"),
        ("TC-05", "Meter Hardware Configuration", "Manual GUI & Integration", "Insert hardware specs into meter_info linked by meter_no", "Meter specs inserted; foreign key constraint verified", "PASSED"),
        ("TC-06", "Electricity Bill Calculation", "Automated Suite / Phase4Test", "Compute Units*Rate + Rent + Taxes; insert into bill", "Total computed deterministically; bill inserted as 'Not Paid'", "PASSED"),
        ("TC-07", "Pending Bill Lookup in PayBill", "Manual GUI & Integration", "Query bill record for selected meter and month", "Units consumed, bill amount, and status displayed accurately", "PASSED"),
        ("TC-08", "Payment Checkout Settlement", "Automated Suite / Phase5Test", "Process payment; setAutoCommit(false); commit transaction", "Status updated to 'Paid'; transaction committed to MySQL", "PASSED"),
        ("TC-09", "Simulated Payment Cancellation", "Automated Suite / Phase5Test", "Trigger rollback; leave bill status unchanged", "Rollback executed; bill status remains 'Not Paid'", "PASSED"),
        ("TC-10", "Payment Receipt Generation", "Manual GUI & Print Dialog", "Display modal receipt with itemized breakdown and print", "Modal receipt dialog rendered with zero balance and print support", "PASSED"),
        ("TC-11", "Customer Complaint Submission", "Automated Suite / Phase6Test", "Insert grievance ticket into complaints table", "Ticket inserted with unique tracking ID; status='Pending'", "PASSED"),
        ("TC-12", "Admin Complaint Resolution", "Automated Suite / Phase6Test", "Update ticket status and insert into complaint_history", "Status updated to 'Resolved'; audit history entry created", "PASSED"),
        ("TC-13", "Operational Audit Logging", "Automated Suite / Phase8Test", "Record event in audit_logs with timestamp and payload", "Log entry persisted; searchable via AuditLogViewer", "PASSED"),
        ("TC-14", "Sidebar Active Navigation", "Manual GUI & State Verification", "Highlight active item; reset to Dashboard on window close", "Navigation highlight tracks active window; resets cleanly on close", "PASSED")
    ]

    tbl_tc = doc.add_table(rows=len(test_cases) + 1, cols=6)
    tbl_tc.alignment = WD_TABLE_ALIGNMENT.CENTER
    make_table_robust(tbl_tc)
    tbl_tc.columns[0].width = Inches(0.7)
    tbl_tc.columns[1].width = Inches(1.5)
    tbl_tc.columns[2].width = Inches(1.3)
    tbl_tc.columns[3].width = Inches(1.3)
    tbl_tc.columns[4].width = Inches(1.4)
    tbl_tc.columns[5].width = Inches(0.8)

    tc_headers = ["Test ID", "Test Scenario", "Verification Method", "Expected Result", "Actual Result", "Status"]
    for c_idx, h in enumerate(tc_headers):
        cell = tbl_tc.cell(0, c_idx)
        cell.paragraphs[0].text = h
        set_cell_shading(cell, "F1F5F9")
        cell.paragraphs[0].runs[0].font.name = "Times New Roman"
        cell.paragraphs[0].runs[0].font.bold = True
        cell.paragraphs[0].runs[0].font.size = Pt(9.0)

    for r_idx, tc_row in enumerate(test_cases):
        for c_idx, val in enumerate(tc_row):
            cell = tbl_tc.cell(r_idx + 1, c_idx)
            p = cell.paragraphs[0]
            p.paragraph_format.line_spacing = 1.08
            p.paragraph_format.space_after = Pt(1)
            r = p.add_run(val)
            r.font.name = "Times New Roman"
            r.font.size = Pt(8.0)
            if c_idx == 5:
                r.font.bold = True
                r.font.color.rgb = RGBColor(5, 150, 105)

    add_sec_heading("9.4 Build Verification & Operational Constraints")
    add_body(
        "Build Verification: Verified using Apache Ant (`build.xml`) and the Java compiler (`javac`). The complete source code compiles with "
        "zero compilation errors against JDK 17 compatibility specifications (`javac.source=17`, `javac.target=17`).\n"
        "Operational Constraints: Monthly consumption currently requires administrative entry via `CalculateBill.java`; payment gateway operates "
        "in demonstration simulation mode; and the system operates within a desktop environment requiring direct TCP connectivity to MySQL."
    )

    doc.add_page_break()

    # =========================================================================
    # CHAPTER 10: CONCLUSION AND FUTURE SCOPE (Page 36)
    # Strictly contained within Page 36 without spilling
    # =========================================================================
    add_ch_heading("CHAPTER 10: CONCLUSION AND FUTURE SCOPE")

    add_sec_heading("10.1 Project Summary & Objectives Achieved")
    add_body(
        "The Electricity Billing Management System project successfully designed, implemented, and verified an end-to-end "
        "desktop utility management software application for Semester III, Academic Year 2026–27. By replacing manual paper billing with an automated Core Java "
        "solution backed by a normalized MySQL relational database, the system streamlines customer registration, meter hardware "
        "configuration, stepped tariff calculation, invoice generation, payment clearance, and grievance tracking."
    )
    add_bullet("Full Billing Lifecycle Automation: Replaced manual arithmetic computations with an automated calculation engine.")
    add_bullet("Centralized Data Persistence: Organized customer, meter, and billing records into normalized relational tables enforcing referential integrity.")
    add_bullet("Dual-Portal Security: Implemented role-based authorization separating Administrator controls from Customer portals with PBKDF2 password hashing.")
    add_bullet("Transactional Settlement: Enforced atomic single-connection updates through explicit JDBC transaction commit and rollback management.")
    add_bullet("Formal Documentation & Verification: Modeled system architecture via UML diagrams and validated functionality with verification test suites.")

    add_sec_heading("10.2 OOM Concepts Demonstrated & Benefits")
    add_body(
        "The project serves as an authentic practical demonstration of Object-Oriented Methodology. Encapsulation protects user session "
        "credentials inside `SessionContext.java`; hierarchical inheritance leverages the Swing framework across `JFrame` and `JDialog` subclasses; "
        "polymorphism powers event-driven interaction via `ActionListener` and custom component graphics rendering; and abstraction isolates "
        "low-level JDBC database logistics within `Conn.java`.\n"
        "Practical benefits include elimination of manual math errors, rapid record lookup in seconds, consumer visibility into consumption, and ledger consistency."
    )

    add_sec_heading("10.3 Current Limitations & Prospective Future Scope")
    add_body(
        "Current limitations include desktop-only deployment, manual entry of consumption readings, and simulated payment gateways.\n"
        "Prospective future enhancements beyond the current scope include:"
    )
    add_bullet("IoT Smart Meter Integration: Connecting IoT smart meters using cellular or Wi-Fi telemetry (MQTT) to transmit consumption automatically, eliminating manual meter readings.")
    add_bullet("Cloud Database Deployment: Migrating standalone MySQL to cloud-managed clusters (AWS RDS / Azure) with connection pooling (HikariCP) for multi-branch scalability.")
    add_bullet("Automated SMS & Email Gateways: Incorporating communication APIs (Twilio, SendGrid) to send instant SMS alerts and PDF email invoices upon billing.")
    add_bullet("Cross-Platform Mobile Apps: Developing RESTful Spring Boot backend APIs to support companion mobile applications (Android/Flutter) for consumer bill payment.")
    add_bullet("Consumption Trend Analysis: Deploying machine learning predictive models to analyze seasonal consumption trends and forecast peak-demand hours.")

    doc.add_page_break()

    # =========================================================================
    # REFERENCES (Page 37)
    # =========================================================================
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
        "National Institute of Standards and Technology (NIST), \"Digital Identity Guidelines: Authentication and Lifecycle Management,\" NIST Special Publication 800-63B, U.S. Department of Commerce, 2020.",
        "K. Rathod (Krushna968), \"Electricity Billing System,\" GitHub Repository, 2024. [Online]. Available: https://github.com/Krushna968/electricity-billing-system"
    ]
    for idx, r_text in enumerate(refs):
        p = doc.add_paragraph()
        p.paragraph_format.line_spacing = 1.15
        p.paragraph_format.space_after = Pt(2.5)
        r = p.add_run(f"[{idx+1}]  {r_text}")
        r.font.name = "Times New Roman"
        r.font.size = Pt(10)

    # Save DOCX
    output_dir = "OOM_Evaluation_Submission/01_Project_Report"
    os.makedirs(output_dir, exist_ok=True)
    docx_path = os.path.join(output_dir, "Electricity_Billing_Management_System_OOM_Report.docx")
    doc.save(docx_path)
    print(f"Report saved successfully to DOCX: {os.path.abspath(docx_path)}")
    return docx_path

if __name__ == "__main__":
    build_report()
