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
# 1. USE CASE DIAGRAM
# =========================================================================
def generate_use_case_diagram():
    w, h = 1600, 950
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "UML Use Case Diagram", "Electricity Billing Management System - Core Use Cases & Roles", w)
    
    # System Boundary Box
    system_box = (340, 90, 1260, 920)
    draw_box(draw, system_box, "#FFFFFF", "#334155", radius=12, width=2)
    draw.text((360, 105), "System Boundary: Electricity Billing Management System", font=get_font(17, bold=True), fill="#1E293B")
    
    # Left Actor: Administrator
    draw_box(draw, (30, 220, 260, 560), "#EFF6FF", "#3B82F6", radius=10, width=2)
    draw.ellipse((120, 240, 170, 290), fill="#2563EB")
    draw.line([(145, 290), (145, 380)], fill="#2563EB", width=5) # body
    draw.line([(95, 330), (195, 330)], fill="#2563EB", width=5) # arms
    draw.line([(145, 380), (105, 460)], fill="#2563EB", width=5) # left leg
    draw.line([(145, 380), (185, 460)], fill="#2563EB", width=5) # right leg
    draw.text((145, 495), "<<Actor>>\nAdministrator", font=get_font(16, bold=True), fill="#1E3A8A", align="center", anchor="mm")

    # Right Actor: Customer
    draw_box(draw, (1340, 220, 1570, 560), "#F0FDF4", "#10B981", radius=10, width=2)
    draw.ellipse((1430, 240, 1480, 290), fill="#059669")
    draw.line([(1455, 290), (1455, 380)], fill="#059669", width=5)
    draw.line([(1405, 330), (1505, 330)], fill="#059669", width=5)
    draw.line([(1455, 380), (1415, 460)], fill="#059669", width=5)
    draw.line([(1455, 380), (1495, 460)], fill="#059669", width=5)
    draw.text((1455, 495), "<<Actor>>\nCustomer", font=get_font(16, bold=True), fill="#065F46", align="center", anchor="mm")

    # Use Cases (Ellipses in center)
    use_cases = [
        ("UC-01: Login & Role Authentication", 165, "#DBEAFE", "#1D4ED8", True, True),
        ("UC-02: Register Customer & Meter Info", 245, "#EFF6FF", "#2563EB", True, False),
        ("UC-03: Manage Tariffs & Tax Rates", 325, "#EFF6FF", "#2563EB", True, False),
        ("UC-04: Calculate Monthly Bill (Units * Rate)", 405, "#EFF6FF", "#2563EB", True, False),
        ("UC-05: View Account Details & Bills", 485, "#F0FDF4", "#059669", False, True),
        ("UC-06: Pay Bill via Payment Gateway", 565, "#F0FDF4", "#059669", False, True),
        ("UC-07: Generate & Print Bill Receipts", 645, "#EFF6FF", "#2563EB", True, True),
        ("UC-08: Submit & Manage Complaints", 725, "#F0FDF4", "#059669", True, True),
        ("UC-09: View Audit Logs & Analytics", 805, "#EFF6FF", "#2563EB", True, False),
    ]

    for uc_text, y_pos, f_col, o_col, admin_link, cust_link in use_cases:
        draw.ellipse([(460, y_pos - 28), (1140, y_pos + 28)], fill=f_col, outline=o_col, width=2)
        draw.text((800, y_pos), uc_text, font=get_font(16, bold=True), fill="#0F172A", anchor="mm")
        
        if admin_link:
            draw.line([(260, 390), (460, y_pos)], fill="#93C5FD", width=2)
        if cust_link:
            draw.line([(1340, 390), (1140, y_pos)], fill="#6EE7B7", width=2)

    img.save("generated_diagrams/use_case_diagram.png")
    print("Saved use_case_diagram.png")

# =========================================================================
# 2. CLASS DIAGRAM (Wide landscape representation)
# =========================================================================
def generate_class_diagram():
    w, h = 1920, 850
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "UML Class Diagram", "Electricity Billing Management System - Structural Hierarchy & OOP Relationships", w)
    
    def draw_class(x, y, w_box, name, stereotype, attrs, methods, header_col="#1E3A8A"):
        h_head = 54
        b_h = 16 + len(attrs)*20 + (12 if attrs else 0) + len(methods)*20 + 12
        tot_h = h_head + b_h
        
        draw.rounded_rectangle([(x, y), (x + w_box, y + tot_h)], radius=8, fill="#FFFFFF", outline="#334155", width=2)
        draw.rounded_rectangle([(x, y), (x + w_box, y + h_head)], radius=8, fill=header_col, outline="#334155", width=2)
        draw.rectangle([(x, y + h_head - 10), (x + w_box, y + h_head)], fill=header_col)
        
        draw.text((x + w_box/2, y + 16), stereotype, font=get_font(12, bold=False), fill="#93C5FD", anchor="mm")
        draw.text((x + w_box/2, y + 38), name, font=get_font(16, bold=True), fill="#FFFFFF", anchor="mm")
        
        cur_y = y + h_head + 10
        for attr in attrs:
            draw.text((x + 12, cur_y), attr, font=get_font(14, bold=False), fill="#1E293B")
            cur_y += 20
        if attrs:
            draw.line([(x, cur_y + 2), (x + w_box, cur_y + 2)], fill="#CBD5E1", width=1)
            cur_y += 8
        for m in methods:
            draw.text((x + 12, cur_y), m, font=get_font(14, bold=False), fill="#0F172A")
            cur_y += 20

    # Top Row: Architecture Core
    draw_class(30, 90, 480, "Project", "<<extends JFrame, implements ActionListener>>",
               ["- String atype, meter", "- JPanel sidebar, mainContent", "- JFrame activeSubWindow"],
               ["+ Project(String atype, String meter)", "+ trackSubWindow(JFrame win, String nav)", "+ setActiveNavigation(String cmd)", "+ closeActiveSubWindow()", "+ actionPerformed(ActionEvent ae)"],
               header_col="#1E3A8A")

    draw_class(535, 90, 360, "Conn", "<<implements AutoCloseable>>",
               ["+ Connection c", "+ Statement s"],
               ["+ Conn()", "+ close()"],
               header_col="#065F46")

    draw_class(920, 90, 460, "SessionContext", "<<Encapsulated Session Model>>",
               ["- final String username", "- final String role", "- final String meterNo"],
               ["+ getUsername(): String", "+ getRole(): String", "+ getMeterNo(): String"],
               header_col="#4C1D95")

    draw_class(1405, 90, 485, "PasswordUtils", "<<Security Utility>>",
               ["- static final int ITERATIONS = 65536", "- static final String ALGORITHM"],
               ["+ static String hashPassword(String plain)", "+ static boolean verifyPassword(plain, hash)"],
               header_col="#831843")

    # Bottom Row: GUI Modules & Workflows
    draw_class(30, 450, 420, "CustomerDetails", "<<extends JFrame, implements ActionListener>>",
               ["- JTable table", "- JButton print, deactivate", "- JTextField tfSearch"],
               ["+ CustomerDetails()", "+ loadCustomers(String query)", "+ actionPerformed(ActionEvent ae)"],
               header_col="#1E293B")

    draw_class(475, 450, 420, "CalculateBill", "<<extends JFrame, implements ActionListener>>",
               ["- Choice meternumber, cmonth", "- JTextField tfunits", "- JButton submit, cancel"],
               ["+ CalculateBill()", "+ actionPerformed(ActionEvent ae)"],
               header_col="#1E293B")

    draw_class(920, 450, 460, "PayBill", "<<extends JFrame, implements ActionListener>>",
               ["- String meter", "- Choice cmonth", "- JLabel labelunits, totalbill", "- JButton pay, back"],
               ["+ PayBill(String meter)", "+ loadBillDetails(String m)", "+ actionPerformed(ActionEvent ae)"],
               header_col="#1E293B")

    draw_class(1405, 450, 485, "PaymentGateway", "<<extends JFrame, implements ActionListener>>",
               ["- String billId, meter, amount", "- JRadioButton rbCard, rbUPI, rbNet", "- JButton btnPayNow"],
               ["+ PaymentGateway(id, meter, m, amt)", "+ processPayment()", "- showPaymentReceipt()"],
               header_col="#0284C7")

    # Connecting Association Lines
    draw.line([(240, 370), (240, 450)], fill="#64748B", width=2) # Project -> CustomerDetails
    draw.line([(380, 370), (600, 450)], fill="#64748B", width=2) # Project -> CalculateBill
    draw.line([(480, 200), (535, 200)], fill="#059669", width=2) # Project uses Conn
    draw.line([(1150, 450), (1405, 450)], fill="#0284C7", width=2) # PayBill -> PaymentGateway

    # Legend / OOP Takeaways Bar
    draw_box(draw, (30, 770, 1890, 830), "#FFFFFF", "#CBD5E1", radius=8, width=1)
    draw.text((45, 782), "OOP Architecture Notes:", font=get_font(15, bold=True), fill="#0F172A")
    draw.text((250, 782), "• Inheritance: All GUI windows extend JFrame   • Interfaces: Event handling via ActionListener   • Encapsulation: SessionContext immutable getters   • Abstraction: Conn abstracts JDBC driver details", font=get_font(14), fill="#334155")

    img.save("generated_diagrams/class_diagram.png")
    print("Saved class_diagram.png")

# =========================================================================
# 3. SEQUENCE DIAGRAM
# =========================================================================
def generate_sequence_diagram():
    w, h = 1600, 950
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "UML Sequence Diagram", "End-to-End Bill Payment and Transaction Settlement Sequence", w)
    
    lifelines = [
        ("Customer\n(User)", 130, "#DBEAFE", "#1D4ED8"),
        (":Project\n(Dashboard)", 370, "#EFF6FF", "#2563EB"),
        (":PayBill\n(Form)", 630, "#EFF6FF", "#2563EB"),
        (":PaymentGateway\n(Payment Form)", 910, "#EFF6FF", "#2563EB"),
        (":Conn / JDBC\n(Data Layer)", 1190, "#F0FDF4", "#059669"),
        ("MySQL DB\n(ebs Database)", 1440, "#FEF3C7", "#D97706")
    ]
    
    for name, x, f_col, o_col in lifelines:
        draw_box(draw, (x - 70, 90, x + 70, 150), f_col, o_col, radius=6, width=2)
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

    draw_msg(190, 130, 370, "1. Click 'Pay Bill' in sidebar")
    draw_msg(240, 370, 630, "2. new PayBill(meterNo)")
    draw_msg(300, 630, 1190, "3. executeQuery('SELECT * FROM bill WHERE meter_no=...')")
    draw_msg(350, 1190, 1440, "4. Query bill records")
    draw_msg(400, 1440, 630, "5. Return units, month, total bill, status", is_return=True, color="#059669")
    draw_msg(460, 130, 630, "6. Select month & click 'Pay'")
    draw_msg(520, 630, 910, "7. new PaymentGateway(meter, month, amount)")
    draw_msg(580, 130, 910, "8. Select payment mode (Card/UPI/NetBanking) & submit")
    draw_msg(640, 910, 1190, "9. conn.setAutoCommit(false); executeUpdate('UPDATE bill SET status=\"Paid\"')")
    draw_msg(700, 1190, 1440, "10. Commit transaction in MySQL")
    draw_msg(750, 1440, 910, "11. Transaction successful", is_return=True, color="#059669")
    draw_msg(810, 910, 630, "12. Display payment receipt dialog", is_return=True, color="#2563EB")
    draw_msg(870, 630, 370, "13. Close window -> Dashboard active navigation", color="#DC2626")

    img.save("generated_diagrams/sequence_diagram.png")
    print("Saved sequence_diagram.png")

# =========================================================================
# 4. ENTITY RELATIONSHIP (ER) DIAGRAM
# =========================================================================
def generate_er_diagram():
    w, h = 1600, 950
    img = Image.new("RGB", (w, h), "#F8FAFC")
    draw = ImageDraw.Draw(img)
    
    draw_header(draw, "Relational Entity-Relationship (ER) Diagram", "Actual MySQL Database Schema for Electricity Billing System (`ebs`)", w)

    def draw_entity(x, y, w_box, title, pk_list, attr_list, header_color="#1E3A8A"):
        header_h = 42
        body_h = 16 + len(pk_list)*24 + (10 if pk_list else 0) + len(attr_list)*24 + 10
        draw.rounded_rectangle([(x, y), (x + w_box, y + header_h + body_h)], radius=8, fill="#FFFFFF", outline="#334155", width=2)
        draw.rounded_rectangle([(x, y), (x + w_box, y + header_h)], radius=8, fill=header_color, outline="#334155", width=2)
        draw.rectangle([(x, y + header_h - 10), (x + w_box, y + header_h)], fill=header_color)
        draw.text((x + w_box/2, y + 21), title, font=get_font(14, bold=True), fill="#FFFFFF", anchor="mm")
        
        cur_y = y + header_h + 10
        for pk in pk_list:
            draw.text((x + 12, cur_y), pk, font=get_font(13, bold=True), fill="#B91C1C")
            cur_y += 24
        if pk_list:
            draw.line([(x, cur_y - 4), (x + w_box, cur_y - 4)], fill="#CBD5E1", width=1)
            cur_y += 6
        for attr in attr_list:
            draw.text((x + 12, cur_y), attr, font=get_font(13, bold=False), fill="#1E293B")
            cur_y += 24

    y_row1 = 120
    y_row2 = 540

    # Row 1: Core Customer & Auth Tables
    draw_entity(30, y_row1, 330, "CUSTOMER (Master Table)", 
                ["[PK] meter_no : VARCHAR(20)"],
                ["name : VARCHAR(100)", "address : VARCHAR(200)", "city : VARCHAR(50)", "state : VARCHAR(50)", "email : VARCHAR(100)", "phone : VARCHAR(20)", "status : VARCHAR(20)"],
                header_color="#1E3A8A")

    draw_entity(420, y_row1, 320, "METER_INFO",
                ["[PK, FK] meter_no : VARCHAR(20)"],
                ["meter_location : VARCHAR(100)", "meter_type : VARCHAR(50)", "phase_code : VARCHAR(20)", "bill_type : VARCHAR(50)", "days : VARCHAR(10)"],
                header_color="#065F46")

    draw_entity(790, y_row1, 310, "LOGIN",
                ["[PK, FK] meter_no : VARCHAR(20)"],
                ["username : VARCHAR(50)", "password : VARCHAR(255)", "name : VARCHAR(100)", "user : VARCHAR(20)"],
                header_color="#4C1D95")

    draw_entity(1150, y_row1, 320, "TAX (Tariff Rates)",
                [],
                ["cost_per_unit : DECIMAL(10,2)", "meter_rent : DECIMAL(10,2)", "service_charge : DECIMAL(10,2)", "swacch_bharat_tax : DECIMAL(10,2)", "fixed_tax : DECIMAL(10,2)"],
                header_color="#B45309")

    # Row 2: Billing & Operational Support Tables
    # Note: BILL strictly matches database/schema.sql (meter_no FK, month, units, total_bill, status)
    draw_entity(30, y_row2, 330, "BILL",
                ["[FK] meter_no : VARCHAR(20)"],
                ["month : VARCHAR(20)", "units : VARCHAR(20)", "total_bill : VARCHAR(20)", "status : VARCHAR(20)"],
                header_color="#1E3A8A")

    draw_entity(420, y_row2, 330, "COMPLAINTS",
                ["[PK] complaint_id : INT AUTO_INC", "[FK] meter_no : VARCHAR(20)"],
                ["reference_number : VARCHAR(30)", "category : VARCHAR(50)", "subject : VARCHAR(150)", "description : TEXT", "priority : VARCHAR(20)", "status : VARCHAR(20)", "created_at : TIMESTAMP"],
                header_color="#991B1B")

    draw_entity(810, y_row2, 340, "COMPLAINT_HISTORY",
                ["[PK] history_id : INT AUTO_INC", "[FK] complaint_id : INT"],
                ["action : VARCHAR(50)", "previous_value : VARCHAR(100)", "new_value : VARCHAR(100)", "performed_by : VARCHAR(50)", "remarks : TEXT", "created_at : TIMESTAMP"],
                header_color="#991B1B")

    draw_entity(1200, y_row2, 320, "AUDIT_LOGS",
                ["[PK] log_id : INT AUTO_INC"],
                ["actor_id : VARCHAR(50)", "actor_role : VARCHAR(20)", "action_type : VARCHAR(50)", "entity_type : VARCHAR(50)", "details : TEXT", "created_at : TIMESTAMP"],
                header_color="#475569")

    # Cardinality Lines
    # 1. CUSTOMER -> METER_INFO (1 --- 1)
    draw.line([(360, y_row1 + 70), (420, y_row1 + 70)], fill="#2563EB", width=2)
    draw.text((390, y_row1 + 50), "1 --- 1", font=get_font(13, bold=True), fill="#1E40AF", anchor="mm")

    # 2. CUSTOMER -> LOGIN (1 --- 1)
    draw.line([(195, y_row1), (195, y_row1 - 25), (945, y_row1 - 25), (945, y_row1)], fill="#7C3AED", width=2)
    draw.text((570, y_row1 - 38), "1 --- 1 (Foreign Key: meter_no)", font=get_font(13, bold=True), fill="#6D28D9", anchor="mm")

    # 3. CUSTOMER -> BILL (1 : N)
    draw.line([(195, y_row1 + 270), (195, y_row2)], fill="#2563EB", width=2)
    draw.text((215, (y_row1 + 270 + y_row2)/2), "1 : N (Foreign Key: meter_no)", font=get_font(13, bold=True), fill="#1E40AF")

    # 4. CUSTOMER -> COMPLAINTS (1 : N)
    draw.line([(360, y_row1 + 230), (390, y_row1 + 230), (390, y_row2 + 60), (420, y_row2 + 60)], fill="#DC2626", width=2)
    draw.text((375, y_row1 + 215), "1 : N", font=get_font(13, bold=True), fill="#B91C1C", anchor="mm")

    # 5. COMPLAINTS -> COMPLAINT_HISTORY (1 : N)
    draw.line([(750, y_row2 + 80), (810, y_row2 + 80)], fill="#DC2626", width=2)
    draw.text((780, y_row2 + 60), "1 : N", font=get_font(13, bold=True), fill="#B91C1C", anchor="mm")

    img.save("generated_diagrams/er_diagram.png")
    print("Saved er_diagram.png")

if __name__ == "__main__":
    generate_use_case_diagram()
    generate_class_diagram()
    generate_sequence_diagram()
    generate_er_diagram()
