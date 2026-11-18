package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

public class AnalyticsDashboard extends JFrame implements ActionListener {

    private final AnalyticsService.FilterCriteria filters = new AnalyticsService.FilterCriteria();

    // Filter controls
    private JComboBox<String> cbMonth;
    private JComboBox<String> cbBillStatus;
    private JTextField tfCustomer;
    private JComboBox<String> cbCompCategory;
    private JComboBox<String> cbCompStatus;
    private JComboBox<String> cbCompPriority;
    private JButton btnApplyFilters, btnResetFilters, btnExportCSV, btnClose;

    // Tabs
    private JTabbedPane tabbedPane;

    // Tab 1: Overview KPIs
    private JLabel lblKpiBilled, lblKpiCollected, lblKpiOutstanding, lblKpiCollectionRate, lblKpiUnits, lblKpiCustomers;
    private ChartComponents.BarChartPanel chartMonthlyBilled;
    private ChartComponents.DoughnutChartPanel chartPaymentStatus;

    // Tab 2: Billing & Revenue
    private JLabel lblRevCollected, lblRevOutstanding, lblRevClassified, lblRevRate, lblRevCommercial;
    private ChartComponents.BarChartPanel chartMonthlyRevenue;
    private JTable tableBillingLedger;
    private DefaultTableModel modelBillingLedger;

    // Tab 3: Consumption
    private JLabel lblConsTotalUnits, lblConsAvgUnits, lblConsTopName, lblConsLowName;
    private ChartComponents.BarChartPanel chartMonthlyUnits;
    private ChartComponents.BarChartPanel chartTopConsumers;
    private JTable tableConsumption;
    private DefaultTableModel modelConsumption;

    // Tab 4: Customer
    private JLabel lblCustTotal, lblCustActive, lblCustInactive;
    private JTable tableCustomers;
    private DefaultTableModel modelCustomers;

    // Tab 5: Complaints
    private JLabel lblCompTotal, lblCompResolved, lblCompInProgress, lblCompPending, lblCompRejected, lblCompRate;
    private ChartComponents.BarChartPanel chartComplaintStatus;
    private ChartComponents.BarChartPanel chartComplaintPriority;
    private JTable tableComplaints;
    private DefaultTableModel modelComplaints;

    // Tab 6: Reports & Export
    private JComboBox<String> cbReportType;
    private JTable tableExportPreview;
    private DefaultTableModel modelExportPreview;

    public AnalyticsDashboard() {
        super("Electricity Billing System - Executive Analytics & Business Intelligence Hub");
        setSize(1260, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // 1. Top Header & Filter Toolbar
        JPanel topContainer = createHeaderAndFilterToolbar();
        add(topContainer, BorderLayout.NORTH);

        // 2. Tabbed Content Center
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabbedPane.setBackground(Color.WHITE);

        Color tabIconTint = new Color(71, 85, 105);
        tabbedPane.addTab("Executive Overview", Project.getScaledIcon("icon/icons8-dashboard-48.png", 16, tabIconTint), createOverviewTab());
        tabbedPane.addTab("Billing & Revenue", Project.getScaledIcon("icon/Total Revenue.png", 16, tabIconTint), createBillingRevenueTab());
        tabbedPane.addTab("Power Consumption", Project.getScaledIcon("icon/icons8-gauge-50.png", 16, tabIconTint), createConsumptionTab());
        tabbedPane.addTab("Customer Insights", Project.getScaledIcon("icon/Total Customers.png", 16, tabIconTint), createCustomerTab());
        tabbedPane.addTab("Complaint Analytics", Project.getScaledIcon("icon/icons8-edit-text-file-50.png", 16, tabIconTint), createComplaintsTab());
        tabbedPane.addTab("Reports & CSV Export", Project.getScaledIcon("icon/icons8-graph-axes-24.png", 16, tabIconTint), createReportsExportTab());

        add(tabbedPane, BorderLayout.CENTER);

        // 3. Bottom Status Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        bottomBar.setBackground(new Color(248, 250, 252));
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JLabel lblNotice = new JLabel("⚡ Strictly Read-Only Analytics Engine • Live MySQL 8.4 Connected");
        lblNotice.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblNotice.setForeground(new Color(100, 116, 139));
        bottomBar.add(lblNotice);

        btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.setBackground(new Color(15, 23, 42));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(this);
        bottomBar.add(btnClose);

        add(bottomBar, BorderLayout.SOUTH);

        // Load initial analytics
        refreshAllAnalytics();

        setVisible(true);
    }

    private JPanel createHeaderAndFilterToolbar() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(new Color(248, 250, 252));
        container.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        // Title row
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        titlePanel.setOpaque(false);
        JLabel titleLbl = new JLabel("Executive Analytics & Reporting Center");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLbl.setForeground(new Color(15, 23, 42));
        titlePanel.add(titleLbl);

        JLabel subLbl = new JLabel("— Real-Time Billing, Revenue, Consumption, Customer & Grievance Intelligence");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(new Color(100, 116, 139));
        titlePanel.add(subLbl);
        container.add(titlePanel);

        // Filter toolbar row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterRow.setOpaque(false);

        // Month
        filterRow.add(createFilterLabel("Month:"));
        cbMonth = new JComboBox<>(new String[]{
            "All Months", "January", "February", "March", "April", "May", "June", 
            "July", "August", "September", "October", "November", "December"
        });
        styleFilterCombo(cbMonth);
        filterRow.add(cbMonth);

        // Bill Status
        filterRow.add(createFilterLabel("Bill Status:"));
        cbBillStatus = new JComboBox<>(new String[]{"All Statuses", "Paid", "Not Paid", "Commercial"});
        styleFilterCombo(cbBillStatus);
        filterRow.add(cbBillStatus);

        // Customer search
        filterRow.add(createFilterLabel("Customer / Meter:"));
        tfCustomer = new JTextField(10);
        tfCustomer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfCustomer.setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225)), BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        filterRow.add(tfCustomer);

        // Complaint Category
        filterRow.add(createFilterLabel("Category:"));
        List<String> catList = new ArrayList<>();
        catList.add("All Categories");
        for (String c : ComplaintService.CATEGORIES) catList.add(c);
        cbCompCategory = new JComboBox<>(catList.toArray(new String[0]));
        styleFilterCombo(cbCompCategory);
        filterRow.add(cbCompCategory);

        // Complaint Status
        filterRow.add(createFilterLabel("Comp Status:"));
        cbCompStatus = new JComboBox<>(new String[]{"All Statuses", "Pending", "In Progress", "Resolved", "Rejected"});
        styleFilterCombo(cbCompStatus);
        filterRow.add(cbCompStatus);

        // Complaint Priority
        filterRow.add(createFilterLabel("Priority:"));
        cbCompPriority = new JComboBox<>(new String[]{"All Priorities", "Low", "Medium", "High", "Urgent"});
        styleFilterCombo(cbCompPriority);
        filterRow.add(cbCompPriority);

        // Action buttons
        btnApplyFilters = new JButton("Apply Filters");
        btnApplyFilters.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnApplyFilters.setBackground(new Color(37, 99, 235)); // Royal Blue
        btnApplyFilters.setForeground(Color.WHITE);
        btnApplyFilters.setFocusPainted(false);
        btnApplyFilters.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnApplyFilters.addActionListener(this);
        filterRow.add(btnApplyFilters);

        btnResetFilters = new JButton("Reset");
        btnResetFilters.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnResetFilters.setBackground(new Color(241, 245, 249));
        btnResetFilters.setForeground(new Color(51, 65, 85));
        btnResetFilters.setFocusPainted(false);
        btnResetFilters.addActionListener(this);
        filterRow.add(btnResetFilters);

        btnExportCSV = new JButton("Export CSV");
        btnExportCSV.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportCSV.setBackground(new Color(16, 185, 129)); // Emerald Green
        btnExportCSV.setForeground(Color.WHITE);
        btnExportCSV.setFocusPainted(false);
        btnExportCSV.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExportCSV.addActionListener(this);
        filterRow.add(btnExportCSV);

        container.add(filterRow);
        return container;
    }

    private JLabel createFilterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(new Color(51, 65, 85));
        return l;
    }

    private void styleFilterCombo(JComboBox<?> cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cb.setBackground(Color.WHITE);
    }

    // =========================================================================
    // TAB 1: EXECUTIVE OVERVIEW
    // =========================================================================

    private JPanel createOverviewTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setBackground(new Color(248, 250, 252));
        tab.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // 6 KPI Cards Grid (2 rows x 3 columns)
        JPanel kpiGrid = new JPanel(new GridLayout(2, 3, 14, 12));
        kpiGrid.setOpaque(false);
        kpiGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        kpiGrid.setPreferredSize(new Dimension(1100, 228));
        kpiGrid.setMinimumSize(new Dimension(600, 200));
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        lblKpiBilled = new JLabel("₹0.00");
        lblKpiCollected = new JLabel("₹0.00");
        lblKpiOutstanding = new JLabel("₹0.00");
        lblKpiCollectionRate = new JLabel("0.0%");
        lblKpiUnits = new JLabel("0.0 Units");
        lblKpiCustomers = new JLabel("0 Customers");

        kpiGrid.add(createKpiCard("Total Billed Amount", lblKpiBilled, "Cumulative across all invoices", new Color(37, 99, 235)));
        kpiGrid.add(createKpiCard("Collected Revenue", lblKpiCollected, "Paid invoices only (Verified)", new Color(16, 185, 129)));
        kpiGrid.add(createKpiCard("Outstanding Due", lblKpiOutstanding, "Not Paid invoices balance", new Color(239, 68, 68)));
        kpiGrid.add(createKpiCard("Collection Efficiency", lblKpiCollectionRate, "Paid vs Not Paid Ratio", new Color(13, 148, 136)));
        kpiGrid.add(createKpiCard("Total Energy Consumption", lblKpiUnits, "Metered kilowatt-hours (Units)", new Color(245, 158, 11)));
        kpiGrid.add(createKpiCard("Active Accounts", lblKpiCustomers, "Registered service connections", new Color(139, 92, 246)));

        tab.add(kpiGrid);
        tab.add(Box.createVerticalStrut(14));

        // Charts Side-by-Side
        JPanel chartRow = new JPanel(new GridLayout(1, 2, 16, 0));
        chartRow.setOpaque(false);
        chartRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartRow.setPreferredSize(new Dimension(1100, 270));
        chartRow.setMinimumSize(new Dimension(600, 220));
        chartRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 340));

        chartMonthlyBilled = new ChartComponents.BarChartPanel("Monthly Billing Comparison (Billed Amount)", "₹", "");
        chartMonthlyBilled.setDefaultColor(new Color(37, 99, 235));
        chartRow.add(chartMonthlyBilled);

        chartPaymentStatus = new ChartComponents.DoughnutChartPanel("Bill Payment Distribution (Count & Ratio)");
        chartPaymentStatus.setCustomColor("Paid", new Color(16, 185, 129));
        chartPaymentStatus.setCustomColor("Not Paid", new Color(239, 68, 68));
        chartPaymentStatus.setCustomColor("Commercial", new Color(100, 116, 139));
        chartRow.add(chartPaymentStatus);

        tab.add(chartRow);
        tab.add(Box.createVerticalStrut(12));

        // Non-standard notice badge
        JPanel noticeCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        noticeCard.setBackground(new Color(254, 243, 199)); // Amber-100
        noticeCard.setBorder(BorderFactory.createLineBorder(new Color(252, 211, 77), 1, true));
        noticeCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        noticeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel noticeLbl = new JLabel("ⓘ Note on Accuracy: Historical Commercial records (MET005) are classified separately and explicitly excluded from Paid/Not Paid collection ratios.");
        noticeLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        noticeLbl.setForeground(new Color(146, 64, 14));
        noticeCard.add(noticeLbl);
        tab.add(noticeCard);

        return wrapInScroll(tab);
    }

    // =========================================================================
    // TAB 2: BILLING & REVENUE ANALYTICS
    // =========================================================================

    private JPanel createBillingRevenueTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setBackground(new Color(248, 250, 252));
        tab.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Revenue Breakdown Cards
        JPanel revCardRow = new JPanel(new GridLayout(1, 5, 12, 0));
        revCardRow.setOpaque(false);
        revCardRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        lblRevCollected = new JLabel("₹0.00");
        lblRevOutstanding = new JLabel("₹0.00");
        lblRevClassified = new JLabel("₹0.00");
        lblRevRate = new JLabel("0.0%");
        lblRevCommercial = new JLabel("₹0.00");

        revCardRow.add(createMiniKpi("Collected Revenue", lblRevCollected, new Color(16, 185, 129)));
        revCardRow.add(createMiniKpi("Outstanding Due", lblRevOutstanding, new Color(239, 68, 68)));
        revCardRow.add(createMiniKpi("Total Standard Billed", lblRevClassified, new Color(37, 99, 235)));
        revCardRow.add(createMiniKpi("Collection %", lblRevRate, new Color(13, 148, 136)));
        revCardRow.add(createMiniKpi("Commercial (Isolated)", lblRevCommercial, new Color(100, 116, 139)));

        tab.add(revCardRow);
        tab.add(Box.createVerticalStrut(16));

        // Monthly Revenue Comparison Bar Chart
        chartMonthlyRevenue = new ChartComponents.BarChartPanel("Revenue Collected vs Expected per Month", "₹", "");
        chartMonthlyRevenue.setDefaultColor(new Color(16, 185, 129));
        chartMonthlyRevenue.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        tab.add(chartMonthlyRevenue);
        tab.add(Box.createVerticalStrut(16));

        // Detailed Billing Table
        JLabel tblTitle = new JLabel("Detailed Customer Billing Ledger (Filtered Dataset)");
        tblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblTitle.setForeground(new Color(15, 23, 42));
        tab.add(tblTitle);
        tab.add(Box.createVerticalStrut(6));

        String[] cols = {"Meter No", "Customer Name", "Billing Month", "Units Consumed", "Total Bill (₹)", "Status"};
        modelBillingLedger = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableBillingLedger = createStyledTable(modelBillingLedger);
        JScrollPane sp = new JScrollPane(tableBillingLedger);
        sp.setPreferredSize(new Dimension(1100, 240));
        tab.add(sp);

        return wrapInScroll(tab);
    }

    // =========================================================================
    // TAB 3: CONSUMPTION ANALYTICS
    // =========================================================================

    private JPanel createConsumptionTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setBackground(new Color(248, 250, 252));
        tab.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRow.setOpaque(false);
        kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        lblConsTotalUnits = new JLabel("0.0 Units");
        lblConsAvgUnits = new JLabel("0.0 Units");
        lblConsTopName = new JLabel("-");
        lblConsLowName = new JLabel("-");

        kpiRow.add(createMiniKpi("Total Grid Consumption", lblConsTotalUnits, new Color(245, 158, 11)));
        kpiRow.add(createMiniKpi("Avg Units / Invoice", lblConsAvgUnits, new Color(37, 99, 235)));
        kpiRow.add(createMiniKpi("Peak Consumer", lblConsTopName, new Color(239, 68, 68)));
        kpiRow.add(createMiniKpi("Minimum Consumer", lblConsLowName, new Color(16, 185, 129)));
        tab.add(kpiRow);
        tab.add(Box.createVerticalStrut(16));

        // Two charts: Monthly units + Top 5 consumers
        JPanel chartRow = new JPanel(new GridLayout(1, 2, 16, 0));
        chartRow.setOpaque(false);
        chartRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        chartMonthlyUnits = new ChartComponents.BarChartPanel("Monthly Power Consumption (Units)", "", " U");
        chartMonthlyUnits.setDefaultColor(new Color(245, 158, 11));
        chartRow.add(chartMonthlyUnits);

        chartTopConsumers = new ChartComponents.BarChartPanel("Highest Consumption Customers (Top Units)", "", " U");
        chartTopConsumers.setDefaultColor(new Color(139, 92, 246));
        chartRow.add(chartTopConsumers);

        tab.add(chartRow);
        tab.add(Box.createVerticalStrut(16));

        // Customer Consumption Table
        JLabel tblTitle = new JLabel("Customer Aggregate Consumption Breakdown");
        tblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblTitle.setForeground(new Color(15, 23, 42));
        tab.add(tblTitle);
        tab.add(Box.createVerticalStrut(6));

        String[] cols = {"Meter Number", "Customer Name", "Total Units Consumed", "Cumulative Billed Amount (₹)", "Invoices Count"};
        modelConsumption = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableConsumption = createStyledTable(modelConsumption);
        JScrollPane sp = new JScrollPane(tableConsumption);
        sp.setPreferredSize(new Dimension(1100, 240));
        tab.add(sp);

        return wrapInScroll(tab);
    }

    // =========================================================================
    // TAB 4: CUSTOMER ANALYTICS
    // =========================================================================

    private JPanel createCustomerTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setBackground(new Color(248, 250, 252));
        tab.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel kpiRow = new JPanel(new GridLayout(1, 3, 14, 0));
        kpiRow.setOpaque(false);
        kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        lblCustTotal = new JLabel("0");
        lblCustActive = new JLabel("0");
        lblCustInactive = new JLabel("0");

        kpiRow.add(createMiniKpi("Total Registered Customers", lblCustTotal, new Color(37, 99, 235)));
        kpiRow.add(createMiniKpi("Active Accounts (Operational)", lblCustActive, new Color(16, 185, 129)));
        kpiRow.add(createMiniKpi("Inactive Accounts (Deactivated)", lblCustInactive, new Color(245, 158, 11)));
        tab.add(kpiRow);
        tab.add(Box.createVerticalStrut(16));

        // Customer directory table
        JLabel tblTitle = new JLabel("Customer Directory & Account Consumption Ledger");
        tblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblTitle.setForeground(new Color(15, 23, 42));
        tab.add(tblTitle);
        tab.add(Box.createVerticalStrut(6));

        String[] cols = {"Meter No", "Customer Name", "City", "Phone", "Account Status", "Invoices Count", "Total Units", "Total Billed (₹)"};
        modelCustomers = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableCustomers = createStyledTable(modelCustomers);
        JScrollPane sp = new JScrollPane(tableCustomers);
        sp.setPreferredSize(new Dimension(1100, 420));
        tab.add(sp);

        return wrapInScroll(tab);
    }

    // =========================================================================
    // TAB 5: COMPLAINT ANALYTICS
    // =========================================================================

    private JPanel createComplaintsTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setBackground(new Color(248, 250, 252));
        tab.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel kpiRow = new JPanel(new GridLayout(1, 6, 10, 0));
        kpiRow.setOpaque(false);
        kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        lblCompTotal = new JLabel("0");
        lblCompResolved = new JLabel("0");
        lblCompInProgress = new JLabel("0");
        lblCompPending = new JLabel("0");
        lblCompRejected = new JLabel("0");
        lblCompRate = new JLabel("0.0%");

        kpiRow.add(createMiniKpi("Total Grievances", lblCompTotal, new Color(15, 23, 42)));
        kpiRow.add(createMiniKpi("Resolved", lblCompResolved, new Color(16, 185, 129)));
        kpiRow.add(createMiniKpi("In Progress", lblCompInProgress, new Color(2, 132, 199)));
        kpiRow.add(createMiniKpi("Pending", lblCompPending, new Color(245, 158, 11)));
        kpiRow.add(createMiniKpi("Rejected", lblCompRejected, new Color(239, 68, 68)));
        kpiRow.add(createMiniKpi("Resolution Rate", lblCompRate, new Color(13, 148, 136)));
        tab.add(kpiRow);
        tab.add(Box.createVerticalStrut(16));

        // Two charts: Status distribution + Priority distribution
        JPanel chartRow = new JPanel(new GridLayout(1, 2, 16, 0));
        chartRow.setOpaque(false);
        chartRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        chartComplaintStatus = new ChartComponents.BarChartPanel("Grievances by Status Workflow", "", "");
        chartComplaintStatus.setCustomColor("Pending", new Color(245, 158, 11)); // Amber
        chartComplaintStatus.setCustomColor("In Progress", new Color(2, 132, 199)); // Blue
        chartComplaintStatus.setCustomColor("Resolved", new Color(16, 185, 129)); // Green
        chartComplaintStatus.setCustomColor("Rejected", new Color(239, 68, 68)); // Red
        chartRow.add(chartComplaintStatus);

        chartComplaintPriority = new ChartComponents.BarChartPanel("Grievances by Priority Level", "", "");
        chartComplaintPriority.setCustomColor("Low", new Color(22, 163, 74));     // Green
        chartComplaintPriority.setCustomColor("Medium", new Color(37, 99, 235));  // Blue
        chartComplaintPriority.setCustomColor("High", new Color(234, 88, 12));    // Orange
        chartComplaintPriority.setCustomColor("Urgent", new Color(220, 38, 38));  // Red
        chartRow.add(chartComplaintPriority);

        tab.add(chartRow);
        tab.add(Box.createVerticalStrut(16));

        // Complaints Report Table
        JLabel tblTitle = new JLabel("Complaints & Grievances Register");
        tblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblTitle.setForeground(new Color(15, 23, 42));
        tab.add(tblTitle);
        tab.add(Box.createVerticalStrut(6));

        String[] cols = {"ID", "Reference No", "Meter No", "Customer Name", "Category", "Subject", "Priority", "Status", "Assigned Admin", "Created Date"};
        modelComplaints = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableComplaints = createStyledTable(modelComplaints);
        JScrollPane sp = new JScrollPane(tableComplaints);
        sp.setPreferredSize(new Dimension(1100, 240));
        tab.add(sp);

        return wrapInScroll(tab);
    }

    // =========================================================================
    // TAB 6: REPORTS & CSV EXPORT
    // =========================================================================

    private JPanel createReportsExportTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setBackground(new Color(248, 250, 252));
        tab.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        controlBar.setOpaque(false);
        controlBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        JLabel l = new JLabel("Select Report Table:");
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        controlBar.add(l);

        cbReportType = new JComboBox<>(new String[]{
            "Billing Summary Report",
            "Revenue & Collection Summary Report",
            "Consumption Summary Report",
            "Customer Summary Report",
            "Complaints Summary Report"
        });
        cbReportType.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbReportType.setBackground(Color.WHITE);
        cbReportType.addActionListener(e -> updateExportPreview());
        controlBar.add(cbReportType);

        JButton btnExport = new JButton("Save / Export to CSV...");
        btnExport.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExport.setBackground(new Color(16, 185, 129));
        btnExport.setForeground(Color.WHITE);
        btnExport.setFocusPainted(false);
        btnExport.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExport.addActionListener(this);
        controlBar.add(btnExport);

        tab.add(controlBar);
        tab.add(Box.createVerticalStrut(10));

        modelExportPreview = new DefaultTableModel();
        tableExportPreview = createStyledTable(modelExportPreview);
        JScrollPane sp = new JScrollPane(tableExportPreview);
        sp.setPreferredSize(new Dimension(1100, 520));
        tab.add(sp);

        return wrapInScroll(tab);
    }

    // =========================================================================
    // DATA REFRESH & ASYNC EXECUTION
    // =========================================================================

    public void refreshAllAnalytics() {
        // Read current filter selections from UI
        filters.month = (String) cbMonth.getSelectedItem();
        filters.billStatus = (String) cbBillStatus.getSelectedItem();
        filters.customerMeter = tfCustomer.getText() != null ? tfCustomer.getText().trim() : "";
        filters.complaintCategory = (String) cbCompCategory.getSelectedItem();
        filters.complaintStatus = (String) cbCompStatus.getSelectedItem();
        filters.complaintPriority = (String) cbCompPriority.getSelectedItem();

        // Run heavy database queries off the Swing Event Dispatch Thread
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            AnalyticsService.BillingSummary billingSummary;
            AnalyticsService.RevenueSummary revenueSummary;
            AnalyticsService.ConsumptionSummary consumptionSummary;
            AnalyticsService.CustomerSummary customerSummary;
            AnalyticsService.ComplaintAnalyticsSummary complaintSummary;

            @Override
            protected Void doInBackground() {
                billingSummary = AnalyticsService.getBillingSummary(filters);
                revenueSummary = AnalyticsService.getRevenueSummary(filters);
                consumptionSummary = AnalyticsService.getConsumptionSummary(filters);
                customerSummary = AnalyticsService.getCustomerSummary(filters);
                complaintSummary = AnalyticsService.getComplaintSummary(filters);
                return null;
            }

            @Override
            protected void done() {
                updateUIWithData(billingSummary, revenueSummary, consumptionSummary, customerSummary, complaintSummary);
            }
        };
        worker.execute();
    }

    private void updateUIWithData(
        AnalyticsService.BillingSummary billing,
        AnalyticsService.RevenueSummary revenue,
        AnalyticsService.ConsumptionSummary consumption,
        AnalyticsService.CustomerSummary customers,
        AnalyticsService.ComplaintAnalyticsSummary complaints
    ) {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        // 1. Overview KPIs
        lblKpiBilled.setText("₹" + nf.format(billing.totalBilledAmount));
        lblKpiCollected.setText("₹" + nf.format(revenue.totalCollectedRevenue));
        lblKpiOutstanding.setText("₹" + nf.format(revenue.totalOutstandingAmount));
        lblKpiCollectionRate.setText(String.format(Locale.US, "%.2f%%", revenue.collectionPercentage));
        lblKpiUnits.setText(String.format(Locale.US, "%,.1f Units", consumption.totalUnitsConsumed));
        lblKpiCustomers.setText(customers.activeCustomers + " Active (" + customers.totalCustomers + " Total)");

        // Overview Charts
        chartMonthlyBilled.setData(billing.monthBilledAmounts);

        Map<String, Integer> paymentCounts = new LinkedHashMap<>();
        paymentCounts.put("Paid", revenue.paidCount);
        paymentCounts.put("Not Paid", revenue.unpaidCount);
        if (revenue.commercialCount > 0) {
            paymentCounts.put("Commercial", revenue.commercialCount);
        }
        chartPaymentStatus.setData(paymentCounts);

        // 2. Billing & Revenue Tab
        lblRevCollected.setText("₹" + nf.format(revenue.totalCollectedRevenue));
        lblRevOutstanding.setText("₹" + nf.format(revenue.totalOutstandingAmount));
        lblRevClassified.setText("₹" + nf.format(revenue.totalStandardBilled));
        lblRevRate.setText(String.format(Locale.US, "%.2f%%", revenue.collectionPercentage));
        lblRevCommercial.setText("₹" + nf.format(revenue.commercialExcludedAmount));

        chartMonthlyRevenue.setData(billing.monthBilledAmounts);

        modelBillingLedger.setRowCount(0);
        for (AnalyticsService.CustomerBillRow r : billing.customerRows) {
            modelBillingLedger.addRow(new Object[]{
                r.meterNo, r.customerName, r.month, r.units, "₹" + nf.format(r.totalBill), r.status
            });
        }

        // 3. Consumption Tab
        lblConsTotalUnits.setText(String.format(Locale.US, "%,.1f U", consumption.totalUnitsConsumed));
        double avgU = billing.totalBills > 0 ? consumption.totalUnitsConsumed.doubleValue() / billing.totalBills : 0.0;
        lblConsAvgUnits.setText(String.format(Locale.US, "%.1f Units", avgU));
        lblConsTopName.setText(!consumption.topConsumers.isEmpty() ? 
            consumption.topConsumers.get(0).customerName + " (" + consumption.topConsumers.get(0).totalUnits + " U)" : "N/A");
        lblConsLowName.setText(!consumption.lowestConsumers.isEmpty() ? 
            consumption.lowestConsumers.get(0).customerName + " (" + consumption.lowestConsumers.get(0).totalUnits + " U)" : "N/A");

        chartMonthlyUnits.setData(consumption.monthlyConsumption);

        Map<String, Double> top5 = new LinkedHashMap<>();
        for (AnalyticsService.CustomerConsumptionRow r : consumption.topConsumers) {
            top5.put(r.customerName, r.totalUnits.doubleValue());
        }
        chartTopConsumers.setData(top5);

        modelConsumption.setRowCount(0);
        for (AnalyticsService.CustomerConsumptionRow r : consumption.customerConsumptionList) {
            modelConsumption.addRow(new Object[]{
                r.meterNo, r.customerName, r.totalUnits, "₹" + nf.format(r.totalBilled), r.billCount
            });
        }

        // 4. Customer Tab
        lblCustTotal.setText(String.valueOf(customers.totalCustomers));
        lblCustActive.setText(String.valueOf(customers.activeCustomers));
        lblCustInactive.setText(String.valueOf(customers.inactiveCustomers));

        modelCustomers.setRowCount(0);
        for (AnalyticsService.CustomerDetailRow r : customers.customerDetails) {
            modelCustomers.addRow(new Object[]{
                r.meterNo, r.name, r.city, r.phone, r.status, r.billCount, r.totalUnits, "₹" + nf.format(r.totalBilled)
            });
        }

        // 5. Complaints Tab
        lblCompTotal.setText(String.valueOf(complaints.totalComplaints));
        lblCompResolved.setText(String.valueOf(complaints.resolvedCount));
        lblCompInProgress.setText(String.valueOf(complaints.inProgressCount));
        lblCompPending.setText(String.valueOf(complaints.pendingCount));
        lblCompRejected.setText(String.valueOf(complaints.rejectedCount));
        lblCompRate.setText(String.format(Locale.US, "%.1f%%", complaints.resolutionPercentage));

        Map<String, Integer> compStatusCounts = new LinkedHashMap<>();
        compStatusCounts.put("Pending", complaints.pendingCount);
        compStatusCounts.put("In Progress", complaints.inProgressCount);
        compStatusCounts.put("Resolved", complaints.resolvedCount);
        compStatusCounts.put("Rejected", complaints.rejectedCount);
        chartComplaintStatus.setData(compStatusCounts);

        Map<String, Integer> compPriCounts = new LinkedHashMap<>();
        compPriCounts.put("Low", complaints.lowPriorityCount);
        compPriCounts.put("Medium", complaints.mediumPriorityCount);
        compPriCounts.put("High", complaints.highPriorityCount);
        compPriCounts.put("Urgent", complaints.urgentPriorityCount);
        chartComplaintPriority.setData(compPriCounts);

        modelComplaints.setRowCount(0);
        for (AnalyticsService.ComplaintReportRow r : complaints.complaintRows) {
            modelComplaints.addRow(new Object[]{
                r.complaintId, r.referenceNumber, r.meterNo, r.customerName, r.category, 
                r.subject, r.priority, r.status, r.assignedAdmin != null ? r.assignedAdmin : "Unassigned", r.createdAt
            });
        }

        // 6. Update Export Preview Tab
        updateExportPreview();
    }

    private void updateExportPreview() {
        if (cbReportType == null) return;
        String selected = (String) cbReportType.getSelectedItem();
        if (selected == null) return;

        modelExportPreview.setRowCount(0);
        modelExportPreview.setColumnCount(0);

        if (selected.startsWith("Billing")) {
            for (int i = 0; i < modelBillingLedger.getColumnCount(); i++) {
                modelExportPreview.addColumn(modelBillingLedger.getColumnName(i));
            }
            for (int r = 0; r < modelBillingLedger.getRowCount(); r++) {
                Vector<Object> row = new Vector<>();
                for (int c = 0; c < modelBillingLedger.getColumnCount(); c++) {
                    row.add(modelBillingLedger.getValueAt(r, c));
                }
                modelExportPreview.addRow(row);
            }
        } else if (selected.startsWith("Revenue")) {
            modelExportPreview.addColumn("Revenue Metric");
            modelExportPreview.addColumn("Amount / Metric Value");
            modelExportPreview.addColumn("Classification Details");

            modelExportPreview.addRow(new Object[]{"Collected Revenue (Paid Bills)", lblRevCollected.getText(), "Actual revenue collected from Paid bills"});
            modelExportPreview.addRow(new Object[]{"Outstanding Due (Not Paid)", lblRevOutstanding.getText(), "Uncollected pending revenue"});
            modelExportPreview.addRow(new Object[]{"Total Standard Billed", lblRevClassified.getText(), "Paid + Not Paid combined billed total"});
            modelExportPreview.addRow(new Object[]{"Collection Efficiency Rate", lblRevRate.getText(), "Paid Revenue / Standard Billed %"});
            modelExportPreview.addRow(new Object[]{"Commercial (Isolated Records)", lblRevCommercial.getText(), "Historical Commercial bills (MET005)"});
        } else if (selected.startsWith("Consumption")) {
            for (int i = 0; i < modelConsumption.getColumnCount(); i++) {
                modelExportPreview.addColumn(modelConsumption.getColumnName(i));
            }
            for (int r = 0; r < modelConsumption.getRowCount(); r++) {
                Vector<Object> row = new Vector<>();
                for (int c = 0; c < modelConsumption.getColumnCount(); c++) {
                    row.add(modelConsumption.getValueAt(r, c));
                }
                modelExportPreview.addRow(row);
            }
        } else if (selected.startsWith("Customer")) {
            for (int i = 0; i < modelCustomers.getColumnCount(); i++) {
                modelExportPreview.addColumn(modelCustomers.getColumnName(i));
            }
            for (int r = 0; r < modelCustomers.getRowCount(); r++) {
                Vector<Object> row = new Vector<>();
                for (int c = 0; c < modelCustomers.getColumnCount(); c++) {
                    row.add(modelCustomers.getValueAt(r, c));
                }
                modelExportPreview.addRow(row);
            }
        } else if (selected.startsWith("Complaints")) {
            for (int i = 0; i < modelComplaints.getColumnCount(); i++) {
                modelExportPreview.addColumn(modelComplaints.getColumnName(i));
            }
            for (int r = 0; r < modelComplaints.getRowCount(); r++) {
                Vector<Object> row = new Vector<>();
                for (int c = 0; c < modelComplaints.getColumnCount(); c++) {
                    row.add(modelComplaints.getValueAt(r, c));
                }
                modelExportPreview.addRow(row);
            }
        }
    }

    private void handleExportCSV() {
        if (modelExportPreview.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data available in the current report to export.", "Empty Report", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        String reportName = ((String) cbReportType.getSelectedItem()).replaceAll("[^a-zA-Z0-9_]", "_");
        fileChooser.setSelectedFile(new File(reportName + ".csv"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }

            int colCount = modelExportPreview.getColumnCount();
            String[] headers = new String[colCount];
            for (int i = 0; i < colCount; i++) {
                headers[i] = modelExportPreview.getColumnName(i);
            }

            List<String[]> rows = new ArrayList<>();
            for (int r = 0; r < modelExportPreview.getRowCount(); r++) {
                String[] row = new String[colCount];
                for (int c = 0; c < colCount; c++) {
                    Object val = modelExportPreview.getValueAt(r, c);
                    row[c] = val != null ? val.toString() : "";
                }
                rows.add(row);
            }

            boolean success = AnalyticsService.exportToCSV(fileToSave, headers, rows);
            if (success) {
                JOptionPane.showMessageDialog(this, 
                    "Report exported successfully!\nFile: " + fileToSave.getAbsolutePath() + "\nTotal Rows: " + rows.size(), 
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to write CSV file. Please check file permissions.", "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == btnApplyFilters) {
            refreshAllAnalytics();
        } else if (ae.getSource() == btnResetFilters) {
            cbMonth.setSelectedIndex(0);
            cbBillStatus.setSelectedIndex(0);
            tfCustomer.setText("");
            cbCompCategory.setSelectedIndex(0);
            cbCompStatus.setSelectedIndex(0);
            cbCompPriority.setSelectedIndex(0);
            refreshAllAnalytics();
        } else if (ae.getSource() == btnExportCSV || ae.getActionCommand().equals("Save / Export to CSV...")) {
            handleExportCSV();
        } else if (ae.getSource() == btnClose) {
            dispose();
        }
    }

    // =========================================================================
    // UI BUILDER HELPERS
    // =========================================================================

    private JPanel createKpiCard(String title, JLabel valueLbl, String subtext, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        card.setPreferredSize(new Dimension(360, 108));
        card.setMinimumSize(new Dimension(220, 96));

        // Top title row with accent indicator
        JPanel topP = new JPanel(new BorderLayout());
        topP.setOpaque(false);
        topP.setPreferredSize(new Dimension(topP.getPreferredSize().width, 18));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(new Color(71, 85, 105));
        topP.add(titleLbl, BorderLayout.WEST);

        JPanel pill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                int size = 10;
                int x = getWidth() - size;
                int y = (getHeight() - size) / 2;
                g2.fillOval(x, y, size, size);
                g2.dispose();
            }
        };
        pill.setOpaque(false);
        pill.setPreferredSize(new Dimension(14, 18));
        topP.add(pill, BorderLayout.EAST);
        card.add(topP, BorderLayout.NORTH);

        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLbl.setForeground(new Color(15, 23, 42));
        valueLbl.setVerticalAlignment(SwingConstants.CENTER);
        valueLbl.setPreferredSize(new Dimension(valueLbl.getPreferredSize().width, 36));
        valueLbl.setMinimumSize(new Dimension(50, 28));
        card.add(valueLbl, BorderLayout.CENTER);

        JLabel subLbl = new JLabel(subtext);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLbl.setForeground(new Color(148, 163, 184));
        subLbl.setPreferredSize(new Dimension(subLbl.getPreferredSize().width, 16));
        card.add(subLbl, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createMiniKpi(String title, JLabel valueLbl, Color accent) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 11));
        t.setForeground(new Color(71, 85, 105));
        card.add(t, BorderLayout.NORTH);

        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        valueLbl.setForeground(accent);
        card.add(valueLbl, BorderLayout.CENTER);

        return card;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable tbl = new JTable(model);
        tbl.setRowHeight(28);
        tbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tbl.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tbl.getTableHeader().setBackground(new Color(241, 245, 249));
        tbl.getTableHeader().setForeground(new Color(15, 23, 42));
        tbl.setShowGrid(true);
        tbl.setGridColor(new Color(226, 232, 240));
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(new Color(15, 23, 42));
                }
                return c;
            }
        });
        return tbl;
    }

    private JPanel wrapInScroll(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        wrapper.add(sp, BorderLayout.CENTER);
        return wrapper;
    }

    public static void main(String[] args) {
        new AnalyticsDashboard();
    }
}
