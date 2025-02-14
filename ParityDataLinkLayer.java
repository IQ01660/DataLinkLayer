// =============================================================================
// IMPORTS

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
// =============================================================================


// =============================================================================
/**
 * @file   ParityDataLinkLayer.java
 * @author Scott F. Kaplan (sfkaplan@cs.amherst.edu)
 * @date   August 2018, original September 2004
 *
 * A data link layer that uses start/stop tags and byte packing to frame the
 * data, and that performs no error management.
 */
public class ParityDataLinkLayer extends DataLinkLayer {
// =============================================================================



    // =========================================================================
    /**
     * Embed a raw sequence of bytes into a framed sequence.
     *
     * @param  data The raw sequence of bytes to be framed.
     * @return A complete frame.
     */
    protected byte[] createFrame (byte[] data) {

        if (_debug)
            System.out.println("====================\nDEBUG: createFrame()\n====================");

        Queue<Byte> framingData = new LinkedList<Byte>();
	

        // Add each byte of original data.
        for (int i = 0; i < data.length; ) {
            // ========================
            // parity count
            int parityCount = 0; // number of 1's in all data bytes
            // ========================

            // Begin with the start tag.
            framingData.add(startTag);

            if (_debug)
                System.out.print("<start>");

            for (int dummy = 0; dummy < BITS_PER_BYTE; dummy++) {
                // break if i is beyond the data
                if (i >= data.length)
                    break;

                // If the current data byte is itself a metadata tag, then precede
                // it with an escape tag.
                byte currentByte = data[i];

                if ((currentByte == startTag) ||
                    (currentByte == stopTag) ||
                    (currentByte == escapeTag)) {

                    framingData.add(escapeTag);
                    
                    if (_debug)
                        System.out.print("<esc>");
                }
                
                // =======================
                // parity count update
                parityCount += bitCount(currentByte);
                // =======================

                // Add the data byte itself.
                framingData.add(currentByte);
                
                if (_debug)
                    System.out.print((char) currentByte);

                // we just added an actual (non meta) byte
                i++;
        
            }
            // =======================
            // parity byte goes here
            framingData.add(parityCount % 2 == 0 ? evenByte : oddByte);

            if (_debug)
                System.out.print("<parity>");
            // =======================
            
            // End with a stop tag.
            framingData.add(stopTag);
            
            if (_debug)
                System.out.println("<stop>");
        }

        // Convert to the desired byte array.
        byte[] framedData = new byte[framingData.size()];
        Iterator<Byte>  i = framingData.iterator();
        int             j = 0;

        while (i.hasNext()) {
            framedData[j++] = i.next();
        }

        return framedData;
        
    } // createFrame ()
    // =========================================================================


    
    // =========================================================================
    /**
     * Determine whether the received, buffered data constitutes a complete
     * frame.  If so, then remove the framing metadata and return the original
     * data.  Note that any data preceding an escaped start tag is assumed to be
     * part of a damaged frame, and is thus discarded.
     *
     * @return If the buffer contains a complete frame, the extracted, original
     * data; <code>null</code> otherwise.
     */
    protected byte[] processFrame () {

        // Search for a start tag.  Discard anything prior to it.
        boolean        startTagFound = false;
        Iterator<Byte>             i = byteBuffer.iterator();
        while (!startTagFound && i.hasNext()) {
            byte current = i.next();

            if (current != startTag) {
                i.remove();
            } else {
                startTagFound = true;
            }
        }

        // If there is no start tag, then there is no frame.
        if (!startTagFound) {
            return null;
        }
        
        // Try to extract data while waiting for an unescaped stop tag.
        Queue<Byte> extractedBytes = new LinkedList<Byte>();
        boolean       stopTagFound = false;
        while (!stopTagFound && i.hasNext()) {

            // Grab the next byte.  If it is...
            //   (a) An escape tag: Skip over it and grab what follows as
            //                      literal data.
            //   (b) A stop tag:    Remove all processed bytes from the buffer and
            //                      end extraction.
            //   (c) A start tag:   All that precedes is damaged, so remove it
            //                      from the buffer and restart extraction.
            //   (d) Otherwise:     Take it as literal data.
            byte current = i.next();
            if (current == escapeTag) {
                if (i.hasNext()) {
                    current = i.next();
                    extractedBytes.add(current);
                } else {
                    // An escape was the last byte available, so this is not a
                    // complete frame.
                    return null;
                }
            } else if (current == stopTag) {
                cleanBufferUpTo(i);
                stopTagFound = true;
            } else if (current == startTag) {
                cleanBufferUpTo(i);
                extractedBytes = new LinkedList<Byte>();
            } else {
                extractedBytes.add(current);
            }

        }

        // If there is no stop tag, then the frame is incomplete.
        if (!stopTagFound) {
            return null;
        }

        // ==========================
        // check for the parity
        if (!checkParity(extractedBytes)) {
            return null;
        }
        // ==========================

        // Convert to the desired byte array.
        if (debug) {
            System.out.println("DumbDataLinkLayer.processFrame(): Got whole frame!");
        }
        byte[] extractedData = new byte[extractedBytes.size()];
        int                j = 0;
        i = extractedBytes.iterator();
        while (i.hasNext()) {
            extractedData[j] = i.next();
            if (debug) {
            System.out.printf("DumbDataLinkLayer.processFrame():\tbyte[%d] = %c\n",
                      j,
                      extractedData[j]);
            }
            j += 1;
        }

        return extractedData;

    } // processFrame ()
    // ===============================================================



    // ===============================================================
    private void cleanBufferUpTo (Iterator<Byte> end) {

	Iterator<Byte> i = byteBuffer.iterator();
	while (i.hasNext() && i != end) {
	    i.next();
	    i.remove();
	}

    }
    // ===============================================================

        
    // ===============================================================
    // Parity Methods
    // ===============================================================
    private static int bitCount(byte dataByte) {
        int toReturn = 0;

        for (int i = 0; i < BITS_PER_BYTE; i++) {
            toReturn += dataByte & 1;
            dataByte = (byte) (dataByte >> 1);
        }

        return toReturn;
    }

    private static boolean checkParity(Queue<Byte> extractedBytes) {
        Iterator<Byte> i = extractedBytes.iterator();

        byte extractedParity = 0;
        int parityCount = 0;

        while (i.hasNext()) {
            byte current = i.next();
            // if this is parity
            if (!i.hasNext()) {
                extractedParity = current;
                i.remove();
                break;
            }

            parityCount += bitCount(current);
        }

        boolean toReturn = false;

        if (parityCount % 2 == 0 && extractedParity == evenByte ||
            parityCount % 2 == 1 && extractedParity == oddByte) 
                toReturn = true;

        return toReturn;
    }
    // ===============================================================
    // DATA MEMBERS
    // ===============================================================



    // ===============================================================
    // The start tag, stop tag, and the escape tag.
    private final byte startTag  = (byte)'{';
    private final byte stopTag   = (byte)'}';
    private final byte escapeTag = (byte)'\\';
    // ===============================================================
    
    // ===============================================================
    // Parity bits
    private final static byte evenByte = (byte)0x6d;
    private final static byte  oddByte = (byte)0xd4;
    // ===============================================================
    
    private final boolean _debug = true;
// ===================================================================
} // class DumbDataLinkLayer
// ===================================================================
