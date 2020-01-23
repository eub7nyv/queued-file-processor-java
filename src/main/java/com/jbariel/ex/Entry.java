/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.jbariel.ex;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jbariel.ex.processor.FileProcessor;
import com.jbariel.ex.processor.JsonProcessor;
import com.jbariel.ex.processor.TxtProcessor;
import com.jbariel.ex.processor.XmlProcessor;
import com.jbariel.ex.queue.QueueManager;

public class Entry {

	/**
	 * Holding place for different exit statuses
	 * 
	 * @see Entry#exit(ExitStatus)
	 * @see EntryManager#printHelpAndExit()
	 */
	public enum ExitStatus {
		NORMAL, HELP_SHOWN, UNKNOWN, FILE_NOT_FOUND, UNKNONW_DATA_TYPE, NO_DATA_TYPE_PROCESSOR_FOUND,
		FILE_NOT_CLOSED_CORRECTLY, FILE_READ_FAILURE,
	}

	/**
	 * All the supported types. Each will need it's own {@link FileProcessor}
	 * 
	 * @see EntryManager#printHelpAndExit()
	 */
	public enum DataType {
		XML, JSON, TXT,
	}

	/**
	 * Logger used for the {@link Entry} class
	 */
	private static final Logger log = LoggerFactory.getLogger(Entry.class);

	/**
	 * Singleon {@link EntryManager}
	 */
	private static EntryManager mgr = EntryManager.instance();

	/**
	 * Main entry point.
	 * 
	 * @param args
	 * @see EntryManager#printHelpAndExit()
	 */
	public static void main(final String[] args) {
		mgr.readArgs(args);

		// ACTUALLY DO STUFF
		FileProcessor processor = null;
		switch (mgr.getDataType()) {
		case XML:
			processor = new XmlProcessor();
			break;
		case JSON:
			processor = new JsonProcessor();
			break;
		case TXT:
			processor = new TxtProcessor();
			break;
		default:
			log.error("Unable to process type '" + mgr.getDataType() + "' as there is no processor setup for it!");
			exit(ExitStatus.NO_DATA_TYPE_PROCESSOR_FOUND);
			break;
		}

		QueueManager queueManager = new QueueManager().withCorePoolSize(mgr.getNumberOfThreads());

		processor.setQueueManager(queueManager);

		processor.process(mgr.getFile());

		while (queueManager.hasQueuedItems()) {
			queueManager.processQueue();
			try {
				Thread.sleep(1000, 0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		log.info("Completed queue.  There were '" + queueManager.getFailedItems().size() + "' failed items");

		exit(ExitStatus.NORMAL);
	}

	/**
	 * system exit controller
	 * 
	 * @param status
	 */
	public static void exit(final ExitStatus status) {
		mgr.closeFileIfOpened();
		int statusInt = (status == null) ? ExitStatus.UNKNOWN.ordinal() : status.ordinal();
		log.info("Exiting with status: '" + status.name() + " (" + statusInt + ")'");
		System.exit(statusInt);
	}

	public static BufferedReader getFileToParse() {
		return mgr.getFile();
	}

	static class EntryManager {

		private static final EntryManager entryManager = new EntryManager();

		protected static EntryManager instance() {
			return entryManager;
		}

		private EntryManager() {
			// hide the default constructor
			super();
		}

		private String filename;

		private BufferedReader file;

		private DataType datatype;

		private int threadCount;

		private String getFilename() {
			return this.filename;
		}

		private void setFilename(final String filename) {
			this.filename = filename;
			tryToLoadFile();
		}

		protected BufferedReader getFile() {
			return this.file;
		}

		protected void tryToLoadFile() {
			try {
				file = new BufferedReader(new FileReader(getFilename()));
			} catch (FileNotFoundException e) {
				log.error("Could not find file with filename: '" + getFilename()
						+ "'!  Check your input and try again.\n");
				printHelpAndExit(ExitStatus.FILE_NOT_FOUND);
			}
			log.info("Successfully loaded file with filename: '" + getFilename() + "'");
		}

		protected DataType getDataType() {
			return this.datatype;
		}

		private void setDataType(final String dataTypeString) {
			switch (StringUtils.trimToEmpty(dataTypeString).toLowerCase()) {
			case "xml":
				setDataType(DataType.XML);
				break;
			case "json":
				setDataType(DataType.JSON);
				break;
			case "txt":
			case "text":
				setDataType(DataType.TXT);
				break;
			default:
				log.error("Unknown data type: '" + dataTypeString + "' (read as '"
						+ StringUtils.trimToEmpty(dataTypeString).toLowerCase()
						+ "') - Please check your input and try again!");
				Entry.exit(ExitStatus.UNKNONW_DATA_TYPE);
				break;

			}
		}

		protected void setDataType(final DataType dataType) {
			this.datatype = dataType;
			log.info("Attmpting to process data of type: '" + getDataType() + "'");
		}

		private void tryToParseDataTypeFromFile() {
			String[] fileParts = getFilename().split("\\.");
			if (0 == fileParts.length) {
				log.error("Cannot determine data type from filename '" + getFilename() + "'");
				Entry.exit(ExitStatus.UNKNONW_DATA_TYPE);
			}
			setDataType(fileParts[fileParts.length - 1]);
		}

		public int getNumberOfThreads() {
			return this.threadCount;
		}

		private void setNumberOfThreads(int threads) {
			this.threadCount = threads;
		}

		private void readArgs(final String[] args) {
			if (args.length == 0) {
				printHelpAndExit();
			}
			int index = 0;
			String tmpFlag, tmpVal;
			do {
				tmpFlag = StringUtils.trimToEmpty(args[index++]);
				tmpVal = (index < (args.length)) ? StringUtils.trimToEmpty(args[index++]) : StringUtils.EMPTY;

				switch (tmpFlag) {
				case "-f":
					setFilename(tmpVal);
					break;
				case "-t":
					setDataType(tmpVal);
					break;
				case "--threads":
					setNumberOfThreads(NumberUtils.toInt(tmpVal, QueueManager.defaultCorePoolSize));
					break;
				case "-?":
				case "-H":
				case "-h":
					printHelpAndExit();
					break;
				default:
					log.error("Unknown flag: '" + tmpFlag + "'!!!");
					printHelpAndExit();
				}

			} while (index < args.length);

			if (null != getFile() && null == getDataType()) {
				tryToParseDataTypeFromFile();
			}

		}

		protected void closeFileIfOpened() {
			if (null != file) {
				try {
					file.close();
					log.info("Successfully closed file: '" + getFilename() + "'");
				} catch (IOException e) {
					log.error("Failed to close file: '" + getFilename() + "'!!!");
					e.printStackTrace();
					file = null;
					Entry.exit(ExitStatus.FILE_NOT_CLOSED_CORRECTLY);
				}
			}
		}

		private void printHelpAndExit() {
			printHelpAndExit(ExitStatus.HELP_SHOWN);
		}

		private void printHelpAndExit(final ExitStatus status) {
			System.out.println(
					"\n===================================== HOW TO USE THIS JAR ====================================="
							+ "\n\tThis jar can be called with switched arguments as follows:"
							+ "\n\n\t\t -f <filename>   => sets the file to process"
							+ "\n\t\t\t THIS FILE MUST BE RELATIVE TO THE CWD - NOT THE JAR FILE" + "\n\n\t\t -t ["
							+ String.join("|",
									Arrays.asList(DataType.values()).stream().map(s -> s.name().toLowerCase())
											.collect(Collectors.toList()))
							+ "]   => format of data to parse"
							+ "\n\t\t\t If no type is provided, the extension of the file will be used."
							+ "\n\t\t\t If no type can be determined from the extension, jar will exit."
							+ "\n\n\t\t --threads [int]    => sets the number of threads"
							+ "\n\t\t\t This defaults to '" + QueueManager.defaultCorePoolSize
							+ "' and cannot be higher than '" + QueueManager.maximumCorePoolSize + "'"
							+ "\n\n\t\t -h | -H | -?    => prints this help and exits"
							+ "\n\n\t=================== EXIT STATUS ==================="
							+ "\n\tThis jar will exit with the following statuses, depending on the case:"
							+ Arrays.asList(ExitStatus.values()).stream()
									.map(s -> "\n\t\t" + s.ordinal() + " => " + s.name())
									.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
									.toString()
							+ "\n\n===============================================================================================\n\n");

			Entry.exit(status);
		}

	}

}
