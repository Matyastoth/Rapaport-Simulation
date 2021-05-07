package rapaportsimulation;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

/**
 *
 * @author Matt
 */
public class OutputWindow extends JFrame {
    
    public static int exit_status = -1;
    public Container mainpanel;
    public JProgressBar jprogressbar = new JProgressBar(0, 100);
    
    public Canvas canvas = new Canvas();
    
    public JLabel swapnum = new JLabel();
    
    public OutputWindow() throws Exception{
        super(RapaportSimulation.title);
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        
        mainpanel = this.getContentPane();
        mainpanel.setLayout(new BorderLayout());
        
        FlowLayout buttonlayout = new FlowLayout(FlowLayout.CENTER, 5, 20);
        Panel buttons = new Panel(buttonlayout);
        initializeButtons(buttons);
        
        
        //MMMMMEEEEEEEEEEEEEEEEHHHHHHHHHHHHHHHHHHHH
        //Note from 2021: Yes, I was annoyed. Please ignore the previous comment.
        Panel progresspanel = new Panel();
        jprogressbar.setLocale(JProgressBar.getDefaultLocale());
        progresspanel.add(jprogressbar, 0);
        
        
        
        mainpanel.add(progresspanel, BorderLayout.NORTH);
        GridBagLayout gbl = new GridBagLayout();
        Panel hp = new Panel(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.ipady = 10;
        gbc.gridy = 1;
        gbl.setConstraints(canvas, gbc);
        gbc.gridy = 2;
        gbl.setConstraints(swapnum, gbc);
        gbc.gridy = 3;
        gbl.setConstraints(buttons, gbc);
        
        //hp.add(im);
        hp.add(canvas);
        hp.add(swapnum);
        hp.add(buttons);
        mainpanel.add(hp, BorderLayout.CENTER);
    }
    
    private void initializeButtons(Panel buttons){
        JButton recalculatebutton = new JButton("Recalculate");
        //buttons.add(recalculatebutton);
        
        JButton exitbutton = new JButton("Exit");
        buttons.add(exitbutton);
        
        ActionListener exitlistener = (ActionEvent e) -> {
            exitpressed();
        };
        exitbutton.addActionListener(exitlistener);
        
        ActionListener recalculatelistener = (ActionEvent e) -> {
            recalculatepressed();
        };
        recalculatebutton.addActionListener(recalculatelistener);
    }
    
    private void recalculatepressed(){
        exit_status = 1;
    }
    
    private void exitpressed(){
        exit_status = 0;
    }
}
