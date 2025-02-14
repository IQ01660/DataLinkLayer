// =============================================================================
// IMPORTS
import java.util.*; // my import
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
// =============================================================================



// =============================================================================
/**
 * The entry point of the simulator.  Based on command-line arguments, it
 * creates the layers to connect two simulated hosts, and then transmits data
 * (read from a file) from one host to the other.
 *
 * @file   Simulator.java
 * @author Scott F. Kaplan (sfkaplan@cs.amherst.edu)
 * @date   August 2017, original September 2004
 */
public class Simulator {
// =============================================================================


    // =========================================================================
    /**
     * The entry point.  Interpret the command-line arguments, aborting if they
     * are invalid.  Set up the layers and start the simulation.
     *
     * @param args The command-line arguments.
     */
    public static void main (String[] args) {
    /**
     * My Unit tests
     */
    /*
    Queue<Byte> codeword = new LinkedList<Byte>();
    codeword.add((byte) 0b10110011);
    codeword.add((byte) 0b11000111);
    codeword.add((byte) 0);

    System.out.println("checksum: " + CRCDataLinkLayer.getRemainder(codeword));

    System.exit(0); // to stop after tests
    */ 
    // ========= ========= =============

	// Check the number of arguments passed.
	if (args.length != 3) {

	    System.err.println("Usage: java Simulator "  +
			       "<medium type> "          +
			       "<data link layer type> " +
			       "<transmission data file>");
	    System.exit(1);

	}

	// Assign names to the arguments.
	String mediumType        = args[0];
	String dataLinkLayerType = args[1];
	String transmissionPath  = args[2];

	// Create the medium, then the sender and receiver.
	Medium medium   = Medium.create(mediumType);
	Host   sender   = new Host(medium, dataLinkLayerType);
	Host   receiver = new Host(medium, dataLinkLayerType);

	// Read the contents of the data to be transmitted into a buffer.
	byte[] dataToTransmit = readFile(transmissionPath);

	// Perform the simulation!
	simulate(sender, receiver, dataToTransmit);

    } // main
    // =========================================================================



    // =========================================================================
    /**
     * Read the whole contents of a given file, returning it in a byte array.
     *
     * @param path The pathname of the file whose data to read.
     * @return a buffer contain the complete contents of the file.
     */
    private static byte[] readFile (String path) {

	// Does the path name a readable file?
	File file = new File(path);
	if (!file.canRead()) {
	    throw new RuntimeException(path + " is not a readable file");
	}

	// Read the entire file.
	if (file.length() > Integer.MAX_VALUE) {
	    throw new RuntimeException(path + " is too large a file");
	}
	int             length = (int)file.length();
	byte[]          buffer = new byte[length];
	try {
	    FileInputStream input  = new FileInputStream(file);
	    input.read(buffer);
	} catch (FileNotFoundException e) {
	    throw new RuntimeException("Unexpected file-not-found for " + path);
	} catch (IOException e) {
	    throw new RuntimeException("Unexpected failure in reading " + path);
	}

	return buffer;
	
    } // readFile()
    // =========================================================================



    // =========================================================================
    /**
     * Perform the simulation, having the sender transmit the given data to the
     * receiver.  Verify that the receiver fully receives the complete and
     * correct data.
     *
     * @param sender   The sending host.
     * @param receiver The receiving host.
     * @param data     The data to be sent.
     */
    private static void simulate (Host sender, Host receiver, byte[] data) {

	sender.send(data);
	byte[] received = receiver.retrieve();
	System.out.println("Transmission received:  " + new String(received));
	System.out.println("Transmission succeeded: " +
			   Arrays.equals(data, received));

    } // simulate()
    // =========================================================================



// =============================================================================
} // class Simulator
// =============================================================================
