package rapaportsimulation;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author Matt
 */
public class InputWindow extends JFrame {

    private static final int temperature = 1;
    private static final int polconcentration = 50;
    private static final int lipidconcentration = 100-polconcentration;
    private static final int fieldsize = 100;

    public static final int component_number = 10;

    JSpinner[] components = new JSpinner[component_number];

    {
        components[0] = new JSpinner(new SpinnerNumberModel(lipidconcentration, 0, 100, 0.1));
        components[1] = new JSpinner(new SpinnerNumberModel(polconcentration, 0, 100, 0.1));
        components[2] = new JSpinner(new SpinnerNumberModel(fieldsize, 10, 150, 1));
        components[3] = new JSpinner(new SpinnerNumberModel(temperature, 0.01, 10000, 0.01));
        components[4] = new JSpinner(new SpinnerNumberModel(1, -10000, 10000, 1));
        components[5] = new JSpinner(new SpinnerNumberModel(1, -10000, 10000, 1));
        components[6] = new JSpinner(new SpinnerNumberModel(2, -10000, 10000, 1));
        components[7] = new JSpinner(new SpinnerNumberModel(2, -10000, 10000, 1));
        components[8] = new JSpinner(new SpinnerNumberModel(100000, 1000, 100000, 100));
        components[9] = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
    }
    String[] labeltitles = new String[component_number];
    {
        labeltitles[0] = "Lipid concentration (%)";
        labeltitles[1] = "Polymer concentration (%)";
        labeltitles[2] = "Dimension (will be a square)";
        labeltitles[3] = "Temperature (relative)";
        labeltitles[4] = "Interaction strength L-L";
        labeltitles[5] = "Interaction strength L-P";
        labeltitles[6] = "Interaction strength P-P";
        labeltitles[7] = "Interaction strength P-P2";
        labeltitles[8] = "Number of steps per exp(*1000)";
        labeltitles[9] = "Number of experiments";
    }
    
    JLabel[] jlabels = new JLabel[component_number];

    public static int exit_status = -1;

    public InputWindow() throws Exception {
        super(RapaportSimulation.title);
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        Container pane = this.getContentPane();

        pane.setLayout(
                new BorderLayout());

        /**
         * These are the input ordering commands.
         */
        GridLayout inputlayout = new GridLayout(1, 2);

        inputlayout.setHgap(
                20);
        Panel inputpanel = new Panel(inputlayout);

        initializePanel(inputpanel);

        /**
         * These are the button-ordering commands.
         */
        Panel buttons = new Panel(new FlowLayout(FlowLayout.CENTER, 40, 10));

        initializeButtons(buttons);

        pane.add(inputpanel,
                "Center");
        pane.add(buttons,
                "South");
    }

    private void initializePanel(Panel inputpanel) {
        //Creating the labels and setting the size of spinners.
        for (int i = 0; i < component_number; i++) {
            jlabels[i] = new JLabel(labeltitles[i]);
        }
        for (int i = 0; i < component_number; i++) {
            JComponent editor = components[i].getEditor();
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setColumns(4);
        }

        //Ordering our labels.
        BorderLayout mainlabellayout = new BorderLayout();
        Panel mainlabel = new Panel(mainlabellayout);

        GridBagLayout labelLayout = new GridBagLayout();
        Panel spinnerlabels = new Panel(labelLayout);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.ipady = 7;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.VERTICAL;
        for (int j = 0; j < component_number; j++) {
            gbc.gridy = j + 1;
            labelLayout.setConstraints(jlabels[j], gbc);
            spinnerlabels.add(jlabels[j]);
        }
        mainlabel.add(spinnerlabels, "East");

        //This part makes our spinners 'listen' to eachother. The sum is always 100.
        components[0].addChangeListener((ChangeEvent e) -> {
            components[1].getModel().setValue(100 - Double.parseDouble(components[0].getModel().getValue().toString()));
        });
        components[1].addChangeListener((ChangeEvent e) -> {
            components[0].getModel().setValue(100 - Double.parseDouble(components[1].getModel().getValue().toString()));
        });

        //Ordering the spinners.
        Panel mainspinnerpanel = new Panel(new BorderLayout());
        GridBagLayout spinnerlayout = new GridBagLayout();
        Panel inputs = new Panel(spinnerlayout);
        gbc.ipady = 1;
        gbc.anchor = GridBagConstraints.WEST;
        for (int j = 0; j < component_number; j++) {
            gbc.gridy = j + 1;
            spinnerlayout.setConstraints(components[j], gbc);
            inputs.add(components[j]);
        }
        mainspinnerpanel.add(inputs, "West");

        inputpanel.add(mainlabel);
        inputpanel.add(mainspinnerpanel);
    }

    private void initializeButtons(Panel buttons) {

        JButton choosedir = new JButton("Choose directory...");
        buttons.add(choosedir);
        JButton okbutton = new JButton("OK");
        JButton cancelbutton = new JButton("Cancel");
        buttons.add(okbutton);
        buttons.add(cancelbutton);

        /**
         * Listeners for buttons.
         */
        ActionListener chdlistener = (ActionEvent e) -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(new File("."));

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                RapaportSimulation.directory = chooser.getSelectedFile();
            }
        };
        choosedir.addActionListener(chdlistener);

        ActionListener oklistener = (ActionEvent e) -> {
            exit_status = 0;
        };
        okbutton.addActionListener(oklistener);

        ActionListener cancellistener = (ActionEvent e) -> {
            exit_status = 1;
        };
        cancelbutton.addActionListener(cancellistener);
    }
}
