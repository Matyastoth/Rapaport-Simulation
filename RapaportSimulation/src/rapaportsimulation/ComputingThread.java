/**
 * A separate class handling the calculations.
 * Contains sensitive commands, modify it with care.
 *
 * Bitwise operations help a lot. In the future, should try using C/C++ for faster computing.
 */
package rapaportsimulation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class ComputingThread implements Runnable {

    private static int exit_status = 0;
    private static int runs = 0;
    private static int[] map;
    private Thread thread;
    private final OutputWindow outwindow;
    //Similar to the main thread; storing the input variables.
    private static double[] var;
    private static double[] weights = new double[121];
    private static double weightsum = 0;
    
    //Molar fraction. Shows how much of the polymers are 'clumped up'.
    private static double xone = -1;

    
    String[] labeltitles = new String[10];
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
    
    public ComputingThread(int run, int[] bmap, OutputWindow outw, double[] variables) {
        runs = run * 1000;
        map = bmap;
        outwindow = outw;
        var = variables;
    }

    @Override
    public void run() {
        //After the stop() is called, finishes the cycle and exits.

        //Array for random swapping directions.
        int[][] SwapDirections = new int[12][2];
        int[][] DirectionsFirstOrder = new int[12][2];
        int[][] DirectionsSecondOrder = new int[24][2];
        initializeDirections(SwapDirections, DirectionsFirstOrder, DirectionsSecondOrder);
        
        //Width of gaussian function.
        double sigma = 2;
        
        for (int i = 0; i < 11; i++) {
            for (int j = 0 ; j < 11; j++) {
                weights[i*11 + j] = Math.exp(-( (double) ((i-5)*(i-5))/(2*(sigma*sigma)) + (double) ((j-5)*(j-5))/(2*(sigma*sigma)) ));
                weightsum += weights[i*11 + j];
            }
        }
        
        //Simply put, I didn't know how to handle this big numbers in java - I wrote this some 7 years ago.
        outwindow.jprogressbar.setIndeterminate(true);
        outwindow.jprogressbar.setStringPainted(true);
        
        //Other variables needed for the run.
        Random r = new Random();
        BufferedImage bi;
        String imagepath = RapaportSimulation.directory.getAbsolutePath() + "\\";
        int swap_num = 0;

        BufferStrategy bs = outwindow.canvas.getBufferStrategy();
        long time = System.currentTimeMillis();
        
        //The cycle itself.
        for ( int j = 0; j < var[9] * exit_status; j++ ) {
            RapaportSimulation.initiateMap(map, var);
            outwindow.jprogressbar.setString(String.valueOf( (int)(j*100/var[9]) + "%" ));
            for (int i = 0; i < runs * exit_status; i++) {
                //Simulation...
                if (i % 100000 == 0 && var[9] == 1) {
                    //Outputs an image to show the progress of the simulation.
                    outwindow.jprogressbar.setString(String.valueOf((i / 10) / (runs / 1000)) + "%");
                    bi = drawImage(map, var);
                    Graphics g = bs.getDrawGraphics();
                    {
                        g.setColor(Color.black);
                        g.fillRect(0, 0, outwindow.canvas.getWidth(), outwindow.canvas.getHeight());
                        g.drawImage(bi, 0, 0, outwindow.canvas.getWidth(), outwindow.canvas.getHeight(), null);
                    }
                    g.dispose();
                    bs.show();
                    outwindow.swapnum.setText(String.valueOf(swap_num));
                    try {
                        RapaportSimulation.saveImage(imagepath.concat("iteration_" + i + ".png"), bi, var);
                    } catch (Exception ex) {
                        System.err.println("Printing image named " + imagepath + " was unsuccessful.");
                    }
                }
                //The step.
                swap_num += step(map, var, r, SwapDirections, DirectionsFirstOrder, DirectionsSecondOrder);
                //debugstep(map, SwapDirections, i, var, DirectionsFirstOrder, DirectionsSecondOrder);
            }
            if (xone == -1) {
                xone = getMolFraction(map, DirectionsFirstOrder, var);
            }
            else {
                xone = (xone + getMolFraction(map, DirectionsFirstOrder, var))/2;
            }
        }
        try {
            finalCommands(time, swap_num);
        } catch (IOException ex) {
            System.err.println("Log file print wasnt successful");
        }
    }
    
    
    /**
     * Calculates the molar fraction of the material 'clumped up'.
     * @param map
     * @param dfo
     * @param var
     * @return 
     */
    private double getMolFraction(int[] map, int[][] dfo, double[] var) {
        int neighbourcount;
        int clusteredcount = 0;
        for (int i = 0; i < map.length; i++) {
            neighbourcount = 0;
            for (int j = 0; j < dfo.length/2; j++) {
                if ( map[getNeighbourLocation(i, (int)var[2],
                        dfo[j + 6*(i/(int)var[2])%2] ) ]  == 1 &&
                        map[i] == 1) {
                    neighbourcount++;
                }
            }
            if (neighbourcount > 1) {
                clusteredcount++;
            }
        }
        return (double) clusteredcount/(var[1]*var[2]*var[2]/100);
    }
    
    private BufferedImage debugstep(BufferedImage bi) {
        int width = bi.getWidth();
        int height = bi.getHeight();
        int x;
        int y;
        int count = 0;
        int scale = 4;
        double color = 0;
        int m = (int) var[2];
        BufferedImage b = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        int[] pixeldata = new int[width*height];
        
        for (int i = 0; i < pixeldata.length; i++) {
            x = i%bi.getWidth();
            y = i/bi.getWidth();
            
            //System.out.println(x + "\t" + y);
            
            color = 0;
            //count = 0;
            for (int j = 0; j < 11; j++) {
                for (int k = 0; k < 11; k++) {
                    /*
                        (1 - map[ //Column number.
                        ((x / scale * m))
                        //nth row. Depends on where we are, as this is hexagonal.
                        + (((x + scale) % (scale * 2)) >> 2) * (((y + m * scale + scale / 2) % (m * scale)) / scale)
                        + ((x % (scale * 2)) >> 2) * (y / scale)]) * 0xffffff);
                    */
                    color += (double) ((map[
                        ((((x + j - 5 + bi.getWidth()) % bi.getWidth()) / scale * m))
                        + ((((x + j - 5 + bi.getWidth()) % bi.getWidth() + scale) % (scale * 2)) >> 2) * ((((y + k - 5 + bi.getHeight()) % bi.getHeight() + m * scale + scale / 2) % (m * scale)) / scale)
                        + (((x + j - 5 + bi.getWidth()) % bi.getWidth() % (scale * 2)) >> 2) * (((y + k - 5 + bi.getHeight()) % bi.getHeight()) / scale)])
                                * weights[11*j + k]);
                    //count -= bi.getRGB( ( width + x + j - 1) % width, ( height + y + k - 1) % height );
                }
            }
            color = color / weightsum;
            //System.out.println(color + "\t" + Integer.toHexString( (int) ( ((int) (255 * color)) * 65536)) + "\t" + Integer.toHexString((int) ( ((int) (255 * color)) * 256 ) ) + "\t" + Integer.toHexString( (int) ( (255 * color) ) ) );
            pixeldata[i] = (int) (( (int) (176 * color)) * 65536 + ((int) (127 * color)) * 256 + (int) (31 * color));
        }
        
        b.setRGB(0, 0, width, height, pixeldata, 0, width);
        
        return b;
        /**
         * Show the location of neighbours.
         * 
         *  int location = 27;
         *  int neighloc = getNeighbourLocation(location, (int) var[2], sd[6 + i]);
         *  //map[neighloc] = 1;
         *  System.out.println(sd[6 + i][0] + "\t" + sd[6 + i][1]);
         *  System.out.println("\t" + countEnergy(map, location, neighloc, var, dfo, dso));
        */
    }

    private int step(int[] map, double[] variables, Random random, int[][] swapdirection, int[][] directionsFirstOrder, int[][] directionSecondOrder) {
        //First step: find a pair.
        int mapdimension = (int) variables[2];
        int location = random.nextInt(map.length);
        int randomdirection = random.nextInt(6);

        //The coordinates of the neighbour.
        int neighbourlocation = getNeighbourLocation(location, mapdimension,
                swapdirection[randomdirection + 6 * ((location / mapdimension) % 2)]);

        //Get the energy associated to this swap. That's an integer!
        int energy = countEnergy(map, location, neighbourlocation, variables, directionsFirstOrder, directionSecondOrder);

        /**
         * There's no real point to use bit-level operations here.
         *
         * I use Boltzmann distribution.
         */
        int was_swap = 0;
        //Methropolis method:
        //Math.random() < (Math.pow(Math.E, ((-1)*energy/var[3])))
        //My version:
        //Math.random() < (1/(1+(Math.pow(Math.E, (energy/var[3])))))
        //As a note, the two methods do not give a percievably different result in this program.
        /**
         * Added note: the Methropolis method prefers swapping, while the formula I intended to use gives a smoother energy function.
         */
        
        //The exponential function should be avoided as that is a huge time waste. Instead, one should take the logarithm of these, but this is more exact.
        
        if (Math.random() < (Math.pow(Math.E, ((-1) * energy / var[3])))) {
            was_swap++;
            swap(map, location, neighbourlocation);
        }

        return was_swap;
    }

    /**
     * This method calculates how much energy is gained or lost by swapping the randomly chosen L/P - L/P pair.
     * The output of this method is used as a boundary for determining how likely is a swap to happen.
     * @param map
     * @param location
     * @param neighbourlocation
     * @param var
     * @param dfo
     * @param dso
     * @return 
     */
    private int countEnergy(int[] map, int location, int neighbourlocation, double[] var, int[][] dfo, int[][] dso) {
        int energy = 0;
        int mapdimension = (int) var[2];

        //Energy at the original place.
        for (int i = 0; i < dfo.length / 2; i++) {
            //Add the original location's energy...
            energy += (int) var[4
                    + map[location]
                    + map[getNeighbourLocation(location, mapdimension, dfo[i + 6 * ((location / mapdimension) % 2)])]];
            energy += (int) var[4
                    + map[neighbourlocation]
                    + map[getNeighbourLocation(neighbourlocation, mapdimension, dfo[i + 6 * ((neighbourlocation / mapdimension) % 2)])]];

            //Minus the new location's energy.
            swap(map, location, neighbourlocation);
            energy -= (int) var[4
                    + map[location]
                    + map[getNeighbourLocation(location, mapdimension, dfo[i + 6 * ((location / mapdimension) % 2)])]];
            energy -= (int) var[4
                    + map[neighbourlocation]
                    + map[getNeighbourLocation(neighbourlocation, mapdimension, dfo[i + 6 * ((neighbourlocation / mapdimension) % 2)])]];
            swap(map, location, neighbourlocation);
        }
        /**
         * If this is a polymer-lipide pair, then does this loop. This is an
         * interesting trick. We have to differentiate the three cases. The sum
         * of L-L, P-L, P-P will give 0, 1, 2; we have to do the loop if the sum
         * is 1. I use the two's binary representations power in this: If you
         * reverse the bit-order of 1, it'll be negative, while the other ones
         * won't. I can use that for the loop.
         *
         */
        int variable = (dso.length / 2) * (-1) * (Integer.reverse(map[location] + map[neighbourlocation]) >> 31);
        int secondcount;
        
        //In case this is not a polymer-lipide pair, the 'variable' is negative, so the method won't enter this loop.
        //Technically, this is just a fancy way to avoid using an IF statement, but not any more effective.
        for (int i = 0; i < variable; i++) {
            secondcount = 0;
            //Add the old locations...
            secondcount += map[location] * map[getNeighbourLocation(location, mapdimension, dso[i + 12 * ((location / mapdimension) % 2)])];
            secondcount += map[neighbourlocation] * map[getNeighbourLocation(neighbourlocation, mapdimension, dso[i + 12 * ((neighbourlocation / mapdimension) % 2)])];

            //Minus the new locations.
            swap(map, location, neighbourlocation);

            secondcount -= map[location] * map[getNeighbourLocation(location, mapdimension, dso[i + 12 * ((location / mapdimension) % 2)])];
            secondcount -= map[neighbourlocation] * map[getNeighbourLocation(neighbourlocation, mapdimension, dso[i + 12 * ((neighbourlocation / mapdimension) % 2)])];

            swap(map, location, neighbourlocation);
            energy += secondcount * (int) var[7];
        }
        return energy;
    }

    /**
     * Swapping locations. Super primitive method, but hey, it works!
     * @param map
     * @param location
     * @param neighbourlocation 
     */
    private void swap(int[] map, int location, int neighbourlocation) {
        int swap_help = map[location];
        map[location] = map[neighbourlocation];
        map[neighbourlocation] = swap_help;
    }

    /**
     * Drawing the output picture aften a given number of steps.
     * Trying hard to avoid branching - this was one of the harder steps to solve in the program.
     * Note from 2021 (7 years later): The scale CAN be changed to any power of two - 
     *  the bit shifts need to be adjusted accordingly.
     * 
     * @param map
     * @param var
     * @return 
     */
    private BufferedImage drawImage(int[] map, double[] var) {
        //Don't change the scale!
        int scale = 4;
        //m means mapdimension. Me mlike malliteration.
        int m = (int) var[2];
        BufferedImage bi = new BufferedImage(m * scale, m * scale, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < m * scale; x++) {
            for (int y = 0; y < m * scale; y++) {
                //Interesting. This is hexagonal drawing.
                //This only works with scale = 4 !
                bi.setRGB(x, y,
                        (1 - map[ //Column number.
                        ((x / scale * m))
                        //nth row. Depends on where we are, as this is hexagonal.
                        + (((x + scale) % (scale * 2)) >> 2) * (((y + m * scale + scale / 2) % (m * scale)) / scale)
                        + ((x % (scale * 2)) >> 2) * (y / scale)]) * 0xffffff);
            }
        }
        return bi;
    }

    /**
     * This method gets the given neighbour relative to actual location. Uses a
     * periodic map.
     *
     * @param location
     * @param mapdimension
     * @param direction
     * @return
     */
    private int getNeighbourLocation(int location, int mapdimension, int[] direction) {
        return 
                (location / mapdimension + mapdimension + direction[0]) % mapdimension * mapdimension
                + (location % mapdimension + mapdimension + direction[1]) % mapdimension;
    }
    /**
     * This method, as the name suggests, initiates the different directional
     *  arrays for easier reference in other methods.
     * @param sd
     * @param dfo
     * @param dso 
     */
    private void initializeDirections(int[][] sd, int[][] dfo, int[][] dso) {
        sd[0] = new int[]{-1, -1};
        sd[1] = new int[]{-1, 0};
        sd[2] = new int[]{0, -1};
        sd[3] = new int[]{0, 1};
        sd[4] = new int[]{1, -1};
        sd[5] = new int[]{1, 0};
        sd[6] = new int[]{-1, 0};
        sd[7] = new int[]{-1, 1};
        sd[8] = new int[]{0, -1};
        sd[9] = new int[]{0, 1};
        sd[10] = new int[]{1, 0};
        sd[11] = new int[]{1, 1};
        //6*((location/mapdimension)%2)

        dfo[0] = new int[]{-1, -1};
        dfo[1] = new int[]{-1, 0};
        dfo[2] = new int[]{0, -1};
        dfo[3] = new int[]{0, 1};
        dfo[4] = new int[]{1, -1};
        dfo[5] = new int[]{1, 0};
        dfo[6] = new int[]{-1, 0};
        dfo[7] = new int[]{-1, 1};
        dfo[8] = new int[]{0, -1};
        dfo[9] = new int[]{0, 1};
        dfo[10] = new int[]{1, 0};
        dfo[11] = new int[]{1, 1};

        dso[0] = new int[]{-2, -1};
        dso[1] = new int[]{-2, 0};
        dso[2] = new int[]{-2, 1};
        dso[3] = new int[]{-1, -2};
        dso[4] = new int[]{-1, 1};
        dso[5] = new int[]{0, -2};
        dso[6] = new int[]{0, 2};
        dso[7] = new int[]{1, -2};
        dso[8] = new int[]{1, 1};
        dso[9] = new int[]{2, -1};
        dso[10] = new int[]{2, 0};
        dso[11] = new int[]{2, 1};
        dso[12] = new int[]{-2, -1};
        dso[13] = new int[]{-2, 0};
        dso[14] = new int[]{-2, 1};
        dso[15] = new int[]{-1, -1};
        dso[16] = new int[]{-1, 2};
        dso[17] = new int[]{0, -2};
        dso[18] = new int[]{0, 2};
        dso[19] = new int[]{1, -1};
        dso[20] = new int[]{1, 2};
        dso[21] = new int[]{2, -1};
        dso[22] = new int[]{2, 0};
        dso[23] = new int[]{2, 1};
    }

    private void finalCommands(long time, int swap_num) throws IOException {
        outwindow.jprogressbar.setIndeterminate(false);
        outwindow.jprogressbar.setValue(100);
        outwindow.jprogressbar.setString("Simulation finished");
        outwindow.swapnum.setText(String.valueOf(swap_num));
        System.out.println(System.currentTimeMillis() - time);
        
        System.out.println("Molfraction: " + xone);
        if (exit_status == 1) {
            System.out.println("Normal termination.");
            System.out.println("Successful swap number: " + swap_num + " out of " + runs + " runs.");
            
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("Log.txt")));
            
            {
                for (int i = 0; i < labeltitles.length; i++) {
                    print(bw, labeltitles[i]);
                    print(bw, "\t");
                    println(bw, String.valueOf(var[i]));
                }
                println(bw, "");
                println(bw, "Molfraction:\t" + String.valueOf(xone));
                println(bw, "(mol polymer in cluster vs mol polymer overall)");
            }
        } else {
            System.out.println("Process stopped manually.");
        }
    }
    
    private void print(BufferedWriter bw, String s) throws IOException{
        bw.write(s);
        bw.flush();
    }
    
    private void println(BufferedWriter bw, String s) throws IOException{
        bw.write(s);
        bw.write("\r\n");
        bw.flush();
    }
    
    public void start() {
        synchronized (ComputingThread.class) {
            exit_status = 1;
        }
        thread = new Thread(this, "Computing");
        thread.start();
    }

    public void stop() {
        synchronized (ComputingThread.class) {
            exit_status = 0;
        }
        try {
            thread.join();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }
}
