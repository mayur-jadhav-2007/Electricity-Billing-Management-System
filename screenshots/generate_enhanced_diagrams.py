import os
from PIL import Image, ImageDraw, ImageFont

os.makedirs("generated_diagrams", exist_ok=True)

def get_font(size, bold=False):
    font_name = "segoeuib.ttf" if bold else "segoeui.ttf"
    try:
        return ImageFont.truetype(font_name, size)
    except Exception:
        try:
            fallback = "arialbd.ttf" if bold else "arial.ttf"
            return ImageFont.truetype(fallback, size)
        except Exception:
            return ImageFont.load_default()

def draw_header(draw, title, subtitle, width, h_bar=95):
    draw.rectangle([(0, 0), (width, h_bar)], fill="#0F172A")
    draw.text((35, 16), title, font=get_font(28, bold=True), fill="#FFFFFF")
    draw.text((35, 58), subtitle, font=get_font(18, bold=False), fill="#94A3B8")

def draw_box(draw, xy, fill, outline, radius=8, width=2):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)

# =========================================================================
# 1. USE CASE DIAGRAM
# =========================================================================
def generate_use_case_diagram():
    w, h = 1800, 1150
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "UML Use Case Diagram", "Electricity Billing Management System - Core Use Cases & Role Interactions", w)
    
    system_box = (380, 115, 1420, 1110)
    draw_box(draw, system_box, "#FFFFFF", "#334155", radius=14, width=3)
    draw.text((410, 130), "System Boundary: Electricity Billing Management System", font=get_font(22, bold=True), fill="#1E293B")
    
    # Left Actor: Administrator
    draw_box(draw, (35, 290, 295, 690), "#EFF6FF", "#3B82F6", radius=12, width=2)
    draw.ellipse((135, 310, 195, 370), fill="#2563EB")
    draw.line([(165, 370), (165, 480)], fill="#2563EB", width=6)
    draw.line([(105, 420), (225, 420)], fill="#2563EB", width=6)
    draw.line([(165, 480), (115, 575)], fill="#2563EB", width=6)
    draw.line([(165, 480), (215, 575)], fill="#2563EB", width=6)
    draw.text((165, 615), "<<Actor>>\nAdministrator", font=get_font(20, bold=True), fill="#1E3A8A", align="center", anchor="mm")
    draw.text((165, 660), "(Utility Staff)", font=get_font(16, bold=False), fill="#475569", anchor="mm")

    # Right Actor: Customer
    draw_box(draw, (1505, 290, 1765, 690), "#F0FDF4", "#10B981", radius=12, width=2)
    draw.ellipse((1605, 310, 1665, 370), fill="#059669")
    draw.line([(1635, 370), (1635, 480)], fill="#059669", width=6)
    draw.line([(1575, 420), (1695, 420)], fill="#059669", width=6)
    draw.line([(1635, 480), (1585, 575)], fill="#059669", width=6)
    draw.line([(1635, 480), (1685, 575)], fill="#059669", width=6)
    draw.text((1635, 615), "<<Actor>>\nCustomer", font=get_font(20, bold=True), fill="#065F46", align="center", anchor="mm")
    draw.text((1635, 660), "(Electricity Consumer)", font=get_font(16, bold=False), fill="#475569", anchor="mm")

    use_cases = [
        ("UC-01: Login & Dual-Role Authentication", 195, "#DBEAFE", "#1D4ED8", True, True),
        ("UC-02: Register Customer & Meter Hardware Specs", 290, "#EFF6FF", "#2563EB", True, False),
        ("UC-03: Manage Tariffs, Taxes & Surcharges", 385, "#EFF6FF", "#2563EB", True, False),
        ("UC-04: Calculate Monthly Bill & Surcharges", 480, "#EFF6FF", "#2563EB", True, False),
        ("UC-05: View Account Details, Bills & Ledger", 575, "#F0FDF4", "#059669", False, True),
        ("UC-06: Pay Bill via Multi-Channel Gateway", 670, "#F0FDF4", "#059669", False, True),
        ("UC-07: Generate & Print Official Tax Receipts", 765, "#EFF6FF", "#2563EB", True, True),
        ("UC-08: Submit, Track & Resolve Grievances", 860, "#F0FDF4", "#059669", True, True),
        ("UC-09: Dispatch Alerts & Notifications", 955, "#EFF6FF", "#2563EB", True, True),
        ("UC-10: View Audit Logs & Analytics Dashboard", 1045, "#EFF6FF", "#2563EB", True, False),
    ]

    for uc_text, y_pos, f_col, o_col, admin_link, cust_link in use_cases:
        draw.ellipse([(520, y_pos - 34), (1280, y_pos + 34)], fill=f_col, outline=o_col, width=2)
        draw.text((900, y_pos), uc_text, font=get_font(18, bold=True), fill="#0F172A", anchor="mm")
        
        if admin_link:
            draw.line([(295, 490), (520, y_pos)], fill="#93C5FD", width=2)
        if cust_link:
            draw.line([(1505, 490), (1280, y_pos)], fill="#6EE7B7", width=2)

    img.save("generated_diagrams/use_case_diagram.png")
    print("Saved use_case_diagram.png")

# =========================================================================
# 2. CLASS DIAGRAM (High Readability, Large Fonts, Spacious Layout)
# =========================================================================
def generate_class_diagram():
    # Portrait aspect ratio: Width 1600, Height 1850 perfectly fits A4 page with heading & caption
    w, h = 1600, 1850
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "UML Class Diagram", "Electricity Billing Management System - Structural Hierarchy, OOP Relationships & Interfaces", w, h_bar=90)
    
    def draw_class(x, y, w_box, name, stereotype, attrs, methods, header_col="#1E3A8A"):
        h_head = 66
        b_h = 12 + len(attrs)*24 + (10 if attrs else 0) + len(methods)*24 + 10
        tot_h = h_head + b_h
        
        draw.rounded_rectangle([(x, y), (x + w_box, y + tot_h)], radius=10, fill="#FFFFFF", outline="#334155", width=3)
        draw.rounded_rectangle([(x, y), (x + w_box, y + h_head)], radius=10, fill=header_col, outline="#334155", width=3)
        draw.rectangle([(x, y + h_head - 10), (x + w_box, y + h_head)], fill=header_col)
        
        draw.text((x + w_box/2, y + 20), stereotype, font=get_font(16, bold=False), fill="#93C5FD", anchor="mm")
        draw.text((x + w_box/2, y + 46), name, font=get_font(23, bold=True), fill="#FFFFFF", anchor="mm")
        
        cur_y = y + h_head + 10
        for attr in attrs:
            draw.text((x + 16, cur_y), attr, font=get_font(18, bold=False), fill="#1E293B")
            cur_y += 24
        if attrs:
            draw.line([(x, cur_y + 3), (x + w_box, cur_y + 3)], fill="#CBD5E1", width=2)
            cur_y += 8
        for m in methods:
            draw.text((x + 16, cur_y), m, font=get_font(18, bold=False), fill="#0F172A")
            cur_y += 24
        return tot_h

    # ROW 1: Architecture Core & Session Security (Y = 110)
    draw_class(35, 110, 490, "Project", "<<extends JFrame, implements ActionListener>>",
               ["- String atype, meter", "- JPanel sidebar, mainContent", "- JFrame activeSubWindow"],
               ["+ Project(String atype, String meter)", "+ trackSubWindow(JFrame win, String nav)", 
                "+ setActiveNavigation(String cmd)", "+ closeActiveSubWindow()", "+ actionPerformed(ActionEvent ae)"],
               header_col="#1E3A8A")

    draw_class(555, 110, 490, "Conn", "<<implements AutoCloseable>>",
               ["+ Connection c", "+ Statement s", "- static String dbUrl, dbUser, dbPass"],
               ["+ Conn()", "- static void loadConfiguration()", "+ close(): void"],
               header_col="#065F46")

    draw_class(1075, 110, 490, "SessionContext", "<<Encapsulated Session Model>>",
               ["- final String username", "- final String role", "- final String meterNo"],
               ["+ getUsername(): String", "+ getRole(): String", "+ getMeterNo(): String", "+ isAdmin(): boolean", "+ isCustomer(): boolean"],
               header_col="#4C1D95")

    # ROW 2: Billing & Calculation Services (Y = 505)
    draw_class(35, 505, 490, "CalculateBill", "<<extends JFrame, implements ActionListener>>",
               ["- Choice meternumber, cmonth", "- JTextField tfunits", "- JButton submit, cancel"],
               ["+ CalculateBill()", "+ processBillCalculation(): void", "+ actionPerformed(ActionEvent ae)"],
               header_col="#1E293B")

    draw_class(555, 505, 490, "LatePaymentService", "<<Business Surcharge Engine>>",
               ["+ static final BigDecimal RATE = 0.0125", "+ static final int GRACE_DAYS = 15"],
               ["+ static BigDecimal calculateSurcharge(...)", "+ static Date computeDueDate(billDate)"],
               header_col="#0F766E")

    draw_class(1075, 505, 490, "PasswordUtils", "<<Security Utility>>",
               ["- static final int ITERATIONS = 65536", "- static final String ALGO = PBKDF2", "- static final int SALT_BYTES = 16"],
               ["+ static String hashPassword(plain)", "+ static boolean verifyPassword(p, h)", "+ static boolean isHashed(stored): boolean"],
               header_col="#831843")

    # ROW 3: Payment Operations & Modal Receipts (Y = 900)
    draw_class(35, 900, 490, "PayBill", "<<extends JFrame, implements ActionListener>>",
               ["- String meter", "- Choice cmonth", "- JLabel lblunits, totalbill, lstatus", "- JButton pay, back"],
               ["+ PayBill(String meter)", "+ loadBillDetails(String m)", "+ actionPerformed(ActionEvent ae)"],
               header_col="#1E293B")

    draw_class(555, 900, 490, "PaymentGateway", "<<extends JFrame, implements ActionListener>>",
               ["- String billId, meter, amount", "- JRadioButton rbCard, rbUPI, rbNet", "- JButton btnPayNow"],
               ["+ PaymentGateway(id, meter, amt)", "+ processPayment(): void", "- showPaymentReceipt(): void", "+ actionPerformed(ae)"],
               header_col="#0284C7")

    draw_class(1075, 900, 490, "PaymentReceiptDialog", "<<extends JDialog, implements ActionListener>>",
               ["- JTextArea area", "- JButton print, close", "- long billId, String totalPaid"],
               ["+ PaymentReceiptDialog(parent, id)", "- loadBillDetails(long id): void", "+ actionPerformed(ActionEvent ae)"],
               header_col="#0284C7")

    # ROW 4: Customer Operations, Auditing & Grievance (Y = 1295)
    draw_class(35, 1295, 490, "CustomerDetails", "<<extends JFrame, implements ActionListener>>",
               ["- JTable table", "- JButton print, deactivate", "- JTextField tfSearch"],
               ["+ CustomerDetails()", "+ loadCustomers(String query)", "+ actionPerformed(ActionEvent ae)"],
               header_col="#334155")

    draw_class(555, 1295, 490, "ComplaintManagement", "<<extends JFrame, implements ActionListener>>",
               ["- JTable tableComplaints", "- JComboBox cbStatus, cbPriority", "- JTextArea taRemarks"],
               ["+ ComplaintManagement()", "+ updateComplaintStatus(): void", "+ actionPerformed(ActionEvent ae)"],
               header_col="#991B1B")

    draw_class(1075, 1295, 490, "NotificationService", "<<Transaction Dispatch Service>>",
               ["+ static final String TARGET_INDIV", "+ static final String TARGET_BCAST"],
               ["+ static ServiceResult sendNotification(...)", "+ static ServiceResult broadcast(...)", "+ static int getUnreadCount(meterNo)"],
               header_col="#B45309")

    # Connecting Association Lines
    draw.line([(280, 430), (280, 505)], fill="#64748B", width=4) # Project -> CalculateBill
    draw.line([(525, 605), (555, 605)], fill="#059669", width=4) # CalculateBill -> LatePaymentService
    draw.line([(280, 825), (280, 900)], fill="#64748B", width=4) # CalculateBill -> PayBill
    draw.line([(525, 1000), (555, 1000)], fill="#0284C7", width=4) # PayBill -> PaymentGateway
    draw.line([(1045, 1000), (1075, 1000)], fill="#0284C7", width=4) # PaymentGateway -> PaymentReceiptDialog

    # Legend / OOM Takeaways Bar
    draw_box(draw, (35, 1715, 1565, 1820), "#FFFFFF", "#CBD5E1", radius=10, width=2)
    draw.text((55, 1730), "Core Object-Oriented Principles Demonstrated:", font=get_font(20, bold=True), fill="#0F172A")
    draw.text((55, 1760), "• Inheritance: Project & CalculateBill extend JFrame; PaymentReceiptDialog extends JDialog", font=get_font(17), fill="#334155")
    draw.text((55, 1788), "• Interfaces: ActionListener event handling; AutoCloseable   • Encapsulation: SessionContext & Conn   • Polymorphism: paintComponent()", font=get_font(17), fill="#334155")

    img.save("generated_diagrams/class_diagram.png")
    print("Saved portrait high-readability class_diagram.png")

# =========================================================================
# 3. SEQUENCE DIAGRAM: BILL GENERATION
# =========================================================================
def generate_sequence_bill_generation():
    w, h = 1800, 1100
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "UML Sequence Diagram - Electricity Bill Calculation & Generation", "Administrative Workflow: Meter Validation, Statutory Tariff Lookup, Surcharge Calculation & Record Persistence", w)
    
    lifelines = [
        ("Administrator\n(Utility Staff)", 170, "#DBEAFE", "#1D4ED8"),
        (":Project\n(Admin Hub)", 510, "#EFF6FF", "#2563EB"),
        (":CalculateBill\n(Form)", 880, "#EFF6FF", "#2563EB"),
        (":Conn / JDBC\n(Data Layer)", 1270, "#F0FDF4", "#059669"),
        ("MySQL DB\n(`ebs` Schema)", 1620, "#FEF3C7", "#D97706")
    ]
    
    for name, x, f_col, o_col in lifelines:
        draw_box(draw, (x - 85, 115, x + 85, 185), f_col, o_col, radius=8, width=2)
        draw.text((x, 150), name, font=get_font(17, bold=True), fill="#0F172A", anchor="mm", align="center")
        for y_dash in range(195, 1060, 18):
            draw.line([(x, y_dash), (x, y_dash + 9)], fill="#94A3B8", width=2)

    def draw_msg(y, x1, x2, label, is_return=False, color="#1E293B"):
        if is_return:
            for x_dash in range(min(x1, x2), max(x1, x2), 14):
                draw.line([(x_dash, y), (min(x_dash + 7, max(x1, x2)), y)], fill=color, width=2)
        else:
            draw.line([(x1, y), (x2, y)], fill=color, width=2)
            
        arrow_dir = 1 if x2 > x1 else -1
        draw.polygon([(x2, y), (x2 - arrow_dir*12, y - 6), (x2 - arrow_dir*12, y + 6)], fill=color)
        draw.text(((x1 + x2)/2, y - 14), label, font=get_font(16, bold=True), fill=color, anchor="mm")

    draw_msg(225, 170, 510, "1. Click 'Calculate Bill' in Admin Sidebar Menu")
    draw_msg(290, 510, 880, "2. new CalculateBill() & trackSubWindow(this, 'Calculate Bill')")
    draw_msg(360, 880, 1270, "3. executeQuery('SELECT meter_no FROM customer WHERE status=\"Active\"')")
    draw_msg(425, 1270, 1620, "4. Fetch active consumer meter identifiers")
    draw_msg(490, 1620, 880, "5. Return ResultSet; populate Choice meter dropdown", is_return=True, color="#059669")
    draw_msg(565, 170, 880, "6. Select Meter No, Month & enter Units Consumed; Click 'Submit'")
    draw_msg(635, 880, 1270, "7. executeQuery('SELECT * FROM tax LIMIT 1')")
    draw_msg(700, 1270, 1620, "8. Retrieve statutory tariff constants (cost_per_unit, meter_rent, taxes)")
    draw_msg(765, 1620, 880, "9. Return tariff slab records", is_return=True, color="#059669")
    draw_msg(830, 880, 880, "10. computeTotal(): units * rate + rent + taxes + due_date calculation")
    draw_msg(905, 880, 1270, "11. executeUpdate('INSERT INTO bill (meter_no, month, units, total_bill, status, due_date) VALUES (...)')")
    draw_msg(970, 1270, 1620, "12. Persist bill record in `bill` table (Status = 'Not Paid')")
    draw_msg(1020, 1620, 880, "13. Return rows affected (1 row inserted)", is_return=True, color="#059669")
    draw_msg(1070, 880, 170, "14. Display JOptionPane: 'Customer Bill Updated Successfully'", is_return=True, color="#2563EB")

    img.save("generated_diagrams/sequence_bill_generation.png")
    print("Saved sequence_bill_generation.png")

# =========================================================================
# 4. SEQUENCE DIAGRAM: PAYMENT SETTLEMENT
# =========================================================================
def generate_sequence_payment():
    w, h = 1800, 1100
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "UML Sequence Diagram - Customer Bill Payment & Transaction Settlement", "Customer Self-Service Workflow: Pending Bill Inspection, Checkout Mode Selection, Atomic Transaction Commit & Receipt", w)
    
    lifelines = [
        ("Customer\n(Consumer)", 170, "#DBEAFE", "#1D4ED8"),
        (":Project\n(Customer Hub)", 490, "#EFF6FF", "#2563EB"),
        (":PayBill\n(Lookup Form)", 810, "#EFF6FF", "#2563EB"),
        (":PaymentGateway\n(Checkout Form)", 1140, "#EFF6FF", "#2563EB"),
        (":Conn / JDBC\n(Data Layer)", 1470, "#F0FDF4", "#059669"),
        ("MySQL DB\n(`ebs` Schema)", 1710, "#FEF3C7", "#D97706")
    ]
    
    for name, x, f_col, o_col in lifelines:
        draw_box(draw, (x - 80, 115, x + 80, 185), f_col, o_col, radius=8, width=2)
        draw.text((x, 150), name, font=get_font(17, bold=True), fill="#0F172A", anchor="mm", align="center")
        for y_dash in range(195, 1060, 18):
            draw.line([(x, y_dash), (x, y_dash + 9)], fill="#94A3B8", width=2)

    def draw_msg(y, x1, x2, label, is_return=False, color="#1E293B"):
        if is_return:
            for x_dash in range(min(x1, x2), max(x1, x2), 14):
                draw.line([(x_dash, y), (min(x_dash + 7, max(x1, x2)), y)], fill=color, width=2)
        else:
            draw.line([(x1, y), (x2, y)], fill=color, width=2)
            
        arrow_dir = 1 if x2 > x1 else -1
        draw.polygon([(x2, y), (x2 - arrow_dir*12, y - 6), (x2 - arrow_dir*12, y + 6)], fill=color)
        draw.text(((x1 + x2)/2, y - 14), label, font=get_font(16, bold=True), fill=color, anchor="mm")

    draw_msg(225, 170, 490, "1. Click 'Pay Bill' in navigation sidebar")
    draw_msg(290, 490, 810, "2. new PayBill(meterNo) & display window")
    draw_msg(360, 810, 1470, "3. executeQuery('SELECT * FROM bill WHERE meter_no = ? AND month = ?')")
    draw_msg(425, 1470, 1710, "4. Query pending billing records for meter")
    draw_msg(490, 1710, 810, "5. Return units, month, total bill amount & status", is_return=True, color="#059669")
    draw_msg(565, 170, 810, "6. Select month & click 'Pay'")
    draw_msg(635, 810, 1140, "7. new PaymentGateway(billId, meter, month, totalAmount)")
    draw_msg(700, 170, 1140, "8. Select payment channel (Card / UPI / NetBanking) & submit 'Pay Now'")
    draw_msg(775, 1140, 1470, "9. conn.c.setAutoCommit(false); executeUpdate('UPDATE bill SET status=\"Paid\"')")
    draw_msg(840, 1470, 1710, "10. Execute atomic UPDATE and conn.c.commit() in MySQL")
    draw_msg(905, 1710, 1140, "11. Transaction committed successfully (ACID guaranteed)", is_return=True, color="#059669")
    draw_msg(970, 1140, 810, "12. AuditService.logAsync('PAYMENT_COMPLETED'); launch PaymentReceiptDialog", is_return=True, color="#2563EB")
    draw_msg(1030, 810, 170, "13. Render printable modal receipt; reset sidebar to Dashboard on close", is_return=True, color="#059669")

    img.save("generated_diagrams/sequence_diagram.png")
    print("Saved sequence_diagram.png")

# =========================================================================
# 5. ACTIVITY DIAGRAM (Large fonts, high resolution)
# =========================================================================
def generate_activity_diagram():
    w, h = 1800, 1200
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "UML Activity Diagram", "End-to-End Electricity Billing, Consumption Logging, and Payment Settlement Workflow", w)
    
    lane1_w = 580
    lane2_w = 620
    lane3_w = 600
    
    draw.rectangle([(0, 95), (lane1_w, h)], fill="#F8FAFC", outline="#CBD5E1", width=1)
    draw.rectangle([(lane1_w, 95), (lane1_w + lane2_w, h)], fill="#FFFFFF", outline="#CBD5E1", width=1)
    draw.rectangle([(lane1_w + lane2_w, 95), (w, h)], fill="#F8FAFC", outline="#CBD5E1", width=1)
    
    draw.rectangle([(0, 95), (lane1_w, 150)], fill="#EFF6FF")
    draw.text((lane1_w/2, 122), "Administrator / Utility Staff", font=get_font(19, bold=True), fill="#1E3A8A", anchor="mm")
    
    draw.rectangle([(lane1_w, 95), (lane1_w + lane2_w, 150)], fill="#F0FDF4")
    draw.text((lane1_w + lane2_w/2, 122), "Billing & Persistence Engine (Core Java & MySQL)", font=get_font(19, bold=True), fill="#065F46", anchor="mm")
    
    draw.rectangle([(lane1_w + lane2_w, 95), (w, 150)], fill="#EFF6FF")
    draw.text((lane1_w + lane2_w + lane3_w/2, 122), "Consumer / Electricity Customer", font=get_font(19, bold=True), fill="#1E3A8A", anchor="mm")

    def draw_activity(x, y, w_box, h_box, text, fill="#EFF6FF", outline="#2563EB"):
        draw_box(draw, (x, y, x + w_box, y + h_box), fill, outline, radius=16, width=2)
        draw.text((x + w_box/2, y + h_box/2), text, font=get_font(16, bold=True), fill="#0F172A", anchor="mm", align="center")

    def draw_arrow(x1, y1, x2, y2, label="", color="#334155"):
        draw.line([(x1, y1), (x2, y2)], fill=color, width=2)
        arrow_dir_x = 1 if x2 > x1 else (-1 if x2 < x1 else 0)
        arrow_dir_y = 1 if y2 > y1 else (-1 if y2 < y1 else 0)
        if arrow_dir_y != 0 and arrow_dir_x == 0:
            draw.polygon([(x2, y2), (x2 - 6, y2 - arrow_dir_y*10), (x2 + 6, y2 - arrow_dir_y*10)], fill=color)
        elif arrow_dir_x != 0 and arrow_dir_y == 0:
            draw.polygon([(x2, y2), (x2 - arrow_dir_x*10, y2 - 6), (x2 - arrow_dir_x*10, y2 + 6)], fill=color)
        if label:
            draw.text(((x1 + x2)/2 + 12, (y1 + y2)/2), label, font=get_font(14, bold=True), fill=color)

    # 1. Start Node
    draw.ellipse([(265, 170), (315, 220)], fill="#0F172A")
    draw_arrow(290, 220, 290, 260)
    
    # 2. Register New Customer
    draw_activity(130, 260, 320, 60, "Register Customer & Assign Meter\n(NewCustomer.java)")
    draw_arrow(290, 320, 290, 360)

    # 3. Enter Monthly Units
    draw_activity(130, 360, 320, 60, "Enter Monthly Units Consumed\n(CalculateBill.java)")
    
    draw.line([(450, 390), (680, 390)], fill="#334155", width=2)
    draw_arrow(680, 390, 680, 430)

    # 4. Query Tax & Compute Total
    draw_activity(680, 430, 400, 65, "Query Tariffs from `tax` &\nCompute Total Payable + Due Date", fill="#FEF3C7", outline="#D97706")
    draw_arrow(880, 495, 880, 535)

    # 5. Insert Record into Bill Table
    draw_activity(680, 535, 400, 65, "Insert into `bill` Table\nStatus = 'Not Paid'", fill="#FEF3C7", outline="#D97706")
    
    draw.line([(1080, 567), (1500, 567)], fill="#334155", width=2)
    draw_arrow(1500, 567, 1500, 610)

    # 6. Customer Log in & View Bill
    draw_activity(1340, 610, 320, 60, "Customer Logs In & Inspects\nPending Bill (PayBill.java)")
    draw_arrow(1500, 670, 1500, 710)

    # 7. Select Payment Channel
    draw_activity(1330, 710, 340, 60, "Select Payment Channel\n(Card / UPI / NetBanking)")
    
    draw.line([(1330, 740), (1080, 740)], fill="#334155", width=2)
    draw_arrow(1080, 740, 880, 780)

    # 8. Decision Diamond
    diamond = [(880, 780), (950, 825), (880, 870), (810, 825)]
    draw.polygon(diamond, fill="#EFF6FF", outline="#2563EB")
    draw.text((880, 825), "Valid\nPayment?", font=get_font(14, bold=True), fill="#1E3A8A", anchor="mm", align="center")

    draw.line([(810, 825), (630, 825)], fill="#DC2626", width=2)
    draw.text((720, 808), "[Failed / Cancelled]", font=get_font(13, bold=True), fill="#DC2626", anchor="mm")
    draw.line([(630, 825), (630, 740), (1330, 740)], fill="#DC2626", width=2)

    draw_arrow(880, 870, 880, 920, label="[Success]")
    draw_activity(680, 920, 400, 65, "conn.setAutoCommit(false);\nUPDATE bill SET status='Paid';\nconn.commit();", fill="#F0FDF4", outline="#059669")
    
    draw.line([(1080, 952), (1500, 952)], fill="#334155", width=2)
    draw_arrow(1500, 952, 1500, 995)

    # 9. Payment Receipt
    draw_activity(1330, 995, 340, 60, "Display Official Payment Receipt\n(PaymentReceiptDialog.java)")
    draw_arrow(1500, 1055, 1500, 1100)

    # 10. End Node
    draw.ellipse([(1475, 1100), (1525, 1150)], fill="#FFFFFF", outline="#0F172A", width=3)
    draw.ellipse([(1485, 1110), (1515, 1140)], fill="#0F172A")

    img.save("generated_diagrams/activity_diagram.png")
    print("Saved activity_diagram.png")

# =========================================================================
# 6. COMPONENT DIAGRAM
# =========================================================================
def generate_component_diagram():
    w, h = 1800, 1150
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "UML Component Diagram", "Architectural Software Components, Layered Subsystems & Communication Interfaces", w)

    def draw_component(x, y, w_box, h_box, title, subtitle, items, color_header="#1E3A8A"):
        draw_box(draw, (x, y, x + w_box, y + h_box), "#FFFFFF", "#334155", radius=8, width=2)
        draw_box(draw, (x + w_box - 50, y + 14, x + w_box - 12, y + 42), "#F1F5F9", "#475569", radius=2, width=1)
        draw.rectangle([(x + w_box - 56, y + 20), (x + w_box - 44, y + 26)], fill="#94A3B8")
        draw.rectangle([(x + w_box - 56, y + 30), (x + w_box - 44, y + 36)], fill="#94A3B8")
        
        draw.text((x + 22, y + 18), title, font=get_font(18, bold=True), fill=color_header)
        draw.text((x + 22, y + 44), subtitle, font=get_font(14, bold=False), fill="#64748B")
        draw.line([(x, y + 68), (x + w_box, y + 68)], fill="#CBD5E1", width=1)
        
        cur_y = y + 80
        for it in items:
            draw.text((x + 20, cur_y), "•  " + it, font=get_font(15, bold=False), fill="#1E293B")
            cur_y += 26

    # 1. UI Presentation Layer
    draw_component(40, 120, 520, 440, "Presentation Layer (Swing / AWT)", "<<Component: UI Tier>>", [
        "Login.java & Signup.java (Authentication)",
        "Project.java (Role-based Navigation)",
        "NewCustomer.java & CustomerDetails.java",
        "CalculateBill.java & GenerateBill.java",
        "PayBill.java & PaymentGateway.java",
        "PaymentReceiptDialog.java (Print Dialog)",
        "SubmitComplaint.java & ComplaintManagement.java",
        "AdminMessageHub.java & CustomerNotificationHub.java",
        "AnalyticsDashboard.java & AuditLogViewer.java"
    ], color_header="#1E3A8A")

    # 2. Session & Security
    draw_component(600, 120, 540, 270, "Session & Security Subsystem", "<<Component: Security>>", [
        "UserSession.java (Static thread-safe session)",
        "SessionContext.java (Immutable session state)",
        "PasswordUtils.java (PBKDF2WithHmacSHA256)",
        "Role-based Access Enforcement (Admin / Customer)",
        "AuditService.java (Action trail recording)"
    ], color_header="#4C1D95")

    # 3. Business Service & Logic
    draw_component(1180, 120, 580, 270, "Business Services & Logic Tier", "<<Component: Domain Logic>>", [
        "Tariff Computation Rules (Stepped rates)",
        "Late Payment Surcharge Engine (UPERC compliance)",
        "Complaint Lifecycle & Status Transitions",
        "In-App Notification Dispatch Hub",
        "Analytics Metric Aggregation & Reporting"
    ], color_header="#065F46")

    # 4. Data Access Layer (JDBC)
    draw_component(40, 630, 800, 420, "Database Persistence Layer (JDBC)", "<<Component: Data Access>>", [
        "Conn.java (Centralized Connection Wrapper & AutoCloseable)",
        "Class.forName('com.mysql.cj.jdbc.Driver')",
        "DriverManager.getConnection(dbUrl, dbUser, dbPass)",
        "PreparedStatement (Parameterized Query & SQL Injection Defense)",
        "Explicit Transaction Management (setAutoCommit, commit, rollback)",
        "DbUtils.java (ResultSet to TableModel conversion)"
    ], color_header="#0284C7")

    # 5. Database Subsystem (MySQL)
    draw_component(900, 630, 860, 420, "Relational Database Subsystem (`ebs` MySQL)", "<<Component: Database>>", [
        "`customer` (Master entity, PK: meter_no)",
        "`meter_info` (Hardware meter specs, PK: meter_no)",
        "`login` (Credentials & authorization roles, PK: meter_no)",
        "`bill` (Monthly unit consumption records, PK: bill_id, FK: meter_no)",
        "`tax` (Tariff parameters: cost per unit, meter rent, taxes)",
        "`complaints` & `complaint_history` (Grievance lifecycle & audit)",
        "`messages` & `message_recipients` (In-app notifications & broadcasts)",
        "`audit_logs` (System activity & action trails)"
    ], color_header="#B45309")

    # Connectors
    draw.line([(560, 210), (600, 210)], fill="#334155", width=2)
    draw.ellipse([(575, 205), (585, 215)], fill="#FFFFFF", outline="#334155", width=2)
    draw.text((580, 190), "UserSession API", font=get_font(13, bold=True), fill="#475569", anchor="mm")

    draw.line([(560, 280), (1180, 280)], fill="#334155", width=2)
    draw.ellipse([(865, 275), (875, 285)], fill="#FFFFFF", outline="#334155", width=2)
    draw.text((870, 260), "Business Services", font=get_font(13, bold=True), fill="#475569", anchor="mm")

    draw.line([(300, 560), (300, 630)], fill="#334155", width=2)
    draw.ellipse([(295, 590), (305, 600)], fill="#FFFFFF", outline="#334155", width=2)
    draw.text((330, 595), "Conn.c / Conn.s", font=get_font(13, bold=True), fill="#475569")

    draw.line([(840, 840), (900, 840)], fill="#334155", width=2)
    draw.ellipse([(865, 835), (875, 845)], fill="#FFFFFF", outline="#334155", width=2)
    draw.text((870, 820), "TCP Socket : 3306", font=get_font(14, bold=True), fill="#0284C7", anchor="mm")

    img.save("generated_diagrams/component_diagram.png")
    print("Saved component_diagram.png")

# =========================================================================
# 7. ER DIAGRAM: COMPLETE 10-TABLE ACTUAL SCHEMA (Large 17-24pt fonts)
# =========================================================================
def generate_er_diagram():
    # Portrait aspect ratio: Width 1600, Height 1850 perfectly fits A4 page with heading & caption
    w, h = 1600, 1850
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "Relational Entity-Relationship (ER) Diagram", "Actual Verified MySQL Database Schema for Electricity Billing System (`ebs`) - All 10 Tables & Constraints", w, h_bar=90)

    def draw_entity(x, y, w_box, title, pk_list, fk_list, attr_list, header_color="#1E3A8A"):
        header_h = 56
        tot_items = len(pk_list) + len(fk_list) + len(attr_list)
        body_h = 12 + tot_items * 24 + 10
        draw.rounded_rectangle([(x, y), (x + w_box, y + header_h + body_h)], radius=10, fill="#FFFFFF", outline="#334155", width=3)
        draw.rounded_rectangle([(x, y), (x + w_box, y + header_h)], radius=10, fill=header_color, outline="#334155", width=3)
        draw.rectangle([(x, y + header_h - 10), (x + w_box, y + header_h)], fill=header_color)
        draw.text((x + w_box/2, y + 28), title, font=get_font(19, bold=True), fill="#FFFFFF", anchor="mm")
        
        cur_y = y + header_h + 10
        for pk in pk_list:
            draw.text((x + 16, cur_y), pk, font=get_font(17, bold=True), fill="#B91C1C")
            cur_y += 24
        for fk in fk_list:
            draw.text((x + 16, cur_y), fk, font=get_font(17, bold=True), fill="#2563EB")
            cur_y += 24
        if pk_list or fk_list:
            draw.line([(x, cur_y + 3), (x + w_box, cur_y + 3)], fill="#CBD5E1", width=2)
            cur_y += 8
        for attr in attr_list:
            draw.text((x + 16, cur_y), attr, font=get_font(17, bold=False), fill="#1E293B")
            cur_y += 24
        return header_h + body_h

    # Layout in 4 Distinct Tiers (Portrait):
    # TIER 1: Master Customer, Meter Info, Login (Y = 110)
    y_tier1 = 110
    draw_entity(35, y_tier1, 490, "CUSTOMER (Master Table)", 
                ["[PK] meter_no : VARCHAR(20)"],
                [],
                ["name : VARCHAR(100)", "address : VARCHAR(200)", "city : VARCHAR(50)", "state : VARCHAR(50)", 
                 "email : VARCHAR(100)", "phone : VARCHAR(20)", "status : VARCHAR(20) DEFAULT 'Active'"],
                header_color="#1E3A8A")

    draw_entity(555, y_tier1, 490, "METER_INFO",
                ["[PK] meter_no : VARCHAR(20)"],
                ["(Logical Link -> customer.meter_no)"],
                ["meter_location : VARCHAR(100)", "meter_type : VARCHAR(50)", "phase_code : VARCHAR(20)", 
                 "bill_type : VARCHAR(50)", "days : VARCHAR(10)"],
                header_color="#065F46")

    draw_entity(1075, y_tier1, 490, "LOGIN",
                ["[PK] meter_no : VARCHAR(20)"],
                ["(Logical Link -> customer / ADMIN)"],
                ["username : VARCHAR(50)", "name : VARCHAR(100)", "password : VARCHAR(255) [PBKDF2]", 
                 "user : VARCHAR(20) ('Admin'/'Customer')"],
                header_color="#4C1D95")

    # TIER 2: Tariff Rates & Monthly Billing Ledger (Y = 495)
    y_tier2 = 495
    draw_entity(35, y_tier2, 560, "TAX (Tariff Rates)",
                [],
                [],
                ["cost_per_unit : DECIMAL(10,2)", "meter_rent : DECIMAL(10,2)", "service_charge : DECIMAL(10,2)", 
                 "swacch_bharat_tax : DECIMAL(10,2)", "fixed_tax : DECIMAL(10,2)"],
                header_color="#B45309")

    draw_entity(625, y_tier2, 940, "BILL (Monthly Invoices & Settlement)",
                ["[PK] bill_id : BIGINT AUTO_INC"],
                ["[FK] meter_no : VARCHAR(20) -> customer(meter_no)"],
                ["month : VARCHAR(20)", "units : VARCHAR(20)", "total_bill : VARCHAR(20)", "status : VARCHAR(20)", 
                 "due_date : DATE", "original_due_date : DATE", "paid_at : DATETIME", "late_fee : DECIMAL(10,2)", "paid_amount : DECIMAL(10,2)"],
                header_color="#1E3A8A")

    # TIER 3: Consumer Grievances & Audit Trail (Y = 905)
    y_tier3 = 905
    draw_entity(35, y_tier3, 750, "COMPLAINTS (Grievance Tickets)",
                ["[PK] complaint_id : INT AUTO_INC"],
                ["[FK] meter_no : VARCHAR(20) -> customer(meter_no)"],
                ["reference_number : VARCHAR(50) UNIQUE", "category : VARCHAR(100)", "subject : VARCHAR(200)", 
                 "description : TEXT", "bill_month : VARCHAR(20)", "priority : VARCHAR(20) DEFAULT 'Medium'", 
                 "status : VARCHAR(20) DEFAULT 'Pending'", "assigned_admin : VARCHAR(100)", 
                 "resolution_remarks : TEXT", "created_at : DATETIME", "updated_at : DATETIME"],
                header_color="#991B1B")

    draw_entity(815, y_tier3, 750, "COMPLAINT_HISTORY (Status Audit)",
                ["[PK] history_id : INT AUTO_INC"],
                ["[FK] complaint_id : INT -> complaints(complaint_id)"],
                ["action : VARCHAR(100)", "previous_value : TEXT", "new_value : TEXT", 
                 "performed_by : VARCHAR(100)", "remarks : TEXT", "created_at : DATETIME"],
                header_color="#991B1B")

    # TIER 4: Notifications & Operational Auditing (Y = 1345)
    y_tier4 = 1345
    draw_entity(35, y_tier4, 490, "MESSAGES (Admin Broadcasts)",
                ["[PK] message_id : INT AUTO_INC"],
                [],
                ["sender_admin : VARCHAR(50)", "target_type : VARCHAR(20) ('INDIV'/'BCAST')", 
                 "title : VARCHAR(150)", "message : TEXT", "priority : VARCHAR(20) DEFAULT 'Normal'", "created_at : DATETIME"],
                header_color="#0F766E")

    draw_entity(555, y_tier4, 490, "MESSAGE_RECIPIENTS",
                ["[PK] recipient_id : INT AUTO_INC"],
                ["[FK] message_id : INT -> messages", "[FK] meter_no : VARCHAR(20) -> customer"],
                ["is_read : TINYINT(1) DEFAULT 0", "read_at : DATETIME", "created_at : DATETIME"],
                header_color="#0F766E")

    draw_entity(1075, y_tier4, 490, "AUDIT_LOGS (Event Logs)",
                ["[PK] audit_id : BIGINT AUTO_INC"],
                [],
                ["actor_id : VARCHAR(50)", "actor_role : VARCHAR(20)", "action_type : VARCHAR(50)", 
                 "entity_type : VARCHAR(30)", "entity_id : VARCHAR(50)", "description : VARCHAR(255)", 
                 "old_value : TEXT", "new_value : TEXT", "ip_address : VARCHAR(45)", "created_at : DATETIME"],
                header_color="#475569")

    # Cardinality Lines & Relationships
    # 1. CUSTOMER -> BILL (1 : N) [Actual SQL FK: bill_ibfk_1]
    draw.line([(280, 390), (280, 460), (700, 460), (700, 495)], fill="#2563EB", width=4)
    draw.polygon([(700, 495), (691, 477), (709, 477)], fill="#2563EB")
    draw.text((490, 435), "1 : N [FK: bill_ibfk_1]", font=get_font(17, bold=True), fill="#1E40AF", anchor="mm")

    # 2. CUSTOMER -> COMPLAINTS (1 : N) [Actual SQL FK: fk_complaints_meter]
    draw.line([(100, 390), (100, 905)], fill="#DC2626", width=4)
    draw.polygon([(100, 905), (91, 887), (109, 887)], fill="#DC2626")
    draw.text((115, 680), "1 : N [FK: fk_complaints_meter]", font=get_font(17, bold=True), fill="#B91C1C")

    # 3. COMPLAINTS -> COMPLAINT_HISTORY (1 : N) [Actual SQL FK: fk_history_complaint]
    draw.line([(785, 1050), (815, 1050)], fill="#DC2626", width=4)
    draw.polygon([(815, 1050), (797, 1041), (797, 1059)], fill="#DC2626")
    draw.text((800, 1025), "1 : N [FK]", font=get_font(17, bold=True), fill="#B91C1C", anchor="mm")

    # 4. MESSAGES -> MESSAGE_RECIPIENTS (1 : N) [Actual SQL FK: fk_msg_recip_msg]
    draw.line([(525, 1500), (555, 1500)], fill="#0F766E", width=4)
    draw.polygon([(555, 1500), (537, 1491), (537, 1509)], fill="#0F766E")
    draw.text((540, 1475), "1 : N [FK]", font=get_font(17, bold=True), fill="#0F766E", anchor="mm")

    # 5. CUSTOMER -> MESSAGE_RECIPIENTS (1 : N) [Actual SQL FK: fk_msg_recip_cust]
    draw.line([(70, 390), (70, 1310), (650, 1310), (650, 1345)], fill="#0F766E", width=4)
    draw.polygon([(650, 1345), (641, 1327), (659, 1327)], fill="#0F766E")
    draw.text((360, 1285), "1 : N [FK: fk_msg_recip_cust]", font=get_font(17, bold=True), fill="#0F766E", anchor="mm")

    # Logical Links (Dashed)
    # CUSTOMER --- METER_INFO (1 : 1 Logical)
    for x_d in range(525, 555, 8):
        draw.line([(x_d, y_tier1 + 70), (x_d + 4, y_tier1 + 70)], fill="#059669", width=3)
    draw.text((540, y_tier1 + 50), "1 : 1", font=get_font(17, bold=True), fill="#059669", anchor="mm")

    # CUSTOMER --- LOGIN (1 : 1 Logical)
    for y_d in range(y_tier1 - 10, y_tier1 - 30, -5):
        draw.line([(280, y_d), (280, y_d - 3)], fill="#7C3AED", width=3)
    for x_d in range(280, 1320, 10):
        draw.line([(x_d, y_tier1 - 30), (x_d + 5, y_tier1 - 30)], fill="#7C3AED", width=3)
    for y_d in range(y_tier1 - 30, y_tier1, 5):
        draw.line([(1320, y_d), (1320, y_d + 3)], fill="#7C3AED", width=3)
    draw.text((800, y_tier1 - 45), "1 : 1 Logical Association (Shared meter_no / Credentials)", font=get_font(17, bold=True), fill="#6D28D9", anchor="mm")

    # Bottom Legend
    draw_box(draw, (35, 1720, 1565, 1825), "#FFFFFF", "#CBD5E1", radius=10, width=2)
    draw.text((55, 1735), "Schema Legend & Constraint Integrity Architecture:", font=get_font(20, bold=True), fill="#0F172A")
    draw.text((55, 1768), "Solid Lines: Actual Foreign Key Constraints verified in MySQL (`ebs`).   Dashed Lines: Application-tier 1:1 logical links.", font=get_font(17), fill="#334155")
    draw.text((55, 1795), "Verified Foreign Keys: [1] bill_ibfk_1  [2] fk_complaints_meter  [3] fk_history_complaint  [4] fk_msg_recip_cust  [5] fk_msg_recip_msg", font=get_font(16, bold=True), fill="#1E3A8A")

    img.save("generated_diagrams/er_diagram.png")
    print("Saved portrait high-readability er_diagram.png")

if __name__ == "__main__":
    generate_use_case_diagram()
    generate_class_diagram()
    generate_sequence_bill_generation()
    generate_sequence_payment()
    generate_activity_diagram()
    generate_component_diagram()
    generate_er_diagram()
