import os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.enum.text import PP_ALIGN
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE

def create_presentation():
    prs = Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)
    blank_layout = prs.slide_layouts[6]

    # Academic Palette
    BG_COLOR = RGBColor(248, 250, 252)       # Light Slate #F8FAFC
    NAVY_PRIMARY = RGBColor(30, 58, 138)     # Navy #1E3A8A
    BLUE_ACCENT = RGBColor(2, 132, 199)      # Royal Sky #0284C7
    BLUE_LIGHT = RGBColor(239, 246, 255)     # Soft blue fill #EFF6FF
    DARK_TEXT = RGBColor(15, 23, 42)         # Slate 900 #0F172A
    MUTED_TEXT = RGBColor(71, 85, 105)       # Slate 600 #475569
    CARD_BG = RGBColor(255, 255, 255)        # Pure White #FFFFFF
    CARD_BORDER = RGBColor(226, 232, 240)    # Light Border #E2E8F0
    SLATE_GRAY = RGBColor(100, 116, 139)     # Slate 500 #64748B

    def set_slide_background(slide):
        bg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
        bg.fill.solid()
        bg.fill.fore_color.rgb = BG_COLOR
        bg.line.fill.background()

    def add_header(slide, title_text, category_text):
        # Header banner
        header_bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(1.05))
        header_bar.fill.solid()
        header_bar.fill.fore_color.rgb = RGBColor(15, 23, 42)
        header_bar.line.fill.background()

        # Accent stripe
        stripe = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, Inches(1.05), Inches(13.333), Inches(0.05))
        stripe.fill.solid()
        stripe.fill.fore_color.rgb = BLUE_ACCENT
        stripe.line.fill.background()

        # Title text
        txBox = slide.shapes.add_textbox(Inches(0.8), Inches(0.12), Inches(11.7), Inches(0.85))
        tf = txBox.text_frame
        tf.word_wrap = True
        tf.margin_left = tf.margin_top = tf.margin_right = tf.margin_bottom = 0

        p0 = tf.paragraphs[0]
        p0.text = category_text.upper()
        p0.font.name = "Segoe UI"
        p0.font.size = Pt(11)
        p0.font.bold = True
        p0.font.color.rgb = RGBColor(147, 197, 253)

        p1 = tf.add_paragraph()
        p1.text = title_text
        p1.font.name = "Segoe UI"
        p1.font.size = Pt(22)
        p1.font.bold = True
        p1.font.color.rgb = RGBColor(255, 255, 255)

    def add_footer(slide, slide_num, total_slides=12):
        footer_line = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(7.05), Inches(11.733), Inches(0.015))
        footer_line.fill.solid()
        footer_line.fill.fore_color.rgb = RGBColor(203, 213, 225)
        footer_line.line.fill.background()

        txBox = slide.shapes.add_textbox(Inches(0.8), Inches(7.1), Inches(11.733), Inches(0.3))
        tf = txBox.text_frame
        tf.margin_left = tf.margin_top = tf.margin_right = tf.margin_bottom = 0

        p = tf.paragraphs[0]
        p.text = "Electricity Billing Management System  |  Object-Oriented Methodology (OOM) Project Evaluation"
        p.font.name = "Segoe UI"
        p.font.size = Pt(9.5)
        p.font.color.rgb = SLATE_GRAY

        p2 = tf.add_paragraph()
        p2.alignment = PP_ALIGN.RIGHT
        p2.text = f"Slide {slide_num} of {total_slides}"
        p2.font.name = "Segoe UI"
        p2.font.size = Pt(9.5)
        p2.font.bold = True
        p2.font.color.rgb = NAVY_PRIMARY

    def add_card(slide, left, top, width, height, bg_color=CARD_BG, border_color=CARD_BORDER):
        card = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
        card.fill.solid()
        card.fill.fore_color.rgb = bg_color
        card.line.color.rgb = border_color
        card.line.width = Pt(1.5)
        return card

    # =========================================================================
    # SLIDE 1: TITLE SLIDE
    # =========================================================================
    s1 = prs.slides.add_slide(blank_layout)
    set_slide_background(s1)

    # Top accent bar
    top_bar = s1.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(0.15))
    top_bar.fill.solid()
    top_bar.fill.fore_color.rgb = BLUE_ACCENT
    top_bar.line.fill.background()

    # Left Branding / Title Card
    add_card(s1, Inches(0.8), Inches(0.6), Inches(6.1), Inches(6.2), bg_color=RGBColor(15, 23, 42), border_color=RGBColor(30, 41, 59))
    
    t_box = s1.shapes.add_textbox(Inches(1.15), Inches(0.9), Inches(5.4), Inches(5.6))
    tf1 = t_box.text_frame
    tf1.word_wrap = True
    tf1.margin_left = tf1.margin_top = tf1.margin_right = tf1.margin_bottom = 0

    p_badge = tf1.paragraphs[0]
    p_badge.text = "OBJECT-ORIENTED METHODOLOGY (OOM) PROJECT"
    p_badge.font.name = "Segoe UI"
    p_badge.font.size = Pt(11)
    p_badge.font.bold = True
    p_badge.font.color.rgb = RGBColor(96, 165, 250)
    p_badge.space_after = Pt(14)

    p_title = tf1.add_paragraph()
    p_title.text = "Electricity Billing\nManagement System"
    p_title.font.name = "Segoe UI"
    p_title.font.size = Pt(32)
    p_title.font.bold = True
    p_title.font.color.rgb = RGBColor(255, 255, 255)
    p_title.space_after = Pt(14)

    p_sub = tf1.add_paragraph()
    p_sub.text = "A desktop utility management application developed in Core Java using Swing/AWT and MySQL database connectivity via JDBC, demonstrating foundational OOP principles: Encapsulation, Inheritance, Polymorphism, and Abstraction."
    p_sub.font.name = "Segoe UI"
    p_sub.font.size = Pt(12)
    p_sub.font.color.rgb = RGBColor(203, 213, 225)
    p_sub.space_after = Pt(26)

    # Tech badges
    p_tech = tf1.add_paragraph()
    p_tech.text = "Core Java (JDK 17)  •  Java Swing & AWT  •  JDBC  •  MySQL  •  Apache Ant"
    p_tech.font.name = "Segoe UI"
    p_tech.font.size = Pt(10.5)
    p_tech.font.bold = True
    p_tech.font.color.rgb = RGBColor(56, 189, 248)

    # Right Card: Student Group & Academic Details
    add_card(s1, Inches(7.15), Inches(0.6), Inches(5.38), Inches(6.2), bg_color=CARD_BG, border_color=CARD_BORDER)

    # Section 1: Submitted By
    g_box = s1.shapes.add_textbox(Inches(7.45), Inches(0.85), Inches(4.78), Inches(2.75))
    gtf = g_box.text_frame
    gtf.word_wrap = True
    gtf.margin_left = gtf.margin_top = gtf.margin_right = gtf.margin_bottom = 0

    p_gh = gtf.paragraphs[0]
    p_gh.text = "SUBMITTED BY:"
    p_gh.font.name = "Segoe UI"
    p_gh.font.size = Pt(12)
    p_gh.font.bold = True
    p_gh.font.color.rgb = NAVY_PRIMARY
    p_gh.space_after = Pt(12)

    group_members = [
        "Mayur Jadhav – IIT2025202",
        "Venkatesh Sarkate – IIT2025176",
        "Pawan Yadav – IIT2025174"
    ]

    for member in group_members:
        p_m = gtf.add_paragraph()
        p_m.text = f"•  {member}"
        p_m.font.name = "Segoe UI"
        p_m.font.size = Pt(11.5)
        p_m.font.bold = True
        p_m.font.color.rgb = DARK_TEXT
        p_m.space_after = Pt(10)

    # Divider line
    div_line = s1.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(7.45), Inches(3.68), Inches(4.78), Inches(0.015))
    div_line.fill.solid()
    div_line.fill.fore_color.rgb = RGBColor(226, 232, 240)
    div_line.line.fill.background()

    # Section 2: Academic Details
    a_box = s1.shapes.add_textbox(Inches(7.45), Inches(3.88), Inches(4.78), Inches(2.75))
    atf = a_box.text_frame
    atf.word_wrap = True
    atf.margin_left = atf.margin_top = atf.margin_right = atf.margin_bottom = 0

    p_ah = atf.paragraphs[0]
    p_ah.text = "ACADEMIC DETAILS:"
    p_ah.font.name = "Segoe UI"
    p_ah.font.size = Pt(12)
    p_ah.font.bold = True
    p_ah.font.color.rgb = NAVY_PRIMARY
    p_ah.space_after = Pt(12)

    acad_items = [
        ("Institute:", "Indian Institute of Information Technology, Allahabad (IIITA)"),
        ("Degree:", "B.Tech"),
        ("Branch:", "Information Technology (IT)"),
        ("Section:", "B"),
        ("Faculty Guide:", "Dr. KM Pooja")
    ]

    for label, val in acad_items:
        p_ac = atf.add_paragraph()
        p_ac.text = f"•  {label} "
        p_ac.font.name = "Segoe UI"
        p_ac.font.size = Pt(11)
        p_ac.font.bold = True
        p_ac.font.color.rgb = DARK_TEXT

        r_val = p_ac.add_run()
        r_val.text = val
        r_val.font.bold = (label == "Faculty Guide:")
        r_val.font.color.rgb = NAVY_PRIMARY if label == "Faculty Guide:" else MUTED_TEXT
        p_ac.space_after = Pt(7)

    # =========================================================================
    # SLIDE 2: PROJECT INTRODUCTION
    # =========================================================================
    s2 = prs.slides.add_slide(blank_layout)
    set_slide_background(s2)
    add_header(s2, "Project Introduction", "Overview & Motivation")
    add_footer(s2, 2)

    col_w = Inches(3.75)
    gap = Inches(0.24)
    y_top = Inches(1.3)
    card_h = Inches(5.5)

    # Card 1: What the System is
    add_card(s2, Inches(0.8), y_top, col_w, card_h)
    c1_box = s2.shapes.add_textbox(Inches(1.0), y_top + Inches(0.2), col_w - Inches(0.4), card_h - Inches(0.4))
    c1_tf = c1_box.text_frame
    c1_tf.word_wrap = True
    c1_tf.margin_left = c1_tf.margin_top = c1_tf.margin_right = c1_tf.margin_bottom = 0

    p = c1_tf.paragraphs[0]
    p.text = "What is the System?"
    p.font.name = "Segoe UI"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = NAVY_PRIMARY
    p.space_after = Pt(14)

    bullets1 = [
        "A desktop software application developed in Core Java using Swing/AWT and a MySQL database connected via JDBC.",
        "Manages core electricity billing tasks: customer registration, meter details, unit consumption calculation, and bill payment.",
        "Provides role-based access with separate dashboards for Administrative staff and Customer self-service.",
        "Uses a normalized relational database schema with primary and foreign key constraints."
    ]
    for b in bullets1:
        pb = c1_tf.add_paragraph()
        pb.text = "• " + b
        pb.font.name = "Segoe UI"
        pb.font.size = Pt(11)
        pb.font.color.rgb = DARK_TEXT
        pb.space_after = Pt(12)

    # Card 2: Why it is Needed
    add_card(s2, Inches(0.8) + col_w + gap, y_top, col_w, card_h)
    c2_box = s2.shapes.add_textbox(Inches(1.0) + col_w + gap, y_top + Inches(0.2), col_w - Inches(0.4), card_h - Inches(0.4))
    c2_tf = c2_box.text_frame
    c2_tf.word_wrap = True
    c2_tf.margin_left = c2_tf.margin_top = c2_tf.margin_right = c2_tf.margin_bottom = 0

    p = c2_tf.paragraphs[0]
    p.text = "Why an Automated System is Needed"
    p.font.name = "Segoe UI"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = NAVY_PRIMARY
    p.space_after = Pt(14)

    bullets2 = [
        "Manual paper ledgers frequently suffer from arithmetic mistakes, misplaced records, and billing delays.",
        "Electricity utility offices need a structured, centralized tool to manage consumer accounts, tariffs, and meter numbers.",
        "Customers need clear visibility into their monthly consumed units, itemized charges, and payment status.",
        "Centralized database records prevent duplicate entries and keep billing status consistent across operations."
    ]
    for b in bullets2:
        pb = c2_tf.add_paragraph()
        pb.text = "• " + b
        pb.font.name = "Segoe UI"
        pb.font.size = Pt(11)
        pb.font.color.rgb = DARK_TEXT
        pb.space_after = Pt(12)

    # Card 3: Desktop Architecture Advantages
    add_card(s2, Inches(0.8) + (col_w + gap)*2, y_top, col_w, card_h)
    c3_box = s2.shapes.add_textbox(Inches(1.0) + (col_w + gap)*2, y_top + Inches(0.2), col_w - Inches(0.4), card_h - Inches(0.4))
    c3_tf = c3_box.text_frame
    c3_tf.word_wrap = True
    c3_tf.margin_left = c3_tf.margin_top = c3_tf.margin_right = c3_tf.margin_bottom = 0

    p = c3_tf.paragraphs[0]
    p.text = "Desktop Application Architecture"
    p.font.name = "Segoe UI"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = NAVY_PRIMARY
    p.space_after = Pt(14)

    bullets3 = [
        "Interactive Swing GUI: Uses standard Java Swing components (JFrame, JPanel, JTable) for clear, responsive user navigation.",
        "Direct Invoice Printing: Integrates with the standard Java Print API to print itemized bills and payment receipts.",
        "Local Database Reliability: Connects directly to the MySQL database server over localhost or a local area network (LAN).",
        "Layered Code Structure: Clearly separates user interface forms (Swing), database connectivity (Conn.java), and business services."
    ]
    for b in bullets3:
        pb = c3_tf.add_paragraph()
        pb.text = "• " + b
        pb.font.name = "Segoe UI"
        pb.font.size = Pt(11)
        pb.font.color.rgb = DARK_TEXT
        pb.space_after = Pt(12)

    # =========================================================================
    # SLIDE 3: PROBLEM STATEMENT
    # =========================================================================
    s3 = prs.slides.add_slide(blank_layout)
    set_slide_background(s3)
    add_header(s3, "Problem Statement", "Challenges in Conventional Utility Billing")
    add_footer(s3, 3)

    qw = Inches(5.75)
    qh = Inches(2.65)
    qy1 = Inches(1.35)
    qy2 = Inches(4.2)
    qx1 = Inches(0.8)
    qx2 = Inches(6.78)

    quadrants = [
        (qx1, qy1, "1. Calculation Errors & Tariff Complexity", [
            "Manual computation of stepped tariff rates, meter rent, and fixed taxes often causes mathematical mistakes.",
            "Calculation discrepancies lead to customer billing disputes and delayed payments."
        ], RGBColor(239, 68, 68)),
        
        (qx2, qy1, "2. Slow Record Keeping & Physical Misplacement", [
            "Maintaining physical paper registers causes noticeable delays between meter reading and bill distribution.",
            "Paper records are vulnerable to physical damage or misplacement, without search or filtering options."
        ], RGBColor(245, 158, 11)),

        (qx1, qy2, "3. Lack of Consumer Transparency", [
            "Consumers cannot easily inspect their billing history, previous units consumed, or pending dues.",
            "Lack of instant payment options requires consumers to visit physical utility offices to settle bills."
        ], RGBColor(59, 130, 246)),

        (qx2, qy2, "4. Access Control & Grievance Auditing", [
            "Manual record books lack role-based access controls to safeguard customer information.",
            "Customer complaints submitted informally lack systematic tracking, status updates, or audit histories."
        ], RGBColor(139, 92, 246))
    ]

    for qx, qy, qtitle, qitems, qcolor in quadrants:
        add_card(s3, qx, qy, qw, qh)
        badge = s3.shapes.add_shape(MSO_SHAPE.RECTANGLE, qx, qy, qw, Inches(0.08))
        badge.fill.solid()
        badge.fill.fore_color.rgb = qcolor
        badge.line.fill.background()

        q_box = s3.shapes.add_textbox(qx + Inches(0.25), qy + Inches(0.2), qw - Inches(0.5), qh - Inches(0.3))
        qtf = q_box.text_frame
        qtf.word_wrap = True
        qtf.margin_left = qtf.margin_top = qtf.margin_right = qtf.margin_bottom = 0

        p = qtf.paragraphs[0]
        p.text = qtitle
        p.font.name = "Segoe UI"
        p.font.size = Pt(14)
        p.font.bold = True
        p.font.color.rgb = NAVY_PRIMARY
        p.space_after = Pt(8)

        for item in qitems:
            pb = qtf.add_paragraph()
            pb.text = "• " + item
            pb.font.name = "Segoe UI"
            pb.font.size = Pt(11)
            pb.font.color.rgb = DARK_TEXT
            pb.space_after = Pt(6)

    # =========================================================================
    # SLIDE 4: PROJECT OBJECTIVES
    # =========================================================================
    s4 = prs.slides.add_slide(blank_layout)
    set_slide_background(s4)
    add_header(s4, "Project Objectives", "Scope & Target Goals")
    add_footer(s4, 4)

    obj_h = Inches(0.98)
    obj_gap = Inches(0.16)
    obj_y = Inches(1.3)
    obj_w = Inches(11.733)

    objectives = [
        ("Objective 1: Automate Electricity Bill Computation",
         "Calculate monthly electricity bills automatically based on recorded units consumed, statutory taxes, and meter rent (CalculateBill.java).",
         RGBColor(37, 99, 235)),
        
        ("Objective 2: Manage Customer & Meter Hardware Records",
         "Maintain structured records of customer demographics, meter numbers, phase codes, and connection types in MySQL (NewCustomer.java, CustomerDetails.java, MeterInfo.java).",
         RGBColor(16, 185, 129)),

        ("Objective 3: Streamline Invoicing & Payment Settlement",
         "Provide instant bill lookup, simulated payment checkout, and formal printable receipts (PayBill.java, PaymentGateway.java, PaymentReceiptDialog.java).",
         RGBColor(245, 158, 11)),

        ("Objective 4: Implement Role-Based Desktop Interface",
         "Design clear Swing desktop interfaces separating Administrator controls from Customer self-service portals, with authenticated session management (Login.java, Project.java).",
         RGBColor(139, 92, 246)),

        ("Objective 5: Track Service Grievances & Operational Activity",
         "Enable consumers to file service complaints, provide an admin resolution interface, and record key system activities (SubmitComplaint.java, ComplaintManagement.java, AuditLogViewer.java).",
         RGBColor(225, 29, 72))
    ]

    for i, (otitle, odesc, ocol) in enumerate(objectives):
        cy = obj_y + i * (obj_h + obj_gap)
        add_card(s4, Inches(0.8), cy, obj_w, obj_h)

        marker = s4.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), cy, Inches(0.12), obj_h)
        marker.fill.solid()
        marker.fill.fore_color.rgb = ocol
        marker.line.fill.background()

        otx = s4.shapes.add_textbox(Inches(1.1), cy + Inches(0.12), obj_w - Inches(0.4), obj_h - Inches(0.24))
        otf = otx.text_frame
        otf.word_wrap = True
        otf.margin_left = otf.margin_top = otf.margin_right = otf.margin_bottom = 0

        p = otf.paragraphs[0]
        p.text = otitle
        p.font.name = "Segoe UI"
        p.font.size = Pt(13)
        p.font.bold = True
        p.font.color.rgb = NAVY_PRIMARY
        p.space_after = Pt(2)

        p2 = otf.add_paragraph()
        p2.text = odesc
        p2.font.name = "Segoe UI"
        p2.font.size = Pt(11)
        p2.font.color.rgb = DARK_TEXT

    # =========================================================================
    # SLIDE 5: TECHNOLOGY STACK
    # =========================================================================
    s5 = prs.slides.add_slide(blank_layout)
    set_slide_background(s5)
    add_header(s5, "Technology Stack", "Architecture, Languages & Frameworks")
    add_footer(s5, 5)

    techs = [
        ("Core Java (JDK 17)", "Application Logic & OOP Implementation", [
            "Implements core business logic, tariff computation rules, data models, and input validation.",
            "Applies object-oriented methodology: encapsulation, inheritance, polymorphism, and abstraction.",
            "Uses Java 17 features: try-with-resources for automatic resource closing and structured exception handling."
        ], RGBColor(234, 88, 12)),

        ("Java Swing & AWT", "Desktop Graphical User Interface (GUI)", [
            "Constructs desktop windows, dialogs, forms, and tables using JFrame, JPanel, JTable, and JScrollPane.",
            "Event-driven architecture with ActionListeners, MouseAdapters, and WindowListeners for user interactivity.",
            "Custom 2D graphics rendering (Graphics2D) for clean card layouts and active navigation indicators."
        ], RGBColor(37, 99, 235)),

        ("JDBC (Java Database Connectivity)", "Database Persistence Layer", [
            "Bridges Java application code with MySQL database using com.mysql.cj.jdbc.Driver.",
            "Centralized Conn.java utility manages database connections using DriverManager.getConnection().",
            "Uses PreparedStatement to execute parameterized SQL queries and safely prevent SQL injection."
        ], RGBColor(16, 185, 129)),

        ("MySQL Database", "Relational Data Storage", [
            "Relational database storing tables: customer, login, meter_info, bill, tax, and operational support tables.",
            "Enforces referential integrity through foreign key constraints linked to meter_no.",
            "Supports transactional commit and rollback for multi-step database writes."
        ], RGBColor(2, 132, 199)),

        ("Apache Ant & NetBeans IDE", "Build Automation & Development", [
            "Standard XML-based build configuration (build.xml) orchestrating compilation, packaging, and execution.",
            "Manages external classpath dependencies including the MySQL Connector/J library (mysql-connector-java.jar).",
            "Standard project structure compatible with NetBeans IDE and command-line execution."
        ], RGBColor(124, 58, 237))
    ]

    card_w = Inches(11.733)
    c_h = Inches(0.98)
    c_gap = Inches(0.16)
    c_y = Inches(1.3)

    for i, (tname, trole, tpts, tcol) in enumerate(techs):
        curr_y = c_y + i * (c_h + c_gap)
        add_card(s5, Inches(0.8), curr_y, card_w, c_h)

        pill = s5.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), curr_y + Inches(0.15), Inches(2.4), Inches(0.68))
        pill.fill.solid()
        pill.fill.fore_color.rgb = BLUE_LIGHT
        pill.line.color.rgb = tcol
        pill.line.width = Pt(1.5)

        ptx = s5.shapes.add_textbox(Inches(0.95), curr_y + Inches(0.18), Inches(2.3), Inches(0.6))
        ptf = ptx.text_frame
        ptf.word_wrap = True
        ptf.margin_left = ptf.margin_top = ptf.margin_right = ptf.margin_bottom = 0
        p = ptf.paragraphs[0]
        p.text = tname
        p.font.name = "Segoe UI"
        p.font.size = Pt(11)
        p.font.bold = True
        p.font.color.rgb = tcol
        p.alignment = PP_ALIGN.CENTER
        p2 = ptf.add_paragraph()
        p2.text = trole
        p2.font.name = "Segoe UI"
        p2.font.size = Pt(8.5)
        p2.font.color.rgb = MUTED_TEXT
        p2.alignment = PP_ALIGN.CENTER

        rtx = s5.shapes.add_textbox(Inches(3.5), curr_y + Inches(0.1), card_w - Inches(2.8), Inches(0.8))
        rtf = rtx.text_frame
        rtf.word_wrap = True
        rtf.margin_left = rtf.margin_top = rtf.margin_right = rtf.margin_bottom = 0
        
        for idx, pt in enumerate(tpts):
            p = rtf.paragraphs[0] if idx == 0 else rtf.add_paragraph()
            p.text = "• " + pt
            p.font.name = "Segoe UI"
            p.font.size = Pt(10)
            p.font.color.rgb = DARK_TEXT
            p.space_after = Pt(2)

    # =========================================================================
    # SLIDE 6: SYSTEM MODULES AND FEATURES
    # =========================================================================
    s6 = prs.slides.add_slide(blank_layout)
    set_slide_background(s6)
    add_header(s6, "System Modules & Features", "Modular Architecture & Capabilities")
    add_footer(s6, 6)

    mod_w = Inches(3.75)
    mod_h = Inches(2.65)
    my1 = Inches(1.35)
    my2 = Inches(4.2)
    mx1 = Inches(0.8)
    mx2 = Inches(4.78)
    mx3 = Inches(8.78)

    modules = [
        (mx1, my1, "1. Authentication & Session Module", [
            "Classes: Login.java, Signup.java, UserSession.java, SessionContext.java",
            "Dual role-based authorization for Admin and Customer.",
            "Password security using PBKDF2WithHmacSHA256 password hashing.",
            "Tracks active user session state in memory across windows."
        ]),
        (mx2, my1, "2. Customer & Meter Management", [
            "Classes: NewCustomer.java, CustomerDetails.java, MeterInfo.java",
            "Customer onboarding with unique meter number assignment.",
            "Stores meter installation type, phase code, and location.",
            "View, search, filter, and print customer records."
        ]),
        (mx3, my1, "3. Billing & Calculation Engine", [
            "Classes: CalculateBill.java, BillDetails.java, GenerateBill.java",
            "Computes total bill: Units * Rate + Taxes + Meter Rent.",
            "Generates formatted, printable invoice receipts.",
            "Allows inspecting full monthly billing history per account."
        ]),
        (mx1, my2, "4. Payment Gateway & Receipts", [
            "Classes: PayBill.java, PaymentGateway.java, PaymentReceiptDialog.java",
            "Displays pending bill details and total payable dues.",
            "Simulated payment channels: Card, UPI, and NetBanking.",
            "Updates bill status to 'Paid' and produces printable receipts."
        ]),
        (mx2, my2, "5. Complaint Management Module", [
            "Classes: SubmitComplaint.java, ComplaintManagement.java, MyComplaints.java",
            "Enables customers to submit complaints with reference tracking.",
            "Admin interface for reviewing complaints and updating resolution status.",
            "Maintains complaint history in complaint_history table."
        ]),
        (mx3, my2, "6. Analytics & Audit Logging", [
            "Classes: AnalyticsDashboard.java, AuditLogViewer.java, AdminMessageHub.java",
            "Displays summary counts: total customers, revenue, and unpaid bills.",
            "Searchable and filterable audit trails with CSV export.",
            "In-app notice dispatch from Admin to Customer notification centers."
        ])
    ]

    for mx, my, mtitle, mpts in modules:
        add_card(s6, mx, my, mod_w, mod_h)
        st = s6.shapes.add_shape(MSO_SHAPE.RECTANGLE, mx, my, mod_w, Inches(0.06))
        st.fill.solid()
        st.fill.fore_color.rgb = BLUE_ACCENT
        st.line.fill.background()

        mtx = s6.shapes.add_textbox(mx + Inches(0.18), my + Inches(0.15), mod_w - Inches(0.36), mod_h - Inches(0.25))
        mtf = mtx.text_frame
        mtf.word_wrap = True
        mtf.margin_left = mtf.margin_top = mtf.margin_right = mtf.margin_bottom = 0

        p = mtf.paragraphs[0]
        p.text = mtitle
        p.font.name = "Segoe UI"
        p.font.size = Pt(13)
        p.font.bold = True
        p.font.color.rgb = NAVY_PRIMARY
        p.space_after = Pt(8)

        for pt in mpts:
            pb = mtf.add_paragraph()
            pb.text = "• " + pt
            pb.font.name = "Segoe UI"
            pb.font.size = Pt(10)
            pb.font.color.rgb = DARK_TEXT
            pb.space_after = Pt(4)

    # =========================================================================
    # SLIDE 7: OBJECT-ORIENTED PROGRAMMING CONCEPTS
    # =========================================================================
    s7 = prs.slides.add_slide(blank_layout)
    set_slide_background(s7)
    add_header(s7, "Object-Oriented Programming (OOP) Concepts", "Practical Implementation in Source Code")
    add_footer(s7, 7)

    oop_w = Inches(5.75)
    oop_h = Inches(1.7)
    ox1 = Inches(0.8)
    ox2 = Inches(6.78)
    oy1 = Inches(1.3)
    oy2 = Inches(3.15)
    oy3 = Inches(5.0)

    oops = [
        (ox1, oy1, "1. Encapsulation (Data Hiding & Protection)", [
            "Concept: Bundles fields and methods into a single unit and restricts direct external modification using private access modifiers.",
            "Project Example: SessionContext.java encapsulates user state using private final String fields (username, role, meterNo) accessible only through public getters.",
            "Project Example: UserSession.java encapsulates static session state using synchronized methods to protect user credentials from tampering."
        ]),
        (ox2, oy1, "2. Inheritance (Code Reuse & Hierarchy)", [
            "Concept: Enables a child class to inherit fields and behaviors from a parent superclass, eliminating redundant code.",
            "Project Example: All GUI screens extend javax.swing.JFrame (e.g., public class Project extends JFrame, class CustomerDetails extends JFrame, class PayBill extends JFrame).",
            "Project Example: Dialogs extend javax.swing.JDialog (e.g., public class PaymentReceiptDialog extends JDialog) acquiring standard modal behavior."
        ]),
        (ox1, oy2, "3. Polymorphism (Interfaces & Method Overriding)", [
            "Concept: Allows methods to take multiple forms; interfaces enable different classes to provide specific implementations.",
            "Project Example (Interface Polymorphism): GUI screens implement java.awt.event.ActionListener and override actionPerformed(ActionEvent ae) to handle custom button events.",
            "Project Example (Method Overriding): Project.java overrides protected void paintComponent(Graphics g) to draw dynamic active navigation indicator bars."
        ]),
        (ox2, oy2, "4. Abstraction (Hiding Complex Implementation Details)", [
            "Concept: Exposes only essential features while hiding the complex underlying implementation from the caller.",
            "Project Example: Conn.java abstracts low-level JDBC driver registration, URL construction, and connection handling behind a concise new Conn() constructor.",
            "Project Example: Service classes (AuditService.java, ComplaintService.java) provide clean public APIs (logAsync(), submitComplaint()), hiding complex SQL operations."
        ]),
        (ox1, oy3, "5. Classes and Objects (Real-World Entity Modeling)", [
            "Concept: Classes serve as blueprints for real-world entities, and objects are discrete runtime instances holding specific state.",
            "Project Example: Real-world billing entities are modeled into dedicated classes (NewCustomer, MeterInfo, CalculateBill, PayBill, PaymentGateway).",
            "Project Example: Instances are dynamically initialized and managed at runtime (e.g., new NewCustomer(), new PaymentGateway(...), new Conn())."
        ])
    ]

    for ox, oy, otitle, oitems in oops[:4]:
        add_card(s7, ox, oy, oop_w, oop_h)
        otx = s7.shapes.add_textbox(ox + Inches(0.2), oy + Inches(0.12), oop_w - Inches(0.4), oop_h - Inches(0.2))
        otf = otx.text_frame
        otf.word_wrap = True
        otf.margin_left = otf.margin_top = otf.margin_right = otf.margin_bottom = 0

        p = otf.paragraphs[0]
        p.text = otitle
        p.font.name = "Segoe UI"
        p.font.size = Pt(12.5)
        p.font.bold = True
        p.font.color.rgb = NAVY_PRIMARY
        p.space_after = Pt(4)

        for item in oitems:
            pb = otf.add_paragraph()
            pb.text = "• " + item
            pb.font.name = "Segoe UI"
            pb.font.size = Pt(9.5)
            pb.font.color.rgb = DARK_TEXT
            pb.space_after = Pt(2)

    # 5th item spans across bottom
    add_card(s7, ox1, oy3, Inches(11.733), Inches(1.85))
    otx5 = s7.shapes.add_textbox(ox1 + Inches(0.2), oy3 + Inches(0.12), Inches(11.333), Inches(1.6))
    otf5 = otx5.text_frame
    otf5.word_wrap = True
    otf5.margin_left = otf5.margin_top = otf5.margin_right = otf5.margin_bottom = 0

    p5 = otf5.paragraphs[0]
    p5.text = oops[4][2]
    p5.font.name = "Segoe UI"
    p5.font.size = Pt(12.5)
    p5.font.bold = True
    p5.font.color.rgb = NAVY_PRIMARY
    p5.space_after = Pt(4)

    for item in oops[4][3]:
        pb = otf5.add_paragraph()
        pb.text = "• " + item
        pb.font.name = "Segoe UI"
        pb.font.size = Pt(10)
        pb.font.color.rgb = DARK_TEXT
        pb.space_after = Pt(3)

    # =========================================================================
    # SLIDE 8: UML DIAGRAMS
    # =========================================================================
    s8 = prs.slides.add_slide(blank_layout)
    set_slide_background(s8)
    add_header(s8, "UML Diagrams", "Use Case, Class & Sequence Specifications")
    add_footer(s8, 8)

    # Top Row: Use Case Diagram (Left) & Sequence Diagram (Right)
    top_w = Inches(5.75)
    top_h = Inches(2.7)
    y_top_diag = Inches(1.25)

    # Card 1: Use Case Diagram
    add_card(s8, Inches(0.8), y_top_diag, top_w, top_h)
    u_tx = s8.shapes.add_textbox(Inches(0.95), y_top_diag + Inches(0.08), top_w - Inches(0.3), Inches(0.35))
    u_tf = u_tx.text_frame
    u_tf.margin_left = u_tf.margin_top = u_tf.margin_right = u_tf.margin_bottom = 0
    p = u_tf.paragraphs[0]
    p.text = "Use Case Diagram (Actors: Admin & Customer)"
    p.font.name = "Segoe UI"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = NAVY_PRIMARY

    if os.path.exists("generated_diagrams/use_case_diagram.png"):
        s8.shapes.add_picture("generated_diagrams/use_case_diagram.png", Inches(0.95), y_top_diag + Inches(0.42), width=top_w - Inches(0.3), height=Inches(2.18))

    # Card 2: Sequence Diagram
    add_card(s8, Inches(6.78), y_top_diag, top_w, top_h)
    seq_tx = s8.shapes.add_textbox(Inches(6.93), y_top_diag + Inches(0.08), top_w - Inches(0.3), Inches(0.35))
    seq_tf = seq_tx.text_frame
    seq_tf.margin_left = seq_tf.margin_top = seq_tf.margin_right = seq_tf.margin_bottom = 0
    p = seq_tf.paragraphs[0]
    p.text = "Sequence Diagram (Bill Payment & Settlement Flow)"
    p.font.name = "Segoe UI"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = NAVY_PRIMARY

    if os.path.exists("generated_diagrams/sequence_diagram.png"):
        s8.shapes.add_picture("generated_diagrams/sequence_diagram.png", Inches(6.93), y_top_diag + Inches(0.42), width=top_w - Inches(0.3), height=Inches(2.18))

    # Bottom Row: Class Diagram (Full Width)
    bot_w = Inches(11.733)
    bot_h = Inches(2.8)
    y_bot_diag = Inches(4.1)

    add_card(s8, Inches(0.8), y_bot_diag, bot_w, bot_h)
    cls_tx = s8.shapes.add_textbox(Inches(0.95), y_bot_diag + Inches(0.08), bot_w - Inches(0.3), Inches(0.35))
    cls_tf = cls_tx.text_frame
    cls_tf.margin_left = cls_tf.margin_top = cls_tf.margin_right = cls_tf.margin_bottom = 0
    p = cls_tf.paragraphs[0]
    p.text = "Class Diagram (GUI Hierarchy, Conn Abstraction, and OOP Structure)"
    p.font.name = "Segoe UI"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = NAVY_PRIMARY

    if os.path.exists("generated_diagrams/class_diagram.png"):
        s8.shapes.add_picture("generated_diagrams/class_diagram.png", Inches(0.95), y_bot_diag + Inches(0.42), width=bot_w - Inches(0.3), height=Inches(2.28))

    # =========================================================================
    # SLIDE 9: DATABASE DESIGN & ER DIAGRAM
    # =========================================================================
    s9 = prs.slides.add_slide(blank_layout)
    set_slide_background(s9)
    add_header(s9, "Database Design & ER Architecture", "MySQL Schema & Entity-Relationship Model")
    add_footer(s9, 9)

    # Left Column: ER Diagram
    add_card(s9, Inches(0.8), Inches(1.3), Inches(7.6), Inches(5.5))
    dtx = s9.shapes.add_textbox(Inches(1.0), Inches(1.45), Inches(7.2), Inches(0.4))
    dtf = dtx.text_frame
    dtf.margin_left = dtf.margin_top = dtf.margin_right = dtf.margin_bottom = 0
    p = dtf.paragraphs[0]
    p.text = "Relational Entity-Relationship (ER) Diagram (Database: `ebs`)"
    p.font.name = "Segoe UI"
    p.font.size = Pt(13)
    p.font.bold = True
    p.font.color.rgb = NAVY_PRIMARY

    if os.path.exists("generated_diagrams/er_diagram.png"):
        s9.shapes.add_picture("generated_diagrams/er_diagram.png", Inches(1.0), Inches(1.9), width=Inches(7.2), height=Inches(4.7))

    # Right Column: Schema Specifications & JDBC
    add_card(s9, Inches(8.65), Inches(1.3), Inches(3.88), Inches(5.5))
    rtx = s9.shapes.add_textbox(Inches(8.85), Inches(1.45), Inches(3.48), Inches(5.2))
    rtf = rtx.text_frame
    rtf.word_wrap = True
    rtf.margin_left = rtf.margin_top = rtf.margin_right = rtf.margin_bottom = 0

    p = rtf.paragraphs[0]
    p.text = "Schema & JDBC Specifications"
    p.font.name = "Segoe UI"
    p.font.size = Pt(14)
    p.font.bold = True
    p.font.color.rgb = NAVY_PRIMARY
    p.space_after = Pt(10)

    db_details = [
        "JDBC Connectivity: Conn.java registers com.mysql.cj.jdbc.Driver and connects via jdbc:mysql://localhost:3306/ebs.",
        "Master Entity: customer table uses meter_no as the primary key for all consumer operations.",
        "1-to-1 Relationships: customer has a 1:1 relation with meter_info (hardware specs) and login (credentials).",
        "1-to-Many Relationships: customer has a 1:N relationship with bill (monthly billing records) and complaints.",
        "Complaint Lifecycle: complaints maintains a 1:N relationship with complaint_history for resolution tracking.",
        "Data Integrity & Safety: Uses parameterized queries (PreparedStatement) to prevent SQL injection, and JDBC transactions with commit/rollback for multi-step updates."
    ]

    for item in db_details:
        pb = rtf.add_paragraph()
        pb.text = "• " + item
        pb.font.name = "Segoe UI"
        pb.font.size = Pt(10)
        pb.font.color.rgb = DARK_TEXT
        pb.space_after = Pt(6)

    # =========================================================================
    # SLIDE 10: APPLICATION SCREENSHOTS
    # =========================================================================
    s10 = prs.slides.add_slide(blank_layout)
    set_slide_background(s10)
    add_header(s10, "Application Screenshots", "Verified User Interface Showcase")
    add_footer(s10, 10)

    gw = Inches(3.75)
    gh = Inches(2.65)
    gy1 = Inches(1.3)
    gy2 = Inches(4.15)
    gx1 = Inches(0.8)
    gx2 = Inches(4.78)
    gx3 = Inches(8.78)

    screenshots = [
        (gx1, gy1, "1. Role-Based Login Screen", "login_screen.png", "Dual authentication interface (Admin / Customer)."),
        (gx2, gy1, "2. Administrator Dashboard", "admin_nav_dashboard_active.png", "Live summary cards and active sidebar navigation."),
        (gx3, gy1, "3. Customer Self-Service Portal", "customer_nav_dashboard_active.png", "Account overview, current meter, and pending dues."),
        (gx1, gy2, "4. Bill Calculation Interface", "calculate_bill_screen.png", "Monthly unit consumption and tariff processing."),
        (gx2, gy2, "5. Pay Bill & Checkout Gateway", "payment_gateway_screen.png", "Multi-mode payment settlement (Card, UPI, NetBanking)."),
        (gx3, gy2, "6. Official Printable Receipt", "payment_receipt_screen.png", "Itemized transaction receipt with regulatory taxes.")
    ]

    for gx, gy, stitle, spath, sdesc in screenshots:
        add_card(s10, gx, gy, gw, gh)
        
        ctx = s10.shapes.add_textbox(gx + Inches(0.12), gy + Inches(0.08), gw - Inches(0.24), Inches(0.35))
        ctf = ctx.text_frame
        ctf.word_wrap = True
        ctf.margin_left = ctf.margin_top = ctf.margin_right = ctf.margin_bottom = 0
        p = ctf.paragraphs[0]
        p.text = stitle
        p.font.name = "Segoe UI"
        p.font.size = Pt(11)
        p.font.bold = True
        p.font.color.rgb = NAVY_PRIMARY

        if os.path.exists(spath):
            s10.shapes.add_picture(spath, gx + Inches(0.12), gy + Inches(0.42), width=gw - Inches(0.24), height=Inches(1.8))

        dtx = s10.shapes.add_textbox(gx + Inches(0.12), gy + Inches(2.26), gw - Inches(0.24), Inches(0.32))
        dtf = dtx.text_frame
        dtf.word_wrap = True
        dtf.margin_left = dtf.margin_top = dtf.margin_right = dtf.margin_bottom = 0
        p = dtf.paragraphs[0]
        p.text = sdesc
        p.font.name = "Segoe UI"
        p.font.size = Pt(9)
        p.font.color.rgb = MUTED_TEXT

    # =========================================================================
    # SLIDE 11: CONCLUSION
    # =========================================================================
    s11 = prs.slides.add_slide(blank_layout)
    set_slide_background(s11)
    add_header(s11, "Conclusion", "Project Summary & OOM Evaluation Takeaways")
    add_footer(s11, 11)

    cw = Inches(5.75)
    ch = Inches(5.5)
    cy = Inches(1.3)

    # Left: What the Project Achieves
    add_card(s11, Inches(0.8), cy, cw, ch)
    ltx = s11.shapes.add_textbox(Inches(1.05), cy + Inches(0.2), cw - Inches(0.5), ch - Inches(0.4))
    ltf = ltx.text_frame
    ltf.word_wrap = True
    ltf.margin_left = ltf.margin_top = ltf.margin_right = ltf.margin_bottom = 0

    p = ltf.paragraphs[0]
    p.text = "Key Project Achievements"
    p.font.name = "Segoe UI"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = NAVY_PRIMARY
    p.space_after = Pt(14)

    achievements = [
        "Billing Automation: Replaced manual paper billing with an automated application that calculates electricity bills based on predefined tariff rates without arithmetic mistakes.",
        "End-to-End Workflow: Integrates customer registration, meter specifications, bill calculation, payment processing, and grievance tracking into a single application.",
        "Role-Based Access: Provides separate, authenticated access for Administrators and Customers using secure PBKDF2 password verification.",
        "Consistent Record Management: Uses a normalized relational MySQL database to keep customer records, bill statuses, and payments accurate and synchronized.",
        "Clean User Interface: Built an intuitive Swing desktop interface with consistent typography, active navigation highlights, and printable receipts."
    ]

    for a in achievements:
        pb = ltf.add_paragraph()
        pb.text = "• " + a
        pb.font.name = "Segoe UI"
        pb.font.size = Pt(10.5)
        pb.font.color.rgb = DARK_TEXT
        pb.space_after = Pt(8)

    # Right: OOM Concepts Demonstrated
    add_card(s11, Inches(6.78), cy, cw, ch)
    rtx = s11.shapes.add_textbox(Inches(7.05), cy + Inches(0.2), cw - Inches(0.5), ch - Inches(0.4))
    rtf = rtx.text_frame
    rtf.word_wrap = True
    rtf.margin_left = rtf.margin_top = rtf.margin_right = rtf.margin_bottom = 0

    p = rtf.paragraphs[0]
    p.text = "OOM Concepts Demonstrated"
    p.font.name = "Segoe UI"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = NAVY_PRIMARY
    p.space_after = Pt(14)

    oom_learnings = [
        "Encapsulation in Action: Demonstrated secure state encapsulation in SessionContext and UserSession using private variables and public accessor methods.",
        "Hierarchical Inheritance: Applied Swing framework inheritance by extending JFrame and JDialog across forms (Project, CustomerDetails, CalculateBill, PaymentReceiptDialog).",
        "Polymorphism & Event Handling: Implemented the ActionListener interface to handle button and menu clicks, and overridden paintComponent for custom rendering.",
        "Layered Abstraction: Separated low-level database connection logistics inside Conn.java and business operations in dedicated service classes.",
        "Software Engineering Discipline: Designed UML specifications (Use Case, Class, Sequence) and a normalized relational schema matching the actual source code."
    ]

    for o in oom_learnings:
        pb = rtf.add_paragraph()
        pb.text = "• " + o
        pb.font.name = "Segoe UI"
        pb.font.size = Pt(10.5)
        pb.font.color.rgb = DARK_TEXT
        pb.space_after = Pt(8)

    # =========================================================================
    # SLIDE 12: FUTURE SCOPE
    # =========================================================================
    s12 = prs.slides.add_slide(blank_layout)
    set_slide_background(s12)
    add_header(s12, "Future Scope", "Prospective Enhancements (Beyond Current Scope)")
    add_footer(s12, 12)

    fw = Inches(11.733)
    fh = Inches(0.98)
    fgap = Inches(0.16)
    fy = Inches(1.3)

    future_items = [
        ("1. IoT Smart Meter Integration",
         "Connect IoT smart electricity meters using standard telemetry protocols (such as MQTT) to transmit unit consumption automatically, reducing the need for manual meter readings.",
         RGBColor(37, 99, 235)),

        ("2. Cloud Database Deployment & Connection Pooling",
         "Deploy the database on a managed cloud service with connection pooling (e.g., HikariCP) to support multiple concurrent branch offices.",
         RGBColor(16, 185, 129)),

        ("3. Automated SMS & Email Notification Services",
         "Integrate external communication APIs to send automated SMS notifications and PDF invoices to consumers when bills are generated or payments are confirmed.",
         RGBColor(245, 158, 11)),

        ("4. Mobile & Web Client Applications",
         "Develop RESTful backend APIs to provide companion mobile or web applications for convenient consumer bill payment on phones and browsers.",
         RGBColor(139, 92, 246)),

        ("5. Consumption Trend Analysis & Load Forecasting",
         "Implement statistical and predictive algorithms to analyze power consumption patterns over time, helping utility managers forecast demand and detect irregularities.",
         RGBColor(225, 29, 72))
    ]

    for i, (ftitle, fdesc, fcol) in enumerate(future_items):
        curr_y = fy + i * (fh + fgap)
        add_card(s12, Inches(0.8), curr_y, fw, fh)

        marker = s12.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), curr_y, Inches(0.12), fh)
        marker.fill.solid()
        marker.fill.fore_color.rgb = fcol
        marker.line.fill.background()

        ftx = s12.shapes.add_textbox(Inches(1.1), curr_y + Inches(0.12), fw - Inches(0.4), fh - Inches(0.24))
        ftf = ftx.text_frame
        ftf.word_wrap = True
        ftf.margin_left = ftf.margin_top = ftf.margin_right = ftf.margin_bottom = 0

        p = ftf.paragraphs[0]
        p.text = ftitle
        p.font.name = "Segoe UI"
        p.font.size = Pt(13)
        p.font.bold = True
        p.font.color.rgb = NAVY_PRIMARY
        p.space_after = Pt(3)

        p2 = ftf.add_paragraph()
        p2.text = fdesc
        p2.font.name = "Segoe UI"
        p2.font.size = Pt(10.5)
        p2.font.color.rgb = DARK_TEXT

    # Output path
    output_dir = "OOM_Evaluation_Submission/02_Presentation"
    os.makedirs(output_dir, exist_ok=True)
    output_pptx = os.path.join(output_dir, "Electricity_Billing_Management_System_OOM.pptx")
    prs.save(output_pptx)
    print(f"Presentation saved successfully to: {os.path.abspath(output_pptx)}")
    return output_pptx

if __name__ == "__main__":
    create_presentation()
