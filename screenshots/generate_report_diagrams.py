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

def draw_header(draw, title, subtitle, width):
    draw.rectangle([(0, 0), (width, 70)], fill="#0F172A")
    draw.text((30, 12), title, font=get_font(22, bold=True), fill="#FFFFFF")
    draw.text((30, 42), subtitle, font=get_font(13, bold=False), fill="#94A3B8")

def draw_box(draw, xy, fill, outline, radius=8, width=2):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)

# =========================================================================
# 1. SEQUENCE DIAGRAM: BILL GENERATION
# =========================================================================
def generate_sequence_bill_generation():
    w, h = 1600, 950
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "UML Sequence Diagram - Electricity Bill Generation", "Administrative Workflow: Tariff Lookup, Unit Calculation, and Bill Ledger Insertion", w)
    
    lifelines = [
        ("Administrator\n(User)", 150, "#DBEAFE", "#1D4ED8"),
        (":Project\n(Admin Hub)", 450, "#EFF6FF", "#2563EB"),
        (":CalculateBill\n(GUI Form)", 800, "#EFF6FF", "#2563EB"),
        (":Conn / JDBC\n(Data Layer)", 1150, "#F0FDF4", "#059669"),
        ("MySQL DB\n(ebs Database)", 1450, "#FEF3C7", "#D97706")
    ]
    
    for name, x, f_col, o_col in lifelines:
        draw_box(draw, (x - 75, 90, x + 75, 150), f_col, o_col, radius=6, width=2)
        draw.text((x, 120), name, font=get_font(14, bold=True), fill="#0F172A", anchor="mm", align="center")
        for y_dash in range(160, 910, 16):
            draw.line([(x, y_dash), (x, y_dash + 8)], fill="#94A3B8", width=2)

    def draw_msg(y, x1, x2, label, is_return=False, color="#1E293B"):
        if is_return:
            for x_dash in range(min(x1, x2), max(x1, x2), 12):
                draw.line([(x_dash, y), (min(x_dash + 6, max(x1, x2)), y)], fill=color, width=2)
        else:
            draw.line([(x1, y), (x2, y)], fill=color, width=2)
            
        arrow_dir = 1 if x2 > x1 else -1
        draw.polygon([(x2, y), (x2 - arrow_dir*10, y - 5), (x2 - arrow_dir*10, y + 5)], fill=color)
        draw.text(((x1 + x2)/2, y - 12), label, font=get_font(13, bold=True), fill=color, anchor="mm")

    draw_msg(190, 150, 450, "1. Click 'Calculate Bill' in Admin Sidebar")
    draw_msg(240, 450, 800, "2. new CalculateBill() & trackSubWindow()")
    draw_msg(300, 800, 1150, "3. executeQuery('SELECT meter_no FROM customer WHERE status=\"Active\"')")
    draw_msg(350, 1150, 1450, "4. Fetch active meter numbers")
    draw_msg(400, 1450, 800, "5. Populate Choice dropdown with meter numbers", is_return=True, color="#059669")
    draw_msg(460, 150, 800, "6. Select Meter No, Month & enter Units Consumed; Click 'Submit'")
    draw_msg(520, 800, 1150, "7. executeQuery('SELECT * FROM tax')")
    draw_msg(570, 1150, 1450, "8. Retrieve tariff slabs (cost_per_unit, meter_rent, service_charge, fixed_tax)")
    draw_msg(620, 1450, 800, "9. Return tariff constants", is_return=True, color="#059669")
    draw_msg(670, 800, 800, "10. computeTotal(): units * cost_per_unit + meter_rent + taxes")
    draw_msg(730, 800, 1150, "11. executeUpdate('INSERT INTO bill VALUES(meter, month, units, total, \"Not Paid\")')")
    draw_msg(780, 1150, 1450, "12. Persist bill record in `bill` table")
    draw_msg(830, 1450, 800, "13. Insertion confirmed (1 row affected)", is_return=True, color="#059669")
    draw_msg(880, 800, 150, "14. JOptionPane.showMessageDialog('Customer Bill Updated Successfully')", is_return=True, color="#2563EB")

    img.save("generated_diagrams/sequence_bill_generation.png")
    print("Saved sequence_bill_generation.png")

# =========================================================================
# 2. ACTIVITY DIAGRAM: CUSTOMER BILLING & PAYMENT WORKFLOW
# =========================================================================
def generate_activity_diagram():
    w, h = 1600, 1100
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "UML Activity Diagram", "End-to-End Electricity Billing, Consumption Logging, and Payment Settlement Workflow", w)
    
    # Swimlanes
    lane1_w = 520
    lane2_w = 540
    lane3_w = 540
    
    draw.rectangle([(0, 70), (lane1_w, h)], fill="#F8FAFC", outline="#CBD5E1", width=1)
    draw.rectangle([(lane1_w, 70), (lane1_w + lane2_w, h)], fill="#FFFFFF", outline="#CBD5E1", width=1)
    draw.rectangle([(lane1_w + lane2_w, 70), (w, h)], fill="#F8FAFC", outline="#CBD5E1", width=1)
    
    draw.rectangle([(0, 70), (lane1_w, 115)], fill="#EFF6FF")
    draw.text((lane1_w/2, 92), "Administrator / Utility Staff", font=get_font(15, bold=True), fill="#1E3A8A", anchor="mm")
    
    draw.rectangle([(lane1_w, 70), (lane1_w + lane2_w, 115)], fill="#F0FDF4")
    draw.text((lane1_w + lane2_w/2, 92), "System / Billing Engine (Core Java & MySQL)", font=get_font(15, bold=True), fill="#065F46", anchor="mm")
    
    draw.rectangle([(lane1_w + lane2_w, 70), (w, 115)], fill="#EFF6FF")
    draw.text((lane1_w + lane2_w + lane3_w/2, 92), "Consumer / Customer", font=get_font(15, bold=True), fill="#1E3A8A", anchor="mm")

    def draw_activity(x, y, w_box, h_box, text, fill="#EFF6FF", outline="#2563EB"):
        draw_box(draw, (x, y, x + w_box, y + h_box), fill, outline, radius=16, width=2)
        draw.text((x + w_box/2, y + h_box/2), text, font=get_font(13, bold=True), fill="#0F172A", anchor="mm", align="center")

    def draw_arrow(x1, y1, x2, y2, label="", color="#334155"):
        draw.line([(x1, y1), (x2, y2)], fill=color, width=2)
        arrow_dir_x = 1 if x2 > x1 else (-1 if x2 < x1 else 0)
        arrow_dir_y = 1 if y2 > y1 else (-1 if y2 < y1 else 0)
        if arrow_dir_y != 0 and arrow_dir_x == 0:
            draw.polygon([(x2, y2), (x2 - 5, y2 - arrow_dir_y*8), (x2 + 5, y2 - arrow_dir_y*8)], fill=color)
        elif arrow_dir_x != 0 and arrow_dir_y == 0:
            draw.polygon([(x2, y2), (x2 - arrow_dir_x*8, y2 - 5), (x2 - arrow_dir_x*8, y2 + 5)], fill=color)
        if label:
            draw.text(((x1 + x2)/2 + 8, (y1 + y2)/2), label, font=get_font(11, bold=True), fill=color)

    # 1. Start Node
    draw.ellipse([(240, 140), (280, 180)], fill="#0F172A")
    draw_arrow(260, 180, 260, 215)
    
    # 2. Register New Customer
    draw_activity(120, 215, 280, 50, "Register Customer & Assign Meter\n(NewCustomer.java)")
    draw_arrow(260, 265, 260, 305)

    # 3. Enter Monthly Units
    draw_activity(120, 305, 280, 50, "Enter Monthly Units Consumed\n(CalculateBill.java)")
    
    # Connect to System: Calculate Bill
    draw.line([(400, 330), (620, 330)], fill="#334155", width=2)
    draw_arrow(620, 330, 620, 370)

    # 4. Query Tax & Compute Total
    draw_activity(620, 370, 320, 55, "Query Tariffs from `tax` &\nCompute Total Payable", fill="#FEF3C7", outline="#D97706")
    draw_arrow(780, 425, 780, 465)

    # 5. Insert Record into Bill Table
    draw_activity(620, 465, 320, 55, "Insert into `bill` Table\nStatus = 'Not Paid'", fill="#FEF3C7", outline="#D97706")
    
    # Connect to Customer: Log In
    draw.line([(940, 492), (1330, 492)], fill="#334155", width=2)
    draw_arrow(1330, 492, 1330, 535)

    # 6. Customer Log in & View Bill
    draw_activity(1200, 535, 260, 50, "Customer Logs In & Inspects\nPending Bill (PayBill.java)")
    draw_arrow(1330, 585, 1330, 625)

    # 7. Select Payment Channel
    draw_activity(1190, 625, 280, 50, "Select Payment Channel\n(Card / UPI / NetBanking)")
    
    # Connect back to System: Process Payment
    draw.line([(1190, 650), (940, 650)], fill="#334155", width=2)
    draw_arrow(940, 650, 780, 690)

    # 8. Decision Diamond: Payment Successful?
    diamond = [(780, 690), (840, 730), (780, 770), (720, 730)]
    draw.polygon(diamond, fill="#EFF6FF", outline="#2563EB")
    draw.text((780, 730), "Valid\nPayment?", font=get_font(11, bold=True), fill="#1E3A8A", anchor="mm", align="center")

    # If Failure (No): Rollback
    draw.line([(720, 730), (560, 730)], fill="#DC2626", width=2)
    draw.text((640, 715), "[Failed / Cancelled]", font=get_font(11, bold=True), fill="#DC2626", anchor="mm")
    draw.line([(560, 730), (560, 650), (1190, 650)], fill="#DC2626", width=2)

    # If Success (Yes): Commit & Update Status
    draw_arrow(780, 770, 780, 815, label="[Success]")
    draw_activity(620, 815, 320, 55, "conn.setAutoCommit(false);\nUPDATE bill SET status='Paid';\nconn.commit();", fill="#F0FDF4", outline="#059669")
    
    # Connect to Customer: View Receipt
    draw.line([(940, 842), (1330, 842)], fill="#334155", width=2)
    draw_arrow(1330, 842, 1330, 885)

    # 9. Payment Receipt
    draw_activity(1190, 885, 280, 50, "Display Official Payment Receipt\n(PaymentReceiptDialog.java)")
    draw_arrow(1330, 935, 1330, 980)

    # 10. End Node
    draw.ellipse([(1310, 980), (1350, 1020)], fill="#FFFFFF", outline="#0F172A", width=3)
    draw.ellipse([(1318, 988), (1342, 1012)], fill="#0F172A")

    img.save("generated_diagrams/activity_diagram.png")
    print("Saved activity_diagram.png")

# =========================================================================
# 3. COMPONENT DIAGRAM
# =========================================================================
def generate_component_diagram():
    w, h = 1600, 950
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "UML Component Diagram", "Architectural Software Components, Layered Subsystems & Communication Interfaces", w)

    def draw_component(x, y, w_box, h_box, title, subtitle, items, color_header="#1E3A8A"):
        # Component outer box
        draw_box(draw, (x, y, x + w_box, y + h_box), "#FFFFFF", "#334155", radius=8, width=2)
        # Component stereotype icon
        draw_box(draw, (x + w_box - 45, y + 10, x + w_box - 10, y + 35), "#F1F5F9", "#475569", radius=2, width=1)
        draw.rectangle([(x + w_box - 50, y + 15), (x + w_box - 40, y + 20)], fill="#94A3B8")
        draw.rectangle([(x + w_box - 50, y + 25), (x + w_box - 40, y + 30)], fill="#94A3B8")
        
        # Header text
        draw.text((x + 18, y + 14), title, font=get_font(15, bold=True), fill=color_header)
        draw.text((x + 18, y + 35), subtitle, font=get_font(11, bold=False), fill="#64748B")
        draw.line([(x, y + 54), (x + w_box, y + 54)], fill="#CBD5E1", width=1)
        
        cur_y = y + 64
        for it in items:
            draw.text((x + 16, cur_y), "•  " + it, font=get_font(12, bold=False), fill="#1E293B")
            cur_y += 22

    # 1. UI Presentation Layer Component
    draw_component(40, 100, 460, 360, "Presentation Layer (Swing / AWT)", "<<Component: UI Tier>>", [
        "Login.java & Signup.java (Authentication)",
        "Project.java (Role-based Navigation)",
        "NewCustomer.java & CustomerDetails.java",
        "CalculateBill.java & GenerateBill.java",
        "PayBill.java & PaymentGateway.java",
        "PaymentReceiptDialog.java (Print Dialog)",
        "SubmitComplaint.java & ComplaintManagement.java",
        "AnalyticsDashboard.java & AuditLogViewer.java"
    ], color_header="#1E3A8A")

    # 2. Session & Security Component
    draw_component(540, 100, 480, 240, "Session & Security Subsystem", "<<Component: Security>>", [
        "UserSession.java (Static thread-safe session)",
        "SessionContext.java (Immutable session state)",
        "PasswordUtils.java (PBKDF2WithHmacSHA256)",
        "Role-based Access Enforcement (Admin / Customer)",
        "AuditService.java (Action trail recording)"
    ], color_header="#4C1D95")

    # 3. Business Service & Logic Component
    draw_component(1060, 100, 500, 240, "Business Services & Logic Tier", "<<Component: Domain Logic>>", [
        "Tariff Computation Rules (Stepped rates)",
        "Late Payment Surcharge Engine (UPERC compliance)",
        "Complaint Lifecycle & Status Transitions",
        "In-App Notification Dispatch Hub",
        "Analytics Metric Aggregation & Reporting"
    ], color_header="#065F46")

    # 4. Data Access Layer Component (JDBC)
    draw_component(40, 540, 720, 330, "Database Persistence Layer (JDBC)", "<<Component: Data Access>>", [
        "Conn.java (Centralized Connection Wrapper & AutoCloseable)",
        "Class.forName('com.mysql.cj.jdbc.Driver')",
        "DriverManager.getConnection(dbUrl, dbUser, dbPass)",
        "PreparedStatement (Parameterized Query & SQL Injection Defense)",
        "Explicit Transaction Management (setAutoCommit, commit, rollback)",
        "DbUtils.java (ResultSet to TableModel conversion)"
    ], color_header="#0284C7")

    # 5. Database Subsystem Component (MySQL)
    draw_component(840, 540, 720, 330, "Relational Database Subsystem (MySQL)", "<<Component: Database>>", [
        "`customer` (Master entity, PK: meter_no)",
        "`meter_info` (Hardware meter specs, PK/FK: meter_no)",
        "`login` (Credentials & authorization roles, PK/FK: meter_no)",
        "`bill` (Monthly unit consumption records, FK: meter_no)",
        "`tax` (Tariff parameters: cost per unit, meter rent, taxes)",
        "`complaints` & `complaint_history` (Consumer grievance lifecycle)",
        "`audit_logs` (System activity & action trails)"
    ], color_header="#B45309")

    # Connectors & Interfaces
    # Presentation to Session & Security
    draw.line([(500, 180), (540, 180)], fill="#334155", width=2)
    draw.ellipse([(515, 175), (525, 185)], fill="#FFFFFF", outline="#334155", width=2)
    draw.text((520, 160), "UserSession API", font=get_font(10, bold=True), fill="#475569", anchor="mm")

    # Presentation to Business Logic
    draw.line([(500, 240), (1060, 240)], fill="#334155", width=2)
    draw.ellipse([(775, 235), (785, 245)], fill="#FFFFFF", outline="#334155", width=2)
    draw.text((780, 225), "Service Calls", font=get_font(10, bold=True), fill="#475569", anchor="mm")

    # Presentation to Data Access
    draw.line([(270, 460), (270, 540)], fill="#334155", width=2)
    draw.ellipse([(265, 495), (275, 505)], fill="#FFFFFF", outline="#334155", width=2)
    draw.text((295, 500), "Conn.c / Conn.s", font=get_font(10, bold=True), fill="#475569")

    # Data Access to MySQL
    draw.line([(760, 705), (840, 705)], fill="#334155", width=2)
    draw.ellipse([(795, 700), (805, 710)], fill="#FFFFFF", outline="#334155", width=2)
    draw.text((800, 685), "TCP/IP Socket: 3306", font=get_font(11, bold=True), fill="#0284C7", anchor="mm")

    img.save("generated_diagrams/component_diagram.png")
    print("Saved component_diagram.png")

if __name__ == "__main__":
    generate_sequence_bill_generation()
    generate_activity_diagram()
    generate_component_diagram()
