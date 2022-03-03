// =============================================================================
// IMPORTS

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
// =============================================================================


// =============================================================================
/**
 * @file   CRCDataLinkLayer.java
 * @author Scott F. Kaplan (sfkaplan@cs.amherst.edu)
 * @date   August 2018, original September 2004
 *
 * A data link layer that uses start/stop tags and byte packing to frame the
 * data, and that performs no error management.
 */
public class CRCDataLinkLayer extends DataLinkLayer {
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

            // Begin with the start tag.
            framingData.add(startTag);

            if (_debug)
                System.out.print("<start>");
            
            // Queue for CRC computation
            Queue<Byte> codewordBytes = new LinkedList<>();

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
                
                // Add the data byte itself.
                framingData.add(currentByte);
                
                // crc codeword queue update
                codewordBytes.add(currentByte);

                if (_debug)
                    System.out.print((char) currentByte);

                // we just added an actual (non meta) byte
                i++;
        
            }
            // =======================
            // my code goes here...
            codewordBytes.add((byte) 0);

            byte checksum = getRemainder(codewordBytes);
            framingData.add(checksum);

            if (_debug)
                System.out.print("<crc>");
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
        // my code goes here...
        if (getRemainder(extractedBytes) != 0) {
            // print the frame separately here...
            printWithoutChecksum(extractedBytes);            

            return null;
        }
        // ==========================
        
        // Convert to the desired byte array.
        if (debug) {
            System.out.println("DumbDataLinkLayer.processFrame(): Got whole frame!");
        }
        byte[] extractedData = new byte[extractedBytes.size() - 1]; // I added (-1) to leave checksum out

        int                j = 0;
        i = extractedBytes.iterator();
        while (i.hasNext()) {
            byte curByte = i.next();

            // ignore the checksum
            if (!i.hasNext()) break;

            extractedData[j] = curByte;

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
    // CRC METHODS
    // ===============================================================
    
    private static void printWithoutChecksum(Queue<Byte> codeword) {
        Iterator<Byte> iter = codeword.iterator();
        System.out.println("=========CF=========");
        while (iter.hasNext()) {
            byte currentByte = iter.next();

            if (!iter.hasNext()) {
                
                System.out.println("\n====================");
                break;
            }

            System.out.print((char) currentByte);
        }

    }


    /**
     * Does long division with the potential checksum as the final Byte
     * by using the "generator" and "generatorLength" variable 
     * @return remainder after long division 
     */
    private static byte getRemainder(Queue<Byte> codeword) {
        // this is my working buffer for carrying out division
        int buffer = 0;

        Iterator<Byte> iter = codeword.iterator();

        while(iter.hasNext()) {
            // my current byte block
            byte currentByte = iter.next();

            // extract every bit into the working buffer
            for (int i = 0; i < BITS_PER_BYTE; i++) {
                // extract...
                byte most_sig_bit = (byte) ( (currentByte & 0x80) >> 7);
                
                // put into buffer
                buffer = (buffer << 1) | most_sig_bit;

                // update the currentByte
                currentByte = (byte) (currentByte << 1);

                // if generator fits - divide
                if (getBitAt(buffer, generatorLength - 1) == 1) {
                    buffer = buffer ^ generator;
                }
            }
        }

        // buffer should contain the remainder
        return (byte) buffer;
    } // getRemainder()

    /**
     * 
     * @param data
     * @param pos starting at 0 from the right 
     * @return
     */
    private static int getBitAt(int data, int pos) {
        return (data >> pos) & 1;
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
    // CRC DATA MEMBERS
    // ===============================================================
    private final static int generator = 0x1D5;
    private final static int generatorLength = 9;

    private final boolean _debug = true;
// ===================================================================
} // class DumbDataLinkLayer
// ===================================================================
