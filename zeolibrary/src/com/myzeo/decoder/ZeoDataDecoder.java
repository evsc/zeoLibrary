/*-----------------------------------------------------------------------------
Copyright (c) 2010, Zeo, Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright 
      notice, this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above 
      copyright notice, this list of conditions and the following 
      disclaimer in the documentation and/or other materials provided 
      with the distribution.

 * Neither the name of Zeo, Inc. nor the names of its contributors 
      may be used to endorse or promote products derived from this 
      software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL ZEO, INC. BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
THE POSSIBILITY OF SUCH DAMAGE.
------------------------------------------------------------------------------*/
package src.com.myzeo.decoder;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Driver application that reads Zeo data records and prints them in XML or
 * human-readable text.
 * <p>
 * This program decodes sleep records in a zeosleep.dat file into either a human
 * readable format or XML depending upon how it is invoked. If the data is
 * written to standard output then it is output as human readable text. If a
 * file is specified and that file's extension is .xml then the data is output
 * as XML. Any other file extension specified will cause the program to output
 * the human readable version to the specified file.
 * <p>
 * To output data to standard output invoke this command with just the
 * zeosleep.dat input file name. To output data to an XML file invoke this
 * command with two arguments; the zeosleep.dat file and the intended XML file
 * zeosleep.xml (please include .xml extension).
 */
public class ZeoDataDecoder {

	/*************************************************************************
	 * constants
	 */

	/* Lookup table used for CRC16 calculation. */
	static final int[] CRC16_TABLE = { 0x0000, 0x1021, 0x2042, 0x3063, 0x4084,
			0x50a5, 0x60c6, 0x70e7, 0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c,
			0xd1ad, 0xe1ce, 0xf1ef, 0x1231, 0x0210, 0x3273, 0x2252, 0x52b5,
			0x4294, 0x72f7, 0x62d6, 0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd,
			0xc39c, 0xf3ff, 0xe3de, 0x2462, 0x3443, 0x0420, 0x1401, 0x64e6,
			0x74c7, 0x44a4, 0x5485, 0xa56a, 0xb54b, 0x8528, 0x9509, 0xe5ee,
			0xf5cf, 0xc5ac, 0xd58d, 0x3653, 0x2672, 0x1611, 0x0630, 0x76d7,
			0x66f6, 0x5695, 0x46b4, 0xb75b, 0xa77a, 0x9719, 0x8738, 0xf7df,
			0xe7fe, 0xd79d, 0xc7bc, 0x48c4, 0x58e5, 0x6886, 0x78a7, 0x0840,
			0x1861, 0x2802, 0x3823, 0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948,
			0x9969, 0xa90a, 0xb92b, 0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71,
			0x0a50, 0x3a33, 0x2a12, 0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79,
			0x8b58, 0xbb3b, 0xab1a, 0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22,
			0x3c03, 0x0c60, 0x1c41, 0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a,
			0xbd0b, 0x8d68, 0x9d49, 0x7e97, 0x6eb6, 0x5ed5, 0x4ef4, 0x3e13,
			0x2e32, 0x1e51, 0x0e70, 0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b,
			0xaf3a, 0x9f59, 0x8f78, 0x9188, 0x81a9, 0xb1ca, 0xa1eb, 0xd10c,
			0xc12d, 0xf14e, 0xe16f, 0x1080, 0x00a1, 0x30c2, 0x20e3, 0x5004,
			0x4025, 0x7046, 0x6067, 0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d,
			0xd31c, 0xe37f, 0xf35e, 0x02b1, 0x1290, 0x22f3, 0x32d2, 0x4235,
			0x5214, 0x6277, 0x7256, 0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e,
			0xe54f, 0xd52c, 0xc50d, 0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466,
			0x6447, 0x5424, 0x4405, 0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f,
			0xf77e, 0xc71d, 0xd73c, 0x26d3, 0x36f2, 0x0691, 0x16b0, 0x6657,
			0x7676, 0x4615, 0x5634, 0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8,
			0x89e9, 0xb98a, 0xa9ab, 0x5844, 0x4865, 0x7806, 0x6827, 0x18c0,
			0x08e1, 0x3882, 0x28a3, 0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e, 0x8bf9,
			0x9bd8, 0xabbb, 0xbb9a, 0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1,
			0x1ad0, 0x2ab3, 0x3a92, 0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa,
			0xad8b, 0x9de8, 0x8dc9, 0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2,
			0x2c83, 0x1ce0, 0x0cc1, 0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b,
			0xbfba, 0x8fd9, 0x9ff8, 0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93,
			0x3eb2, 0x0ed1, 0x1ef0 };

	/*
	 * Strings with program documentation.
	 */
	static final String DOCUMENTATION = "Decode an Zeo save data file (zeosleep.dat).\n\n"
			+ "This program decodes sleep records in a zeosleep.dat file into\n"
			+ "either a human readable format or XML depending upon how it is\n"
			+ "invoked. If the data is written to standard output then it is\n"
			+ "output as human readable text. If a file is specified and that\n"
			+ "file's extension is .xml then the data is output as XML. Any other\n"
			+ "file extension specified will cause the program to output the human\n"
			+ "readable version to the specified file.\n"
			+ "\n"
			+ "To output data to standard output invoke this command with just\n"
			+ "the zeosleep.dat input file name. To output data to an XML file\n"
			+ "invoke this command with two arguments; the zeosleep.dat file and\n"
			+ "the intended XML file zeosleep.xml (please include .xml extension).\n"
			+ "\n";

	static final String OPTIONS = "Options:\n"
			+ "-e or --expand  : Don't remove related records for each night.\n"
			+ "-r or --resets  : Output just watchdog reset records.\n"
			+ "-V or --version : Display decoder version and exit";

	static final String USAGE = "Usage:\n"
			+ "java -jar OpenDecoder.jar [options] directory/zeosleep.dat\n"
			+ "or\n"
			+ "java -jar OpenDecoder.jar [options] directory/zeosleep.dat\n"
			+ "                                    directory/zeosleep.xml\n"
			+ "or\n"
			+ "java -jar OpenDecoder.jar [options] directory/zeosleep.dat\n"
			+ "                                    directory/zeosleep.txt\n";

	/**
	 * ZeoDataDecoder version.
	 * 
	 * This is a unique identifier for the version of the Zeo data decoder. The
	 * number is displayed to the user with invocation of the Zeo data decoder
	 * in the following manner: java -jar ZeoDataDecoder.jar --version
	 * 
	 * NOTE!!! This number should be incremented after any updates to any of the
	 * Java files comprising the decoder software.
	 * 
	 * The following chart displays each version number and a brief synopsis of
	 * the version's changes
	 * 
	 * Rev# Change ---- ------- 10 Updated for public release and use with the
	 * OpenZeo firmware
	 */
	public final static int ZEO_DATA_DECODER_VERSION = 10;

	/*************************************************************************
	 * variables
	 */

	/* The collection of zeo data records. */
	private Vector<ZeoData> records = new Vector<ZeoData>(10, 10);

	/*************************************************************************
	 * methods
	 */

	/**
	 * Populate the records inside the Zeo decoder based upon the data given in
	 * the input byte array. If we find no records throw an EOF exception.
	 * 
	 * @param in
	 *            A byte buffer that is the source of data for populating the
	 *            Zeo records from.
	 */
	public ZeoDataDecoder(ByteBuffer in) throws EOFException {
		/* A blank byte array. */
		byte[] blank = new byte[ZeoData.IDENTIFIER_SIZE];
		byte[] identifier = new byte[ZeoData.IDENTIFIER_SIZE];
		int record_count;
		int record_offset;
		int record_size;
		int version;
		byte[] version_str = new byte[ZeoData.VERSION_SIZE];

		/*
		 * This code assumes the array used by "in" buffer can be indexed using
		 * same index values as the buffer itself.
		 */
		assert (in.arrayOffset() == 0);

		/* Set the byte order to little endian. */
		in.order(ByteOrder.LITTLE_ENDIAN);
		in.rewind();

		/*
		 * First check if the file is encrypted. We do this by looking for a
		 * valid record identifier. If we don't find one then we assume it's
		 * because the file is encrypted.
		 */
		if (find_next_record(in) == -1) {
			System.err
					.println("ERROR: File may be encrypted."
							+ "Please update to firmware version 2.6.3O to disable SD card encryption.");
		}
		in.rewind();

		/* Process all the data in the file */
		record_count = 0;
		while (in.hasRemaining()) {
			/* Remember where this record starts */
			in.mark();
			record_offset = in.position();

			record_size = 0;
			version = 0;

			/* Stop parsing if there isn't enough data left */
			if (in.remaining() < ZeoData.HEADER_SIZE) {
				System.err
						.println("WARNING: File ended with incomplete record.");
				break;
			}

			/*
			 * Read in the header data for this record. If the header's
			 * identifier value is valid, decode the version value.
			 */
			in.get(identifier);
			in.get(version_str);
			if (!Arrays.equals(identifier, ZeoData.IDENTIFIER)) {
				System.err.println("WARNING: Invalid record identifier "
						+ "skipped after record " + record_count + ".");
			} else {
				/* Check the version of the record. */
				if (Arrays.equals(version_str, ZeoData.V22)) {
					record_size = ZeoData.V22_SIZE;
					version = 22;
				} else {
					/*
					 * We don't have code to handle this record version so
					 * report an error.
					 */
					System.err
							.println("WARNING: Unable to handle Zeo record version "
									+ (version_str[1] * 256 + version_str[0])
									+ " after record " + record_count + ".");
				}
			}

			/* Stop parsing if there isn't enough data left */
			if (in.remaining() < record_size - ZeoData.HEADER_SIZE) {
				System.err
						.println("WARNING: File ended with incomplete record.");
				break;
			}

			/* If we recognized the version then decode the record */
			if (version != 0) {
				try {
					/*
					 * Set up a byte buffer that provides access to just the
					 * record's contents minus the header. Note, this buffer
					 * shares access to the same data array as "in" so that the
					 * parsing code can make changes to the data if required
					 * (e.g. to clear a CRC value).
					 */
					ByteBuffer bstream = ByteBuffer.wrap(in.array(),
							record_offset + ZeoData.HEADER_SIZE, record_size
									- ZeoData.HEADER_SIZE);

					/* Decode the record and store it */
					ZeoData record = new ZeoData(bstream, version);

					/* Verify checksum for those records that have one */
					if (version >= 20) {
						int crc = crc16(in.array(), record_offset, record_size);

						if (crc != record.get_crc()) {
							System.err
									.println("WARNING: Skipping the record after record "
											+ record_count + " due to bad CRC.");
							version = 0;
						}
					}

					/* Store records that we successfully parsed. */
					if (version != 0) {
						records.addElement(record);

						/* Advance to start of next record */
						in.position(record_offset + record_size);
					}
				} catch (Throwable e) {
					/*
					 * We had some type of error during parsing so report the
					 * problem and then set record version to 0 so this record
					 * will be discarded.
					 */
					System.err
							.println("WARNING: Exception parsing the record after record "
									+ record_count + ": " + e.toString());
					version = 0;
				}
			}

			/*
			 * Try to find new record to parse when there are parsing problems.
			 */
			if (version == 0) {
				/*
				 * We were unable to process a record for some reason so try to
				 * find a new record to process starting one byte past the
				 * previous starting record position.
				 */
				in.reset();
				in.get();

				if (find_next_record(in) == -1) {
					System.err
							.println("WARNING: Valid records stopped before file ended.");
					break;
				}
			} else {
				/* Good record found so advance record number */
				record_count++;
			}

			/* Reset the identifier and version to get ready for next pass. */
			System.arraycopy(blank, 0, identifier, 0, ZeoData.IDENTIFIER_SIZE);
			System.arraycopy(blank, 0, version_str, 0, ZeoData.VERSION_SIZE);
		}
	}

	/**
	 * Compute the CRC16 value for the input byte buffer. The CRC starts with an
	 * initial CRC value of 0.
	 * 
	 * @param buffer
	 *            Byte array with data to calculate CRC for.
	 * @param offset
	 *            Starting offset for chunk of data to calculate CRC for.
	 * @param length
	 *            Number of bytes in buffer to use in CRC calculation.
	 * 
	 * @return CRC value.
	 */
	public static int crc16(byte[] buffer, int offset, int length) {
		int crc = 0; // the CRC
		int t;
		int value;

		for (int i = 0; i < length; ++i, ++offset) {
			value = buffer[offset] & 0xff;
			t = (crc >> 8) ^ value;
			crc = ((crc << 8) & 0xffff) ^ CRC16_TABLE[t];
		}
		return crc;
	}

	/*
	 * Given the contents of a ZEOSLEEP.DAT file, decode the file. Remove the
	 * duplicate records and label the naps. Return the records as a list of
	 * ZeoData objects.
	 * 
	 * @param data ZEOSLEEP.DAT file contents to decode
	 * 
	 * @return list of records
	 */
	public static List<ZeoData> decode(byte[] data) throws IOException {
		ByteBuffer in = ByteBuffer.wrap(data);

		ZeoDataDecoder decoder = new ZeoDataDecoder(in);

		decoder.reduce_records();

		decoder.label_naps();

		return decoder.get_records();
	}

	/**
	 * Given a byte buffer, attempt to find the start of the next record given
	 * the current position in the buffer. This function searches for the
	 * identifier string ZeoData.IDENTIFIER that marks the start of all records.
	 * It halts the search after ZeoData.MAX_RECORD_BYTES since no valid record
	 * would start further along than that. This function leaves the buffer
	 * positioned at the start of the record it finds.
	 * 
	 * @param in
	 *            A ByteBuffer that will be used to search for the next record.
	 * 
	 * @return position of the start of the next Zeo record or -1 if no new
	 *         record found.
	 */
	private int find_next_record(ByteBuffer in) {
		int bytes_to_search;
		byte[] identifier = new byte[ZeoData.IDENTIFIER_SIZE];

		/*
		 * Determine how many bytes to search to find an identifier. We search
		 * at most ZeoData.MAX_RECORD_BYTES or until we get so close to the end
		 * of the buffer that it couldn't store an identifier value.
		 */
		bytes_to_search = in.remaining() - identifier.length;
		if (bytes_to_search > ZeoData.MAX_RECORD_BYTES) {
			bytes_to_search = ZeoData.MAX_RECORD_BYTES;
		}

		while (bytes_to_search > 0) {
			in.mark();
			in.get(identifier);
			if (Arrays.equals(identifier, ZeoData.IDENTIFIER)) {
				/*
				 * Reset the buffer back to the start of the identifier and
				 * return its position in the buffer.
				 */
				in.reset();
				return in.position();
			}
			/* Reset the buffer to the previous mark. */
			in.reset();
			/* Advance the buffer by a single byte and try again. */
			in.get();
			--bytes_to_search;
		}
		/* Didn't find any record so indicate error */
		return -1;
	}

	/**
	 * Return the decoded records as a list of ZeoData objects.
	 * 
	 * @return the decoded records as a list of ZeoData objects
	 */
	public List<ZeoData> get_records() {
		return Collections.unmodifiableList(records);
	}

	/**
	 * Reduce the decoder's records down to just the first reset record
	 * following any/each watchdog reset.
	 */
	@SuppressWarnings("unused")
	private void keep_resets() {
		Iterator<ZeoData> iterator = records.iterator();

		/* Process all the records. */
		while (iterator.hasNext()) {
			ZeoData record = iterator.next();

			/* If it isn't a reset record, get rid of it */
			if (!record.is_reset_record()) {
				iterator.remove();
			}
		}
	}

	/**
	 * Iterate over the collection of records labeling each record as either a
	 * nap or not a nap. The largest record for any day (as determined by the
	 * compareLength method) is not a nap. All other records for the day are
	 * naps. All the records get sleep_date set through a call to the
	 * set_sleep_date method.
	 */
	public void label_naps() {
		Iterator<ZeoData> iterator = records.iterator();
		ZeoData largest_record = null;

		/*
		 * Initialize the largest record to the first record and set the
		 * sleep_date for the record.
		 */
		if (iterator.hasNext()) {
			largest_record = iterator.next();
			largest_record.set_sleep_date();
		}

		/* Process the rest of the records */
		while (iterator.hasNext()) {
			ZeoData record = (ZeoData) iterator.next();

			/* Fill in the sleep_date for the record */
			record.set_sleep_date();

			/*
			 * Determine if record is a nap or not. The last longest sleep
			 * episode for a given day is not a nap.
			 */
			if (largest_record.same_night(record)) {
				/*
				 * These records fall within the same 6am to 6am day so we want
				 * to see if the length of sleep is the same or longer than the
				 * current largest record. If it is then it's the new largest.
				 * This way we keep the last largest record for a day.
				 */
				if (largest_record.compareLength(record) == 1) {
					/*
					 * The largest_record has a longer length than the new
					 * record so we do not have a new largest record. This means
					 * the new record is considered to be a nap and we set its
					 * is_nap field to true.
					 */
					record.is_nap = true;
				} else {
					/*
					 * There is a new largest or equal record. The previous
					 * largest record is therefor a nap.
					 */
					largest_record.is_nap = true;
					largest_record = record;
				}
			} else {
				/*
				 * The record being processed represents a new night of sleep.
				 * Therefore, the largest record represents the real night of
				 * sleep for its day so we set the largest_record's is_nap value
				 * to false. Establish the current record as the initial
				 * largest_record for this new day.
				 */
				largest_record.is_nap = false;
				largest_record = record;
			}
		}

		/*
		 * If we haven't handled the largest record from the last day of sleep,
		 * handle it now (it's not a nap).
		 */
		if (largest_record != null) {
			largest_record.is_nap = false;
		}
	}

	/**
	 * Reduce the decoder's records down to core records by removing duplicate
	 * records for each sleep episode. If within a collection of records there
	 * are multiple records that correspond to the same sleep episode (ie. they
	 * have the same start_of_night value) then we take the last record with the
	 * longest "sleep length" that was written out with a write reason of
	 * FS_REASON_SLEEP_RATED. If no such record exists we take the first record
	 * that has the longest "sleep length". The sleep length is as determined by
	 * the ZeoData compareLength method. Records reflecting incomplete nights
	 * (i.e. records without both a start and end time) are discarded.
	 * 
	 * Because all the records for a sleep episode might not be sequential, we
	 * need to sort the sleep records to order them based on start_of_night
	 * values. We do an initial reduction pass before sorting the records to
	 * decrease the sorting overhead.
	 */
	public void reduce_records() {
		ZeoData largest_record = null; /* largest record for a night */

		Vector<ZeoData> reduced_records = new Vector<ZeoData>(10, 10);

		/*
		 * Process all the records. Find each set of valid records with matching
		 * start_of_night values and keep only the "largest" one. If a record of
		 * the same size as the current largest record and a write reason of
		 * FS_REASON_SLEEP_RATED is encountered, it replaces the current largest
		 * record. In this context, "valid" means there is both a start and an
		 * end of night value and largest means the first one with the latest
		 * record end time and longest hypnogram length. During this process we
		 * transfer the records we are keeping to the reduced_records vector.
		 */
		Iterator<ZeoData> iterator = records.iterator();
		while (iterator.hasNext()) {
			ZeoData record = iterator.next();

			if (record.get_start_of_night() == null
					|| record.get_end_of_night() == null) {
				/* Incomplete record so ignore it so it is discarded */
			} else if (largest_record == null) {
				/* First good record */
				largest_record = record;
			} else if (!largest_record.same_start_time(record)) {
				/*
				 * We have a record with a different starting time so we store
				 * the largest record found for the previous start_of_night
				 * value and use the current record as the starting point for
				 * finding the largest record for the new sleep episode.
				 */
				reduced_records.addElement(largest_record);
				largest_record = record;
			} else if (largest_record.compareLength(record) == -1) {
				/*
				 * We have a night that is larger than the current largest night
				 * so we update our largest record (discarding the old largest
				 * record). This, implementation keeps the first instance of the
				 * largest record.
				 */
				largest_record = record;
			} else if (largest_record.compareLength(record) == 0
					&& record.is_sleep_rating_record()) {
				/*
				 * We have a record of the same length as the current largest
				 * night and a write reason of FS_REASON_SLEEP_RATED. Replace
				 * the current largest record with it. We assume that any
				 * records for a given night that were written out as a result
				 * of a sleep rating occur in chronological order in the file,
				 * so we will retain only the most recent one.
				 */
				largest_record = record;
			}
		}
		/* Save the last sleep episode if there is a record that isn't saved */
		if (largest_record != null) {
			reduced_records.addElement(largest_record);
			largest_record = null;
		}

		/*
		 * Sort the reduced list of records so that nights are sequential and
		 * all records for a given night are contiguous. The "sort" function
		 * uses the compareTo method implemented in the ZeoData class to
		 * determine how to order records. That method bases the decision solely
		 * on the start_night values. Records with the same start_night values
		 * equivalent and are output in the same order they originally occur due
		 * to the fact that this sort is "stable".
		 */
		Collections.sort(reduced_records);

		/*
		 * Get rid of the original set of records so we can fill the vector back
		 * in with the reduced records.
		 */
		records.clear();

		/*
		 * Now that the records are sorted and all records from the same night
		 * are contiguous, scan the records again to handle duplicates. This
		 * uses almost the same algorithm as above except that we don't have to
		 * worry about incomplete records. As such, we perform a scan on the
		 * sorted records keeping only the largest record from each sleep
		 * episode. We keep the most recent largest record with a write reason
		 * of FS_REASON_SLEEP_RATED, or the first largest record in the absence
		 * of any records with a write reason of FS_REASON_SLEEP_RATED. During
		 * this process we transfer the records we are keeping back to the
		 * records vector.
		 */
		Iterator<ZeoData> iterator2 = reduced_records.iterator();
		while (iterator2.hasNext()) {
			ZeoData record = iterator2.next();

			if (largest_record == null) {
				/* First good record */
				largest_record = record;
			} else if (!largest_record.same_start_time(record)) {
				/*
				 * We have a record with a different starting time so we store
				 * the largest record found for the previous start_of_night
				 * value and use the current record as the starting point for
				 * finding the largest record for the new sleep episode.
				 */
				records.addElement(largest_record);
				largest_record = record;
			} else if (largest_record.compareLength(record) == -1) {
				/*
				 * We have a night that is larger than the current largest night
				 * so we update our largest record (discarding the old largest
				 * record). This, implementation keeps the first instance of the
				 * largest record.
				 */
				largest_record = record;
			} else if (largest_record.compareLength(record) == 0
					&& record.is_sleep_rating_record()) {
				/*
				 * We have a record of the same length as the current largest
				 * night and a write reason of FS_REASON_SLEEP_RATED. Replace
				 * the current largest record with it. We assume that any
				 * records for a given night that were written out as a result
				 * of a sleep rating occur in chronological order in the file,
				 * so we will retain only the most recent one.
				 */
				largest_record = record;
			}
		}
		/* Save the last sleep episode if there is a record that isn't saved */
		if (largest_record != null) {
			records.addElement(largest_record);
		}
	}

	/**
	 * Return the number of records being handled by the decoder.
	 * 
	 * @return an integer that specifies how many records are held within the
	 *         decoder.
	 */
	public int size() {
		return records.size();
	}

	/**
	 * Generate a human readable string representation of this decoder.
	 * 
	 * @return a string containing a human readable version of the decoder.
	 */
	public String toHuman() {
		StringWriter txt = new StringWriter();
		PrintWriter out = new PrintWriter(txt);
		Iterator<ZeoData> iterator = records.iterator();

		while (iterator.hasNext()) {
			ZeoData record = iterator.next();
			out.println(record.toHuman());
		}
		out.flush();
		return txt.toString();
	}

	/**
	 * Generate an XML string representation of this decoder.
	 * 
	 * @return a string containing an XML representation of the decoder.
	 */
	public String toXML() {
		StringWriter txt = new StringWriter();
		PrintWriter out = new PrintWriter(txt);
		Iterator<ZeoData> iterator = records.iterator();

		out.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
		out.println("<sleep_records>");

		while (iterator.hasNext()) {
			ZeoData record = iterator.next();
			out.println(record.toXML());
		}
		out.println("</sleep_records>");
		out.flush();
		return txt.toString();
	}

	/**
	 * The main method. Processes command line arguments and invokes decoder
	 * methods on the input data file.
	 */
	public static void main(String[] args) {
		ArrayList<String> arguments = new ArrayList<String>();
		FileChannel ichannel = null;
		boolean reduce = true;
		int size;

		/* Process command line arguments. */
		for (String arg : args) {
			/*
			 * Check for the special options
			 */
			if (arg.equals("-e") || arg.equals("--expand")) {
				/*
				 * We got expand option (which is the opposite of normal record
				 * reduction).
				 */
				reduce = false;
			} else if (arg.equals("-V") || arg.equals("--version")) {
				System.out.println("ZeoDataDecoder version "
						+ ZeoDataDecoder.ZEO_DATA_DECODER_VERSION + ".\n"
						+ "Copyright 2010 by Zeo Inc."
						+ " All Rights Reserved.");
				System.exit(0);
			} else {
				if (!arguments.add(arg)) {
					System.err.println("WARNING: could not add argument " + arg
							+ "to arguments ArrayList");
				}
			}
		}

		if (arguments.size() < 1 || arguments.size() > 2) {
			System.out.println(DOCUMENTATION);
			System.out.println(USAGE);
			System.out.println(OPTIONS);
			System.err.println("Invalid program arguments. ");
			System.err.println("You must specify the location of a "
					+ "zeosleep.dat file.");
			System.exit(1);
		}

		/* Try opening the zeo sleep input file. */
		try {
			ichannel = new FileInputStream(arguments.get(0)).getChannel();
		} catch (FileNotFoundException e) {
			System.err.println("ERROR: Zeosleep file " + arguments.get(0)
					+ " not found.");
			System.exit(1);
		}

		/* Attempt to process the zeo sleep file. */
		try {
			/*
			 * Setup byte array to hold the full input file and wrap it in a
			 * byte buffer.
			 */
			byte[] data = new byte[(int) ichannel.size()];
			ByteBuffer in = ByteBuffer.wrap(data);

			/* Read the file data into the byte array. */
			ichannel.read(in);
			in.rewind();

			/* Setup the decoder (which parses the data records). */
			ZeoDataDecoder decoder = new ZeoDataDecoder(in);

			/* Figure out how many records we parsed. */
			size = decoder.size();

			/*
			 * Filter the set of records based on program options. Keeping only
			 * reset records takes precedence over reducing records to remove
			 * duplicates.
			 */
			if (size == 0) {
				/*
				 * No records were found in the file so there is nothing to do.
				 */
			} else if (reduce) {
				/*
				 * Reduce the records down to only records that comprise
				 * distinct nights.
				 */
				decoder.reduce_records();

				/* Determine how many records are left */
				size = decoder.size();

				/*
				 * If the reduction brought the collection of records down to no
				 * records then notify the user of this and suggest the --expand
				 * option.
				 */
				if (size == 0) {
					System.err.println("WARNING: recorded reduced down to "
							+ "0 records. There will be no " + "output.\n"
							+ "Please try executing the command "
							+ "with the --expand option.\n");
				}
			}

			/* Fill in is_nap and sleep_date information */
			decoder.label_naps();

			/*
			 * Based upon what arguments were given we decide what form of
			 * output the decoder should produce.
			 */
			if (arguments.size() == 1) {
				/*
				 * No output file was specified so the human readable form is
				 * written to standard output.
				 */
				System.out.println(decoder.toHuman());
			} else {
				/* User specified an output file. Open that file. */
				String filename = arguments.get(1);
				FileOutputStream ostream = null;

				try {
					ostream = new FileOutputStream(filename);
				} catch (FileNotFoundException e) {
					System.err.println("ERROR: Could not open output file "
							+ filename);
					System.exit(1);
				}
				PrintWriter out = new PrintWriter(ostream);

				/*
				 * Check the extension of the output file to determine what form
				 * to output the records as. If the user specifies .xml
				 * extension then we output as xml. Otherwise we output as text.
				 */
				String extension = (filename.lastIndexOf(".") == -1) ? ""
						: filename.substring(filename.lastIndexOf(".") + 1,
								filename.length());
				if (extension.toLowerCase().equals("xml")) {
					out.println(decoder.toXML());
				} else {
					out.println(decoder.toHuman());
				}
				out.flush();
				out.close();
			}
		} catch (IOException e) {
			System.err.println("ERROR: Received an IOException.");
		}
	}
}
