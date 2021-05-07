/**
 * 
 * This program can be used as a demonstration tool, but it's original purpose
 * was to assist a fellow student in a project.
 * 
 * This is a Rapaport simulation using a Monte-Carlo method. In this program,
 * a polymer solution was injected on sheet coated with freely moving lipids.
 * The goal was to showcase the role of second level interactions between
 * polymer molecules above the lipid sheet.
 * 
 * This program is open-source. You are allowed to use it as a whole program
 * or its components, you are allowed to modify, or redistribute it,
 * while you are doing that for free.

    Aka GNU 2.0.
 */
package rapaportsimulation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.ParseException;
import javax.swing.JFrame;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 *
 * @author Matt
 */
public class RapaportSimulation {

    public static String title = "Rapaport Simulation";
    private static boolean exit = false;
    public static File directory = new File("");
    //Indicates whether the individual frames should be saved after a certain number of steps.
    private static final boolean saveImageboolean = true;

    public static void main(String[] args) throws InterruptedException, Exception {

        //Generating windows.
        InputWindow input = new InputWindow();
        input.pack();
        input.setSize(400, 300);
        input.setResizable(false);
        input.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        input.setLocationRelativeTo(null);

        OutputWindow out = new OutputWindow();
        out.setResizable(false);
        out.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        out.setLocationRelativeTo(null);
        
        //This array will store our variables for the calculations.
        double[] var = new double[InputWindow.component_number];

        while (!exit) {
            if (!exit) {
                inputActive(input, var);
            }
            if (!exit) {
                outputActive(out, var);
            }
        }

        //Releasing any system resources related to this project, then exit.
        System.gc();
        input.dispose();
        out.dispose();
        System.exit(0);
    }

    /**
     * Handling input window object and related operations.
     * @param input
     * @param var
     * @throws InterruptedException
     * @throws IOException 
     */
    public static void inputActive(InputWindow input, double[] var) throws InterruptedException, IOException {
        InputWindow.exit_status = -1;
        input.setVisible(true);
        while (InputWindow.exit_status == -1) {
            Thread.sleep(100);
        }
        input.setVisible(false);
        if (InputWindow.exit_status == 0) {
            for (int i = 0; i < InputWindow.component_number; i++) {
                try {
                    input.components[i].commitEdit();
                } catch (ParseException ex) {
                    throw new IOException("Problem with spinnerfields");
                }
                var[i] = Double.parseDouble(input.components[i].getValue().toString());
            }
        } else {
            exit();
        }
    }

    
    /**
     * Handling the output window and related operations.
     * @param out Output window Object
     * @param var variable storing the input values from the initial window
     * @throws InterruptedException
     * @throws IOException 
     */
    public static void outputActive(OutputWindow out, double[] var) throws InterruptedException, IOException {
        OutputWindow.exit_status = -1;
        
        //Initiating the map for simulations.
        /**
         * True, or 1 stands for polymer, false, or 0 for lipide.
         */
        int[] map = new int[(int) var[2] * (int) var[2]];
        initiateMap(map, var);
        
        out.setSize((int)var[2]*4+150, (int)var[2]*4+150);
        out.setLocationRelativeTo(null);
        out.setVisible(true);
        out.canvas.setSize((int)var[2]*4, (int)var[2]*4);
        out.canvas.createBufferStrategy(3);
        ComputingThread ct = new ComputingThread((int)var[8], map, out, var);
        ct.start();
        while (OutputWindow.exit_status == -1) {
            Thread.sleep(100);
        }
        ct.stop();
        
        out.setVisible(false);
        if (OutputWindow.exit_status == 0) {
            exit();
        }
    }

    public static BufferedImage saveImage(String imagepath, BufferedImage bi, double[] var) throws IOException {
        if (saveImageboolean) {
            ImageIO.write(bi, "png", new File (imagepath));
        }
        return bi;
    }

    public static void initiateMap(int[] map, double[] var) {
        int polymercount = (int) Math.round(var[1]*map.length/100);
        
        Random r = new Random();
        //Sluggish at high dimensions with high polymer concentration.
        //Perfectly distributed data.
        
        Arrays.fill(map, 0);
        ArrayList<Integer> indexes = new ArrayList<>(map.length);
        for (int i = 0; i < map.length; i++) {
            indexes.add(i);
        }
        for (int i = 0; i < polymercount; i ++) {
            int index = r.nextInt(indexes.size());
            map[indexes.get(index)] = 1;
            indexes.remove(index);
        }
    }

    public static void exit() {
        exit = true;
    }
}
