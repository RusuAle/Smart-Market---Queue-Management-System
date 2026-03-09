package GUI;

import BusinessLogic.SelectionPolicy;
import BusinessLogic.SimulationManager;
import Model.Task;
import Model.Server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimulationFrame extends JFrame {
    private JTextField numClientsField, numQueuesField, simulationTimeField;
    private JTextField minArrivalField, maxArrivalField, minServiceField, maxServiceField;
    private JComboBox<String> strategyComboBox;
    private JButton startButton;
    private JTextArea logArea;
    private JScrollPane scrollPane;
    private JPanel visualPanel;
    private JSlider speedSlider;
    private JLabel speedLabel;
    private boolean isManualMode = false;
    private boolean readyForNextStep = false;
    private JCheckBox manualModeCheckbox;
    private JButton showLogButton;
    private String historyLogPath = "simulation_history.txt";

    // Added for manual client addition
    private List<Task> manualClients = new ArrayList<>();
    private JTextField clientArrivalField, clientServiceField;
    private JButton addClientButton;
    private JTextArea clientListArea;
    private int nextClientId = 1; // Auto-generate client IDs

    // Colors for visualization
    private final Color EMPTY_QUEUE_COLOR = new Color(230, 230, 230);
    private final Color ACTIVE_QUEUE_COLOR = new Color(144, 238, 144);
    private final Color CLIENT_COLOR = new Color(70, 130, 180);

    // Colors for title
    private final Color TITLE_BACKGROUND = new Color(25, 118, 210);
    private final Color TITLE_FOREGROUND = Color.WHITE;

    public SimulationFrame() {
        setTitle("Queue Management Simulation");
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Fullscreen at start
        setPreferredSize(new Dimension(1600, 900)); // Set default size if not maximized
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Set up UI manager properties for split pane
        UIManager.put("SplitPane.oneTouchButtonSize", 15);
        UIManager.put("SplitPane.dividerSize", 10);

        // Create store title panel (new addition)
        JPanel titlePanel = createStoreTitle();
        add(titlePanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));

        // Panel for inputs
        JPanel inputPanel = new JPanel(new GridLayout(9, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                "Simulation Settings", 1, 0, new Font("Arial", Font.BOLD, 16)));

        // Create larger fonts
        Font labelFont = new Font("Arial", Font.BOLD, 16);
        Font inputFont = new Font("Arial", Font.PLAIN, 16);

        // Create colored panels for inputs with better styling
        addLabelAndField(inputPanel, "Number of Clients (N):", numClientsField = new JTextField("50"), labelFont, inputFont);
        addLabelAndField(inputPanel, "Number of Queues (Q):", numQueuesField = new JTextField("5"), labelFont, inputFont);
        addLabelAndField(inputPanel, "Simulation Time (seconds):", simulationTimeField = new JTextField("60"), labelFont, inputFont);
        addLabelAndField(inputPanel, "Min Arrival Time:", minArrivalField = new JTextField("2"), labelFont, inputFont);
        addLabelAndField(inputPanel, "Max Arrival Time:", maxArrivalField = new JTextField("40"), labelFont, inputFont);
        addLabelAndField(inputPanel, "Min Service Time:", minServiceField = new JTextField("1"), labelFont, inputFont);
        addLabelAndField(inputPanel, "Max Service Time:", maxServiceField = new JTextField("7"), labelFont, inputFont);

        JLabel strategyLabel = new JLabel("Selection Strategy:");
        strategyLabel.setFont(labelFont);
        inputPanel.add(strategyLabel);

        // In SimulationFrame constructor or where you initialize the strategyComboBox
        strategyComboBox = new JComboBox<>(new String[]{"Shortest Wait Time", "Shortest Queue", "Adaptive"});
        strategyComboBox.setFont(inputFont);
        strategyComboBox.setBackground(new Color(240, 248, 255)); // Alice Blue
        inputPanel.add(strategyComboBox);

        // Manual mode checkbox
        JLabel modeLabel = new JLabel("Step-by-Step Mode:");
        modeLabel.setFont(labelFont);
        inputPanel.add(modeLabel);

        manualModeCheckbox = new JCheckBox("Press ENTER to advance");
        manualModeCheckbox.setFont(inputFont);
        manualModeCheckbox.setSelected(false);
        inputPanel.add(manualModeCheckbox);

        // Simulation speed slider
        JPanel speedPanel = new JPanel(new BorderLayout(5, 0));
        speedLabel = new JLabel("Simulation Speed: Normal");
        speedLabel.setFont(labelFont);
        speedPanel.add(speedLabel, BorderLayout.NORTH);

        speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 10, 5);
        speedSlider.setMajorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setFont(new Font("Arial", Font.PLAIN, 12));
        speedSlider.addChangeListener(e -> {
            int value = speedSlider.getValue();
            if (value < 5) {
                speedLabel.setText("Simulation Speed: Slow " + (5 - value));
            } else if (value > 5) {
                speedLabel.setText("Simulation Speed: Fast " + (value - 5));
            } else {
                speedLabel.setText("Simulation Speed: Normal");
            }
        });
        speedPanel.add(speedSlider, BorderLayout.CENTER);

        // Start button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        startButton = new JButton("Start Simulation");
        startButton.setPreferredSize(new Dimension(250, 50));
        startButton.setFont(new Font("Arial", Font.BOLD, 18));
        startButton.setBackground(new Color(46, 139, 87)); // Sea green
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        buttonPanel.add(startButton);

        showLogButton = new JButton("Show History");
        showLogButton.setPreferredSize(new Dimension(150, 50));
        showLogButton.setFont(new Font("Arial", Font.BOLD, 16));
        showLogButton.setBackground(new Color(70, 130, 180)); // Steel blue
        showLogButton.setForeground(Color.WHITE);
        showLogButton.setFocusPainted(false);
        showLogButton.addActionListener(e -> showHistoryLog());
        buttonPanel.add(showLogButton);

        // Test scenario buttons
        JPanel testButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        addTestButton(testButtonsPanel, "Test Case 1", 1);
        addTestButton(testButtonsPanel, "Test Case 2", 2);
        addTestButton(testButtonsPanel, "Test Case 3", 3);

        // Manual client input panel (new addition)
        JPanel manualClientPanel = createManualClientPanel(labelFont, inputFont);

        // Left panel layout with settings
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.add(inputPanel, BorderLayout.NORTH);
        leftPanel.add(speedPanel, BorderLayout.CENTER);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel layout
        JPanel topContentPanel = new JPanel(new BorderLayout());
        topContentPanel.add(leftPanel, BorderLayout.WEST);
        topContentPanel.add(manualClientPanel, BorderLayout.CENTER);
        topContentPanel.add(testButtonsPanel, BorderLayout.SOUTH);

        mainContent.add(topContentPanel, BorderLayout.NORTH);

        // Log area for events - with larger font
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 18)); // Larger font
        logArea.setBackground(new Color(250, 250, 250));

        // Add key listener for manual mode advancement
        logArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && isManualMode) {
                    readyForNextStep = true;
                    synchronized (SimulationFrame.this) {
                        SimulationFrame.this.notifyAll();
                    }
                }
            }
        });

        scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                "Simulation Log", 1, 0,
                new Font("Arial", Font.BOLD, 16)));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(600, 400)); // Set preferred size

        // Create visual simulation panel
        visualPanel = new JPanel();
        visualPanel.setLayout(new BorderLayout());
        visualPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                "Visual Simulation", 1, 0,
                new Font("Arial", Font.BOLD, 16)));
        visualPanel.setBackground(Color.WHITE);
        visualPanel.setPreferredSize(new Dimension(600, 400)); // Set preferred size


        JScrollPane visualScrollPane = new JScrollPane(visualPanel);
        visualScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        visualScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JSplitPane horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, visualScrollPane);
        horizontalSplitPane.setResizeWeight(0.5); // Equal space initially
        horizontalSplitPane.setOneTouchExpandable(true);
        horizontalSplitPane.setContinuousLayout(true);
        horizontalSplitPane.setDividerSize(10);

        JSplitPane verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topContentPanel, horizontalSplitPane);
        verticalSplitPane.setResizeWeight(0.3); // Give more space to the bottom panels
        verticalSplitPane.setOneTouchExpandable(true);
        verticalSplitPane.setContinuousLayout(true);
        verticalSplitPane.setDividerSize(10);


        scrollPane.setMinimumSize(new Dimension(200, 100));
        visualScrollPane.setMinimumSize(new Dimension(200, 100));
        topContentPanel.setMinimumSize(new Dimension(800, 150));

        mainContent.add(verticalSplitPane, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);

        // Set default test values
        setDefaultTestValues(1); // Default to Test 1

        // Welcome message
        logArea.setText("Welcome to Queue Management Simulation!\n\n"
                + "Ready to begin!");


        pack();
    }

    private JPanel createStoreTitle() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(TITLE_BACKGROUND);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JLabel titleLabel = new JLabel("SMART MARKET - QUEUE MANAGEMENT SYSTEM", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(TITLE_FOREGROUND);

        JLabel subtitleLabel = new JLabel("Efficient Customer Service Solution", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        subtitleLabel.setForeground(new Color(220, 220, 220));

        JPanel labelPanel = new JPanel(new GridLayout(2, 1));
        labelPanel.setBackground(TITLE_BACKGROUND);
        labelPanel.add(titleLabel);
        labelPanel.add(subtitleLabel);

        titlePanel.add(labelPanel, BorderLayout.CENTER);

        return titlePanel;
    }

    private JPanel createManualClientPanel(Font labelFont, Font inputFont) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "Manual Client Entry", 1, 0,
                new Font("Arial", Font.BOLD, 16)));


        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));

        JLabel arrivalLabel = new JLabel("Arrival Time (tarrivare):");
        arrivalLabel.setFont(labelFont);
        inputPanel.add(arrivalLabel);

        clientArrivalField = new JTextField();
        clientArrivalField.setFont(inputFont);
        clientArrivalField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        clientArrivalField.setBackground(new Color(255, 250, 240)); // Floral White
        inputPanel.add(clientArrivalField);

        JLabel serviceLabel = new JLabel("Service Time (tservire):");
        serviceLabel.setFont(labelFont);
        inputPanel.add(serviceLabel);

        clientServiceField = new JTextField();
        clientServiceField.setFont(inputFont);
        clientServiceField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        clientServiceField.setBackground(new Color(255, 250, 240)); // Floral White
        inputPanel.add(clientServiceField);

        // Add button
        addClientButton = new JButton("Add Client");
        addClientButton.setFont(new Font("Arial", Font.BOLD, 16));
        addClientButton.setBackground(new Color(70, 130, 180)); // Steel Blue
        addClientButton.setForeground(Color.WHITE);
        addClientButton.setFocusPainted(false);
        addClientButton.addActionListener(e -> addManualClient());

        // Client list display
        clientListArea = new JTextArea(8, 20); // Increased height
        clientListArea.setEditable(false);
        clientListArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        clientListArea.setBorder(BorderFactory.createTitledBorder("Added Clients"));
        JScrollPane clientListScroll = new JScrollPane(clientListArea);

        // Layout the components
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(addClientButton, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(clientListScroll, BorderLayout.CENTER);

        return panel;
    }
    private void showHistoryLog() {
        try {
            // Check if file exists, create it if it doesn't
            File logFile = new File(historyLogPath);
            if (!logFile.exists()) {
                logFile.createNewFile();
                FileWriter writer = new FileWriter(logFile);
                writer.write("=== Queue Management Simulation History ===\n\n");
                writer.write("No simulations have been run yet.\n");
                writer.close();
            }

            // Open the file with the default system application
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(logFile);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Cannot open file automatically. File location: " + logFile.getAbsolutePath(),
                        "Information", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error opening log file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to add a manual client - MODIFIED to use auto-generated ID
    private void addManualClient() {
        try {
            int arrivalTime = Integer.parseInt(clientArrivalField.getText());
            int serviceTime = Integer.parseInt(clientServiceField.getText());

            Task client = new Task(nextClientId++, arrivalTime, serviceTime);
            manualClients.add(client);

            // Update the client list display
            updateClientListDisplay();

            // Clear input fields
            clientArrivalField.setText("");
            clientServiceField.setText("");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Please enter valid numbers for client details.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Update the display of manually added clients
    private void updateClientListDisplay() {
        StringBuilder sb = new StringBuilder();
        for (Task client : manualClients) {
            sb.append("Client ID: ").append(client.getId())
                    .append(", Arrival: ").append(client.getArrivalTime())
                    .append(", Service: ").append(client.getServiceTime())
                    .append("\n");
        }
        clientListArea.setText(sb.toString());
    }

    // Get the list of manually added clients
    public List<Task> getManualClients() {
        return new ArrayList<>(manualClients);
    }

    // Clear the list of manually added clients
    public void clearManualClients() {
        manualClients.clear();
        clientListArea.setText("");
        nextClientId = 1; // Reset auto-generated ID counter
    }

    private void addLabelAndField(JPanel panel, String labelText, JTextField field, Font labelFont, Font fieldFont) {
        JLabel label = new JLabel(labelText);
        label.setFont(labelFont);
        panel.add(label);

        field.setFont(fieldFont);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        field.setBackground(new Color(240, 248, 255)); // Alice Blue
        panel.add(field);
    }

    private void addTestButton(JPanel panel, String text, int testCase) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(135, 206, 250)); // Light sky blue
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.addActionListener(e -> setDefaultTestValues(testCase));
        panel.add(button);
    }

    public void addStartButtonListener(ActionListener actionListener) {
        startButton.addActionListener(actionListener);
    }

    public void setDefaultTestValues(int testCase) {
        switch (testCase) {
            case 1:
                numClientsField.setText("4");
                numQueuesField.setText("2");
                simulationTimeField.setText("60");
                minArrivalField.setText("2");
                maxArrivalField.setText("30");
                minServiceField.setText("2");
                maxServiceField.setText("4");
                break;
            case 2:
                numClientsField.setText("50");
                numQueuesField.setText("5");
                simulationTimeField.setText("60");
                minArrivalField.setText("2");
                maxArrivalField.setText("40");
                minServiceField.setText("1");
                maxServiceField.setText("7");
                break;
            case 3:
                numClientsField.setText("1000");
                numQueuesField.setText("20");
                simulationTimeField.setText("200");
                minArrivalField.setText("10");
                maxArrivalField.setText("100");
                minServiceField.setText("3");
                maxServiceField.setText("9");
                break;
        }
    }

    public int getNumClients() {
        return Integer.parseInt(numClientsField.getText());
    }

    public int getNumQueues() {
        return Integer.parseInt(numQueuesField.getText());
    }

    public int getSimulationTime() {
        return Integer.parseInt(simulationTimeField.getText());
    }

    public int getMinArrival() {
        return Integer.parseInt(minArrivalField.getText());
    }

    public int getMaxArrival() {
        return Integer.parseInt(maxArrivalField.getText());
    }

    public int getMinService() {
        return Integer.parseInt(minServiceField.getText());
    }

    public int getMaxService() {
        return Integer.parseInt(maxServiceField.getText());
    }


    public SelectionPolicy getSelectedStrategy() {
        int selectedIndex = strategyComboBox.getSelectedIndex();
        switch (selectedIndex) {
            case 0:
                return SelectionPolicy.SHORTEST_TIME;
            case 1:
                return SelectionPolicy.SHORTEST_QUEUE;
            case 2:
                return SelectionPolicy.ADAPTIVE;
            default:
                return SelectionPolicy.SHORTEST_TIME;
        }
    }

    public boolean isManualMode() {
        return manualModeCheckbox.isSelected();
    }

    public int getSimulationSpeed() {
        return speedSlider.getValue();
    }

    public synchronized void waitForUserInput() {
        if (isManualMode) {
            readyForNextStep = false;
            try {
                while (!readyForNextStep) {
                    appendLog("Press ENTER to continue to next step...");
                    wait();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            // Auto-scroll to bottom
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void clearLog() {
        logArea.setText("");
    }

    public void setManualMode(boolean manual) {
        this.isManualMode = manual;
        if (manual) {
            logArea.requestFocusInWindow(); // Focus on the text area for key events
            appendLog("MANUAL MODE: Press ENTER to advance simulation step-by-step");
        }
    }

    public void updateVisualRepresentation(int currentTime, List<Server> servers, int waitingClients) {
        SwingUtilities.invokeLater(() -> {
            visualPanel.removeAll();
            visualPanel.setLayout(new BorderLayout());

            // Top info panel
            JPanel infoPanel = new JPanel();
            infoPanel.setBackground(Color.WHITE);
            JLabel timeLabel = new JLabel("Current Time: " + currentTime);
            timeLabel.setFont(new Font("Arial", Font.BOLD, 16));
            infoPanel.add(timeLabel);

            JLabel waitingLabel = new JLabel("   Waiting Clients: " + waitingClients);
            waitingLabel.setFont(new Font("Arial", Font.BOLD, 16));
            infoPanel.add(waitingLabel);

            visualPanel.add(infoPanel, BorderLayout.NORTH);

            // Queues visualization - use a larger size
            JPanel queuesPanel = new JPanel();
            queuesPanel.setBackground(Color.WHITE);
            queuesPanel.setLayout(new GridLayout(1, servers.size(), 20, 0)); // Increased spacing

            for (int i = 0; i < servers.size(); i++) {
                Server server = servers.get(i);
                boolean queueActive = server.getProcessingTask() != null;
                int queueSize = server.getQueueSize();

                JPanel queuePanel = new JPanel();
                queuePanel.setLayout(new BorderLayout());
                queuePanel.setPreferredSize(new Dimension(200, 600)); // Make queue panels larger
                queuePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

                // Queue label
                JLabel queueLabel = new JLabel("Queue " + server.getQueueId(), JLabel.CENTER);
                queueLabel.setFont(new Font("Arial", Font.BOLD, 18)); // Larger font
                queuePanel.add(queueLabel, BorderLayout.NORTH);

                // Queue visualization
                JPanel queueVis = new JPanel();
                queueVis.setLayout(new BoxLayout(queueVis, BoxLayout.Y_AXIS));

                if (queueSize == 0) {
                    queueVis.setBackground(EMPTY_QUEUE_COLOR);
                    queueVis.add(Box.createVerticalGlue());
                    JLabel emptyLabel = new JLabel("CLOSED", JLabel.CENTER);
                    emptyLabel.setFont(new Font("Arial", Font.ITALIC, 18)); // Larger font
                    emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    queueVis.add(emptyLabel);
                    queueVis.add(Box.createVerticalGlue());
                } else {
                    queueVis.setBackground(queueActive ? ACTIVE_QUEUE_COLOR : EMPTY_QUEUE_COLOR);

                    // Get the tasks to display
                    List<Task> tasksToDisplay = new ArrayList<>();

                    // First add the processing task if any
                    Task processingTask = server.getProcessingTask();
                    if (processingTask != null) {
                        tasksToDisplay.add(processingTask);
                    }

                    // Then add all queued tasks
                    for (Task task : server.getTasks()) {
                        if (processingTask == null || !task.equals(processingTask)) {
                            tasksToDisplay.add(task);
                        }
                    }

                    // Add clients as colored rectangles with detailed info
                    for (int j = 0; j < tasksToDisplay.size(); j++) {
                        Task task = tasksToDisplay.get(j);

                        JPanel clientPanel = new JPanel();
                        clientPanel.setLayout(new GridLayout(3, 1));
                        clientPanel.setPreferredSize(new Dimension(200, 80)); // Taller for more info
                        clientPanel.setMaximumSize(new Dimension(200, 80));
                        clientPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                        // Set background color based on position
                        if (j == 0 && queueActive) {
                            clientPanel.setBackground(new Color(255, 165, 0)); // Orange for processing
                        } else {
                            clientPanel.setBackground(CLIENT_COLOR);
                        }

                        // Client ID
                        JLabel idLabel = new JLabel("ID: " + task.getId(), JLabel.CENTER);
                        idLabel.setForeground(Color.WHITE);
                        idLabel.setFont(new Font("Arial", Font.BOLD, 14));

                        // Service Time
                        JLabel serviceLabel = new JLabel("Service: " + task.getRemainingServiceTime() + "/"
                                + task.getServiceTime(), JLabel.CENTER);
                        serviceLabel.setForeground(Color.WHITE);
                        serviceLabel.setFont(new Font("Arial", Font.PLAIN, 14));

                        // Status label
                        JLabel statusLabel;
                        if (j == 0 && queueActive) {
                            statusLabel = new JLabel("PROCESSING", JLabel.CENTER);
                        } else {
                            statusLabel = new JLabel("WAITING", JLabel.CENTER);
                        }
                        statusLabel.setForeground(Color.WHITE);
                        statusLabel.setFont(new Font("Arial", Font.ITALIC, 14));

                        clientPanel.add(idLabel);
                        clientPanel.add(serviceLabel);
                        clientPanel.add(statusLabel);

                        queueVis.add(clientPanel);
                        queueVis.add(Box.createVerticalStrut(10)); // More spacing between clients
                    }
                }

                queuePanel.add(queueVis, BorderLayout.CENTER);
                queuesPanel.add(queuePanel);
            }

            JScrollPane queuesScrollPane = new JScrollPane(queuesPanel);
            queuesScrollPane.setBorder(null);
            queuesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            queuesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            visualPanel.add(queuesScrollPane, BorderLayout.CENTER);

            visualPanel.revalidate();
            visualPanel.repaint();
        });
    }
    public void displaySimulationEnd(int currentTime, List<Server> servers, double avgWaitingTime, double avgServiceTime, int peakHour) {
        SwingUtilities.invokeLater(() -> {
            visualPanel.removeAll();
            visualPanel.setLayout(new BorderLayout());

            // Create colorful end panel
            JPanel endPanel = new JPanel(new BorderLayout(10, 20));
            endPanel.setBackground(new Color(240, 248, 255)); // Light blue background
            endPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 130, 180), 3),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)));

            // Main title
            JLabel titleLabel = new JLabel("SIMULATION COMPLETED", JLabel.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
            titleLabel.setForeground(new Color(25, 25, 112)); // Dark blue
            endPanel.add(titleLabel, BorderLayout.NORTH);

            // Stats panel
            JPanel statsPanel = new JPanel(new GridLayout(5, 1, 5, 15));
            statsPanel.setBackground(new Color(240, 248, 255));

            // Add stats with colored panels
            addStatLabel(statsPanel, "Simulation Time: " + currentTime + " units", new Color(144, 238, 144));
            addStatLabel(statsPanel, "Average Waiting Time: " + String.format("%.2f", avgWaitingTime), new Color(152, 251, 152));
            addStatLabel(statsPanel, "Average Service Time: " + String.format("%.2f", avgServiceTime), new Color(144, 238, 144));
            addStatLabel(statsPanel, "Peak Hour: " + peakHour, new Color(152, 251, 152));
            addStatLabel(statsPanel, "All Queues Closed", new Color(144, 238, 144));

            endPanel.add(statsPanel, BorderLayout.CENTER);

            // Queues visualization
            JPanel queuesPanel = new JPanel();
            queuesPanel.setBackground(Color.WHITE);
            queuesPanel.setLayout(new GridLayout(1, servers.size(), 20, 0));

            for (Server server : servers) {
                JPanel queuePanel = new JPanel();
                queuePanel.setLayout(new BorderLayout());
                queuePanel.setPreferredSize(new Dimension(200, 150));
                queuePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

                // Queue label
                JLabel queueLabel = new JLabel("Queue " + server.getQueueId(), JLabel.CENTER);
                queueLabel.setFont(new Font("Arial", Font.BOLD, 18));
                queuePanel.add(queueLabel, BorderLayout.NORTH);

                // Empty queue visualization
                JPanel queueVis = new JPanel();
                queueVis.setLayout(new BorderLayout());
                queueVis.setBackground(EMPTY_QUEUE_COLOR);

                JLabel emptyLabel = new JLabel("CLOSED", JLabel.CENTER);
                emptyLabel.setFont(new Font("Arial", Font.ITALIC, 22));
                emptyLabel.setForeground(new Color(128, 0, 0)); // Dark red
                queueVis.add(emptyLabel, BorderLayout.CENTER);

                queuePanel.add(queueVis, BorderLayout.CENTER);
                queuesPanel.add(queuePanel);
            }

            JScrollPane queuesScrollPane = new JScrollPane(queuesPanel);
            queuesScrollPane.setBorder(null);
            endPanel.add(queuesScrollPane, BorderLayout.SOUTH);

            visualPanel.add(endPanel, BorderLayout.CENTER);
            visualPanel.revalidate();
            visualPanel.repaint();
        });
    }

    private void addStatLabel(JPanel panel, String text, Color bgColor) {
        JPanel statPanel = new JPanel();
        statPanel.setBackground(bgColor);
        statPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 18));
        statPanel.add(label);

        panel.add(statPanel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimulationFrame frame = new SimulationFrame();
            frame.setVisible(true);

            frame.addStartButtonListener(e -> {
                try {
                    frame.clearLog();
                    frame.appendLog("Starting simulation...");

                    int numClients = frame.getNumClients();
                    int numQueues = frame.getNumQueues();
                    int simulationTime = frame.getSimulationTime();
                    int minArrival = frame.getMinArrival();
                    int maxArrival = frame.getMaxArrival();
                    int minService = frame.getMinService();
                    int maxService = frame.getMaxService();
                    SelectionPolicy policy = frame.getSelectedStrategy();
                    List<Task> manualClients = frame.getManualClients();

                    frame.appendLog("Configuration:");
                    frame.appendLog("Clients: " + numClients);
                    frame.appendLog("Queues: " + numQueues);
                    frame.appendLog("Simulation Time: " + simulationTime);
                    frame.appendLog("Arrival Time Range: " + minArrival + " - " + maxArrival);
                    frame.appendLog("Service Time Range: " + minService + " - " + maxService);
                    frame.appendLog("Strategy: " + policy);

                    if (!manualClients.isEmpty()) {
                        frame.appendLog("Manual clients added: " + manualClients.size());
                    }

                    frame.appendLog("\nStarting simulation...\n");

                    // Disable start button during simulation
                    frame.startButton.setEnabled(false);

                    // Set manual mode if selected
                    frame.setManualMode(frame.isManualMode());

                    // Create and start simulation manager in a separate thread
                    SimulationManager simulationManager = new SimulationManager(
                            frame, numClients, numQueues, simulationTime,
                            minArrival, maxArrival,
                            minService, maxService,
                            policy, manualClients // Pass manual clients to the SimulationManager
                    );

                    Thread simulationThread = new Thread(simulationManager);
                    simulationThread.start();

                    // Re-enable start button when simulation completes
                    new Thread(() -> {
                        try {
                            simulationThread.join();
                            SwingUtilities.invokeLater(() -> {
                                frame.startButton.setEnabled(true);
                                frame.clearManualClients();
                            });
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }).start();

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame,
                            "Please enter valid numbers in all fields.",
                            "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        });
    }
}